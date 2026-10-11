package com.landbay.matching.funding;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.landbay.matching.domain.Funder;
import com.landbay.matching.domain.MortgageApplication;
import com.landbay.matching.domain.Product;
import com.landbay.matching.domain.Reason;
import com.landbay.matching.domain.ReasonCode;
import com.landbay.matching.eligibility.EligibilityRule;

/**
 * A funder's appetite: which products it funds, and the limits it applies to the application.
 *
 * <p>It reuses {@link EligibilityRule}: a funder saying "no HMOs" or "maximum 70% LTV" is exactly the
 * same kind of check as a product's own criteria. A funder can only narrow what a product already
 * allows (product eligibility is checked first); it can never widen it.
 *
 * <p>Limitation: the rules apply to every product the funder supports. A funder that wanted a
 * different limit for each product would need the rules to be held per product instead.
 *
 * @param funder           the funder these criteria belong to
 * @param fundedProductIds ids of the products this funder is willing to fund at all
 * @param rules            the limits applied to the application for any of those products
 */
public record FunderCriteria(Funder funder, Set<String> fundedProductIds, List<EligibilityRule> rules) {

	public FunderCriteria {
		Objects.requireNonNull(funder, "funder must not be null");

		// Take unmodifiable copies so the caller cannot change them afterwards.
		fundedProductIds = Set.copyOf(fundedProductIds);
		rules = List.copyOf(rules);

		// A funder that funds nothing is surely a mistake.
		if (fundedProductIds.isEmpty()) {
			throw new IllegalArgumentException("fundedProductIds must not be empty");
		}
	}

	/**
	 * Decides whether this funder can fund this product for this application, and if not, why not.
	 */
	public FunderEvaluation evaluate(Product product, MortgageApplication application) {
		// A funder that does not fund the product at all gets ONE reason and nothing else: listing
		// LTV or location problems as well would be noise, since it would never fund this product.
		if (!fundedProductIds.contains(product.id())) {
			// Sorted, because a Set has no guaranteed order and the text must be the same every time.
			String funded = fundedProductIds.stream().sorted().collect(Collectors.joining(", "));
			return new FunderEvaluation(funder, List.of(new Reason(
					ReasonCode.PRODUCT_NOT_FUNDED,
					funder.name() + " does not fund " + product.name(),
					product.id(),
					"one of " + funded)));
		}

		// Otherwise run EVERY rule and keep the failures, so all objections are reported at once.
		List<Reason> failures = rules.stream()
				// Each rule answers with Optional<Reason>: empty when passed, a Reason when failed.
				.map(rule -> rule.check(application))
				// Unwrap the Optionals, dropping the empty ones (the rules that passed).
				.flatMap(Optional::stream)
				.toList();

		return new FunderEvaluation(funder, failures);
	}

}
