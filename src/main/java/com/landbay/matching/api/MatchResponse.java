package com.landbay.matching.api;

import java.math.BigDecimal;
import java.util.List;

import com.landbay.matching.domain.Funder;
import com.landbay.matching.domain.Product;
import com.landbay.matching.domain.Reason;
import com.landbay.matching.domain.ReasonCode;
import com.landbay.matching.eligibility.ProductEvaluation;
import com.landbay.matching.funding.FunderEvaluation;
import com.landbay.matching.match.EligibleProduct;
import com.landbay.matching.match.FundingOption;
import com.landbay.matching.match.MatchResult;

/**
 * The JSON body of a match response: what the application could get, from whom, and why not for the rest.
 *
 * <p>This is the API contract, kept separate from the internal {@link MatchResult} so the two can
 * change independently. Lists are already in display order (cheapest first for products and
 * funders, nearest to qualifying first for the ones that failed). Rates are percentages, so
 * {@code 5.09} means 5.09%.
 *
 * @param caseReference      the case reference from the request, so the response can be traced back to it
 * @param eligibleProducts   products the application qualifies for, best first
 * @param ineligibleProducts products it does not qualify for, each with every reason why
 */
public record MatchResponse(
		String caseReference,
		List<EligibleProductResponse> eligibleProducts,
		List<IneligibleProductResponse> ineligibleProducts) {

	/**
	 * A product the application qualifies for.
	 *
	 * @param fundable              true if at least one funder can fund it
	 * @param indicativeRatePercent the product's own headline rate, before any funder margin
	 * @param fundingOptions        funders able to fund it, cheapest first, each with its rate
	 * @param declinedFunders       funders unable to fund it, each with every reason why
	 */
	public record EligibleProductResponse(
			String productId,
			String productName,
			boolean fundable,
			BigDecimal indicativeRatePercent,
			List<FundingOptionResponse> fundingOptions,
			List<DeclinedFunderResponse> declinedFunders) {
	}

	/** A funder that can fund a product, and the rate it would charge, as a percentage. */
	public record FundingOptionResponse(String funderId, String funderName, BigDecimal ratePercent) {
	}

	/** A funder that cannot fund a product, with every reason. */
	public record DeclinedFunderResponse(String funderId, String funderName, List<ReasonResponse> reasons) {
	}

	/** A product the application does not qualify for, with every reason. */
	public record IneligibleProductResponse(String productId, String productName, List<ReasonResponse> reasons) {
	}

	/**
	 * One reason an option is unavailable. Switch on {@code code}; {@code message} is for people and
	 * its wording may change.
	 *
	 * @param actual      the value from the application that caused the failure
	 * @param requirement what would have passed, such as {@code <= 0.70} or {@code one of FLAT, HOUSE}
	 */
	public record ReasonResponse(ReasonCode code, String message, String actual, String requirement) {
	}

	/** Builds the response from the internal result. */
	public static MatchResponse from(MatchResult result) {
		return new MatchResponse(
				result.caseReference(),
				result.eligibleProducts().stream().map(MatchResponse::toResponse).toList(),
				result.ineligibleProducts().stream().map(MatchResponse::toResponse).toList());
	}

	private static EligibleProductResponse toResponse(EligibleProduct eligible) {
		Product product = eligible.product();
		return new EligibleProductResponse(
				product.id(),
				product.name(),
				eligible.fundable(),
				percent(product.indicativeRate()),
				eligible.fundingOptions().stream().map(MatchResponse::toResponse).toList(),
				eligible.declinedFunders().stream().map(MatchResponse::toResponse).toList());
	}

	private static FundingOptionResponse toResponse(FundingOption option) {
		Funder funder = option.funder();
		return new FundingOptionResponse(funder.id(), funder.name(), percent(option.rate()));
	}

	private static DeclinedFunderResponse toResponse(FunderEvaluation declined) {
		Funder funder = declined.funder();
		return new DeclinedFunderResponse(funder.id(), funder.name(), reasons(declined.failures()));
	}

	private static IneligibleProductResponse toResponse(ProductEvaluation ineligible) {
		Product product = ineligible.product();
		return new IneligibleProductResponse(product.id(), product.name(), reasons(ineligible.failures()));
	}

	private static List<ReasonResponse> reasons(List<Reason> failures) {
		return failures.stream()
				.map(reason -> new ReasonResponse(reason.code(), reason.message(), reason.actual(), reason.requirement()))
				.toList();
	}

	// Internally a rate is a fraction (0.0509); the API shows a percentage (5.09). Moving the
	// decimal point is exact, unlike multiplying a floating-point number.
	private static BigDecimal percent(BigDecimal fraction) {
		return fraction.movePointRight(2);
	}

}
