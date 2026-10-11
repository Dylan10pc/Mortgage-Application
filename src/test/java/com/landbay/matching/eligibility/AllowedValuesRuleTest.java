package com.landbay.matching.eligibility;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import static com.landbay.matching.ApplicationBuilder.anApplication;
import com.landbay.matching.domain.ApplicantType;
import com.landbay.matching.domain.Location;
import com.landbay.matching.domain.MortgageApplication;
import com.landbay.matching.domain.PropertyType;
import com.landbay.matching.domain.Reason;
import com.landbay.matching.domain.ReasonCode;


class AllowedValuesRuleTest {

	@Test
	void passesWhenThePropertyTypeIsAllowed() {
		AllowedValuesRule<PropertyType> rule = AllowedValuesRule.propertyTypes(PropertyType.HOUSE, PropertyType.FLAT);

		// The default application is a HOUSE.
		assertThat(rule.check(anApplication().build())).isEmpty();
	}

	@Test
	void failsWhenThePropertyTypeIsNotAllowedAndListsWhatIs() {
		AllowedValuesRule<PropertyType> rule = AllowedValuesRule.propertyTypes(PropertyType.HOUSE, PropertyType.FLAT);

		Reason reason = rule.check(anApplication().propertyType(PropertyType.HMO).build()).orElseThrow();

		assertThat(reason.code()).isEqualTo(ReasonCode.PROPERTY_TYPE_NOT_ACCEPTED);
		assertThat(reason.actual()).isEqualTo("HMO");
		// Sorted alphabetically, so the text is the same every time whatever order the set holds.
		assertThat(reason.requirement()).isEqualTo("one of FLAT, HOUSE");
		assertThat(reason.message()).contains("Property type").contains("HMO");
	}

	@Test
	void failsWhenTheApplicantTypeIsNotAllowed() {
		AllowedValuesRule<ApplicantType> rule = AllowedValuesRule.applicantTypes(ApplicantType.LIMITED_COMPANY);

		Reason reason = rule.check(anApplication().applicantType(ApplicantType.INDIVIDUAL).build()).orElseThrow();

		assertThat(reason.code()).isEqualTo(ReasonCode.APPLICANT_TYPE_NOT_ACCEPTED);
		assertThat(reason.actual()).isEqualTo("INDIVIDUAL");
		assertThat(reason.requirement()).isEqualTo("one of LIMITED_COMPANY");
	}

	@Test
	void failsWhenTheLocationIsNotSupported() {
		AllowedValuesRule<Location> rule = AllowedValuesRule.locations(Location.ENGLAND, Location.WALES, Location.SCOTLAND);

		Reason reason = rule.check(anApplication().location(Location.NORTHERN_IRELAND).build()).orElseThrow();

		assertThat(reason.code()).isEqualTo(ReasonCode.LOCATION_NOT_SUPPORTED);
		assertThat(reason.actual()).isEqualTo("NORTHERN_IRELAND");
		assertThat(reason.requirement()).isEqualTo("one of ENGLAND, SCOTLAND, WALES");
	}

	@Test
	void rejectsAnEmptySetOfAllowedValues() {
		assertThatThrownBy(() -> new AllowedValuesRule<>(ReasonCode.LOCATION_NOT_SUPPORTED, "Location",
				MortgageApplication::location, Set.<Location>of()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("allowed must not be empty");
	}

}
