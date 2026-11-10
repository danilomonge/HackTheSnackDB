package de.pecus.api.controllers.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.pecus.api.entities.EvaluationDO;
import de.pecus.api.entities.RecipeDO;
import de.pecus.api.entities.ResultItemDO;
import de.pecus.api.repositories.usuarios.EvaluationRepository;
import de.pecus.api.repositories.usuarios.RecipeRepository;
import de.pecus.api.repositories.usuarios.ResultItemRepository;
import de.pecus.api.util.NameLookup;
import de.pecus.api.util.ResponseUtil;
import de.pecus.api.util.ServiceUtil;
import de.pecus.api.vo.ResponseErrorVO;
import de.pecus.api.vo.ResponseVO;

/**
 * Product ↔ ingredient associations ("recipes"). Exposed under /recipe.
 *
 * <pre>
 * POST   /recipe                                         {"idProduct": 1, "idIngredient": 2}
 * GET    /recipe/list?id={idProduct}                     active ingredients of a product, in order
 * GET    /recipe/detailRecipeByProduct?idProduct=&idIngredient=
 * DELETE /recipe/deleteByProduct?idProduct=&idIngredient= soft delete
 * </pre>
 */
@RestController
@RequestMapping("/recipe")
public class RecipeControllerImpl {

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private ResultItemRepository resultItemRepository;

    @Autowired
    private NameLookup nameLookup;

    /**
     * Creates the association between a product and an ingredient.
     * A previously soft-deleted association is re-activated (keeping its id).
     *
     * @return 201 {"success": true, "data": recipeId};
     *         400 with DUPLICATED_ERROR if it is already active;
     *         400 with REQUIRED_PARAMETERS_ERROR / NOT_FOUND_ERROR for bad ids.
     */
    @Transactional
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseVO<Long>> createRecipe(@RequestBody Map<String, Object> body) {
        try {
            Long idProduct = toLong(body.get("idProduct"));
            Long idIngredient = toLong(body.get("idIngredient"));

            if (idProduct == null || idIngredient == null) {
                return error(HttpStatus.BAD_REQUEST, "REQUIRED_PARAMETERS_ERROR", "idProduct and idIngredient are required");
            }
            if (!nameLookup.productExists(idProduct) || !nameLookup.ingredientExists(idIngredient)) {
                return error(HttpStatus.BAD_REQUEST, "NOT_FOUND_ERROR", "Unknown product or ingredient id");
            }

            List<RecipeDO> existing = recipeRepository.findAllByProductIdAndSubstanceId(idProduct, idIngredient);
            RecipeDO recipe;
            if (existing.isEmpty()) {
                recipe = new RecipeDO();
                recipe.setProductId(idProduct);
                recipe.setSubstanceId(idIngredient);
            } else {
                recipe = existing.stream().filter(r -> Boolean.TRUE.equals(r.getActive())).findFirst()
                        .orElse(existing.get(0));
                if (Boolean.TRUE.equals(recipe.getActive())) {
                    return error(HttpStatus.BAD_REQUEST, "DUPLICATED_ERROR",
                            "Recipe already exists for this product and ingredient");
                }
            }
            ServiceUtil.setAuditFields(recipe, null);
            recipe = recipeRepository.saveAndFlush(recipe);

            ResponseVO<Long> response = new ResponseVO<>();
            response.setSuccess(true);
            response.setData(recipe.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtil.getErrorResponse(e));
        }
    }

    /**
     * Active ingredients of a product in label order (most to least used).
     * Each item: idRecipe, idProduct, idIngredient, nameIngredient.
     *
     * The order is taken from the product's latest saved evaluation, which
     * stores the ingredients exactly as they were entered. Ingredients that
     * are not part of it (associated later) follow in the order they were added.
     */
    @Transactional(readOnly = true)
    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseVO<List<Map<String, Object>>>> listRecipes(
            @RequestParam(value = "id", required = false) Long idProduct) {
        ResponseVO<List<Map<String, Object>>> response = new ResponseVO<>();
        try {
            List<RecipeDO> recipes = idProduct == null
                    ? Collections.emptyList()
                    : inLabelOrder(idProduct, recipeRepository.findByProductId(idProduct));
            Map<Long, String> names = nameLookup.ingredientNames(
                    recipes.stream().map(RecipeDO::getSubstanceId).collect(Collectors.toList()));

            List<Map<String, Object>> result = new ArrayList<>();
            for (RecipeDO r : recipes) {
                Map<String, Object> item = toMap(r);
                item.put("nameIngredient", names.get(r.getSubstanceId()));
                result.add(item);
            }
            response.setSuccess(true);
            response.setData(result);
            response.setTotalRows((long) result.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtil.getErrorResponse(e));
        }
    }

