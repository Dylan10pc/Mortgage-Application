package com.landbay.matching.domain;

import java.util.Objects;

/**
 * One reason an option is not available, with enough detail for another system to understand it.
 *
 * <p>Example: code {@code LTV_TOO_HIGH}, actual {@code 0.8000}, requirement {@code <= 0.75}.
 *
 * @param code        stable machine-readable code to switch on
 * @param message     human-readable explanation (wording may change; do not parse it)
 * @param actual      the value from the application that caused the failure, as text
 * @param requirement what would have passed, as text, for example {@code <= 0.75} or {@code one of FLAT, HOUSE}
 */
public record Reason(ReasonCode code, String message, String actual, String requirement) {

	public Reason {
		// Every part is required: a reason without detail would defeat its purpose.
		Objects.requireNonNull(code, "code must not be null");
		Checks.requireText(message, "message");
		Checks.requireText(actual, "actual");
		Checks.requireText(requirement, "requirement");
	}

}
