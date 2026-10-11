package com.landbay.matching.eligibility;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.landbay.matching.domain.Reason;
import com.landbay.matching.domain.ReasonCode;

import static com.landbay.matching.ApplicationBuilder.anApplication;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MinIcrRuleTest {

	// Shorthand so the numbers in the tests are easy to read.
	private static BigDecimal bd(String number) {
		return new BigDecimal(number);
	}

	// Require 125% cover, tested at a 5.5% stress rate.
	private final MinIcrRule rule = new MinIcrRule(bd("1.25"), bd("0.055"));

	@Test
	void passesWhenTheRentCoversTheInterestComfortably() {
		// The default application has an ICR of 1.4545 at 5.5%.
		assertThat(rule.check(anApplication().build())).isEmpty();
	}

	@Test
	void passesWhenTheCoverIsExactlyTheMinimum() {
		// Annual rent 7,500 / annual interest (120,000 x 0.05 = 6,000) = exactly 1.25.
		MinIcrRule exactRule = new MinIcrRule(bd("1.25"), bd("0.05"));

		assertThat(exactRule.check(anApplication().loan("120000").rent("625").build())).isEmpty();
	}

	@Test
	void failsJustBelowTheMinimumBecauseRoundingGoesDown() {
		// True ICR is 1.249989..., which rounds DOWN to 1.2499, so it must not sneak through as 1.25.
		MinIcrRule exactRule = new MinIcrRule(bd("1.25"), bd("0.05"));

		assertThat(exactRule.check(anApplication().loan("120001").rent("625").build())).isPresent();
	}

	@Test
	void failsWithAReasonThatExplainsWhy() {
		// Annual rent 6,000 / annual interest 8,250 = 0.7272...
		Reason reason = rule.check(anApplication().rent("500").build()).orElseThrow();

		assertThat(reason.code()).isEqualTo(ReasonCode.ICR_TOO_LOW);
		assertThat(reason.actual()).isEqualTo("0.7272");
		assertThat(reason.requirement()).isEqualTo(">= 1.25");
		assertThat(reason.message()).contains("0.7272").contains("1.25").contains("0.055");
	}

	@Test
	void rejectsANonPositiveMinimumOrStressRate() {
		assertThatThrownBy(() -> new MinIcrRule(BigDecimal.ZERO, bd("0.055")))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("minIcr must be greater than zero");
		assertThatThrownBy(() -> new MinIcrRule(bd("1.25"), bd("-0.01")))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("stressRate must be greater than zero");
	}

}
