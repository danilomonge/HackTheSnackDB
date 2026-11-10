package de.pecus.api.repositories.usuarios;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import de.pecus.api.entities.ResultItemDO;

/**
 * Result lines of evaluations. Every query returns rows in insertion order
 * (evaluation by evaluation, ingredient by ingredient).
 */
public interface ResultItemRepository extends JpaRepository<ResultItemDO, Serializable> {

    @Query("SELECT r FROM ResultItemDO r WHERE r.active = true AND r.evaluationId = :evaluationId ORDER BY r.id ASC")
    List<ResultItemDO> findByEvaluationId(@Param("evaluationId") Long evaluationId);

    @Query("SELECT r FROM ResultItemDO r WHERE r.active = true AND r.productId = :productId AND r.substanceId = :substanceId ORDER BY r.id ASC")
    List<ResultItemDO> findByProductIdAndSubstanceId(@Param("productId") Long productId, @Param("substanceId") Long substanceId);

    @Query("SELECT r FROM ResultItemDO r WHERE r.active = true AND r.productId = :productId ORDER BY r.id ASC")
    List<ResultItemDO> findByProductId(@Param("productId") Long productId);

    @Query("SELECT r FROM ResultItemDO r WHERE r.active = true AND r.substanceId = :substanceId ORDER BY r.id ASC")
    List<ResultItemDO> findBySubstanceId(@Param("substanceId") Long substanceId);
}
