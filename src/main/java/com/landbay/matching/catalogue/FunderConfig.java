package com.landbay.matching.catalogue;

import java.math.BigDecimal;
import java.util.List;

/**
 * One funder as written in the catalogue file: its pricing, the products it funds and its limits.
 *
 * @param id            stable machine-readable code, for example MERIDIAN_BANK
 * @param name          display name
 * @param rateMargin    extra annual interest the funder adds to a product's rate, as a fraction (0.003 = 0.30%)
 * @param fundsProducts ids of the products this funder is willing to fund at all
 * @param rules         the limits applied to the application, all of which must pass
 */
public record FunderConfig(String id, String name, BigDecimal rateMargin, List<String> fundsProducts,
		List<RuleConfig> rules) {

	public FunderConfig {
		// A missing list means "none given", which is easier to work with than null.
		fundsProducts = fundsProducts == null ? List.of() : List.copyOf(fundsProducts);
		rules = rules == null ? List.of() : List.copyOf(rules);
	}

}
