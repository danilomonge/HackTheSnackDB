package de.pecus.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.pecus.api.services.nutrition.NutritionService;
import de.pecus.api.services.nutrition.UpstreamException;
import de.pecus.api.services.nutrition.UpstreamHttp;

/** USDA lookup and MyMemory translation, with the HTTP layer faked. */
class NutritionServiceTest {

    /** Records requests and answers with a fixed status and body. */
    static class FakeHttp implements UpstreamHttp {
        final List<String> urls = new ArrayList<>();
        final List<Map<String, String>> headers = new ArrayList<>();
        final List<String> bodies = new ArrayList<>();
        int status = 200;
        String body = "{}";

        @Override
        public Result get(String url) {
            urls.add(url);
            return new Result(status, body);
        }

        @Override
        public Result postJson(String url, Map<String, String> requestHeaders, String json) {
            urls.add(url);
            headers.add(requestHeaders);
            bodies.add(json);
            return new Result(status, body);
        }
    }

    private static String food(String description, double kcal, double fat, Double sodium) {
        return "{\"description\":\"" + description + "\",\"foodNutrients\":["
                + "{\"nutrientName\":\"Energy\",\"value\":" + kcal + "},"
                + "{\"nutrientName\":\"Carbohydrate, by difference\",\"value\":20},"
                + "{\"nutrientName\":\"Total lipid (fat)\",\"value\":" + fat + "},"
                + "{\"nutrientName\":\"Protein\",\"value\":25}"
                + (sodium == null ? "" : ",{\"nutrientName\":\"Sodium, Na\",\"value\":" + sodium + "}")
                + "]}";
    }

    @Test
    void usdaUsesTheMedianOfAllExactBrandedMatches() throws Exception {
        FakeHttp http = new FakeHttp();
        http.body = "{\"foods\":[" + food("PEANUTS", 571, 39.3, 1390.0) + ","
                + food("PEANUT BUTTER", 600, 50, 400.0) + ","
                + food("Peanuts", 567, 49.2, 18.0) + ","
                + food("PEANUTS", 585, 46.4, 384.0) + "]}";
        NutritionService service = new NutritionService(http, "usda-key", "");

        Map<String, Object> data = service.lookupUsda("peanuts");

        assertEquals("PEANUTS", data.get("name"));
        assertEquals(571.0, data.get("kcal"));
        assertEquals(46.4, data.get("fat"));
        assertEquals(20.0, data.get("carbs"));
        assertEquals(25.0, data.get("protein"));
        assertEquals(384.0, data.get("sodium"));
        assertEquals("100g", data.get("per"));
        String url = http.urls.get(0);
        assertTrue(url.startsWith("https://api.nal.usda.gov/fdc/v1/foods/search?"), url);
        assertTrue(url.contains("query=peanuts") && url.contains("dataType=Branded")
                && url.contains("pageSize=25") && url.contains("api_key=usda-key"), url);
    }

    @Test
    void usdaMedianOfAnEvenNumberAveragesTheMiddleValuesAndSkipsMissingOnes() throws Exception {
        FakeHttp http = new FakeHttp();
        http.body = "{\"foods\":[" + food("SALT", 0, 0, 38000.0) + "," + food("SALT", 0, 0, 39000.0) + ","
                + food("SALT", 0, 0, null) + "]}";
        Map<String, Object> data = new NutritionService(http, "k", "").lookupUsda("Salt");
        assertEquals(38500.0, data.get("sodium"));
    }

    @Test
    void usdaReturnsNothingForPartialMatchesShortNamesOrMissingKey() throws Exception {
        FakeHttp http = new FakeHttp();
        http.body = "{\"foods\":[" + food("SUGAR SUBSTITUTE", 10, 0, 0.0) + "]}";
        assertNull(new NutritionService(http, "k", "").lookupUsda("sugar"));
        assertNull(new NutritionService(http, "k", "").lookupUsda("ab"));
        assertTrue(!new NutritionService(http, "", "").hasUsdaKey());
    }

    @Test
    void usdaQueryIsUrlEncoded() throws Exception {
        FakeHttp http = new FakeHttp();
        http.body = "{\"foods\":[]}";
        new NutritionService(http, "k", "").lookupUsda("Crème fraîche & M&M's");
        assertTrue(http.urls.get(0).contains("query=Cr%C3%A8me+fra%C3%AEche+%26+M%26M%27s"), http.urls.get(0));
    }

    @Test
    void upstreamErrorsAreReportedWithoutTheKey() {
        FakeHttp http = new FakeHttp();
        http.status = 429;
        UpstreamException e = assertThrows(UpstreamException.class,
                () -> new NutritionService(http, "secret-key", "").lookupUsda("sugar"));
        assertEquals(429, e.getStatus());
        assertTrue(!e.getMessage().contains("secret-key"));
    }

    @Test
    void blankTextIsReturnedWithoutARequest() throws Exception {
        FakeHttp http = new FakeHttp();
        assertEquals("  ", new NutritionService(http, "", "").translate("  ", "DE", "EN"));
        assertTrue(http.urls.isEmpty());
    }

    @Test
    void myMemoryTranslatesGermanWithIngredientContext() throws Exception {
        FakeHttp http = new FakeHttp();
        http.body = "{\"responseData\":{\"translatedText\":\"Ingredient: skimmed milk powder\"},"
                + "\"responseStatus\":200,\"quotaFinished\":false}";

        assertEquals("skimmed milk powder",
                new NutritionService(http, "", "").translate("Magermilchpulver", "DE", "EN"));
        assertEquals("https://api.mymemory.translated.net/get?q=Zutat%3A+Magermilchpulver&langpair=de%7Cen",
                http.urls.get(0));

        new NutritionService(http, "", "team@example.org").translate("Zucker", "DE", "EN");
        assertTrue(http.urls.get(1).endsWith("&de=team%40example.org"), http.urls.get(1));
    }

    @Test
    void myMemoryQuotaWarningIsAnUpstreamError() {
        FakeHttp http = new FakeHttp();
        http.body = "{\"responseData\":{\"translatedText\":\"MYMEMORY WARNING: YOU USED ALL AVAILABLE FREE TRANSLATIONS\"},"
                + "\"responseStatus\":429,\"quotaFinished\":true}";
        UpstreamException e = assertThrows(UpstreamException.class,
                () -> new NutritionService(http, "", "").translate("Zucker", "DE", "EN"));
        assertEquals(429, e.getStatus());
    }

    @Test
    void translationsAreCached() throws Exception {
        FakeHttp http = new FakeHttp();
        http.body = "{\"responseData\":{\"translatedText\":\"Ingredient: sugar\"},\"responseStatus\":200}";
        NutritionService service = new NutritionService(http, "", "");

        assertEquals("sugar", service.translate("Zucker", "DE", "EN"));
        assertEquals("sugar", service.translate(" Zucker ", "de", "en"));
        assertEquals(1, http.urls.size());
    }
}
