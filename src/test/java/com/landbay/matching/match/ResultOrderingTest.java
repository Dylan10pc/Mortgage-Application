package com.landbay.matching.match;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.landbay.matching.domain.Funder;
import com.landbay.matching.domain.Product;
import com.landbay.matching.domain.Reason;
import com.landbay.matching.domain.ReasonCode;
import com.landbay.matching.eligibility.ProductEvaluation;
import com.landbay.matching.funding.FunderEvaluation;

import static org.assertj.core.api.Assertions.assertThat;

class ResultOrderingTest {

	// ---- Small builders so each test shows only the numbers that matter ----

	private static Funder funder(String id) {
		return new Funder(id, id, BigDecimal.ZERO);
	}

	private static Product product(String id, String rate) {
		return new Product(id, id, new BigDecimal(rate));
	}

	private static FundingOption option(String funderId, String rate) {
		return new FundingOption(funder(funderId), new BigDecimal(rate));
	}

	// A list containing the given number of (identical) failure reasons.
	private static List<Reason> reasons(int count) {
		return Collections.nCopies(count, new Reason(ReasonCode.LTV_TOO_HIGH, "message", "actual", "requirement"));
	}

	private static FunderEvaluation declined(String funderId, int reasonCount) {
		return new FunderEvaluation(funder(funderId), reasons(reasonCount));
	}

	private static ProductEvaluation ineligible(String productId, int reasonCount) {
		return new ProductEvaluation(product(productId, "0.05"), reasons(reasonCount));
	}

	// An eligible product that at least one funder can fund.
	private static EligibleProduct fundable(String productId, String productRate, FundingOption... options) {
		return new EligibleProduct(product(productId, productRate), List.of(options), List.of());
	}

	// An eligible product that no funder will fund.
	private static EligibleProduct unfunded(String productId, String productRate) {
		return new EligibleProduct(product(productId, productRate), List.of(), List.of());
	}

	// Sorts a copy of the items with the comparator and returns the ids, so tests read as "id order".
	private static <T> List<String> idsAfterSorting(List<T> items, Comparator<T> comparator, Function<T, String> id) {
		List<T> sorted = new ArrayList<>(items);
		sorted.sort(comparator);
		return sorted.stream().map(id).toList();
	}

	// ---- FUNDING_OPTIONS ----

	@Test
	void fundingOptionsAreOrderedCheapestFirst() {
		List<FundingOption> options = List.of(option("B", "0.0500"), option("A", "0.0450"), option("C", "0.0600"));

		assertThat(idsAfterSorting(options, ResultOrdering.FUNDING_OPTIONS, o -> o.funder().id()))
				.containsExactly("A", "B", "C");
	}

	@Test
	void fundingOptionsWithTheSameRateFallBackToFunderId() {
		// 0.05 and 0.0500 are the same rate, so the funder id decides.
		List<FundingOption> options = List.of(option("B", "0.05"), option("A", "0.0500"));

		assertThat(idsAfterSorting(options, ResultOrdering.FUNDING_OPTIONS, o -> o.funder().id()))
				.containsExactly("A", "B");
	}

	// ---- DECLINED_FUNDERS ----

	@Test
	void declinedFundersAreOrderedByFewestReasonsThenId() {
		List<FunderEvaluation> funders = List.of(declined("C", 1), declined("A", 3), declined("B", 1));

		assertThat(idsAfterSorting(funders, ResultOrdering.DECLINED_FUNDERS, e -> e.funder().id()))
				.containsExactly("B", "C", "A");
	}

	// ---- INELIGIBLE_PRODUCTS ----

	@Test
	void ineligibleProductsAreOrderedByFewestReasonsThenId() {
		List<ProductEvaluation> products = List.of(ineligible("C", 1), ineligible("A", 3), ineligible("B", 1));

		assertThat(idsAfterSorting(products, ResultOrdering.INELIGIBLE_PRODUCTS, e -> e.product().id()))
				.containsExactly("B", "C", "A");
	}

	// ---- ELIGIBLE_PRODUCTS ----

	@Test
	void aFundableProductComesBeforeAnUnfundableOneEvenIfTheUnfundableIsCheaper() {
		List<EligibleProduct> products = List.of(unfunded("CHEAP", "0.0300"), fundable("DEAR", "0.0400", option("F", "0.0600")));

		assertThat(idsAfterSorting(products, ResultOrdering.ELIGIBLE_PRODUCTS, p -> p.product().id()))
				.containsExactly("DEAR", "CHEAP");
	}

	@Test
	void fundableProductsAreOrderedByTheBestRateOnOfferNotByTheProductsOwnRate() {
		// P_LOW has the lower product rate (4%) but its funders charge 6% or 7%; P_HIGH (5%) is offered at 5%.
		List<EligibleProduct> products = List.of(
				fundable("P_LOW", "0.0400", option("X", "0.0700"), option("Y", "0.0600")),
				fundable("P_HIGH", "0.0500", option("Z", "0.0500")));

		assertThat(idsAfterSorting(products, ResultOrdering.ELIGIBLE_PRODUCTS, p -> p.product().id()))
				.containsExactly("P_HIGH", "P_LOW");
	}

	@Test
	void unfundableProductsAreOrderedByTheirOwnRate() {
		List<EligibleProduct> products = List.of(unfunded("A", "0.0600"), unfunded("B", "0.0500"));

		assertThat(idsAfterSorting(products, ResultOrdering.ELIGIBLE_PRODUCTS, p -> p.product().id()))
				.containsExactly("B", "A");
	}

	@Test
	void eligibleProductsOnTheSameBestRateFallBackToProductId() {
		List<EligibleProduct> products = List.of(
				fundable("B", "0.0400", option("F", "0.0500")),
				fundable("A", "0.0450", option("F", "0.0500")));

		assertThat(idsAfterSorting(products, ResultOrdering.ELIGIBLE_PRODUCTS, p -> p.product().id()))
				.containsExactly("A", "B");
	}

}
