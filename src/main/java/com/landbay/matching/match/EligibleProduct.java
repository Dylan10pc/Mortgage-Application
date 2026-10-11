package com.landbay.matching.match;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.landbay.matching.domain.Product;
import com.landbay.matching.funding.FunderEvaluation;

/**
 * A product the application is eligible for, with who could and could not fund it.
 *
 * <p>A product can be eligible yet unfundable: the application meets the product's own criteria
 * but no funder will take it. That is a real outcome, and {@code declinedFunders} then explains why
 * each funder said no.
 *
 * @param product         the eligible product
 * @param fundingOptions  funders able to fund it, with their rates
 * @param declinedFunders funders unable to fund it, each with every reason why
 */
public record EligibleProduct(Product product, List<FundingOption> fundingOptions, List<FunderEvaluation> declinedFunders) {

	public EligibleProduct {
		Objects.requireNonNull(product, "product must not be null");

		// Take unmodifiable copies so the result cannot be changed after it is created.
		fundingOptions = List.copyOf(fundingOptions);
		declinedFunders = List.copyOf(declinedFunders);
	}

	/** True when at least one funder can fund this product for the application. */
	public boolean fundable() {
		return !fundingOptions.isEmpty();
	}

	/** The lowest rate on offer, or empty if no funder can fund the product. */
	public Optional<BigDecimal> bestRate() {
		return fundingOptions.stream()
				.map(FundingOption::rate)
				.min(Comparator.naturalOrder());
	}

}
