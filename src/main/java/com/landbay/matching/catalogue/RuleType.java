package com.landbay.matching.catalogue;

/**
 * The kinds of rule that can be written in the catalogue file. In the file they are spelt in
 * lower case with hyphens ({@code max-ltv}); Spring matches that to the constant here.
 *
 * <p>A closed list on purpose: a misspelt rule type stops the application starting, with an
 * error, instead of being silently ignored.
 */
public enum RuleType {

	/** Loan-to-value must not exceed {@code value}. */
	MAX_LTV,

	/** Interest cover must be at least {@code value}, tested at {@code stress-rate}. */
	MIN_ICR,

	/** Loan must be between {@code min} and {@code max}. */
	LOAN_AMOUNT,

	/** The applicant must have been a landlord before. Takes no parameters. */
	EXPERIENCED_LANDLORD,

	/** The property type must be one of {@code values}. */
	PROPERTY_TYPES,

	/** The applicant type must be one of {@code values}. */
	APPLICANT_TYPES,

	/** The property location must be one of {@code values}. */
	LOCATIONS
}
