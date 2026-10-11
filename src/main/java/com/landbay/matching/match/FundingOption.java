package com.landbay.matching.match;

import java.math.BigDecimal;
import java.util.Objects;

import com.landbay.matching.domain.Checks;
import com.landbay.matching.domain.Funder;

/**
 * One funder that can fund a product for an application, and the rate it would charge.
 *
 * @param funder the funder able to fund the product
 * @param rate   the annual interest rate on offer as a fraction: the product's indicative rate
 *               plus this funder's margin (0.0509 = 5.09%)
 */
public record FundingOption(Funder funder, BigDecimal rate) {

	public FundingOption {
		Objects.requireNonNull(funder, "funder must not be null");
		Checks.requirePositive(rate, "rate");
	}

}
