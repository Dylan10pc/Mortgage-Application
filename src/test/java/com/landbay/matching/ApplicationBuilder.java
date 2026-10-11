package com.landbay.matching;

import java.math.BigDecimal;

import com.landbay.matching.domain.ApplicantType;
import com.landbay.matching.domain.Location;
import com.landbay.matching.domain.MortgageApplication;
import com.landbay.matching.domain.PropertyType;

/**
 * Test helper: builds a MortgageApplication starting from a sensible default, so each test only
 * states the one or two fields it cares about.
 *
 * <p>The defaults describe a healthy application: 75% LTV, an interest cover ratio of 1.4545 at a
 * 5.5% stress rate, an experienced individual landlord with a house in England.
 *
 * <p>Example: {@code anApplication().loan("250000").propertyType(PropertyType.HMO).build()}
 */
public final class ApplicationBuilder {

	// Defaults for every field. Each method below overrides one and returns this builder for chaining.
	private String caseReference = "CASE-1";
	private BigDecimal loanAmount = new BigDecimal("150000");
	private BigDecimal propertyValue = new BigDecimal("200000");
	private BigDecimal monthlyRent = new BigDecimal("1000");
	private PropertyType propertyType = PropertyType.HOUSE;
	private ApplicantType applicantType = ApplicantType.INDIVIDUAL;
	private Location location = Location.ENGLAND;
	private boolean hasLandlordExperience = true;

	private ApplicationBuilder() {
	}

	/** Starts a builder holding the default (healthy) application. */
	public static ApplicationBuilder anApplication() {
		return new ApplicationBuilder();
	}

	public ApplicationBuilder loan(String amount) {
		this.loanAmount = new BigDecimal(amount);
		return this;
	}

	public ApplicationBuilder value(String amount) {
		this.propertyValue = new BigDecimal(amount);
		return this;
	}

	public ApplicationBuilder rent(String amount) {
		this.monthlyRent = new BigDecimal(amount);
		return this;
	}

	public ApplicationBuilder propertyType(PropertyType propertyType) {
		this.propertyType = propertyType;
		return this;
	}

	public ApplicationBuilder applicantType(ApplicantType applicantType) {
		this.applicantType = applicantType;
		return this;
	}

	public ApplicationBuilder location(Location location) {
		this.location = location;
		return this;
	}

	public ApplicationBuilder experienced(boolean hasLandlordExperience) {
		this.hasLandlordExperience = hasLandlordExperience;
		return this;
	}

	/** Creates the immutable application. */
	public MortgageApplication build() {
		return new MortgageApplication(caseReference, loanAmount, propertyValue, monthlyRent,
				propertyType, applicantType, location, hasLandlordExperience);
	}

}
