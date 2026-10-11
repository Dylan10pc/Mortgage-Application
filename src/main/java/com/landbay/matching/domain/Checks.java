package com.landbay.matching.domain;

import java.math.BigDecimal;

/**
 * Small validation helpers shared by the records and rules, so each constructor
 * stays short and every error message is worded the same way.
 */
public final class Checks {

	// Utility class: it only holds static methods, so nobody should create one.
	private Checks() {
	}

	/** Rejects null, empty and whitespace-only text. */
	public static void requireText(String value, String name) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(name + " must not be blank");
		}
	}

	/** Rejects null, zero and negative amounts. (signum() returns -1, 0 or 1 for negative, zero, positive.) */
	public static void requirePositive(BigDecimal amount, String name) {
		if (amount == null || amount.signum() <= 0) {
			throw new IllegalArgumentException(name + " must be greater than zero");
		}
	}

	/** Rejects null and negative amounts, but allows zero. */
	public static void requireNotNegative(BigDecimal amount, String name) {
		if (amount == null || amount.signum() < 0) {
			throw new IllegalArgumentException(name + " must not be negative");
		}
	}

}
