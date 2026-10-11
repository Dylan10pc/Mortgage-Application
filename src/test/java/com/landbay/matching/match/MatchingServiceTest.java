package com.landbay.matching.match;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.landbay.matching.domain.ApplicantType;
import com.landbay.matching.domain.Funder;
import com.landbay.matching.domain.Location;
import com.landbay.matching.domain.Product;
import com.landbay.matching.domain.Reason;
import com.landbay.matching.domain.ReasonCode;
import com.landbay.matching.eligibility.ProductCriteria;
import com.landbay.matching.eligibility.ProductEvaluation;
import com.landbay.matching.funding.FunderCriteria;
import com.landbay.matching.funding.FunderEvaluation;

import static com.landbay.matching.ApplicationBuilder.anApplication;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end checks of the whole matching process: the products and funders from the shipped
 * catalogue.yml, realistic applications, and the matching service exactly as the application wires it.
 * The default test application is 75% LTV, interest cover 1.4545, an experienced individual with a house in England.
 */
@SpringBootTest
class MatchingServiceTest {

	@Autowired
	private MatchingService service;

	private static List<String> productIds(List<EligibleProduct> products) {
		return products.stream().map(p -> p.product().id()).toList();
	}

	private static List<String> funderIds(List<FundingOption> options) {
		return options.stream().map(o -> o.funder().id()).toList();
	}

	private static List<String> rates(List<FundingOption> options) {
		return options.stream().map(o -> o.rate().toPlainString()).toList();
	}

	private static List<String> declinedIds(List<FunderEvaluation> declined) {
		return declined.stream().map(e -> e.funder().id()).toList();
	}

	@Test
	void aStandardApplicationMatchesOneProductAndTwoOfTheThreeFunders() {
		MatchResult result = service.match(anApplication().build());

		assertThat(result.caseReference()).isEqualTo("CASE-1");
		assertThat(productIds(result.eligibleProducts())).containsExactly("STANDARD_75");

		EligibleProduct standard75 = result.eligibleProducts().get(0);
		// Cheapest first: 4.79% product rate plus each funder's margin (0.30% and 0.75%).
		assertThat(funderIds(standard75.fundingOptions())).containsExactly("ATLAS_PENSION_FUND", "CORVUS_CREDIT_FUND");
		assertThat(rates(standard75.fundingOptions())).containsExactly("0.0509", "0.0554");

		// The bank said no, and the result says exactly why.
		assertThat(declinedIds(standard75.declinedFunders())).containsExactly("MERIDIAN_BANK");
		assertThat(standard75.declinedFunders().get(0).failures())
				.extracting(Reason::code).containsExactly(ReasonCode.LTV_TOO_HIGH);
	}

	@Test
	void ineligibleProductsAreListedNearestToQualifyingFirstWithAnIdTieBreak() {
		MatchResult result = service.match(anApplication().build());

		// One reason each: FIRST_TIME_LANDLORD_70, LIMITED_COMPANY_75, STANDARD_65 (by id). Two reasons: HMO_70, MULTI_UNIT_BLOCK_70.
		assertThat(result.ineligibleProducts()).extracting(e -> e.product().id()).containsExactly(
				"FIRST_TIME_LANDLORD_70", "LIMITED_COMPANY_75", "STANDARD_65", "HMO_70", "MULTI_UNIT_BLOCK_70");
		// Each carries its reasons.
		assertThat(result.ineligibleProducts().get(0).failures()).isNotEmpty();
	}

	@Test
	void aLowerLtvOpensMoreProductsAndEachListsEveryFunderOnce() {
		// 120,000 / 200,000 = 60% LTV.
		MatchResult result = service.match(anApplication().loan("120000").build());

		// Cheapest best offer first.
		assertThat(productIds(result.eligibleProducts()))
				.containsExactly("STANDARD_65", "STANDARD_75", "FIRST_TIME_LANDLORD_70");

		// STANDARD_65: all three funders can fund it, cheapest (the bank) first.
		EligibleProduct standard65 = result.eligibleProducts().get(0);
		assertThat(funderIds(standard65.fundingOptions()))
				.containsExactly("MERIDIAN_BANK", "ATLAS_PENSION_FUND", "CORVUS_CREDIT_FUND");
		assertThat(rates(standard65.fundingOptions())).containsExactly("0.0439", "0.0459", "0.0504");

		// FIRST_TIME_LANDLORD_70: the pension fund does not fund it at all, and says so.
		EligibleProduct firstTime = result.eligibleProducts().get(2);
		assertThat(funderIds(firstTime.fundingOptions())).containsExactly("MERIDIAN_BANK", "CORVUS_CREDIT_FUND");
		assertThat(declinedIds(firstTime.declinedFunders())).containsExactly("ATLAS_PENSION_FUND");
		assertThat(firstTime.declinedFunders().get(0).failures())
				.extracting(Reason::code).containsExactly(ReasonCode.PRODUCT_NOT_FUNDED);

		// For every eligible product, each of the three funders appears exactly once: as an option or as a decline.
		for (EligibleProduct product : result.eligibleProducts()) {
			assertThat(product.fundingOptions().size() + product.declinedFunders().size()).isEqualTo(3);
		}

		// The rest are ineligible, one reason each, so ordered by id.
		assertThat(result.ineligibleProducts()).extracting(e -> e.product().id())
				.containsExactly("HMO_70", "LIMITED_COMPANY_75", "MULTI_UNIT_BLOCK_70");
	}

