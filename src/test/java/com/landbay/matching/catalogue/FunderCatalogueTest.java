package com.landbay.matching.catalogue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.landbay.matching.domain.ApplicantType;
import com.landbay.matching.domain.Location;
import com.landbay.matching.domain.MortgageApplication;
import com.landbay.matching.domain.Product;
import com.landbay.matching.domain.PropertyType;
import com.landbay.matching.domain.Reason;
import com.landbay.matching.domain.ReasonCode;
import com.landbay.matching.eligibility.ProductCriteria;
import com.landbay.matching.funding.FunderCriteria;
import com.landbay.matching.funding.FunderEvaluation;

import static com.landbay.matching.ApplicationBuilder.anApplication;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks of the funders in the shipped catalogue.yml: the file is consistent, and each funder's
 * appetite behaves as described. Loaded exactly as the running application loads it.
 */
@SpringBootTest
class FunderCatalogueTest {

	@Autowired
	private Catalogue catalogue;

	// The product with this id, from the catalogue.
	private Product product(String id) {
		return catalogue.products().stream()
				.map(ProductCriteria::product)
				.filter(product -> product.id().equals(id))
				.findFirst().orElseThrow();
	}

	// The full evaluation of one named funder for a product and application.
	private FunderEvaluation evaluationBy(String funderId, String productId, MortgageApplication application) {
		return catalogue.funders().stream()
				.filter(criteria -> criteria.funder().id().equals(funderId))
				.findFirst().orElseThrow()
				.evaluate(product(productId), application);
	}

	// Ids of every funder able to fund this product and application.
	private List<String> fundersThatCanFund(String productId, MortgageApplication application) {
		return catalogue.funders().stream()
				.map(criteria -> criteria.evaluate(product(productId), application))
				.filter(FunderEvaluation::canFund)
				.map(evaluation -> evaluation.funder().id())
				.toList();
	}

	// ---- The catalogue itself ----

	@Test
	void hasThreeFundersWithUniqueIds() {
		List<String> ids = catalogue.funders().stream().map(criteria -> criteria.funder().id()).toList();

		assertThat(ids).containsExactlyInAnyOrder("MERIDIAN_BANK", "ATLAS_PENSION_FUND", "CORVUS_CREDIT_FUND");
	}

	@Test
	void everyProductAFunderListsReallyExists() {
		// Also checked at startup; this confirms the shipped file passes that check.
		Set<String> realProductIds = catalogue.products().stream()
				.map(ProductCriteria::product).map(Product::id).collect(Collectors.toSet());

		for (FunderCriteria criteria : catalogue.funders()) {
			assertThat(realProductIds).containsAll(criteria.fundedProductIds());
		}
	}

	@Test
	void everyProductIsFundedByAtLeastOneFunder() {
		for (ProductCriteria productCriteria : catalogue.products()) {
			String productId = productCriteria.product().id();

			assertThat(catalogue.funders().stream().anyMatch(f -> f.fundedProductIds().contains(productId)))
					.as("some funder should fund " + productId).isTrue();
		}
	}

	@Test
	void ratesAndMarginsAreReadExactlyFromTheFile() {
		assertThat(product("STANDARD_75").indicativeRate()).isEqualByComparingTo("0.0479");
		assertThat(catalogue.funders().stream()
				.filter(criteria -> criteria.funder().id().equals("CORVUS_CREDIT_FUND"))
				.findFirst().orElseThrow().funder().rateMargin()).isEqualByComparingTo("0.0075");
	}

	// ---- Product appetite ----

	@Test
	void theBankAndThePensionFundDoNotFundHmosSoOnlyTheCreditFundCan() {
		MortgageApplication hmo = anApplication().propertyType(PropertyType.HMO).loan("140000").rent("1200").build();

		assertThat(fundersThatCanFund("HMO_70", hmo)).containsExactly("CORVUS_CREDIT_FUND");

		// A funder that does not fund the product gives one clear reason, with the products it does fund.
		FunderEvaluation bank = evaluationBy("MERIDIAN_BANK", "HMO_70", hmo);
		assertThat(bank.failures()).hasSize(1);
		Reason reason = bank.failures().get(0);
		assertThat(reason.code()).isEqualTo(ReasonCode.PRODUCT_NOT_FUNDED);
		assertThat(reason.actual()).isEqualTo("HMO_70");
		assertThat(reason.requirement())
				.isEqualTo("one of FIRST_TIME_LANDLORD_70, LIMITED_COMPANY_75, STANDARD_65, STANDARD_75");
	}

