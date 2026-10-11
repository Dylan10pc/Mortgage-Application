package com.landbay.matching.domain;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;


class LendingMetricsTest {

	// Shorthand so the numbers in the tests are easy to read.
	private static BigDecimal bd(String number) {
		return new BigDecimal(number);
	}

	// ---- TODO 1: loanToValue ----

	@Test
	void loanToValueGivesTheExactRatio() {
		// 150,000 / 200,000 = 75%
		assertThat(LendingMetrics.loanToValue(bd("150000"), bd("200000"))).isEqualByComparingTo("0.7500");
	}

	@Test
	void loanToValueRoundsUpNeverDown() {
		// True value is 0.750005. Rounding UP gives 0.7501, which correctly fails a 75% limit.
		assertThat(LendingMetrics.loanToValue(bd("150001"), bd("200000"))).isEqualByComparingTo("0.7501");
	}

	@Test
	void loanToValueCanExceedOneHundredPercent() {
		assertThat(LendingMetrics.loanToValue(bd("250000"), bd("200000"))).isEqualByComparingTo("1.2500");
	}

	@Test
	void loanToValueAlwaysHasFourDecimalPlaces() {
		assertThat(LendingMetrics.loanToValue(bd("100000"), bd("200000")).scale()).isEqualTo(4);
	}

	// ---- TODO 2: annualRent ----

	@Test
	void annualRentIsTwelveMonths() {
		assertThat(LendingMetrics.annualRent(bd("1000"))).isEqualByComparingTo("12000");
	}

	// ---- TODO 3: interestCoverRatio ----

	@Test
	void interestCoverRatioGivesTheExactRatio() {
		// Annual rent 7,500. Annual interest 120,000 x 0.05 = 6,000. 7,500 / 6,000 = 125%.
		assertThat(LendingMetrics.interestCoverRatio(bd("625"), bd("120000"), bd("0.05"))).isEqualByComparingTo("1.2500");
	}

	@Test
	void interestCoverRatioRoundsDownNeverUp() {
		// True value is 7,500 / 6,000.05 = 1.249989... Rounding DOWN gives 1.2499, which correctly fails a 125% requirement.
		assertThat(LendingMetrics.interestCoverRatio(bd("625"), bd("120001"), bd("0.05"))).isEqualByComparingTo("1.2499");
	}

	@Test
	void interestCoverRatioForATypicalLoan() {
		// Annual rent 12,000. Annual interest 150,000 x 0.055 = 8,250. 12,000 / 8,250 = 1.4545...
		assertThat(LendingMetrics.interestCoverRatio(bd("1000"), bd("150000"), bd("0.055"))).isEqualByComparingTo("1.4545");
	}

	@Test
	void interestCoverRatioRejectsAZeroStressRate() {
		assertThatThrownBy(() -> LendingMetrics.interestCoverRatio(bd("1000"), bd("150000"), BigDecimal.ZERO))
				.isInstanceOf(IllegalArgumentException.class);
	}

	// ---- The same maths, reached through the application ----

	@Test
	void theApplicationDelegatesToTheseMetrics() {
		MortgageApplication application = new MortgageApplication("CASE-1", bd("150000"), bd("200000"), bd("1000"),
				PropertyType.HOUSE, ApplicantType.INDIVIDUAL, Location.ENGLAND, true);

		assertThat(application.loanToValue()).isEqualByComparingTo("0.7500");
		assertThat(application.interestCoverRatio(bd("0.055"))).isEqualByComparingTo("1.4545");
	}

}
