package com.landbay.matching.match;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.landbay.matching.domain.MortgageApplication;
import com.landbay.matching.domain.Product;
import com.landbay.matching.eligibility.ProductCriteria;
import com.landbay.matching.eligibility.ProductEvaluation;
import com.landbay.matching.funding.FunderCriteria;
import com.landbay.matching.funding.FunderEvaluation;

/**
 * The heart of the application. Given a mortgage application it works out which products the
 * applicant is eligible for, which funders could fund each, and why not for everything else.
 *
 * <p>Two filters, in order: first every product's own criteria; then, only for the products that
 * passed, every funder's appetite.
 *
 * <p>Plain Java with no Spring: it is handed the products and funders it works with, so it can be
 * tested directly with any data and wired into the web layer later.
 */
public class MatchingService {

	private final List<ProductCriteria> products;
	private final List<FunderCriteria> funders;

	/**
	 * @param products every product with its criteria
	 * @param funders  every funder with its appetite
	 */
	public MatchingService(List<ProductCriteria> products, List<FunderCriteria> funders) {
		// Take unmodifiable copies so the lists cannot be changed behind the service's back.
		this.products = List.copyOf(products);
		this.funders = List.copyOf(funders);
	}

	/** Matches one application against every product and funder, with results in display order. */
	public MatchResult match(MortgageApplication application) {
		Objects.requireNonNull(application, "application must not be null");

		List<EligibleProduct> eligible = new ArrayList<>();
		List<ProductEvaluation> ineligible = new ArrayList<>();

		// Filter 1: check the application against each product's own criteria.
		for (ProductCriteria criteria : products) {
			ProductEvaluation evaluation = criteria.evaluate(application);

			if (evaluation.eligible()) {
				// Filter 2: for an eligible product, find out who could fund it.
				eligible.add(fundingFor(evaluation.product(), application));
			} else {
				// Keep the failed evaluation: it carries every reason why not.
				ineligible.add(evaluation);
			}
		}

		// Put both sections into display order.
		eligible.sort(ResultOrdering.ELIGIBLE_PRODUCTS);
		ineligible.sort(ResultOrdering.INELIGIBLE_PRODUCTS);

		return new MatchResult(application.caseReference(), eligible, ineligible);
	}

	/**
	 * Asks every funder whether it can fund this product for this application. Funders that can
	 * become funding options with a rate; funders that cannot are kept, with their reasons.
	 */
	private EligibleProduct fundingFor(Product product, MortgageApplication application) {
		List<FundingOption> options = new ArrayList<>();
		List<FunderEvaluation> declined = new ArrayList<>();

		for (FunderCriteria criteria : funders) {
			// Each funder's verdict: able to fund, or every reason it cannot.
			FunderEvaluation evaluation = criteria.evaluate(product, application);

			if (evaluation.canFund()) {
				// The rate on offer is the product's rate plus this funder's margin.
				BigDecimal rate = product.indicativeRate().add(evaluation.funder().rateMargin());
				options.add(new FundingOption(evaluation.funder(), rate));
			} else {
				declined.add(evaluation);
			}
		}

		// Cheapest funder first; the funders nearest to saying yes first among the declines.
		options.sort(ResultOrdering.FUNDING_OPTIONS);
		declined.sort(ResultOrdering.DECLINED_FUNDERS);

		return new EligibleProduct(product, options, declined);
	}

}
