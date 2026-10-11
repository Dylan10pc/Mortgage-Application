package com.landbay.matching.catalogue;

import java.math.BigDecimal;
import java.util.List;

/**
 * One product as written in the catalogue file: its details and the rules an application must meet.
 *
 * @param id             stable machine-readable code, for example STANDARD_75
 * @param name           display name
 * @param indicativeRate headline annual rate as a fraction (0.0479 = 4.79%)
 * @param rules          the criteria, all of which must pass
 */
public record ProductConfig(String id, String name, BigDecimal indicativeRate, List<RuleConfig> rules) {

	public ProductConfig {
		// A missing list means "none given", which is easier to work with than null.
		rules = rules == null ? List.of() : List.copyOf(rules);
	}

}
