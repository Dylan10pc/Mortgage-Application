package com.landbay.matching.match;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landbay.matching.domain.Funder;
import com.landbay.matching.domain.Product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tests for the small result types: FundingOption, EligibleProduct and MatchResult. */
class MatchResultTypesTest {

	private static final Product PRODUCT = new Product("P1", "Product one", new BigDecimal("0.0450"));

	private static FundingOption option(String funderId, String rate) {
		return new FundingOption(new Funder(funderId, funderId, BigDecimal.ZERO), new BigDecimal(rate));
	}

	@Test
	void aFundingOptionNeedsAFunderAndAPositiveRate() {
		assertThatThrownBy(() -> new FundingOption(null, new BigDecimal("0.05")))
				.isInstanceOf(NullPointerException.class).hasMessage("funder must not be null");
		assertThatThrownBy(() -> new FundingOption(new Funder("F", "F", BigDecimal.ZERO), BigDecimal.ZERO))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("rate must be greater than zero");
	}

	@Test
	void aProductWithNoFundingOptionsIsNotFundable() {
		EligibleProduct unfunded = new EligibleProduct(PRODUCT, List.of(), List.of());

		assertThat(unfunded.fundable()).isFalse();
		assertThat(unfunded.bestRate()).isEmpty();
	}

	@Test
	void theBestRateIsTheLowestWhateverOrderTheOptionsAreIn() {
		EligibleProduct product = new EligibleProduct(PRODUCT, List.of(option("A", "0.0600"), option("B", "0.0500")), List.of());

		assertThat(product.fundable()).isTrue();
		assertThat(product.bestRate()).hasValueSatisfying(rate -> assertThat(rate).isEqualByComparingTo("0.0500"));
	}

	@Test
	void anEligibleProductTakesACopyOfItsLists() {
		List<FundingOption> mutable = new ArrayList<>();
		EligibleProduct product = new EligibleProduct(PRODUCT, mutable, List.of());

		// Adding to the original list afterwards must not change the result.
		mutable.add(option("A", "0.0600"));

		assertThat(product.fundingOptions()).isEmpty();
	}

	@Test
	void aMatchResultNeedsACaseReference() {
		assertThatThrownBy(() -> new MatchResult(" ", List.of(), List.of()))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("caseReference must not be blank");
	}

}
