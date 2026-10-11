package com.landbay.matching.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The two ratios lenders use to judge a buy-to-let loan: LTV and ICR.
 *
 * <p>THIS FILE IS YOURS TO WRITE. Fill in the three methods marked TODO, then remove
 * the {@code @Disabled} line from {@code LendingMetricsTest} and run {@code ./mvnw verify}.
 * Every method here is pure: same inputs always give the same output, no Spring, no state.
 *
 * <p>Why BigDecimal and not double: money must be exact. {@code 0.1 + 0.2} in a double is
 * {@code 0.30000000000000004}; in a BigDecimal it is exactly {@code 0.3}.
 */
public final class LendingMetrics {

	/** Ratios are reported to 4 decimal places, so 0.7500 means 75.00%. */
	static final int RATIO_SCALE = 4;

	// Utility class: only static methods, so nobody should create one.
	private LendingMetrics() {
	}

	/**
	 * TODO 1: Loan-to-value (LTV) = loanAmount / propertyValue.
	 *
	 * <p>Example: 150,000 / 200,000 = 0.7500 (75% LTV). Higher LTV means more risk.
	 *
	 * <p>Rounding rule: ROUND UP (RoundingMode.CEILING). Lenders round in the cautious
	 * direction. If a true LTV of 75.0005% were rounded down to 75.00%, a loan that is
	 * slightly over a 75% limit would wrongly pass.
	 *
	 * <p>Hint: BigDecimal.divide needs to know how many decimal places to keep and how to round,
	 * otherwise it throws an ArithmeticException on results like 1/3. Use the 3-argument version:
	 * {@code a.divide(b, RATIO_SCALE, RoundingMode.CEILING)}.
	 */
	public static BigDecimal loanToValue(BigDecimal loanAmount, BigDecimal propertyValue) {

		return loanAmount.divide(propertyValue, RATIO_SCALE, RoundingMode.CEILING);
	}

	/**
	 * TODO 2: Annual rent = monthlyRent x 12.
	 *
	 * <p>Hint: BigDecimal is an object, so there is no {@code *} operator. Use
	 * {@code monthlyRent.multiply(BigDecimal.valueOf(12))}. BigDecimal is immutable:
	 * multiply returns a NEW value and leaves the original unchanged.
	 */
	public static BigDecimal annualRent(BigDecimal monthlyRent) {
		return monthlyRent.multiply(BigDecimal.valueOf(12));
	}

	/**
	 * TODO 3: Interest cover ratio (ICR) = annualRent / annualInterest, where
	 * annualInterest = loanAmount x stressRate.
	 *
	 * <p>ICR asks "does the rent comfortably cover the interest?" Lenders want a ratio such as
	 * 1.25 (125%) or more. The stressRate is a deliberately pessimistic interest rate (for example
	 * 0.055 for 5.5%) so the loan still works if rates rise.
	 *
	 * <p>Example: rent 1,000 a month, loan 150,000, stress rate 0.055.
	 * Annual rent 12,000. Annual interest 8,250. ICR = 12,000 / 8,250 = 1.4545.
	 *
	 * <p>Rounding rule: ROUND DOWN (RoundingMode.FLOOR). This is the opposite direction to LTV
	 * because here a LOWER number is the risky one. Rounding down never makes the cover look
	 * better than it is.
	 *
	 * <p>Guard: if the annual interest is zero or negative (a zero loan or a zero stress rate),
	 * the division is meaningless, so throw an IllegalArgumentException with a clear message.
	 * Hint: {@code annualInterest.signum() <= 0}.
	 *
	 * <p>Hint: reuse your annualRent method, then divide with
	 * {@code RATIO_SCALE} and {@code RoundingMode.FLOOR}.
	 */
	public static BigDecimal interestCoverRatio(BigDecimal monthlyRent, BigDecimal loanAmount, BigDecimal stressRate) {
		BigDecimal annualInterest = loanAmount.multiply(stressRate);
		if (annualInterest.signum() <= 0) {
			throw new IllegalArgumentException("Annual interest must be positive");
		}
		return annualRent(monthlyRent).divide(annualInterest, RATIO_SCALE, RoundingMode.FLOOR);
	}

}
