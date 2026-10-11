package com.landbay.matching.eligibility;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import static com.landbay.matching.ApplicationBuilder.anApplication;
import com.landbay.matching.domain.Reason;
import com.landbay.matching.domain.ReasonCode;

class MaxLtvRuleTest {

	// The limit is written as 0.75 (two decimal places) on purpose: the application's LTV is 0.7500
	// (four decimal places), so this checks that the rule compares with compareTo and not equals.
	private final MaxLtvRule rule = new MaxLtvRule(new BigDecimal("0.75"));

	@Test
	void passesWhenTheLtvIsUnderTheLimit() {
		// 100,000 / 200,000 = 50%
		assertThat(rule.check(anApplication().loan("100000").build())).isEmpty();
	}

	@Test
	void passesWhenTheLtvIsExactlyTheLimitEvenThoughTheScalesDiffer() {
		// 150,000 / 200,000 = 0.7500, and the limit is 0.75. Same number, different scale.
		assertThat(rule.check(anApplication().loan("150000").build())).isEmpty();
	}

	@Test
	void failsJustOverTheLimit() {
		// 150,001 / 200,000 = 0.750005, which rounds UP to 0.7501, so it is over 0.75.
		Reason reason = rule.check(anApplication().loan("150001").build()).orElseThrow();

		assertThat(reason.code()).isEqualTo(ReasonCode.LTV_TOO_HIGH);
		assertThat(reason.actual()).isEqualTo("0.7501");
		assertThat(reason.requirement()).isEqualTo("<= 0.75");
		assertThat(reason.message()).contains("0.7501").contains("0.75");
	}

	@Test
	void failsWhenTheLoanIsLargerThanThePropertyIsWorth() {
		// 250,000 / 200,000 = 125% LTV. Declined with a reason, not treated as a malformed request.
		Reason reason = rule.check(anApplication().loan("250000").build()).orElseThrow();

		assertThat(reason.code()).isEqualTo(ReasonCode.LTV_TOO_HIGH);
		assertThat(reason.actual()).isEqualTo("1.2500");
	}

	@Test
	void rejectsANonPositiveLimit() {
		assertThatThrownBy(() -> new MaxLtvRule(BigDecimal.ZERO))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("maxLtv must be greater than zero");
	}

}
