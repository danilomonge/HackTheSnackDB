package de.pecus.api.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.pecus.api.entities.ProductDO;
import de.pecus.api.entities.SubstanceDO;
import de.pecus.api.repositories.usuarios.ProductRepository;
import de.pecus.api.repositories.usuarios.SubstanceRepository;

/**
 * Resolves product and ingredient names for a batch of ids with one query each
 * (instead of one query per row).
 */
@Component
public class NameLookup {

	@Autowired
	private SubstanceRepository substanceRepository;

	@Autowired
	private ProductRepository productRepository;

	/** Map of ingredient (substance) id to name. Unknown ids are absent. */
	public Map<Long, String> ingredientNames(Collection<Long> ids) {
		Map<Long, String> names = new HashMap<>();
		for (SubstanceDO s : substanceRepository.findAllById(distinct(ids))) {
			names.put(s.getId(), s.getName());
		}
		return names;
	}

	/** Map of product id to name. Unknown ids are absent. */
	public Map<Long, String> productNames(Collection<Long> ids) {
		Map<Long, String> names = new HashMap<>();
		for (ProductDO p : productRepository.findAllById(distinct(ids))) {
			names.put(p.getId(), p.getName());
		}
		return names;
	}

	public boolean productExists(Long id) {
		return id != null && productRepository.findById(id) != null;
	}

	public boolean ingredientExists(Long id) {
		return id != null && substanceRepository.findById(id) != null;
	}

	private static List<Serializable> distinct(Collection<Long> ids) {
		Set<Long> unique = new LinkedHashSet<>(ids);
		unique.removeIf(Objects::isNull);
		return new ArrayList<>(unique);
	}
}
