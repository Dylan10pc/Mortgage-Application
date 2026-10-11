package com.landbay.matching.eligibility;

import java.math.BigDecimal;
import java.util.Optional;

import com.landbay.matching.domain.Checks;
import com.landbay.matching.domain.MortgageApplication;
import com.landbay.matching.domain.Reason;
import com.landbay.matching.domain.ReasonCode;

/**
 * Requires the loan to sit between a minimum and a maximum size (both inclusive).
 *
 * @param minLoan the smallest loan the product will make
 * @param maxLoan the largest loan the product will make
 */
public record LoanAmountRule(BigDecimal minLoan, BigDecimal maxLoan) implements EligibilityRule {

	public LoanAmountRule {
		Checks.requirePositive(minLoan, "minLoan");
		Checks.requirePositive(maxLoan, "maxLoan");

		// A range that ends before it starts could never be satisfied.
		if (maxLoan.compareTo(minLoan) < 0) {
			throw new IllegalArgumentException("maxLoan must not be less than minLoan");
		}
	}

	@Override
	public Optional<Reason> check(MortgageApplication application) {
		BigDecimal loan = application.loanAmount();

		// Too small. The boundary itself is allowed, so only strictly below fails.
		if (loan.compareTo(minLoan) < 0) {
			return Optional.of(new Reason(
					ReasonCode.LOAN_TOO_SMALL,
					"Loan amount " + loan + " is below the minimum " + minLoan,
					loan.toPlainString(),
					">= " + minLoan.toPlainString()));
		}

		// Too large. Likewise, only strictly above fails.
		if (loan.compareTo(maxLoan) > 0) {
			return Optional.of(new Reason(
					ReasonCode.LOAN_TOO_LARGE,
					"Loan amount " + loan + " is above the maximum " + maxLoan,
					loan.toPlainString(),
					"<= " + maxLoan.toPlainString()));
		}

		return Optional.empty();
	}

}
