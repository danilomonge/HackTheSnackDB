package de.pecus.api.services.nutrition;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Nutrition facts (USDA FoodData Central) and German → English translation
 * (Azure AI Translator) for the web app's nutrition dialog. The API keys stay on
 * the server (env USDA_API_KEY / AZURE_TRANSLATOR_KEY); the browser only calls this API.
 *
 * Same behaviour as the app's Python implementation
 * (HackTheSnack: hts_lib/backend/nutrition_service.py).
 */
@Service
public class NutritionService {

    static final String USDA_SEARCH_URL = "https://api.nal.usda.gov/fdc/v1/foods/search";
    static final String TRANSLATOR_URL = "https://api.cognitive.microsofttranslator.com/translate";
    /** How many USDA search hits are checked for an exact name match. */
    static final int USDA_PAGE_SIZE = 25;

    private static final ObjectMapper JSON = new ObjectMapper();

    private final UpstreamHttp http;
    private final String usdaApiKey;
    private final String translatorKey;
    private final String translatorRegion;

    @Autowired
    public NutritionService(UpstreamHttp http,
                            @Value("${app.nutrition.usda-api-key:}") String usdaApiKey,
                            @Value("${app.nutrition.translator-key:}") String translatorKey,
                            @Value("${app.nutrition.translator-region:}") String translatorRegion) {
        this.http = http;
        this.usdaApiKey = usdaApiKey == null ? "" : usdaApiKey.trim();
        this.translatorKey = translatorKey == null ? "" : translatorKey.trim();
        this.translatorRegion = translatorRegion == null ? "" : translatorRegion.trim();
    }

    public boolean hasUsdaKey() {
        return !usdaApiKey.isEmpty();
    }

    /**
     * Nutrition facts per 100 g, or null if no branded USDA food has exactly this name.
     *
     * Only exact (case-insensitive) name matches are accepted, so a vague query
     * never returns data for a different food. Every value is the median over
     * all exact matches, so one unusual product does not decide the result.
     *
     * @return {name, kcal, carbs, fat, protein, sodium (mg), per}
     */
    public Map<String, Object> lookupUsda(String ingredientName) throws UpstreamException {
        String query = ingredientName == null ? "" : ingredientName.trim();
        String searched = query.toLowerCase(Locale.ROOT);
        if (!hasUsdaKey() || searched.length() < 3) {
            return null;
        }

        String url = USDA_SEARCH_URL
                + "?query=" + encode(query)
                + "&api_key=" + encode(usdaApiKey)
                + "&pageSize=" + USDA_PAGE_SIZE
                + "&dataType=Branded";
        JsonNode foods = request("USDA", () -> http.get(url)).path("foods");

        List<JsonNode> matches = new ArrayList<>();
        for (JsonNode food : foods) {
            if (food.path("description").asText("").trim().toLowerCase(Locale.ROOT).equals(searched)) {
                matches.add(food);
            }
        }
        if (matches.isEmpty()) {
            return null;
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", matches.get(0).path("description").asText());
        data.put("kcal", median(matches, "Energy"));
        data.put("carbs", median(matches, "Carbohydrate, by difference"));
        data.put("fat", median(matches, "Total lipid (fat)"));
        data.put("protein", median(matches, "Protein"));
        data.put("sodium", median(matches, "Sodium, Na"));
        data.put("per", "100g");
        return data;
    }

    /**
     * Translates text with Azure AI Translator; returns it unchanged without a key or without text.
     * Language codes are case-insensitive (DE / EN). The region header is required for
     * regional resources and is sent only when configured.
     */
    public String translate(String text, String sourceLang, String targetLang) throws UpstreamException {
        if (translatorKey.isEmpty() || text == null || text.trim().isEmpty()) {
            return text;
        }
        String url = TRANSLATOR_URL
                + "?api-version=3.0"
                + "&from=" + encode(sourceLang.trim().toLowerCase(Locale.ROOT))
                + "&to=" + encode(targetLang.trim().toLowerCase(Locale.ROOT));

        String json;
        try {
            json = JSON.writeValueAsString(Collections.singletonList(Collections.singletonMap("Text", text)));
        } catch (IOException e) {
            throw new UpstreamException("Translator", 0);
        }

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Ocp-Apim-Subscription-Key", translatorKey);
        if (!translatorRegion.isEmpty()) {
            headers.put("Ocp-Apim-Subscription-Region", translatorRegion);
        }
        JsonNode translations = request("Translator", () -> http.postJson(url, headers, json))
                .path(0).path("translations");
        return translations.size() > 0 ? translations.get(0).path("text").asText(text) : text;
    }

    // ------------------------------------------------------------------ helpers

    private interface Call {
        UpstreamHttp.Result run() throws IOException;
    }

    private static JsonNode request(String service, Call call) throws UpstreamException {
        UpstreamHttp.Result result;
        try {
            result = call.run();
        } catch (IOException | RuntimeException e) {
            throw new UpstreamException(service, 0);
        }
        if (result.status() >= 400) {
            throw new UpstreamException(service, result.status());
        }
        try {
            return JSON.readTree(result.body() == null ? "{}" : result.body());
        } catch (IOException e) {
            throw new UpstreamException(service, 0);
        }
    }

    /** Median of a nutrient over the matching foods (missing values skipped; 0 if none). */
    private static double median(List<JsonNode> foods, String nutrientName) {
        List<Double> values = new ArrayList<>();
        for (JsonNode food : foods) {
            for (JsonNode nutrient : food.path("foodNutrients")) {
                if (nutrientName.equals(nutrient.path("nutrientName").asText()) && nutrient.hasNonNull("value")) {
                    values.add(nutrient.path("value").asDouble());
                    break;
                }
            }
        }
        if (values.isEmpty()) {
            return 0.0;
        }
        Collections.sort(values);
        int n = values.size();
        return n % 2 == 1 ? values.get(n / 2) : (values.get(n / 2 - 1) + values.get(n / 2)) / 2.0;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
