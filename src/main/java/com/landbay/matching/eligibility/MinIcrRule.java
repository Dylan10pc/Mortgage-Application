package com.landbay.matching.eligibility;

import java.math.BigDecimal;
import java.util.Optional;

import com.landbay.matching.domain.Checks;
import com.landbay.matching.domain.MortgageApplication;
import com.landbay.matching.domain.Reason;
import com.landbay.matching.domain.ReasonCode;

/**
 * Requires the rent to cover the loan's interest by at least a minimum ratio.
 *
 * <p>Both numbers belong to the product, not the application: the minimum ICR (for example 1.25
 * for 125%) and the stress rate it is tested at (for example 0.055 for 5.5%).
 *
 * <p>This is a finished example. Read it, then write {@link MaxLtvRule} in the same style.
 *
 * @param minIcr     the lowest acceptable interest cover ratio, as a fraction (1.25 = 125%)
 * @param stressRate the annual interest rate to test the loan against, as a fraction (0.055 = 5.5%)
 */
public record MinIcrRule(BigDecimal minIcr, BigDecimal stressRate) implements EligibilityRule {

	public MinIcrRule {
		// A zero or negative figure here would be a configuration mistake, so fail immediately.
		Checks.requirePositive(minIcr, "minIcr");
		Checks.requirePositive(stressRate, "stressRate");
	}

	@Override
	public Optional<Reason> check(MortgageApplication application) {
		// The application's own ICR at this product's stress rate (already rounded DOWN, the cautious way).
		BigDecimal icr = application.interestCoverRatio(stressRate);

		// compareTo, not equals: BigDecimal.equals treats 1.25 and 1.2500 as different numbers.
		if (icr.compareTo(minIcr) >= 0) {
			return Optional.empty();
		}

		// Failed: say what was found and what would have passed.
		return Optional.of(new Reason(
				ReasonCode.ICR_TOO_LOW,
				"Interest cover ratio " + icr + " is below the minimum " + minIcr
						+ " (tested at a stress rate of " + stressRate + ")",
				icr.toPlainString(),
				">= " + minIcr.toPlainString()));
	}

}
