package com.landbay.matching.domain;

import java.math.BigDecimal;

/**
 * An institutional funder (a bank or similar) that provides the money for a mortgage.
 * Landbay does not lend from its own balance sheet, so every offer needs a funder.
 *
 * <p>Holds its identity and its pricing. What each funder is willing to fund (its appetite)
 * is modelled separately as rules.
 *
 * <p>Pricing: a funder needing a higher return for the risk it takes adds a margin on top of the
 * product's rate. The rate on offer for a funded product is the product's indicative rate plus
 * the funder's margin.
 *
 * @param id         a stable machine-readable code, for example {@code HIGH_STREET_BANK}
 * @param name       a human-readable name
 * @param rateMargin extra annual interest the funder needs, as a fraction (0.0030 = 0.30%); zero is allowed
 */
public record Funder(String id, String name, BigDecimal rateMargin) {

	public Funder {
		// The id and name are required and must contain real text.
		Checks.requireText(id, "id");
		Checks.requireText(name, "name");

		// A margin may be zero (a funder happy with the product rate) but never negative.
		Checks.requireNotNegative(rateMargin, "rateMargin");
	}

}
