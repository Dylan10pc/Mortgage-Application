package com.landbay.matching.eligibility;

import java.util.Optional;

import com.landbay.matching.domain.MortgageApplication;
import com.landbay.matching.domain.Reason;
import com.landbay.matching.domain.ReasonCode;

/**
 * Requires the applicant to have previous experience as a landlord. Lenders commonly ask for
 * this on riskier property such as HMOs and multi-unit blocks.
 *
 * <p>A record with no fields: the rule has nothing to configure, so it is created with
 * {@code new ExperiencedLandlordRule()}.
 */
public record ExperiencedLandlordRule() implements EligibilityRule {

	@Override
	public Optional<Reason> check(MortgageApplication application) {
		// Experienced applicants pass.
		if (application.hasLandlordExperience()) {
			return Optional.empty();
		}

		// First-time landlords fail, with a reason that spells out what is needed.
		return Optional.of(new Reason(
				ReasonCode.LANDLORD_EXPERIENCE_REQUIRED,
				"This product requires previous experience as a landlord",
				"false",
				"true"));
	}

}
