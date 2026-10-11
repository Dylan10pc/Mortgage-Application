package com.landbay.matching.catalogue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landbay.matching.domain.ReasonCode;
import com.landbay.matching.eligibility.AllowedValuesRule;
import com.landbay.matching.eligibility.EligibilityRule;
import com.landbay.matching.eligibility.ExperiencedLandlordRule;
import com.landbay.matching.eligibility.LoanAmountRule;
import com.landbay.matching.eligibility.MaxLtvRule;
import com.landbay.matching.eligibility.MinIcrRule;
import com.landbay.matching.eligibility.ProductCriteria;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests of the catalogue checking: what is accepted, and that every kind of mistake in the file is
 * reported, with where it is, and all together.
 */
class CatalogueFactoryTest {

	// ---- Small builders so each test shows only what it is about ----

	private static BigDecimal bd(String number) {
		return new BigDecimal(number);
	}

	private static RuleConfig maxLtv(String value) {
		return new RuleConfig(RuleType.MAX_LTV, bd(value), null, null, null, null);
	}

	private static RuleConfig minIcr(String value, String stressRate) {
		return new RuleConfig(RuleType.MIN_ICR, bd(value), null, null, bd(stressRate), null);
	}

	private static RuleConfig loanAmount(String min, String max) {
		return new RuleConfig(RuleType.LOAN_AMOUNT, null, bd(min), bd(max), null, null);
	}

	private static RuleConfig experienced() {
		return new RuleConfig(RuleType.EXPERIENCED_LANDLORD, null, null, null, null, null);
	}

	private static RuleConfig values(RuleType type, String... names) {
		return new RuleConfig(type, null, null, null, null, List.of(names));
	}

	private static ProductConfig product(String id, RuleConfig... rules) {
		return new ProductConfig(id, "Product " + id, bd("0.05"), List.of(rules));
	}

	private static FunderConfig funder(String id, List<String> fundsProducts, RuleConfig... rules) {
		return new FunderConfig(id, "Funder " + id, bd("0.001"), fundsProducts, List.of(rules));
	}

	private static CatalogueProperties properties(List<ProductConfig> products, List<FunderConfig> funders) {
		return new CatalogueProperties(products, funders);
	}

	// A small valid catalogue: one product funded by one funder.
	private static CatalogueProperties valid() {
		return properties(List.of(product("P1", maxLtv("0.75"))), List.of(funder("F1", List.of("P1"))));
	}

	// Builds a catalogue that must be rejected, and returns the problems it was rejected for.
	private static List<String> problemsOf(CatalogueProperties properties) {
		try {
			CatalogueFactory.build(properties);
		} catch (InvalidCatalogueException e) {
			return e.problems();
		}
		throw new AssertionError("expected the catalogue to be rejected, but it was accepted");
	}

	// ---- A valid file ----

	@Test
	void buildsProductsAndFundersInTheOrderTheyAreWritten() {
		Catalogue catalogue = CatalogueFactory.build(properties(
				List.of(product("P2", maxLtv("0.7")), product("P1", maxLtv("0.8"))),
				List.of(funder("F2", List.of("P1", "P2")), funder("F1", List.of("P1")))));

		assertThat(catalogue.products()).extracting(criteria -> criteria.product().id()).containsExactly("P2", "P1");
		assertThat(catalogue.funders()).extracting(criteria -> criteria.funder().id()).containsExactly("F2", "F1");
		assertThat(catalogue.funders().get(0).fundedProductIds()).containsExactlyInAnyOrder("P1", "P2");
	}

