package com.landbay.matching.domain;

/**
 * A Landbay mortgage product that an application might be eligible for.
 *
 * <p>Deliberately holds only identity and a display name. The lending criteria are
 * modelled separately as rules, so they can change (or later be loaded from
 * configuration) without touching this type.
 *
 * @param id   a stable machine-readable code, for example {@code STANDARD_75}
 * @param name a human-readable name, for example "Standard Buy-to-Let 75%"
 */
public record Product(String id, String name) {

	public Product {
		// Both fields are required and must contain real text.
		Checks.requireText(id, "id");
		Checks.requireText(name, "name");
	}

}