	@Test
	void aProductCanBeEligibleYetHaveNoFunderAndTheResultExplainsEachRefusal() {
		// A first-time-landlord company borrowing 1.8m (75% LTV): the product accepts it, no funder does.
		MatchResult result = service.match(anApplication().applicantType(ApplicantType.LIMITED_COMPANY)
				.loan("1800000").value("2400000").rent("12000").experienced(false).build());

		assertThat(productIds(result.eligibleProducts())).containsExactly("LIMITED_COMPANY_75");
		EligibleProduct product = result.eligibleProducts().get(0);

		assertThat(product.fundable()).isFalse();
		assertThat(product.fundingOptions()).isEmpty();
		// Nearest to saying yes first: pension fund (1 reason), credit fund (1 reason), bank (2 reasons).
		assertThat(declinedIds(product.declinedFunders()))
				.containsExactly("ATLAS_PENSION_FUND", "CORVUS_CREDIT_FUND", "MERIDIAN_BANK");
	}

	@Test
	void northernIrelandMatchesNothingButExplainsEverything() {
		MatchResult result = service.match(anApplication().location(Location.NORTHERN_IRELAND).build());

		assertThat(result.eligibleProducts()).isEmpty();
		// STANDARD_75 fails only on location, so it is nearest; the rest follow by reason count then id.
		assertThat(result.ineligibleProducts()).extracting(e -> e.product().id()).containsExactly(
				"STANDARD_75", "FIRST_TIME_LANDLORD_70", "LIMITED_COMPANY_75", "STANDARD_65", "HMO_70", "MULTI_UNIT_BLOCK_70");
		for (ProductEvaluation evaluation : result.ineligibleProducts()) {
			assertThat(evaluation.failures()).extracting(Reason::code).contains(ReasonCode.LOCATION_NOT_SUPPORTED);
		}
	}

	@Test
	void productsAreRankedByTheRateActuallyOnOfferNotTheirOwnHeadlineRate() {
		BigDecimal none = new BigDecimal("0.0000");
		Product low = new Product("P_LOW", "Low", new BigDecimal("0.0400"));
		Product high = new Product("P_HIGH", "High", new BigDecimal("0.0500"));
		Product orphan = new Product("P_ORPHAN", "Orphan", new BigDecimal("0.0300"));
		Funder cheap = new Funder("CHEAP", "Cheap", none);
		Funder pricey = new Funder("PRICEY", "Pricey", new BigDecimal("0.0200"));

		// Products with no rules accept everyone. The pricey funder funds only P_LOW; the cheap one only P_HIGH.
		MatchingService custom = new MatchingService(
				List.of(new ProductCriteria(low, List.of()), new ProductCriteria(high, List.of()), new ProductCriteria(orphan, List.of())),
				List.of(new FunderCriteria(cheap, Set.of("P_HIGH"), List.of()), new FunderCriteria(pricey, Set.of("P_LOW"), List.of())));

		MatchResult result = custom.match(anApplication().build());

		// P_HIGH is offered at 5%; P_LOW at 4% + 2% = 6%, so P_HIGH leads despite its dearer headline rate.
		// P_ORPHAN has the cheapest headline rate (3%) but nobody funds it, so it comes last.
		assertThat(productIds(result.eligibleProducts())).containsExactly("P_HIGH", "P_LOW", "P_ORPHAN");
	}

	@Test
	void theSameApplicationAlwaysGivesTheSameResult() {
		assertThat(service.match(anApplication().loan("120000").build()))
				.isEqualTo(service.match(anApplication().loan("120000").build()));
	}

	@Test
	void rejectsAMissingApplication() {
		assertThatThrownBy(() -> service.match(null))
				.isInstanceOf(NullPointerException.class).hasMessage("application must not be null");
	}

}
