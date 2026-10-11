package com.landbay.matching.domain;

/**
 * The kind of property being mortgaged. This is a closed list on purpose:
 * an unknown value is rejected at the edge of the system instead of
 * silently flowing into the eligibility rules.
 */
public enum PropertyType {

	/** An ordinary house let to one household. */
	HOUSE,

	/** A single flat or apartment. */
	FLAT,

	/** House in multiple occupation: let to several unrelated tenants who share facilities. Usually higher risk. */
	HMO,

	/** Multi-unit block: one freehold building containing several self-contained flats. */
	MULTI_UNIT_BLOCK
}
