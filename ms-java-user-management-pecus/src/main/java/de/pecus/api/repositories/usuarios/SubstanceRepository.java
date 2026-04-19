package de.pecus.api.repositories.usuarios;

import de.pecus.api.entities.SubstanceDO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.io.Serializable;

public interface SubstanceRepository extends JpaRepository<SubstanceDO, Serializable> {

	/**
	 * Consulta por name sin implementacion de query especifico
	 * @param name Identificador de nombre buscado
	 * @return Objeto de mapeo a la entidad
	 */
	@Query(value = " SELECT r" 
			+ " FROM  SubstanceDO r"
			+ " WHERE r.active = true "
			+ " AND r.name = :name")
	SubstanceDO findByName(@Param("name") String name);
	
	/**
	 * Consulta por id sin implementacion de query especifico
	 * 
	 * @return Objeto de mapeo a la entidad
	 * 
	 * @param id Identificador de registro buscado
	 */
	@Query(value = " SELECT r" 
			+ " FROM  SubstanceDO r"
			+ " WHERE r.active = true "
			+ " AND r.id = :id")
	SubstanceDO findById(@Param("id") Long id);
	
    /**
	 * Consulta por nombre . Y se prepara para paginacion
	 * 
	 * @return List<Objeto> con el resultado
	 * 
	 * @param name cadena descriptiva de registro buscado
	 * @param pageable
	 */
	@Query(value = " SELECT r" 
			+ " FROM  SubstanceDO r"
			+ " WHERE r.active = true "
			+ " AND (:name IS NULL OR (TRANSLATE(UPPER(r.name),'áéíóú','aeiou') LIKE %:name%))")
	Page<SubstanceDO> findList(@Param("name") String name,
                             Pageable pageable);
	
}
