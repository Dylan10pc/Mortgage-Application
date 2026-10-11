package com.landbay.matching.funding;

import java.util.List;
import java.util.Objects;

import com.landbay.matching.domain.Funder;
import com.landbay.matching.domain.Reason;

/**
 * The outcome of checking one funder against one product and application: able to fund,
 * or unable with every reason why.
 *
 * @param funder   the funder that was checked
 * @param failures every reason the funder cannot fund this; empty means it can
 */
public record FunderEvaluation(Funder funder, List<Reason> failures) {

	public FunderEvaluation {
		Objects.requireNonNull(funder, "funder must not be null");

		// Take an unmodifiable copy so the result cannot be changed after it is created.
		failures = List.copyOf(failures);
	}

	/** True when the funder has no objection to funding this. */
	public boolean canFund() {
		return failures.isEmpty();
	}

}
