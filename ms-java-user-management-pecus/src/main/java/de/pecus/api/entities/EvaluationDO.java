package de.pecus.api.entities;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Nutritional evaluation for a product-ingredient combination.
 * Maps to the EVALUATION table.
 */
@Entity
@Table(name = "EVALUATION")
public class EvaluationDO extends AuditBase<Long> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PK_ID")
    private Long id;

    @Column(name = "FK_PRODUCT_ID")
    private Long productId;

    @Column(name = "FK_SUBSTANCE_ID")
    private Long substanceId;

    @Column(name = "FK_RECIPE_ID")
    private Long recipeId;

    @Column(name = "DN_INGREDIENT_MEAN")
    private Double ingredientMeanPercentage;

    @Column(name = "DN_INGREDIENT_STD")
    private Double ingredientStdPercentage;

    public EvaluationDO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getSubstanceId() { return substanceId; }
    public void setSubstanceId(Long substanceId) { this.substanceId = substanceId; }

    public Long getRecipeId() { return recipeId; }
    public void setRecipeId(Long recipeId) { this.recipeId = recipeId; }

    public Double getIngredientMeanPercentage() { return ingredientMeanPercentage; }
    public void setIngredientMeanPercentage(Double ingredientMeanPercentage) {
        this.ingredientMeanPercentage = ingredientMeanPercentage;
    }

    public Double getIngredientStdPercentage() { return ingredientStdPercentage; }
    public void setIngredientStdPercentage(Double ingredientStdPercentage) {
        this.ingredientStdPercentage = ingredientStdPercentage;
    }
}
