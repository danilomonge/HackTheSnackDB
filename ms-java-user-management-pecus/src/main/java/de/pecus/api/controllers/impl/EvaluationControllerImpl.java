package de.pecus.api.controllers.impl;

import de.pecus.api.entities.EvaluationDO;
import de.pecus.api.entities.ResultItemDO;
import de.pecus.api.repositories.usuarios.EvaluationRepository;
import de.pecus.api.repositories.usuarios.ResultItemRepository;
import de.pecus.api.util.ResponseUtil;
import de.pecus.api.vo.ResponseVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Controller for nutritional evaluations.
 * Exposes endpoints under /evaluation.
 *
 * Python payload for POST /evaluation:
 * {
 *   "idProduct": Long,
 *   "idIngredient": Long,
 *   "idRecipe": Long,
 *   "ingredientMeanPercentage": Double,
 *   "ingredientStdPercentage": Double,
 *   "resultList": [
 *     {
 *       "recipeId": Long,
 *       "ingredientId": Long,
 *       "ingredientMeanPercentage": Double,
 *       "ingredientStdPercentage": Double
 *     }, ...
 *   ]
 * }
 */
@RestController
@RequestMapping("/evaluation")
public class EvaluationControllerImpl {

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private ResultItemRepository resultItemRepository;

