package com.landbay.matching.eligibility;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.landbay.matching.domain.MortgageApplication;
import com.landbay.matching.domain.Product;
import com.landbay.matching.domain.Reason;

/**
 * A product together with the lending criteria an application must meet to be eligible for it.
 *
 * @param product the product these criteria belong to
 * @param rules   every rule that must pass; the order only affects the order of reported failures
 */
public record ProductCriteria(Product product, List<EligibilityRule> rules) {

	public ProductCriteria {
		Objects.requireNonNull(product, "product must not be null");

		// Take an unmodifiable copy so the caller cannot change the rules afterwards (also rejects null rules).
		rules = List.copyOf(rules);
	}

	/**
	 * Runs EVERY rule, not just until the first failure, so the caller sees all the problems
	 * at once instead of fixing them one at a time.
	 */
	public ProductEvaluation evaluate(MortgageApplication application) {
		List<Reason> failures = rules.stream()
				// Each rule answers with Optional<Reason>: empty when passed, a Reason when failed.
				.map(rule -> rule.check(application))
				// Unwrap the Optionals, dropping the empty ones (the rules that passed).
				.flatMap(Optional::stream)
				.toList();

		return new ProductEvaluation(product, failures);
	}

}