    /**
     * The active association for a product/ingredient pair.
     * The frontend reads response.data.idRecipe.
     */
    @Transactional(readOnly = true)
    @GetMapping(value = "/detailRecipeByProduct", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseVO<Map<String, Object>>> detailRecipeByProduct(
            @RequestParam("idProduct") Long idProduct,
            @RequestParam("idIngredient") Long idIngredient) {
        try {
            RecipeDO recipe = recipeRepository.findByProductIdAndSubstanceId(idProduct, idIngredient);
            if (recipe == null) {
                return error(HttpStatus.NOT_FOUND, "NOT_FOUND_ERROR", "No recipe for this product and ingredient");
            }
            Map<String, Object> data = toMap(recipe);
            data.put("nameIngredient", nameLookup.ingredientNames(Collections.singletonList(idIngredient)).get(idIngredient));
            ResponseVO<Map<String, Object>> response = new ResponseVO<>();
            response.setSuccess(true);
            response.setData(data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtil.getErrorResponse(e));
        }
    }

    /** Soft-deletes the association between a product and an ingredient (idempotent). */
    @Transactional
    @DeleteMapping(value = {"/deleteByProduct", "/deleteByProduct/"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseVO<Boolean>> deleteByProduct(
            @RequestParam("idProduct") Long idProduct,
            @RequestParam("idIngredient") Long idIngredient) {
        try {
            RecipeDO recipe = recipeRepository.findByProductIdAndSubstanceId(idProduct, idIngredient);
            if (recipe != null) {
                ServiceUtil.setDisabledEntity(recipe, null);
                recipeRepository.saveAndFlush(recipe);
            }
            ResponseVO<Boolean> response = new ResponseVO<>();
            response.setSuccess(true);
            response.setData(Boolean.TRUE);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtil.getErrorResponse(e));
        }
    }

    // --- helpers ---

    /** Sorts recipes by their position in the latest evaluation; the rest keep insertion order. */
    private List<RecipeDO> inLabelOrder(Long idProduct, List<RecipeDO> recipes) {
        if (recipes.size() < 2) {
            return recipes;
        }
        EvaluationDO latest = evaluationRepository.findFirstByProductIdAndActiveTrueOrderByIdDesc(idProduct);
        if (latest == null) {
            return recipes;
        }
        Map<Long, Integer> position = new HashMap<>();
        List<ResultItemDO> items = resultItemRepository.findByEvaluationId(latest.getId());
        for (int i = 0; i < items.size(); i++) {
            position.putIfAbsent(items.get(i).getSubstanceId(), i);
        }
        List<RecipeDO> sorted = new ArrayList<>(recipes);
        sorted.sort(Comparator
                .comparing((RecipeDO r) -> position.getOrDefault(r.getSubstanceId(), Integer.MAX_VALUE))
                .thenComparing(RecipeDO::getId));
        return sorted;
    }

    private static Map<String, Object> toMap(RecipeDO r) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("idRecipe", r.getId());
        item.put("idProduct", r.getProductId());
        item.put("idIngredient", r.getSubstanceId());
        return item;
    }

    private static <T> ResponseEntity<ResponseVO<T>> error(HttpStatus status, String key, String message) {
        ResponseVO<T> response = new ResponseVO<>();
        response.setSuccess(false);
        response.setErrors(Collections.singletonList(
                new ResponseErrorVO(String.valueOf(status.value()), key, message, null)));
        return ResponseEntity.status(status).body(response);
    }

    private static Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.valueOf(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