	// ---- Risk and lending-type appetite ----

	@Test
	void theBankCapsLtvAtSeventyPercentSoItDeclinesStandard75AtSeventyFive() {
		MortgageApplication seventyFivePercent = anApplication().build();

		FunderEvaluation bank = evaluationBy("MERIDIAN_BANK", "STANDARD_75", seventyFivePercent);

		assertThat(bank.failures()).extracting(Reason::code).containsExactly(ReasonCode.LTV_TOO_HIGH);
		// The other two funders have no objection.
		assertThat(fundersThatCanFund("STANDARD_75", seventyFivePercent))
				.containsExactlyInAnyOrder("ATLAS_PENSION_FUND", "CORVUS_CREDIT_FUND");
	}

	@Test
	void theBankDoesNotLendInScotlandAndReportsEverythingItObjectsTo() {
		MortgageApplication scotland = anApplication().location(Location.SCOTLAND).build();

		FunderEvaluation bank = evaluationBy("MERIDIAN_BANK", "STANDARD_75", scotland);

		// Both the Scottish location and the 75% LTV, not just the first problem found.
		assertThat(bank.failures()).extracting(Reason::code)
				.containsExactlyInAnyOrder(ReasonCode.LOCATION_NOT_SUPPORTED, ReasonCode.LTV_TOO_HIGH);
	}

	@Test
	void thePensionFundOnlyLendsToExperiencedLandlords() {
		MortgageApplication firstTimer = anApplication().loan("120000").experienced(false).build();

		FunderEvaluation pensionFund = evaluationBy("ATLAS_PENSION_FUND", "STANDARD_65", firstTimer);

		assertThat(pensionFund.failures()).extracting(Reason::code)
				.containsExactly(ReasonCode.LANDLORD_EXPERIENCE_REQUIRED);
	}

	// ---- An option that is eligible but unfunded ----

	@Test
	void aProductCanBeEligibleYetHaveNoFunderWillingToFundIt() {
		// A company borrowing 1.8m (75% LTV) from a first-time landlord, against LIMITED_COMPANY_75.
		MortgageApplication large = anApplication().applicantType(ApplicantType.LIMITED_COMPANY)
				.loan("1800000").value("2400000").rent("12000").experienced(false).build();

		// The product itself is happy with it...
		ProductCriteria product = catalogue.products().stream()
				.filter(c -> c.product().id().equals("LIMITED_COMPANY_75")).findFirst().orElseThrow();
		assertThat(product.evaluate(large).eligible()).isTrue();

		// ...but the bank's loan limit, the pension fund's experience rule and the credit fund's loan limit rule out every funder.
		assertThat(fundersThatCanFund("LIMITED_COMPANY_75", large)).isEmpty();
		assertThat(evaluationBy("MERIDIAN_BANK", "LIMITED_COMPANY_75", large).failures())
				.extracting(Reason::code).contains(ReasonCode.LOAN_TOO_LARGE);
		assertThat(evaluationBy("CORVUS_CREDIT_FUND", "LIMITED_COMPANY_75", large).failures())
				.extracting(Reason::code).containsExactly(ReasonCode.LOAN_TOO_LARGE);

		// The same loan from an experienced landlord is fundable by the pension fund alone.
		MortgageApplication experienced = anApplication().applicantType(ApplicantType.LIMITED_COMPANY)
				.loan("1800000").value("2400000").rent("12000").experienced(true).build();
		assertThat(fundersThatCanFund("LIMITED_COMPANY_75", experienced))
				.containsExactly("ATLAS_PENSION_FUND");
	}

}
