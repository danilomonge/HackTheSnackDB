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
 * (DeepL) for the web app's nutrition dialog. The API keys stay on the server
 * (env USDA_API_KEY / DEEPL_API_KEY); the browser only calls this API.
 *
 * Same behaviour as the app's Python implementation
 * (HackTheSnack: hts_lib/backend/nutrition_service.py).
 */
@Service
public class NutritionService {

    static final String USDA_SEARCH_URL = "https://api.nal.usda.gov/fdc/v1/foods/search";
    static final String DEEPL_FREE_URL = "https://api-free.deepl.com/v2/translate";
    static final String DEEPL_PRO_URL = "https://api.deepl.com/v2/translate";
    /** How many USDA search hits are checked for an exact name match. */
    static final int USDA_PAGE_SIZE = 25;

    private static final ObjectMapper JSON = new ObjectMapper();

    private final UpstreamHttp http;
    private final String usdaApiKey;
    private final String deeplApiKey;

    @Autowired
    public NutritionService(UpstreamHttp http,
                            @Value("${app.nutrition.usda-api-key:}") String usdaApiKey,
                            @Value("${app.nutrition.deepl-api-key:}") String deeplApiKey) {
        this.http = http;
        this.usdaApiKey = usdaApiKey == null ? "" : usdaApiKey.trim();
        this.deeplApiKey = deeplApiKey == null ? "" : deeplApiKey.trim();
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
     * Translates text with DeepL; returns it unchanged without a key or without text.
     * DeepL Free keys end with ":fx" and use their own host; authentication is by header
     * (the legacy auth_key parameter is rejected).
     */
    public String translate(String text, String sourceLang, String targetLang) throws UpstreamException {
        if (deeplApiKey.isEmpty() || text == null || text.trim().isEmpty()) {
            return text;
        }
        String endpoint = deeplApiKey.endsWith(":fx") ? DEEPL_FREE_URL : DEEPL_PRO_URL;

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("text", Collections.singletonList(text));
        payload.put("source_lang", sourceLang);
        payload.put("target_lang", targetLang);
        String json;
        try {
            json = JSON.writeValueAsString(payload);
        } catch (IOException e) {
            throw new UpstreamException("DeepL", 0);
        }

        Map<String, String> headers = Collections.singletonMap("Authorization", "DeepL-Auth-Key " + deeplApiKey);
        JsonNode translations = request("DeepL", () -> http.postJson(endpoint, headers, json)).path("translations");
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
