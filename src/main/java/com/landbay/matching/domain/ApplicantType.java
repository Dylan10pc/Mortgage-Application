package com.landbay.matching.domain;

/**
 * Who is taking out the mortgage. Lenders and funders treat these
 * borrowers differently because the legal and tax position differs.
 */
public enum ApplicantType {

	/** A person borrowing in their own name. */
	INDIVIDUAL,

	/** A limited company, typically an SPV (special purpose vehicle) set up just to hold property. */
	LIMITED_COMPANY
}
