package de.pecus.api.repositories.usuarios;

import de.pecus.api.entities.RecipeDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.io.Serializable;
import java.util.List;

public interface RecipeRepository extends JpaRepository<RecipeDO, Serializable> {

    @Query("SELECT r FROM RecipeDO r WHERE r.active = true AND r.productId = :productId AND r.substanceId = :substanceId")
    RecipeDO findByProductIdAndSubstanceId(@Param("productId") Long productId, @Param("substanceId") Long substanceId);

    @Query("SELECT r FROM RecipeDO r WHERE r.active = true AND r.productId = :productId")
    List<RecipeDO> findByProductId(@Param("productId") Long productId);
}