    /**
     * POST /evaluation
     * Creates an evaluation with all its result items.
     * Returns 201 with {"success": true, "data": <evaluationId>}.
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseVO<Long>> createEvaluation(@RequestBody Map<String, Object> body) {
        ResponseVO<Long> response = new ResponseVO<>();
        try {
            Long idProduct = toLong(body.get("idProduct"));
            Long idIngredient = toLong(body.get("idIngredient"));
            Long idRecipe = toLong(body.get("idRecipe"));
            Double meanPct = toDouble(body.get("ingredientMeanPercentage"));
            Double stdPct = toDouble(body.get("ingredientStdPercentage"));

            if (idProduct == null || idIngredient == null || idRecipe == null) {
                response.setSuccess(false);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            Date now = new Date();

            EvaluationDO evaluation = new EvaluationDO();
            evaluation.setProductId(idProduct);
            evaluation.setSubstanceId(idIngredient);
            evaluation.setRecipeId(idRecipe);
            evaluation.setIngredientMeanPercentage(meanPct != null ? meanPct : 0.0);
            evaluation.setIngredientStdPercentage(stdPct != null ? stdPct : 0.0);
            evaluation.setCreationDate(now);
            evaluation.setCreatorUsername(1L);
            evaluation.setLastModifiedDate(now);
            evaluation.setLastModifiedUsername(1L);
            evaluation.setActive(Boolean.TRUE);

            evaluation = evaluationRepository.saveAndFlush(evaluation);
            Long evaluationId = evaluation.getId();

            // Persist each result item
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> resultList = (List<Map<String, Object>>) body.get("resultList");
            if (resultList != null) {
                for (Map<String, Object> item : resultList) {
                    ResultItemDO ri = new ResultItemDO();
                    ri.setEvaluationId(evaluationId);
                    ri.setRecipeId(toLong(item.get("recipeId")));
                    ri.setSubstanceId(toLong(item.get("ingredientId")));
                    ri.setProductId(idProduct);
                    ri.setIngredientMeanPercentage(toDouble(item.get("ingredientMeanPercentage")));
                    ri.setIngredientStdPercentage(toDouble(item.get("ingredientStdPercentage")));
                    ri.setCreationDate(now);
                    ri.setCreatorUsername(1L);
                    ri.setLastModifiedDate(now);
                    ri.setLastModifiedUsername(1L);
                    ri.setActive(Boolean.TRUE);
                    resultItemRepository.saveAndFlush(ri);
                }
            }

            response.setSuccess(true);
            response.setData(evaluationId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtil.getErrorResponse(e));
        }
    }

    /**
     * GET /evaluation/list?page=1&size=10&orderBy&orderType&idProduct={idProduct}
     * Returns all evaluations for a given product.
     */
    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseVO<List<Map<String, Object>>>> listEvaluations(
            @RequestParam(value = "idProduct", required = false) Long idProduct) {
        ResponseVO<List<Map<String, Object>>> response = new ResponseVO<>();
        try {
            List<EvaluationDO> evaluations = evaluationRepository.findByProductId(idProduct);
            List<Map<String, Object>> result = new ArrayList<>();
            for (EvaluationDO e : evaluations) {
                result.add(evaluationToMap(e));
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
     * GET /evaluation/detail?id={id}
     * Returns a single evaluation by ID, including its result items.
     */
    @GetMapping(value = "/detail", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseVO<Map<String, Object>>> detailEvaluation(
            @RequestParam("id") Long id) {
        ResponseVO<Map<String, Object>> response = new ResponseVO<>();
        try {
            EvaluationDO evaluation = evaluationRepository.findById(id);
            if (evaluation == null) {
                response.setSuccess(false);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            Map<String, Object> data = evaluationToMap(evaluation);

            List<ResultItemDO> items = resultItemRepository.findByEvaluationId(id);
            List<Map<String, Object>> itemList = new ArrayList<>();
            for (ResultItemDO ri : items) {
                itemList.add(resultItemToMap(ri));
            }
            data.put("resultList", itemList);

            response.setSuccess(true);
            response.setData(data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtil.getErrorResponse(e));
        }
    }

    /**
     * GET /evaluation/listResult?page=1&size=50&orderBy&orderType&idProduct={idProduct}&idIngredient={idIngredient}
     * Returns result items filtered by product and ingredient.
     */
    @GetMapping(value = "/listResult", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseVO<List<Map<String, Object>>>> listResult(
            @RequestParam(value = "idProduct", required = false) Long idProduct,
            @RequestParam(value = "idIngredient", required = false) Long idIngredient) {
        ResponseVO<List<Map<String, Object>>> response = new ResponseVO<>();
        try {
            List<ResultItemDO> items;
            if (idProduct != null && idIngredient != null) {
                items = resultItemRepository.findByProductIdAndSubstanceId(idProduct, idIngredient);
            } else if (idProduct != null) {
                // Find all result items whose evaluation belongs to this product
                List<EvaluationDO> evals = evaluationRepository.findByProductId(idProduct);
                items = new ArrayList<>();
                for (EvaluationDO e : evals) {
                    items.addAll(resultItemRepository.findByEvaluationId(e.getId()));
                }
            } else {
                items = new ArrayList<>();
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (ResultItemDO ri : items) {
                result.add(resultItemToMap(ri));
            }
            response.setSuccess(true);
            response.setData(result);
            response.setTotalRows((long) result.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtil.getErrorResponse(e));
        }
    }

    // --- helpers ---

    private Map<String, Object> evaluationToMap(EvaluationDO e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("idProduct", e.getProductId());
        m.put("idIngredient", e.getSubstanceId());
        m.put("idRecipe", e.getRecipeId());
        m.put("ingredientMeanPercentage", e.getIngredientMeanPercentage());
        m.put("ingredientStdPercentage", e.getIngredientStdPercentage());
        return m;
    }

    private Map<String, Object> resultItemToMap(ResultItemDO ri) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", ri.getId());
        m.put("evaluationId", ri.getEvaluationId());
        m.put("recipeId", ri.getRecipeId());
        m.put("ingredientId", ri.getSubstanceId());
        m.put("idProduct", ri.getProductId());
        m.put("ingredientMeanPercentage", ri.getIngredientMeanPercentage());
        m.put("ingredientStdPercentage", ri.getIngredientStdPercentage());
        return m;
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        try { return Long.valueOf(value.toString()); } catch (NumberFormatException e) { return null; }
    }

    private Double toDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Double) return (Double) value;
        if (value instanceof Integer) return ((Integer) value).doubleValue();
        if (value instanceof Float) return ((Float) value).doubleValue();
        try { return Double.valueOf(value.toString()); } catch (NumberFormatException e) { return null; }
    }
}
