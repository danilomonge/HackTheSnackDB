package de.pecus.api.controllers.impl;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.pecus.api.entities.EvaluationDO;
import de.pecus.api.entities.ResultItemDO;
import de.pecus.api.repositories.usuarios.EvaluationRepository;
import de.pecus.api.repositories.usuarios.ResultItemRepository;
import de.pecus.api.util.NameLookup;
import de.pecus.api.util.ResponseUtil;
import de.pecus.api.util.ServiceUtil;
import de.pecus.api.vo.ResponseErrorVO;
import de.pecus.api.vo.ResponseVO;

/**
 * Saved recipe estimations ("evaluations"). Exposed under /evaluation.
 *
 * An evaluation is one saved run of the recipe generator for a product; its
 * result items hold the estimated mean / variation percentage of every
 * ingredient. Payload of POST /evaluation:
 *
 * <pre>
 * {
 *   "idProduct": 1,
 *   "idIngredient": 2,                 // optional, defaults to the first result item
 *   "idRecipe": 3,                     // optional, defaults to the first result item
 *   "ingredientMeanPercentage": 60.0,  // optional, defaults to the first result item
 *   "ingredientStdPercentage": 1.5,    // optional, defaults to the first result item
 *   "resultList": [
 *     {"recipeId": 3, "ingredientId": 2, "ingredientMeanPercentage": 60.0, "ingredientStdPercentage": 1.5},
 *     ...
 *   ]
 * }
 * </pre>
 */
@RestController
@RequestMapping("/evaluation")
public class EvaluationControllerImpl {

    private static final DateTimeFormatter ISO_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'+00:00'").withZone(ZoneOffset.UTC);

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private ResultItemRepository resultItemRepository;

    @Autowired
    private NameLookup nameLookup;

    /**
     * Creates an evaluation together with all its result items in one transaction.
     *
     * @return 201 {"success": true, "data": evaluationId}; 400 when ids are missing or unknown.
     */
    @Transactional
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseVO<Long>> createEvaluation(@RequestBody Map<String, Object> body) {
        try {
            Long idProduct = toLong(body.get("idProduct"));
            List<Map<String, Object>> resultList = toItemList(body.get("resultList"));

            if (idProduct == null || resultList.isEmpty()) {
                return error(HttpStatus.BAD_REQUEST, "REQUIRED_PARAMETERS_ERROR",
                        "idProduct and a non-empty resultList are required");
            }
            if (!nameLookup.productExists(idProduct)) {
                return error(HttpStatus.BAD_REQUEST, "NOT_FOUND_ERROR", "Unknown product id " + idProduct);
            }
            for (Map<String, Object> item : resultList) {
                Long ingredientId = toLong(item.get("ingredientId"));
                if (toLong(item.get("recipeId")) == null || ingredientId == null) {
                    return error(HttpStatus.BAD_REQUEST, "REQUIRED_PARAMETERS_ERROR",
                            "Every result item needs recipeId and ingredientId");
                }
                if (!nameLookup.ingredientExists(ingredientId)) {
                    return error(HttpStatus.BAD_REQUEST, "NOT_FOUND_ERROR", "Unknown ingredient id " + ingredientId);
                }
            }

            Map<String, Object> first = resultList.get(0);
            EvaluationDO evaluation = new EvaluationDO();
            evaluation.setProductId(idProduct);
            evaluation.setSubstanceId(firstNonNull(toLong(body.get("idIngredient")), toLong(first.get("ingredientId"))));
            evaluation.setRecipeId(firstNonNull(toLong(body.get("idRecipe")), toLong(first.get("recipeId"))));
            evaluation.setIngredientMeanPercentage(firstNonNull(
                    toDouble(body.get("ingredientMeanPercentage")), toDouble(first.get("ingredientMeanPercentage")), 0.0));
            evaluation.setIngredientStdPercentage(firstNonNull(
                    toDouble(body.get("ingredientStdPercentage")), toDouble(first.get("ingredientStdPercentage")), 0.0));
            ServiceUtil.setAuditFields(evaluation, null);
            evaluation = evaluationRepository.saveAndFlush(evaluation);

            Date created = evaluation.getCreationDate();
            List<ResultItemDO> items = new ArrayList<>();
            for (Map<String, Object> item : resultList) {
                ResultItemDO ri = new ResultItemDO();
                ri.setEvaluationId(evaluation.getId());
                ri.setProductId(idProduct);
                ri.setRecipeId(toLong(item.get("recipeId")));
                ri.setSubstanceId(toLong(item.get("ingredientId")));
                ri.setIngredientMeanPercentage(firstNonNull(toDouble(item.get("ingredientMeanPercentage")), 0.0));
                ri.setIngredientStdPercentage(firstNonNull(toDouble(item.get("ingredientStdPercentage")), 0.0));
                ServiceUtil.setAuditFields(ri, null);
                // All lines of one evaluation share its timestamp so they group together.
                ri.setCreationDate(created);
                ri.setLastModifiedDate(created);
                items.add(ri);
            }
            resultItemRepository.saveAll(items);
            resultItemRepository.flush();

            ResponseVO<Long> response = new ResponseVO<>();
            response.setSuccess(true);
            response.setData(evaluation.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtil.getErrorResponse(e));
        }
    }

