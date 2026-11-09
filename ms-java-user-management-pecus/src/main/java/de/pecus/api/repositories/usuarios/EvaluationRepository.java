package de.pecus.api.repositories.usuarios;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import de.pecus.api.entities.EvaluationDO;

public interface EvaluationRepository extends JpaRepository<EvaluationDO, Serializable> {

    /** Active evaluations of a product, oldest first. */
    @Query("SELECT e FROM EvaluationDO e WHERE e.active = true AND e.productId = :productId ORDER BY e.id ASC")
    List<EvaluationDO> findByProductId(@Param("productId") Long productId);

    @Query("SELECT e FROM EvaluationDO e WHERE e.active = true AND e.id = :id")
    EvaluationDO findById(@Param("id") Long id);
}
