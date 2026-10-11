package com.landbay.matching.eligibility;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.landbay.matching.domain.Product;
import com.landbay.matching.domain.Reason;
import com.landbay.matching.domain.ReasonCode;

import static com.landbay.matching.ApplicationBuilder.anApplication;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductCriteriaTest {

	private final Product product = new Product("HMO_70", "HMO 70%", new BigDecimal("0.0549"));

	// A product with three rules: a loan size range, a 145% interest cover and experience required.
	private final ProductCriteria criteria = new ProductCriteria(product, List.of(
			new LoanAmountRule(new BigDecimal("25000"), new BigDecimal("1000000")),
			new MinIcrRule(new BigDecimal("1.45"), new BigDecimal("0.055")),
			new ExperiencedLandlordRule()));

	@Test
	void isEligibleWhenEveryRulePasses() {
		ProductEvaluation evaluation = criteria.evaluate(anApplication().build());

		assertThat(evaluation.eligible()).isTrue();
		assertThat(evaluation.failures()).isEmpty();
		assertThat(evaluation.product()).isEqualTo(product);
	}

	@Test
	void reportsEveryFailureNotJustTheFirst() {
		// Loan too small, rent too low for the interest, and a first-time landlord: three problems at once.
		ProductEvaluation evaluation = criteria.evaluate(
				anApplication().loan("10000").rent("10").experienced(false).build());

		assertThat(evaluation.eligible()).isFalse();
		// Failures come back in the same order as the rules.
		assertThat(evaluation.failures()).extracting(Reason::code).containsExactly(
				ReasonCode.LOAN_TOO_SMALL,
				ReasonCode.ICR_TOO_LOW,
				ReasonCode.LANDLORD_EXPERIENCE_REQUIRED);
	}

	@Test
	void aProductWithNoRulesAcceptsEveryone() {
		ProductCriteria openProduct = new ProductCriteria(product, List.of());

		assertThat(openProduct.evaluate(anApplication().build()).eligible()).isTrue();
	}

	@Test
	void rulesCanBeWrittenAsLambdasBecauseTheInterfaceHasOneMethod() {
		// A rule that always fails, written inline, to show the interface is easy to stub in tests.
		EligibilityRule alwaysFails = application -> Optional.of(
				new Reason(ReasonCode.LTV_TOO_HIGH, "always fails", "x", "y"));
		ProductCriteria strict = new ProductCriteria(product, List.of(alwaysFails));

		assertThat(strict.evaluate(anApplication().build()).eligible()).isFalse();
	}

	@Test
	void takesACopyOfTheRulesSoLaterChangesDoNotAffectIt() {
		List<EligibilityRule> mutable = new ArrayList<>();
		ProductCriteria copy = new ProductCriteria(product, mutable);

		// Adding a rule to the original list afterwards must not change the product's criteria.
		mutable.add(new ExperiencedLandlordRule());

		assertThat(copy.rules()).isEmpty();
	}

	@Test
	void rejectsMissingParts() {
		assertThatThrownBy(() -> new ProductCriteria(null, List.of()))
				.isInstanceOf(NullPointerException.class).hasMessage("product must not be null");
		assertThatThrownBy(() -> new ProductCriteria(product, null))
				.isInstanceOf(NullPointerException.class);
	}

}
