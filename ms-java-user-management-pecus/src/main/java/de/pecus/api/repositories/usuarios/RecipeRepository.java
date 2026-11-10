package de.pecus.api.repositories.usuarios;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import de.pecus.api.entities.RecipeDO;

public interface RecipeRepository extends JpaRepository<RecipeDO, Serializable> {

    /** Active association between a product and an ingredient, or null. */
    @Query("SELECT r FROM RecipeDO r WHERE r.active = true AND r.productId = :productId AND r.substanceId = :substanceId")
    RecipeDO findByProductIdAndSubstanceId(@Param("productId") Long productId, @Param("substanceId") Long substanceId);

    /** Every association (active or soft-deleted) for a product/ingredient pair, newest first. */
    @Query("SELECT r FROM RecipeDO r WHERE r.productId = :productId AND r.substanceId = :substanceId ORDER BY r.id DESC")
    List<RecipeDO> findAllByProductIdAndSubstanceId(@Param("productId") Long productId, @Param("substanceId") Long substanceId);

    /** Active ingredients of a product in the order they were first associated. */
    @Query("SELECT r FROM RecipeDO r WHERE r.active = true AND r.productId = :productId ORDER BY r.id ASC")
    List<RecipeDO> findByProductId(@Param("productId") Long productId);
}
