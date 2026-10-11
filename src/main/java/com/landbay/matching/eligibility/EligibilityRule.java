package com.landbay.matching.eligibility;

import java.util.Optional;

import com.landbay.matching.domain.MortgageApplication;
import com.landbay.matching.domain.Reason;

/**
 * One lending criterion that an application must meet, for example "loan-to-value at most 75%".
 *
 * <p>A product's eligibility is simply a list of these rules. Keeping each rule small and
 * separate means rules can be tested alone, reused across products, and later built from configuration.
 *
 * <p>It is a functional interface (exactly one abstract method), so tests can write a rule as a lambda.
 */
@FunctionalInterface
public interface EligibilityRule {

	/**
	 * Checks the application against this rule.
	 *
	 * @return empty if the application passes, or the {@link Reason} it fails.
	 *         (Empty means "nothing wrong", which keeps the passing case simple.)
	 */
	Optional<Reason> check(MortgageApplication application);

}
