package com.landbay.matching.domain;

import java.math.BigDecimal;

/**
 * Small validation helpers shared by the domain records, so each record's
 * constructor stays short and every error message is worded the same way.
 * Package-private: only the domain package needs these.
 */
final class Checks {

	// Utility class: it only holds static methods, so nobody should create one.
	private Checks() {
	}

	/** Rejects null, empty and whitespace-only text. */
	static void requireText(String value, String name) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(name + " must not be blank");
		}
	}

	/** Rejects null, zero and negative amounts. (signum() returns -1, 0 or 1 for negative, zero, positive.) */
	static void requirePositive(BigDecimal amount, String name) {
		if (amount == null || amount.signum() <= 0) {
			throw new IllegalArgumentException(name + " must be greater than zero");
		}
	}

}
