package de.pecus.api.repositories.usuarios;

import de.pecus.api.entities.EvaluationDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.io.Serializable;
import java.util.List;

public interface EvaluationRepository extends JpaRepository<EvaluationDO, Serializable> {

    @Query("SELECT e FROM EvaluationDO e WHERE e.active = true AND e.productId = :productId")
    List<EvaluationDO> findByProductId(@Param("productId") Long productId);

    @Query("SELECT e FROM EvaluationDO e WHERE e.active = true AND e.id = :id")
    EvaluationDO findById(@Param("id") Long id);
}
