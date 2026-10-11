package com.landbay.matching.eligibility;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.landbay.matching.domain.Reason;
import com.landbay.matching.domain.ReasonCode;

import static com.landbay.matching.ApplicationBuilder.anApplication;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoanAmountRuleTest {

	// Loans between 25,000 and 1,000,000 inclusive.
	private final LoanAmountRule rule = new LoanAmountRule(new BigDecimal("25000"), new BigDecimal("1000000"));

	@Test
	void passesForALoanInsideTheRange() {
		assertThat(rule.check(anApplication().loan("150000").build())).isEmpty();
	}

	@Test
	void passesExactlyOnBothBoundaries() {
		assertThat(rule.check(anApplication().loan("25000").build())).isEmpty();
		assertThat(rule.check(anApplication().loan("1000000").build())).isEmpty();
	}

	@Test
	void failsBelowTheMinimum() {
		Reason reason = rule.check(anApplication().loan("24999").build()).orElseThrow();

		assertThat(reason.code()).isEqualTo(ReasonCode.LOAN_TOO_SMALL);
		assertThat(reason.actual()).isEqualTo("24999");
		assertThat(reason.requirement()).isEqualTo(">= 25000");
	}

	@Test
	void failsAboveTheMaximum() {
		Reason reason = rule.check(anApplication().loan("1000001").value("2000000").build()).orElseThrow();

		assertThat(reason.code()).isEqualTo(ReasonCode.LOAN_TOO_LARGE);
		assertThat(reason.actual()).isEqualTo("1000001");
		assertThat(reason.requirement()).isEqualTo("<= 1000000");
	}

	@Test
	void rejectsARangeThatEndsBeforeItStarts() {
		assertThatThrownBy(() -> new LoanAmountRule(new BigDecimal("100"), new BigDecimal("50")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("maxLoan must not be less than minLoan");
	}

}
