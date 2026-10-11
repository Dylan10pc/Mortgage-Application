package com.landbay.matching.domain;

/**
 * Where the property is. Assumption: country-level is detailed enough for
 * this exercise. A real lender would likely need regions or postcodes
 * (for example to treat London differently), which is noted as an open question.
 */
public enum Location {

	ENGLAND,
	WALES,
	SCOTLAND,

	/** Included so that a product or funder can decline it and the API can explain why. */
	NORTHERN_IRELAND
}
