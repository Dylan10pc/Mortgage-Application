package com.landbay.matching.catalogue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.landbay.matching.domain.ApplicantType;
import com.landbay.matching.domain.Location;
import com.landbay.matching.domain.MortgageApplication;
import com.landbay.matching.domain.PropertyType;
import com.landbay.matching.domain.Reason;
import com.landbay.matching.domain.ReasonCode;
import com.landbay.matching.eligibility.ProductCriteria;
import com.landbay.matching.eligibility.ProductEvaluation;

import static com.landbay.matching.ApplicationBuilder.anApplication;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end checks of the whole eligibility layer using the products in the shipped catalogue.yml:
 * real rules, loaded from configuration, against realistic applications.
 * The default test application is 75% LTV, interest cover 1.4545, an experienced individual with a house in England.
 */
@SpringBootTest
class ProductCatalogueTest {

	// The catalogue exactly as the running application loads it from catalogue.yml.
	@Autowired
	private Catalogue catalogue;

	// Ids of the products this application is eligible for, in catalogue order.
	private List<String> eligibleProductIds(MortgageApplication application) {
		return catalogue.products().stream()
				.map(criteria -> criteria.evaluate(application))
				.filter(ProductEvaluation::eligible)
				.map(evaluation -> evaluation.product().id())
				.toList();
	}

	// The full evaluation of one product for an application.
	private ProductEvaluation evaluationOf(String productId, MortgageApplication application) {
		return catalogue.products().stream()
				.filter(criteria -> criteria.product().id().equals(productId))
				.findFirst().orElseThrow()
				.evaluate(application);
	}

	@Test
	void hasSixProductsWithUniqueIds() {
		List<String> ids = catalogue.products().stream().map(ProductCriteria::product).map(p -> p.id()).toList();

		assertThat(ids).hasSize(6).doesNotHaveDuplicates();
	}

	@Test
	void aStandardApplicationAtSeventyFivePercentLtvOnlyQualifiesForStandard75() {
		assertThat(eligibleProductIds(anApplication().build())).containsExactly("STANDARD_75");
	}

	@Test
	void aLowerLtvOpensUpMoreProductsInCatalogueOrder() {
		// 120,000 / 200,000 = 60% LTV, interest cover 1.8181.
		assertThat(eligibleProductIds(anApplication().loan("120000").build()))
				.containsExactly("STANDARD_65", "STANDARD_75", "FIRST_TIME_LANDLORD_70");
	}

	@Test
	void aFirstTimeLandlordMissesStandard75AndIsToldWhy() {
		MortgageApplication firstTimer = anApplication().experienced(false).build();

		// Everything else about the application suits STANDARD_75, so experience is the only failure.
		assertThat(evaluationOf("STANDARD_75", firstTimer).failures())
				.extracting(Reason::code).containsExactly(ReasonCode.LANDLORD_EXPERIENCE_REQUIRED);
	}

	@Test
	void aFirstTimeLandlordExactlyOnTheLtvLimitStillQualifiesForTheProductsThatAllowIt() {
		// 130,000 / 200,000 = exactly 65% LTV, which is on the STANDARD_65 limit and so allowed.
		assertThat(eligibleProductIds(anApplication().loan("130000").experienced(false).build()))
				.containsExactly("STANDARD_65", "FIRST_TIME_LANDLORD_70");
	}

	@Test
	void aLimitedCompanyQualifiesForTheCompanyProductAndTheStandardOne() {
		assertThat(eligibleProductIds(anApplication().applicantType(ApplicantType.LIMITED_COMPANY).build()))
				.containsExactly("STANDARD_75", "LIMITED_COMPANY_75");
	}

	@Test
	void anHmoOnlyQualifiesForTheHmoProduct() {
		// 140,000 / 200,000 = exactly 70% LTV, rent 1,200 a month.
		assertThat(eligibleProductIds(anApplication()
				.propertyType(PropertyType.HMO).loan("140000").rent("1200").build()))
				.containsExactly("HMO_70");
	}

	@Test
	void aMultiUnitBlockOnlyQualifiesForTheBlockProduct() {
		// 1,400,000 / 2,000,000 = exactly 70% LTV, rent 9,000 a month.
		assertThat(eligibleProductIds(anApplication()
				.propertyType(PropertyType.MULTI_UNIT_BLOCK).loan("1400000").value("2000000").rent("9000").build()))
				.containsExactly("MULTI_UNIT_BLOCK_70");
	}

	@Test
	void northernIrelandIsDeclinedByEveryProductWithAClearReason() {
		MortgageApplication northernIreland = anApplication().location(Location.NORTHERN_IRELAND).build();

		assertThat(eligibleProductIds(northernIreland)).isEmpty();
		// Every single product reports the location as one of its failures.
		for (ProductCriteria criteria : catalogue.products()) {
			assertThat(criteria.evaluate(northernIreland).failures())
					.extracting(Reason::code).contains(ReasonCode.LOCATION_NOT_SUPPORTED);
		}
	}

	@Test
	void anApplicationThatFailsSeveralRulesReportsAllOfThem() {
		// Too much loan (150% LTV), far too little rent, and a first-time landlord, against STANDARD_75.
		MortgageApplication bad = anApplication().loan("300000").rent("100").experienced(false).build();

		assertThat(evaluationOf("STANDARD_75", bad).failures())
				.extracting(Reason::code)
				.containsExactlyInAnyOrder(ReasonCode.LTV_TOO_HIGH, ReasonCode.ICR_TOO_LOW, ReasonCode.LANDLORD_EXPERIENCE_REQUIRED);
	}

}
