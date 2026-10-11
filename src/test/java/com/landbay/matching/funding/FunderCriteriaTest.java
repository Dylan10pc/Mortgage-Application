package com.landbay.matching.funding;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import static com.landbay.matching.ApplicationBuilder.anApplication;
import com.landbay.matching.domain.Funder;
import com.landbay.matching.domain.Product;
import com.landbay.matching.domain.Reason;
import com.landbay.matching.domain.ReasonCode;
import com.landbay.matching.eligibility.EligibilityRule;

class FunderCriteriaTest {

	private final Funder funder = new Funder("TEST_FUNDER", "Test Funder");
	private final Product funded = new Product("P1", "Product one");
	private final Product notFunded = new Product("P2", "Product two");

	// A rule that always passes, and a factory for rules that always fail with a chosen code.
	private static final EligibilityRule PASSES = application -> Optional.empty();

	private static EligibilityRule failsWith(ReasonCode code) {
		return application -> Optional.of(new Reason(code, "fails", "x", "y"));
	}

	@Test
	void canFundAProductItSupportsWhenEveryRulePasses() {
		FunderCriteria criteria = new FunderCriteria(funder, Set.of("P1"), List.of(PASSES));

		FunderEvaluation evaluation = criteria.evaluate(funded, anApplication().build());

		assertThat(evaluation.canFund()).isTrue();
		assertThat(evaluation.failures()).isEmpty();
		assertThat(evaluation.funder()).isEqualTo(funder);
	}

	@Test
	void declinesAProductItDoesNotFundWithASingleReasonAndIgnoresTheRules() {
		// The rule would fail, but it must not be reported: the funder never funds this product anyway.
		FunderCriteria criteria = new FunderCriteria(funder, Set.of("P1"), List.of(failsWith(ReasonCode.LTV_TOO_HIGH)));

		FunderEvaluation evaluation = criteria.evaluate(notFunded, anApplication().build());

		assertThat(evaluation.canFund()).isFalse();
		assertThat(evaluation.failures()).hasSize(1);
		Reason reason = evaluation.failures().get(0);
		assertThat(reason.code()).isEqualTo(ReasonCode.PRODUCT_NOT_FUNDED);
		assertThat(reason.actual()).isEqualTo("P2");
		assertThat(reason.requirement()).isEqualTo("one of P1");
		assertThat(reason.message()).contains("Test Funder").contains("Product two");
	}

	@Test
	void listsTheFundedProductsAlphabeticallySoTheTextIsStable() {
		FunderCriteria criteria = new FunderCriteria(funder, Set.of("B", "A", "C"), List.of());

		Reason reason = criteria.evaluate(notFunded, anApplication().build()).failures().get(0);

		assertThat(reason.requirement()).isEqualTo("one of A, B, C");
	}

	@Test
	void reportsEveryRuleThatFailsNotJustTheFirst() {
		FunderCriteria criteria = new FunderCriteria(funder, Set.of("P1"), List.of(
				failsWith(ReasonCode.LTV_TOO_HIGH), PASSES, failsWith(ReasonCode.ICR_TOO_LOW)));

		FunderEvaluation evaluation = criteria.evaluate(funded, anApplication().build());

		// Failures keep the order of the rules; the rule that passed contributes nothing.
		assertThat(evaluation.failures()).extracting(Reason::code)
				.containsExactly(ReasonCode.LTV_TOO_HIGH, ReasonCode.ICR_TOO_LOW);
	}

	@Test
	void rejectsAFunderThatFundsNothing() {
		assertThatThrownBy(() -> new FunderCriteria(funder, Set.of(), List.of()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("fundedProductIds must not be empty");
	}

	@Test
	void rejectsMissingParts() {
		assertThatThrownBy(() -> new FunderCriteria(null, Set.of("P1"), List.of()))
				.isInstanceOf(NullPointerException.class).hasMessage("funder must not be null");
	}

}
