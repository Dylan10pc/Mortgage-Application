package com.landbay.matching.eligibility;

import java.math.BigDecimal;
import java.util.Optional;

import com.landbay.matching.domain.Checks;
import com.landbay.matching.domain.MortgageApplication;
import com.landbay.matching.domain.Reason;
import com.landbay.matching.domain.ReasonCode;

/**
 * Requires the loan-to-value to be no higher than a maximum, for example 0.75 for "75% LTV".
 * It mirrors {@link MinIcrRule}, with the comparison the other way round.
 *
 * @param maxLtv the highest acceptable loan-to-value, as a fraction (0.75 = 75%)
 */
public record MaxLtvRule(BigDecimal maxLtv) implements EligibilityRule {

	public MaxLtvRule {
		// A zero or negative limit would be a configuration mistake, so fail immediately.
		Checks.requirePositive(maxLtv, "maxLtv");
	}

	@Override
	public Optional<Reason> check(MortgageApplication application) {
		// The application's own LTV (already rounded UP, the cautious way).
		BigDecimal ltv = application.loanToValue();

		// compareTo, not equals: BigDecimal.equals treats 0.75 and 0.7500 as different numbers,
		// so an LTV of exactly 75% would wrongly fail against a limit written as 0.75.
		if (ltv.compareTo(maxLtv) <= 0) {
			return Optional.empty();
		}

		// Failed: say what was found and what would have passed.
		return Optional.of(new Reason(
				ReasonCode.LTV_TOO_HIGH,
				"Loan-to-value " + ltv + " is above the maximum " + maxLtv,
				ltv.toPlainString(),
				"<= " + maxLtv.toPlainString()));
	}

}