	@Test
	void everyRuleTypeBecomesTheMatchingRuleObjectWithItsValues() {
		Catalogue catalogue = CatalogueFactory.build(properties(
				List.of(product("P1",
						maxLtv("0.75"), minIcr("1.25", "0.055"), loanAmount("25000", "1000000"), experienced(),
						values(RuleType.PROPERTY_TYPES, "HOUSE", "FLAT"),
						values(RuleType.APPLICANT_TYPES, "INDIVIDUAL"),
						values(RuleType.LOCATIONS, "ENGLAND", "WALES"))),
				List.of(funder("F1", List.of("P1")))));

		List<EligibilityRule> rules = catalogue.products().get(0).rules();

		assertThat(rules).hasSize(7);
		assertThat(((MaxLtvRule) rules.get(0)).maxLtv()).isEqualByComparingTo("0.75");
		assertThat(((MinIcrRule) rules.get(1)).minIcr()).isEqualByComparingTo("1.25");
		assertThat(((MinIcrRule) rules.get(1)).stressRate()).isEqualByComparingTo("0.055");
		assertThat(((LoanAmountRule) rules.get(2)).minLoan()).isEqualByComparingTo("25000");
		assertThat(((LoanAmountRule) rules.get(2)).maxLoan()).isEqualByComparingTo("1000000");
		assertThat(rules.get(3)).isInstanceOf(ExperiencedLandlordRule.class);
		assertThat(((AllowedValuesRule<?>) rules.get(4)).failureCode()).isEqualTo(ReasonCode.PROPERTY_TYPE_NOT_ACCEPTED);
		assertThat(((AllowedValuesRule<?>) rules.get(4)).allowed()).hasSize(2);
		assertThat(((AllowedValuesRule<?>) rules.get(5)).failureCode()).isEqualTo(ReasonCode.APPLICANT_TYPE_NOT_ACCEPTED);
		assertThat(((AllowedValuesRule<?>) rules.get(6)).failureCode()).isEqualTo(ReasonCode.LOCATION_NOT_SUPPORTED);
	}

	@Test
	void aFunderWithNoRulesOfItsOwnIsAllowed() {
		Catalogue catalogue = CatalogueFactory.build(properties(
				List.of(product("P1", maxLtv("0.75"))), List.of(funder("F1", List.of("P1")))));

		assertThat(catalogue.funders().get(0).rules()).isEmpty();
	}

	// ---- Mistakes in a rule ----

	@Test
	void reportsAMissingParameterWithWhereItIs() {
		CatalogueProperties bad = properties(
				List.of(product("P1", new RuleConfig(RuleType.MAX_LTV, null, null, null, null, null))),
				List.of(funder("F1", List.of("P1"))));

		assertThat(problemsOf(bad)).containsExactly("products[0] (P1) > rules[0] (max-ltv): requires 'value'");
	}

	@Test
	void reportsAParameterTheRuleDoesNotUse() {
		// A 'value' on a loan-amount rule is probably a typo or a parameter on the wrong rule.
		RuleConfig confused = new RuleConfig(RuleType.LOAN_AMOUNT, bd("5"), bd("25000"), bd("1000000"), null, null);

		assertThat(problemsOf(properties(List.of(product("P1", confused)), List.of(funder("F1", List.of("P1"))))))
				.containsExactly("products[0] (P1) > rules[0] (loan-amount): does not use 'value'");
	}

	@Test
	void reportsAMissingRuleType() {
		RuleConfig untyped = new RuleConfig(null, bd("0.75"), null, null, null, null);

		assertThat(problemsOf(properties(List.of(product("P1", untyped)), List.of(funder("F1", List.of("P1"))))))
				.containsExactly("products[0] (P1) > rules[0]: 'type' is required");
	}

	@Test
	void reportsAnUnknownEnumNameAndListsTheAcceptedOnes() {
		RuleConfig bungalow = values(RuleType.PROPERTY_TYPES, "HOUSE", "BUNGALOW");

		assertThat(problemsOf(properties(List.of(product("P1", bungalow)), List.of(funder("F1", List.of("P1"))))))
				.containsExactly("products[0] (P1) > rules[0] (property-types): 'BUNGALOW' is not a valid PropertyType; "
						+ "accepted values: HOUSE, FLAT, HMO, MULTI_UNIT_BLOCK");
	}

