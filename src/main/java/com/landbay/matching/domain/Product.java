package com.landbay.matching.domain;

import java.math.BigDecimal;

/**
 * A Landbay mortgage product that an application might be eligible for.
 *
 * <p>Holds its identity, a display name and a headline rate. The lending criteria are
 * modelled separately as rules, so they can change (or later be loaded from
 * configuration) without touching this type.
 *
 * <p>Assumption: one indicative rate per product. A real rate also depends on fees, the loan
 * term, the revert rate and more; this is only enough to rank options by price.
 *
 * @param id             a stable machine-readable code, for example {@code STANDARD_75}
 * @param name           a human-readable name, for example "Standard Buy-to-Let 75%"
 * @param indicativeRate the headline annual interest rate as a fraction (0.0479 = 4.79%)
 */
public record Product(String id, String name, BigDecimal indicativeRate) {

	public Product {
		// The id and name are required and must contain real text.
		Checks.requireText(id, "id");
		Checks.requireText(name, "name");

		// A rate of zero or less would not be a real mortgage rate.
		Checks.requirePositive(indicativeRate, "indicativeRate");
	}

}
