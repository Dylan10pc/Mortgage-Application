package com.landbay.matching.match;

import java.util.Comparator;

import com.landbay.matching.eligibility.ProductEvaluation;
import com.landbay.matching.funding.FunderEvaluation;

/**
 * How results are ordered. Kept in one place, apart from the matching logic, so changing the
 * ordering (for example "now sort by something else") touches only this file.
 *
 * <p>The principles: cheapest first where there is a price, nearest to qualifying first where
 * there is not, and always a final tie-break on id so the same input gives the same output.
 *
 * <p>How comparators work: a {@code Comparator<T>} says which of two T comes first.
 * {@code Comparator.comparing(keyExtractor)} orders by a key taken from each item (the key must be
 * Comparable, as BigDecimal, String and Integer are; BigDecimal is compared by value, so 0.050 and
 * 0.05 are equal). {@code .thenComparing(...)} adds a tie-break used only when the first key is equal.
 *
 * <p>Gotcha: when a chain STARTS with a lambda, Java cannot work out its parameter type, so it is
 * given explicitly, as in {@code (FunderEvaluation e) -> ...}. Method references such as
 * {@code FundingOption::rate} carry their type, so they need no help.
 */
public final class ResultOrdering {

	/** Funding options for one product: cheapest rate first; if rates are equal, by funder id. */
	public static final Comparator<FundingOption> FUNDING_OPTIONS = Comparator
			.comparing(FundingOption::rate)
			.thenComparing(option -> option.funder().id());

	/** Funders that declined a product: fewest reasons first (nearest to saying yes), then by funder id. */
	public static final Comparator<FunderEvaluation> DECLINED_FUNDERS = Comparator
			.comparingInt((FunderEvaluation evaluation) -> evaluation.failures().size())
			.thenComparing(evaluation -> evaluation.funder().id());

	/** Products the application is ineligible for: fewest reasons first (nearest to qualifying), then by product id. */
	public static final Comparator<ProductEvaluation> INELIGIBLE_PRODUCTS = Comparator
			.comparingInt((ProductEvaluation evaluation) -> evaluation.failures().size())
			.thenComparing(evaluation -> evaluation.product().id());

	/**
	 * Eligible products, in three levels:
	 * <ol>
	 *   <li>Products a funder can fund come before products nobody can fund.</li>
	 *   <li>Then cheapest first, by the best (lowest) rate on offer. A product nobody can fund has no
	 *       offer, so its own indicative rate is used instead.</li>
	 *   <li>Then by product id.</li>
	 * </ol>
	 */
	public static final Comparator<EligibleProduct> ELIGIBLE_PRODUCTS = Comparator
			// false sorts before true, so "not fundable" = false for fundable products puts them first.
			.comparing((EligibleProduct product) -> !product.fundable())
			.thenComparing(product -> product.bestRate().orElse(product.product().indicativeRate()))
			.thenComparing(product -> product.product().id());

	// Utility class: only static members, so nobody should create one.
	private ResultOrdering() {
	}

}
