package de.pecus.api.entities;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Association between a Product and a Substance (ingredient).
 * Maps to the RECIPE table.
 */
@Entity
@Table(name = "RECIPE")
public class RecipeDO extends AuditBase<Long> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PK_ID")
    private Long id;

    @Column(name = "FK_PRODUCT_ID")
    private Long productId;

    @Column(name = "FK_SUBSTANCE_ID")
    private Long substanceId;

    public RecipeDO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getSubstanceId() { return substanceId; }
    public void setSubstanceId(Long substanceId) { this.substanceId = substanceId; }
}
