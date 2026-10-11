package com.landbay.matching.eligibility;

import java.util.List;
import java.util.Objects;

import com.landbay.matching.domain.Product;
import com.landbay.matching.domain.Reason;

/**
 * The outcome of checking one application against one product: eligible, or not eligible
 * with every reason why.
 *
 * @param product  the product that was checked
 * @param failures every rule the application failed; empty means eligible
 */
public record ProductEvaluation(Product product, List<Reason> failures) {

	public ProductEvaluation {
		Objects.requireNonNull(product, "product must not be null");

		// Take an unmodifiable copy so the result cannot be changed after it is created.
		failures = List.copyOf(failures);
	}

	/** True when the application passed every rule. */
	public boolean eligible() {
		return failures.isEmpty();
	}

}
