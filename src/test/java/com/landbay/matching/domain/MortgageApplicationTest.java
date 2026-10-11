package com.landbay.matching.domain;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MortgageApplicationTest {

	// Builds an application with sensible defaults so each test only spells out the field it cares about.
	private static MortgageApplication anApplication(String caseReference, String loan, String value, String rent) {
		return new MortgageApplication(
				caseReference,
				new BigDecimal(loan),
				new BigDecimal(value),
				new BigDecimal(rent),
				PropertyType.HOUSE,
				ApplicantType.INDIVIDUAL,
				Location.ENGLAND,
				true);
	}

	@Test
	void createsAValidApplication() {
		MortgageApplication application = anApplication("CASE-1", "150000", "200000", "1000");

		// Records generate accessors named after the fields.
		assertThat(application.caseReference()).isEqualTo("CASE-1");
		assertThat(application.loanAmount()).isEqualByComparingTo("150000");
		assertThat(application.propertyType()).isEqualTo(PropertyType.HOUSE);
		assertThat(application.hasLandlordExperience()).isTrue();
	}

	// Covers null, "" and "   " (whitespace only).
	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = "   ")
	void rejectsABlankCaseReference(String caseReference) {
		assertThatThrownBy(() -> anApplication(caseReference, "150000", "200000", "1000"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("caseReference must not be blank");
	}

	@ParameterizedTest
	@ValueSource(strings = { "0", "-1" })
	void rejectsALoanAmountThatIsNotPositive(String loan) {
		assertThatThrownBy(() -> anApplication("CASE-1", loan, "200000", "1000"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("loanAmount must be greater than zero");
	}

	@ParameterizedTest
	@ValueSource(strings = { "0", "-1" })
	void rejectsAPropertyValueThatIsNotPositive(String value) {
		assertThatThrownBy(() -> anApplication("CASE-1", "150000", value, "1000"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("propertyValue must be greater than zero");
	}

	@Test
	void rejectsNegativeRent() {
		assertThatThrownBy(() -> anApplication("CASE-1", "150000", "200000", "-1"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("monthlyRent must not be negative");
	}

	@Test
	void acceptsZeroRentSoTheRulesCanDeclineItWithAReason() {
		assertThatCode(() -> anApplication("CASE-1", "150000", "200000", "0")).doesNotThrowAnyException();
	}

	@Test
	void acceptsALoanLargerThanThePropertyValue() {
		// LTV over 100% is a valid request that the eligibility rules decline later, not a malformed one.
		assertThatCode(() -> anApplication("CASE-1", "250000", "200000", "1000")).doesNotThrowAnyException();
	}

	@Test
	void rejectsMissingRequiredFields() {
		// Each of these passes null for exactly one required field.
		assertThatThrownBy(() -> new MortgageApplication("CASE-1", new BigDecimal("1"), new BigDecimal("1"),
				null, PropertyType.HOUSE, ApplicantType.INDIVIDUAL, Location.ENGLAND, false))
				.isInstanceOf(NullPointerException.class).hasMessage("monthlyRent must not be null");
		assertThatThrownBy(() -> new MortgageApplication("CASE-1", new BigDecimal("1"), new BigDecimal("1"),
				BigDecimal.ONE, null, ApplicantType.INDIVIDUAL, Location.ENGLAND, false))
				.isInstanceOf(NullPointerException.class).hasMessage("propertyType must not be null");
		assertThatThrownBy(() -> new MortgageApplication("CASE-1", new BigDecimal("1"), new BigDecimal("1"),
				BigDecimal.ONE, PropertyType.HOUSE, null, Location.ENGLAND, false))
				.isInstanceOf(NullPointerException.class).hasMessage("applicantType must not be null");
		assertThatThrownBy(() -> new MortgageApplication("CASE-1", new BigDecimal("1"), new BigDecimal("1"),
				BigDecimal.ONE, PropertyType.HOUSE, ApplicantType.INDIVIDUAL, null, false))
				.isInstanceOf(NullPointerException.class).hasMessage("location must not be null");
	}

}
