package com.landbay.matching.domain;

/**
 * An institutional funder (a bank or similar) that provides the money for a mortgage.
 * Landbay does not lend from its own balance sheet, so every offer needs a funder.
 *
 * <p>Like {@link Product}, this holds only identity. What each funder is willing
 * to fund (its appetite) is modelled separately as rules.
 *
 * @param id   a stable machine-readable code, for example {@code HIGH_STREET_BANK}
 * @param name a human-readable name
 */
public record Funder(String id, String name) {

	public Funder {
		// Both fields are required and must contain real text.
		Checks.requireText(id, "id");
		Checks.requireText(name, "name");
	}

}
