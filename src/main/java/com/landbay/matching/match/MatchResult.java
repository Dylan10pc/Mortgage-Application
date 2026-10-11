package com.landbay.matching.match;

import java.util.List;

import com.landbay.matching.domain.Checks;
import com.landbay.matching.eligibility.ProductEvaluation;

/**
 * The full answer for one application: what it could get, from whom, and why not for the rest.
 *
 * <p>Two sections, so a consumer never has to filter a mixed list. Both are already in the
 * order they should be shown in (see {@link ResultOrdering}).
 *
 * @param caseReference      the case reference from the application, so the result can be traced back to it
 * @param eligibleProducts   products the application qualifies for, best first, each with its funding options
 * @param ineligibleProducts products it does not qualify for, nearest to qualifying first, each with every reason
 */
public record MatchResult(String caseReference, List<EligibleProduct> eligibleProducts, List<ProductEvaluation> ineligibleProducts) {

	public MatchResult {
		Checks.requireText(caseReference, "caseReference");

		// Take unmodifiable copies so the result cannot be changed after it is created.
		eligibleProducts = List.copyOf(eligibleProducts);
		ineligibleProducts = List.copyOf(ineligibleProducts);
	}

}
