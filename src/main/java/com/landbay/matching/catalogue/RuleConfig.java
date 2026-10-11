package com.landbay.matching.catalogue;

import java.math.BigDecimal;
import java.util.List;

/**
 * One rule as written in the catalogue file, for example
 * {@code { type: max-ltv, value: 0.75 }}.
 *
 * <p>Which parameters apply depends on the type, so every parameter is optional here and
 * {@link CatalogueFactory} checks that exactly the right ones are present.
 *
 * @param type       which kind of rule this is
 * @param value      a single number (used by max-ltv and min-icr)
 * @param min        a lower bound (used by loan-amount)
 * @param max        an upper bound (used by loan-amount)
 * @param stressRate the interest rate to test against (used by min-icr)
 * @param values     a list of allowed names (used by property-types, applicant-types and locations)
 */
public record RuleConfig(RuleType type, BigDecimal value, BigDecimal min, BigDecimal max,
		BigDecimal stressRate, List<String> values) {

	public RuleConfig {
		// A missing list means "none given", which is easier to work with than null.
		values = values == null ? List.of() : List.copyOf(values);
	}

}
