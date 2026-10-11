package com.landbay.matching.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A request for a buy-to-let mortgage: everything we know about the borrower,
 * the property and the loan. This is the input to the whole matching process.
 *
 * <p>It is a Java record, so it is immutable and the compiler generates the
 * constructor, accessors (for example {@code loanAmount()}), equals, hashCode and toString.
 *
 * <p>Assumption: all money amounts are in pounds sterling (GBP), so there is no currency field.
 *
 * @param caseReference       the caller's identifier for this case, used to trace it through logs and responses
 * @param loanAmount          how much the customer wants to borrow
 * @param propertyValue       the value of the property
 * @param monthlyRent         the expected monthly rental income
 * @param propertyType        what kind of property it is
 * @param applicantType       who is borrowing (individual or company)
 * @param location            where the property is
 * @param hasLandlordExperience whether the applicant has been a landlord before
 */
public record MortgageApplication(
		String caseReference,
		BigDecimal loanAmount,
		BigDecimal propertyValue,
		BigDecimal monthlyRent,
		PropertyType propertyType,
		ApplicantType applicantType,
		Location location,
		boolean hasLandlordExperience) {

	/**
	 * Compact constructor: runs every time a MortgageApplication is created, before the
	 * fields are assigned. If any check fails, the object never exists, so the rest of
	 * the code can trust that every application it receives is well-formed.
	 */
	public MortgageApplication {
		// The case reference must contain real text.
		Checks.requireText(caseReference, "caseReference");

		// A loan and a property must each have a value above zero.
		Checks.requirePositive(loanAmount, "loanAmount");
		Checks.requirePositive(propertyValue, "propertyValue");

		// Rent may be zero (the rules will then reject it with a clear reason), but never negative.
		Objects.requireNonNull(monthlyRent, "monthlyRent must not be null");
		if (monthlyRent.signum() < 0) {
			throw new IllegalArgumentException("monthlyRent must not be negative");
		}

		// Enums must be present. Closed enums mean any unknown value is rejected earlier, when the request is read.
		Objects.requireNonNull(propertyType, "propertyType must not be null");
		Objects.requireNonNull(applicantType, "applicantType must not be null");
		Objects.requireNonNull(location, "location must not be null");

		// Deliberately NOT checked: loanAmount > propertyValue. That is a valid (if risky) request
		// with an LTV over 100%. The eligibility rules decline it with a reason, rather than the
		// system treating it as a malformed request.
	}

	/**
	 * Loan-to-value ratio as a fraction, for example 0.7500 means 75% LTV.
	 * See {@link LendingMetrics#loanToValue} for the rounding rule.
	 */
	public BigDecimal loanToValue() {
		return LendingMetrics.loanToValue(loanAmount, propertyValue);
	}

	/**
	 * Interest cover ratio as a fraction, for example 1.2500 means rent covers interest 125%.
	 * The stress rate is supplied by the caller because it belongs to the product or funder,
	 * not to the application.
	 *
	 * @param stressRate the annual interest rate to test against, as a fraction (0.055 = 5.5%)
	 */
	public BigDecimal interestCoverRatio(BigDecimal stressRate) {
		return LendingMetrics.interestCoverRatio(monthlyRent, loanAmount, stressRate);
	}

}
