package de.pecus.api.controllers.impl;

import de.pecus.api.entities.RecipeDO;
import de.pecus.api.repositories.usuarios.RecipeRepository;
import de.pecus.api.util.ResponseUtil;
import de.pecus.api.vo.ResponseErrorVO;
import de.pecus.api.vo.ResponseVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Controller for product-ingredient recipe associations.
 * Exposes endpoints under /recipe.
 */
@RestController
@RequestMapping("/recipe")
public class RecipeControllerImpl {

    @Autowired
    private RecipeRepository recipeRepository;

    /**
     * POST /recipe
     * Creates an association between a product and an ingredient.
     * Body: {"idProduct": Long, "idIngredient": Long, "fechaRegistro": "yyyy-MM-dd"}
     * Returns 201 with {"success": true, "data": <recipeId>} on success.
     * Returns 400 if the association already exists (Python reads the detail separately).
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseVO<Long>> createRecipe(@RequestBody Map<String, Object> body) {
        ResponseVO<Long> response = new ResponseVO<>();
        try {
            Long idProduct = toLong(body.get("idProduct"));
            Long idIngredient = toLong(body.get("idIngredient"));

            if (idProduct == null || idIngredient == null) {
                response.setSuccess(false);
                response.setErrors(Collections.singletonList(
                        new ResponseErrorVO("400", "REQUIRED_PARAMETERS_ERROR", "idProduct and idIngredient are required", null)));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            RecipeDO existing = recipeRepository.findByProductIdAndSubstanceId(idProduct, idIngredient);
            if (existing != null) {
                response.setSuccess(false);
                response.setErrors(Collections.singletonList(
                        new ResponseErrorVO("400", "DUPLICATED_ERROR", "Recipe already exists for this product and ingredient", null)));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            RecipeDO recipe = new RecipeDO();
            recipe.setProductId(idProduct);
            recipe.setSubstanceId(idIngredient);
            Date now = new Date();
            recipe.setCreationDate(now);
            recipe.setCreatorUsername(1L);
            recipe.setLastModifiedDate(now);
            recipe.setLastModifiedUsername(1L);
            recipe.setActive(Boolean.TRUE);

            recipe = recipeRepository.saveAndFlush(recipe);

            response.setSuccess(true);
            response.setData(recipe.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtil.getErrorResponse(e));
        }
    }

    /**
     * GET /recipe/list?page=1&size=10&orderBy&orderType&id={idProduct}
     * Returns all active recipe associations for a product.
     */
    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseVO<List<Map<String, Object>>>> listRecipes(
            @RequestParam(value = "id", required = false) Long idProduct) {
        ResponseVO<List<Map<String, Object>>> response = new ResponseVO<>();
        try {
            List<RecipeDO> recipes = recipeRepository.findByProductId(idProduct);
            List<Map<String, Object>> result = new ArrayList<>();
            for (RecipeDO r : recipes) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("idRecipe", r.getId());
                item.put("idProduct", r.getProductId());
                item.put("idIngredient", r.getSubstanceId());
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
     * GET /recipe/detailRecipeByProduct?idProduct={idProduct}&idIngredient={idIngredient}
     * Returns the recipe association for a specific product-ingredient pair.
     * Python reads response.json()['data']['idRecipe'] from this endpoint.
     */
    @GetMapping(value = "/detailRecipeByProduct", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseVO<Map<String, Object>>> detailRecipeByProduct(
            @RequestParam("idProduct") Long idProduct,
            @RequestParam("idIngredient") Long idIngredient) {
        ResponseVO<Map<String, Object>> response = new ResponseVO<>();
        try {
            RecipeDO recipe = recipeRepository.findByProductIdAndSubstanceId(idProduct, idIngredient);
            if (recipe == null) {
                response.setSuccess(false);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("idRecipe", recipe.getId());
            data.put("idProduct", recipe.getProductId());
            data.put("idIngredient", recipe.getSubstanceId());
            response.setSuccess(true);
            response.setData(data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtil.getErrorResponse(e));
        }
    }

    /**
     * DELETE /recipe/deleteByProduct?idProduct={idProduct}&idIngredient={idIngredient}
     * Soft-deletes the association between a product and an ingredient.
     */
    @DeleteMapping(value = {"/deleteByProduct", "/deleteByProduct/"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseVO<Boolean>> deleteByProduct(
            @RequestParam("idProduct") Long idProduct,
            @RequestParam("idIngredient") Long idIngredient) {
        ResponseVO<Boolean> response = new ResponseVO<>();
        try {
            RecipeDO recipe = recipeRepository.findByProductIdAndSubstanceId(idProduct, idIngredient);
            if (recipe != null) {
                recipe.setActive(Boolean.FALSE);
                recipe.setLastModifiedDate(new Date());
                recipe.setLastModifiedUsername(1L);
                recipeRepository.saveAndFlush(recipe);
            }
            response.setSuccess(true);
            response.setData(Boolean.TRUE);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtil.getErrorResponse(e));
        }
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
