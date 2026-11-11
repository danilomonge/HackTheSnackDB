package de.pecus.api.controllers.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.pecus.api.services.nutrition.NutritionService;
import de.pecus.api.services.nutrition.UpstreamException;

/**
 * Nutrition and translation endpoints for the web app's nutrition dialog.
 * The web build sets {@code nutrition_proxy_url} to this API's base URL.
 *
 * <pre>
 * GET  /health                          → {"ok": true}
 * GET  /api/nutrition?ingredient=sugar  → {"data": {...} | null}
 * POST /api/translate {"text", "source_lang"?, "target_lang"?} → {"text": "..."}
 * </pre>
 *
 * Errors: 400 invalid input, 503 USDA key not configured, 502 upstream failure
 * ({"detail": "..."}, never containing URLs or keys).
 */
@RestController
public class NutritionControllerImpl {

    private static final int MAX_INGREDIENT_LENGTH = 200;

    private final NutritionService nutritionService;

    public NutritionControllerImpl(NutritionService nutritionService) {
        this.nutritionService = nutritionService;
    }

    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> health() {
        return Collections.singletonMap("ok", true);
    }

    @GetMapping(value = "/api/nutrition", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> nutrition(@RequestParam("ingredient") String ingredient) {
        String name = ingredient.trim();
        if (name.isEmpty() || name.length() > MAX_INGREDIENT_LENGTH) {
            return detail(HttpStatus.BAD_REQUEST, "ingredient must have 1 to " + MAX_INGREDIENT_LENGTH + " characters");
        }
        if (!nutritionService.hasUsdaKey()) {
            return detail(HttpStatus.SERVICE_UNAVAILABLE, "USDA_API_KEY is not configured on the server.");
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("data", nutritionService.lookupUsda(name));
            return ResponseEntity.ok(body);
        } catch (UpstreamException e) {
            return detail(HttpStatus.BAD_GATEWAY, e.getMessage());
        }
    }

    @PostMapping(value = "/api/translate", consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> translate(@RequestBody Map<String, Object> request) {
        Object text = request.get("text");
        if (!(text instanceof String)) {
            return detail(HttpStatus.BAD_REQUEST, "text is required");
        }
        String source = request.get("source_lang") instanceof String ? (String) request.get("source_lang") : "DE";
        String target = request.get("target_lang") instanceof String ? (String) request.get("target_lang") : "EN";
        try {
            return ResponseEntity.ok(Collections.singletonMap("text",
                    nutritionService.translate((String) text, source, target)));
        } catch (UpstreamException e) {
            return detail(HttpStatus.BAD_GATEWAY, e.getMessage());
        }
    }

    private static ResponseEntity<Map<String, Object>> detail(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Collections.singletonMap("detail", message));
    }
}
