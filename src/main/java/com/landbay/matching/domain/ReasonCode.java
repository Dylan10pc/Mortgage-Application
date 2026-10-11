package com.landbay.matching.domain;

/**
 * Machine-readable codes for why an option is not available. Another system can switch on
 * these safely; the human-readable wording lives in {@link Reason#message()} and may change.
 *
 * <p>A closed enum on purpose: every possible failure is listed here, so nothing can be
 * reported with a misspelt or undocumented code.
 */
public enum ReasonCode {

	/** Loan-to-value is higher than the product allows. */
	LTV_TOO_HIGH,

	/** Rent does not cover the interest by enough (interest cover ratio too low). */
	ICR_TOO_LOW,

	/** Loan is smaller than the product's minimum. */
	LOAN_TOO_SMALL,

	/** Loan is larger than the product's maximum. */
	LOAN_TOO_LARGE,

	/** The product does not lend against this kind of property. */
	PROPERTY_TYPE_NOT_ACCEPTED,

	/** The product does not lend to this kind of applicant. */
	APPLICANT_TYPE_NOT_ACCEPTED,

	/** The product does not lend in this part of the UK. */
	LOCATION_NOT_SUPPORTED,

	/** The product requires the applicant to have been a landlord before. */
	LANDLORD_EXPERIENCE_REQUIRED
}