    /** Active evaluations of a product, oldest first. */
    @Transactional(readOnly = true)
    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseVO<List<Map<String, Object>>>> listEvaluations(
            @RequestParam(value = "idProduct", required = false) Long idProduct) {
        ResponseVO<List<Map<String, Object>>> response = new ResponseVO<>();
        try {
            List<EvaluationDO> evaluations = idProduct == null
                    ? Collections.emptyList()
                    : evaluationRepository.findByProductId(idProduct);
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

    /** One evaluation by id, including its result items. */
    @Transactional(readOnly = true)
    @GetMapping(value = "/detail", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseVO<Map<String, Object>>> detailEvaluation(@RequestParam("id") Long id) {
        try {
            EvaluationDO evaluation = evaluationRepository.findById(id);
            if (evaluation == null) {
                return error(HttpStatus.NOT_FOUND, "NOT_FOUND_ERROR", "Evaluation " + id + " not found");
            }
            Map<String, Object> data = evaluationToMap(evaluation);
            data.put("resultList", enrich(resultItemRepository.findByEvaluationId(id)));

            ResponseVO<Map<String, Object>> response = new ResponseVO<>();
            response.setSuccess(true);
            response.setData(data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtil.getErrorResponse(e));
        }
    }

    /**
     * Result items filtered by product, by ingredient, or by both, in the order
     * they were saved. Each item also carries ingredientName, productName and
     * evaluationDate (ISO-8601, UTC).
     */
    @Transactional(readOnly = true)
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
                items = resultItemRepository.findByProductId(idProduct);
            } else if (idIngredient != null) {
                items = resultItemRepository.findBySubstanceId(idIngredient);
            } else {
                items = Collections.emptyList();
            }
            List<Map<String, Object>> result = enrich(items);
            response.setSuccess(true);
            response.setData(result);
            response.setTotalRows((long) result.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtil.getErrorResponse(e));
        }
    }

    // --- helpers ---

    /** Converts result items to maps and adds names and the evaluation date (2 queries in total). */
    private List<Map<String, Object>> enrich(List<ResultItemDO> items) {
        Map<Long, String> ingredientNames = nameLookup.ingredientNames(
                items.stream().map(ResultItemDO::getSubstanceId).collect(Collectors.toList()));
        Map<Long, String> productNames = nameLookup.productNames(
                items.stream().map(ResultItemDO::getProductId).collect(Collectors.toList()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (ResultItemDO ri : items) {
            Map<String, Object> m = resultItemToMap(ri);
            m.put("ingredientName", ingredientNames.getOrDefault(ri.getSubstanceId(), "Unknown"));
            m.put("productName", productNames.getOrDefault(ri.getProductId(), "Unknown"));
            m.put("evaluationDate", ri.getCreationDate() != null
                ? ISO_FMT.format(ri.getCreationDate().toInstant()) : null);
            result.add(m);
        }
        return result;
    }

    private static Map<String, Object> evaluationToMap(EvaluationDO e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("idProduct", e.getProductId());
        m.put("idIngredient", e.getSubstanceId());
        m.put("idRecipe", e.getRecipeId());
        m.put("ingredientMeanPercentage", e.getIngredientMeanPercentage());
        m.put("ingredientStdPercentage", e.getIngredientStdPercentage());
        m.put("evaluationDate", e.getCreationDate() != null
            ? ISO_FMT.format(e.getCreationDate().toInstant()) : null);
        return m;
    }

    private static Map<String, Object> resultItemToMap(ResultItemDO ri) {
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

    private static <T> ResponseEntity<ResponseVO<T>> error(HttpStatus status, String key, String message) {
        ResponseVO<T> response = new ResponseVO<>();
        response.setSuccess(false);
        response.setErrors(Collections.singletonList(
                new ResponseErrorVO(String.valueOf(status.value()), key, message, null)));
        return ResponseEntity.status(status).body(response);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> toItemList(Object value) {
        if (!(value instanceof List)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (Object o : (List<Object>) value) {
            if (o instanceof Map) {
                items.add((Map<String, Object>) o);
            }
        }
        return items;
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        for (T v : values) {
            if (v != null) return v;
        }
        return null;
    }

    private static Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try { return Long.valueOf(value.toString().trim()); } catch (NumberFormatException e) { return null; }
    }

    private static Double toDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try { return Double.valueOf(value.toString().trim()); } catch (NumberFormatException e) { return null; }
    }
}