	@Test
	void reportsValuesTheDomainRejectsWithWhereTheyAre() {
		ProductConfig zeroRate = new ProductConfig("P1", "Product P1", BigDecimal.ZERO, List.of(maxLtv("-1")));

		assertThat(problemsOf(properties(List.of(zeroRate), List.of(funder("F1", List.of("P1"))))))
				.containsExactly(
						"products[0] (P1): indicativeRate must be greater than zero",
						"products[0] (P1) > rules[0] (max-ltv): maxLtv must be greater than zero");
	}

	// ---- Mistakes in a product or funder ----

	@Test
	void reportsDuplicateIds() {
		CatalogueProperties bad = properties(
				List.of(product("P1", maxLtv("0.75")), product("P1", maxLtv("0.7"))),
				List.of(funder("F1", List.of("P1")), funder("F1", List.of("P1"))));

		assertThat(problemsOf(bad)).containsExactly(
				"products[1] (P1): the id 'P1' is used by more than one product",
				"funders[1] (F1): the id 'F1' is used by more than one funder");
	}

	@Test
	void reportsAFunderListingAProductThatDoesNotExist() {
		CatalogueProperties bad = properties(
				List.of(product("P1", maxLtv("0.75"))),
				List.of(funder("F1", List.of("P1", "P9"))));

		assertThat(problemsOf(bad)).containsExactly(
				"funders[0] (F1): 'funds-products' lists 'P9', which is not a product in the catalogue");
	}

	@Test
	void reportsAFunderThatFundsNothing() {
		CatalogueProperties bad = properties(List.of(product("P1", maxLtv("0.75"))), List.of(funder("F1", List.of())));

		assertThat(problemsOf(bad)).containsExactly("funders[0] (F1): 'funds-products' must list at least one product");
	}

	@Test
	void reportsAProductWithNoRulesBecauseItWouldAcceptEveryone() {
		CatalogueProperties bad = properties(List.of(product("P1")), List.of(funder("F1", List.of("P1"))));

		assertThat(problemsOf(bad)).containsExactly("products[0] (P1): at least one rule is required");
	}

	@Test
	void reportsAnEmptyCatalogue() {
		assertThat(problemsOf(properties(List.of(), List.of()))).containsExactly(
				"products: at least one product is required",
				"funders: at least one funder is required");
	}

	// ---- All problems at once ----

	@Test
	void reportsEveryProblemTogetherInOneException() {
		CatalogueProperties bad = properties(
				List.of(
						product("P1", new RuleConfig(RuleType.MAX_LTV, null, null, null, null, null)),
						product("P2", values(RuleType.LOCATIONS, "ATLANTIS"))),
				List.of(funder("F1", List.of("NOPE"))));

		InvalidCatalogueException thrown = null;
		try {
			CatalogueFactory.build(bad);
		} catch (InvalidCatalogueException e) {
			thrown = e;
		}

		assertThat(thrown).isNotNull();
		assertThat(thrown.problems()).hasSize(3);
		// The message is what an operator sees when the application refuses to start.
		assertThat(thrown.getMessage())
				.startsWith("The product and funder catalogue is invalid (3 problems):")
				.contains("products[0] (P1) > rules[0] (max-ltv): requires 'value'")
				.contains("products[1] (P2) > rules[0] (locations): 'ATLANTIS' is not a valid Location")
				.contains("funders[0] (F1): 'funds-products' lists 'NOPE'");
	}

	@Test
	void aSingleProblemIsNotPluralised() {
		assertThat(problemsOf(properties(List.of(), List.of(funder("F1", List.of("P1"))))))
				.isNotEmpty();
		try {
			CatalogueFactory.build(properties(List.of(product("P1")), List.of(funder("F1", List.of("P1")))));
		} catch (InvalidCatalogueException e) {
			assertThat(e.getMessage()).startsWith("The product and funder catalogue is invalid (1 problem):");
		}
	}

	@Test
	void aValidCatalogueDoesNotThrow() {
		assertThat(CatalogueFactory.build(valid()).products()).extracting(ProductCriteria::product)
				.hasSize(1);
	}

}
