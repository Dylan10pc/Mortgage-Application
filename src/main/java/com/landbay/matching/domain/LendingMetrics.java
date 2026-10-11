package com.landbay.matching.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The two ratios lenders use to judge a buy-to-let loan: LTV (loan-to-value) and
 * ICR (interest cover ratio). Every method is pure: the same inputs always give the
 * same output, with no Spring and no stored state.
 *
 * <p>BigDecimal is used instead of double because money must be exact.
 */
public final class LendingMetrics {

	/** Ratios are reported to 4 decimal places, so 0.7500 means 75.00%. */
	static final int RATIO_SCALE = 4;

	// Utility class: only static methods, so nobody should create one.
	private LendingMetrics() {
	}

	/**
	 * Loan-to-value = loan amount / property value, e.g. 150,000 / 200,000 = 0.7500 (75% LTV).
	 * Rounds UP, so rounding can never make a risky loan look safer than it is.
	 */
	public static BigDecimal loanToValue(BigDecimal loanAmount, BigDecimal propertyValue) {
		// divide needs a scale and rounding mode, otherwise it throws on results like 1/3.
		return loanAmount.divide(propertyValue, RATIO_SCALE, RoundingMode.CEILING);
	}

	/** Annual rent = monthly rent x 12. BigDecimal is immutable, so multiply returns a new value. */
	public static BigDecimal annualRent(BigDecimal monthlyRent) {
		return monthlyRent.multiply(BigDecimal.valueOf(12));
	}

	/**
	 * Interest cover ratio = annual rent / annual interest, where annual interest = loan x stress rate.
	 * The stress rate is a deliberately pessimistic interest rate (e.g. 0.055 for 5.5%).
	 * Example: rent 1,000 a month, loan 150,000, stress rate 0.055 gives 12,000 / 8,250 = 1.4545.
	 * Rounds DOWN, the opposite of LTV, because a LOWER cover is the risky direction.
	 */
	public static BigDecimal interestCoverRatio(BigDecimal monthlyRent, BigDecimal loanAmount, BigDecimal stressRate) {
		// What the loan would cost in interest over a year at the stress rate.
		BigDecimal annualInterest = loanAmount.multiply(stressRate);

		// Dividing by zero or a negative number is meaningless, so reject it with a clear message.
		if (annualInterest.signum() <= 0) {
			throw new IllegalArgumentException("Annual interest must be positive");
		}

		return annualRent(monthlyRent).divide(annualInterest, RATIO_SCALE, RoundingMode.FLOOR);
	}

}
