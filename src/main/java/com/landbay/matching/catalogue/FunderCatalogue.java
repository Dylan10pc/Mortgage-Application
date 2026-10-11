package com.landbay.matching.catalogue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import com.landbay.matching.domain.Funder;
import com.landbay.matching.domain.Location;
import com.landbay.matching.domain.PropertyType;
import com.landbay.matching.eligibility.AllowedValuesRule;
import com.landbay.matching.eligibility.ExperiencedLandlordRule;
import com.landbay.matching.eligibility.LoanAmountRule;
import com.landbay.matching.eligibility.MaxLtvRule;
import com.landbay.matching.eligibility.MinIcrRule;
import com.landbay.matching.funding.FunderCriteria;

/**
 * The three institutional funders used by this exercise, with deliberately different appetites.
 * The funders and figures are invented; they do not reproduce Landbay's real arrangements.
 *
 * <p>Like {@link ProductCatalogue}, this hardcoded list is a stepping stone before configuration files.
 */
public final class FunderCatalogue {

	// The three funders (identity only), as constants so other code can refer to them directly.
	public static final Funder MERIDIAN_BANK = new Funder("MERIDIAN_BANK", "Meridian Bank");
	public static final Funder ATLAS_PENSION_FUND = new Funder("ATLAS_PENSION_FUND", "Atlas Pension Fund");
	public static final Funder CORVUS_CREDIT_FUND = new Funder("CORVUS_CREDIT_FUND", "Corvus Credit Fund");

	// Utility class: only static members, so nobody should create one.
	private FunderCatalogue() {
	}

	/** Every funder with its appetite, in display order. */
	public static List<FunderCriteria> all() {
		// A cautious high-street bank: cheap money, strict limits. No HMOs or blocks, and it
		// does not lend in Scotland. Products are referred to by constant so a typo cannot compile.
		FunderCriteria meridianBank = new FunderCriteria(
				MERIDIAN_BANK,
				Set.of(
						ProductCatalogue.STANDARD_65.id(),
						ProductCatalogue.STANDARD_75.id(),
						ProductCatalogue.FIRST_TIME_LANDLORD_70.id(),
						ProductCatalogue.LIMITED_COMPANY_75.id()
				),
				List.of(
						new MaxLtvRule(new BigDecimal("0.70")),
						new MinIcrRule(new BigDecimal("1.30"), new BigDecimal("0.055")),
						new LoanAmountRule(new BigDecimal("50000"), new BigDecimal("750000")),
						AllowedValuesRule.propertyTypes(PropertyType.HOUSE, PropertyType.FLAT),
						AllowedValuesRule.locations(Location.ENGLAND, Location.WALES)
				)
		);

		// A long-term investor that likes large, income-producing loans, but only to experienced landlords.
		FunderCriteria atlasPensionFund = new FunderCriteria(
				ATLAS_PENSION_FUND,
				Set.of(
						ProductCatalogue.STANDARD_65.id(),
						ProductCatalogue.STANDARD_75.id(),
						ProductCatalogue.LIMITED_COMPANY_75.id(),
						ProductCatalogue.MULTI_UNIT_BLOCK_70.id()
				),
				List.of(
						new ExperiencedLandlordRule(),
						new LoanAmountRule(new BigDecimal("100000"), new BigDecimal("3000000")),
						AllowedValuesRule.propertyTypes(PropertyType.HOUSE, PropertyType.FLAT, PropertyType.MULTI_UNIT_BLOCK),
						AllowedValuesRule.locations(Location.ENGLAND, Location.WALES, Location.SCOTLAND)
				)
		);

		// A credit fund that accepts more risk (so wants higher returns): it funds every product,
		// with only a loan size cap and a location restriction.
		FunderCriteria corvusCreditFund = new FunderCriteria(
				CORVUS_CREDIT_FUND,
				Set.of(
						ProductCatalogue.STANDARD_65.id(),
						ProductCatalogue.STANDARD_75.id(),
						ProductCatalogue.FIRST_TIME_LANDLORD_70.id(),
						ProductCatalogue.LIMITED_COMPANY_75.id(),
						ProductCatalogue.HMO_70.id(),
						ProductCatalogue.MULTI_UNIT_BLOCK_70.id()
				),
				List.of(
						new LoanAmountRule(new BigDecimal("25000"), new BigDecimal("1500000")),
						AllowedValuesRule.locations(Location.ENGLAND, Location.WALES, Location.SCOTLAND)
				)
		);

		return List.of(meridianBank, atlasPensionFund, corvusCreditFund);
	}

}
