package com.landbay.matching.catalogue;

import java.math.BigDecimal;
import java.util.List;

import com.landbay.matching.domain.ApplicantType;
import com.landbay.matching.domain.Location;
import com.landbay.matching.domain.Product;
import com.landbay.matching.domain.PropertyType;
import com.landbay.matching.eligibility.AllowedValuesRule;
import com.landbay.matching.eligibility.EligibilityRule;
import com.landbay.matching.eligibility.ExperiencedLandlordRule;
import com.landbay.matching.eligibility.LoanAmountRule;
import com.landbay.matching.eligibility.MaxLtvRule;
import com.landbay.matching.eligibility.MinIcrRule;
import com.landbay.matching.eligibility.ProductCriteria;

/**
 * The small, representative set of Landbay products used by this exercise, written directly in code.
 * The figures are invented but realistic; they do not reproduce Landbay's real products.
 *
 * <p>This hardcoded list is a stepping stone: a later layer replaces it with products loaded from
 * a configuration file, so a product can change without recompiling.
 */
public final class ProductCatalogue {

	// The six products with their indicative annual rates (0.0479 = 4.79%). Higher LTV and
	// riskier property cost more. Constants, so other code (such as the funder definitions) can
	// refer to a product without retyping its id text, and a typo becomes a compile error.
	public static final Product STANDARD_65 = new Product("STANDARD_65", "Standard 65% LTV", bd("0.0429"));
	public static final Product STANDARD_75 = new Product("STANDARD_75", "Standard 75% LTV", bd("0.0479"));
	public static final Product FIRST_TIME_LANDLORD_70 = new Product("FIRST_TIME_LANDLORD_70", "First-time Landlord 70% LTV", bd("0.0499"));
	public static final Product LIMITED_COMPANY_75 = new Product("LIMITED_COMPANY_75", "Limited Company 75% LTV", bd("0.0489"));
	public static final Product HMO_70 = new Product("HMO_70", "HMO 70% LTV", bd("0.0549"));
	public static final Product MULTI_UNIT_BLOCK_70 = new Product("MULTI_UNIT_BLOCK_70", "Multi-unit Block 70% LTV", bd("0.0529"));

	// Assumption: every product tests interest cover at a 5.5% stress rate.
	private static final BigDecimal STRESS_RATE = new BigDecimal("0.055");

	// Rules shared by several products. Rule objects are immutable, so sharing one instance is safe.
	private static final EligibilityRule HOUSE_OR_FLAT = AllowedValuesRule.propertyTypes(PropertyType.HOUSE, PropertyType.FLAT);
	private static final EligibilityRule MAINLAND_UK = AllowedValuesRule.locations(Location.ENGLAND, Location.WALES, Location.SCOTLAND);
	private static final EligibilityRule EXPERIENCED = new ExperiencedLandlordRule();

	// Each product with the criteria an application must meet. Order here is the order products are listed.
	private static final List<ProductCriteria> ALL = List.of(

			// Cautious, lowest LTV, open to first-time landlords.
			new ProductCriteria(STANDARD_65, List.of(
					HOUSE_OR_FLAT, MAINLAND_UK,
					new MaxLtvRule(bd("0.65")),
					new MinIcrRule(bd("1.25"), STRESS_RATE),
					new LoanAmountRule(bd("25000"), bd("1000000")))),

			// Higher LTV, so a stricter interest cover and an experienced landlord are required.
			new ProductCriteria(STANDARD_75, List.of(
					HOUSE_OR_FLAT, MAINLAND_UK, EXPERIENCED,
					new MaxLtvRule(bd("0.75")),
					new MinIcrRule(bd("1.40"), STRESS_RATE),
					new LoanAmountRule(bd("25000"), bd("1000000")))),

			// Individuals only, with a lower LTV cap and smaller loans to reflect the extra risk.
			new ProductCriteria(FIRST_TIME_LANDLORD_70, List.of(
					AllowedValuesRule.applicantTypes(ApplicantType.INDIVIDUAL),
					HOUSE_OR_FLAT, MAINLAND_UK,
					new MaxLtvRule(bd("0.70")),
					new MinIcrRule(bd("1.45"), STRESS_RATE),
					new LoanAmountRule(bd("25000"), bd("500000")))),

			// Companies (typically SPVs) only, with larger loans allowed.
			new ProductCriteria(LIMITED_COMPANY_75, List.of(
					AllowedValuesRule.applicantTypes(ApplicantType.LIMITED_COMPANY),
					HOUSE_OR_FLAT, MAINLAND_UK,
					new MaxLtvRule(bd("0.75")),
					new MinIcrRule(bd("1.25"), STRESS_RATE),
					new LoanAmountRule(bd("50000"), bd("2000000")))),

			// Houses in multiple occupation: higher risk, so a strict cover and experience are required.
			new ProductCriteria(HMO_70, List.of(
					AllowedValuesRule.propertyTypes(PropertyType.HMO),
					MAINLAND_UK, EXPERIENCED,
					new MaxLtvRule(bd("0.70")),
					new MinIcrRule(bd("1.45"), STRESS_RATE),
					new LoanAmountRule(bd("50000"), bd("1000000")))),

			// Blocks of flats under one freehold: larger loans, experienced landlords only.
			new ProductCriteria(MULTI_UNIT_BLOCK_70, List.of(
					AllowedValuesRule.propertyTypes(PropertyType.MULTI_UNIT_BLOCK),
					MAINLAND_UK, EXPERIENCED,
					new MaxLtvRule(bd("0.70")),
					new MinIcrRule(bd("1.30"), STRESS_RATE),
					new LoanAmountRule(bd("100000"), bd("3000000")))));

	// Utility class: only static members, so nobody should create one.
	private ProductCatalogue() {
	}

	/** Every product with its criteria, in display order. The list cannot be modified. */
	public static List<ProductCriteria> all() {
		return ALL;
	}

	// Shorthand so the figures above are easy to read.
	private static BigDecimal bd(String number) {
		return new BigDecimal(number);
	}

}
