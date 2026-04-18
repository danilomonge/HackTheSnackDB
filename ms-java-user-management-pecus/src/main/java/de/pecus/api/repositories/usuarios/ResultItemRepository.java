package de.pecus.api.repositories.usuarios;

import de.pecus.api.entities.ResultItemDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.io.Serializable;
import java.util.List;

public interface ResultItemRepository extends JpaRepository<ResultItemDO, Serializable> {

    @Query("SELECT r FROM ResultItemDO r WHERE r.active = true AND r.evaluationId = :evaluationId")
    List<ResultItemDO> findByEvaluationId(@Param("evaluationId") Long evaluationId);

    @Query("SELECT r FROM ResultItemDO r WHERE r.active = true AND r.productId = :productId AND r.substanceId = :substanceId")
    List<ResultItemDO> findByProductIdAndSubstanceId(@Param("productId") Long productId, @Param("substanceId") Long substanceId);
}
