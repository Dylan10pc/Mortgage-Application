package com.landbay.matching.api;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import com.landbay.matching.domain.ApplicantType;
import com.landbay.matching.domain.Location;
import com.landbay.matching.domain.MortgageApplication;
import com.landbay.matching.domain.PropertyType;

/**
 * The JSON body of a match request: the details of a proposed mortgage.
 *
 * <p>This is the API contract, kept separate from the internal {@link MortgageApplication} so the two
 * can change independently. The annotations are checked before the request reaches the matching
 * code, and every failing field is reported together. Money is in pounds sterling (GBP).
 *
 * <p>The enum fields are exact, upper-case names; any other value is rejected with a message
 * listing the accepted ones. {@code hasLandlordExperience} is a {@code Boolean}, not a {@code boolean},
 * so leaving it out is an error instead of silently meaning "no experience".
 *
 * @param caseReference         the caller's identifier for the case (1 to 64 letters, digits or . _ / -)
 * @param loanAmount            how much the customer wants to borrow, in pounds
 * @param propertyValue         the value of the property, in pounds
 * @param monthlyRent           the expected monthly rental income, in pounds (zero is allowed)
 * @param propertyType          what kind of property it is
 * @param applicantType         who is borrowing
 * @param location              where the property is
 * @param hasLandlordExperience whether the applicant has been a landlord before
 */
public record MatchRequest(

		// Restricted to a safe character set so it can be logged and echoed without surprises.
		@NotBlank
		@Size(max = 64)
		@Pattern(regexp = "[A-Za-z0-9._/-]*", message = "may only contain letters, digits and . _ / -")
		String caseReference,

		// Money: required, in the right range, and no more than 12 whole digits and 2 decimal places (pence).
		@NotNull @Positive @Digits(integer = 12, fraction = 2)
		BigDecimal loanAmount,

		@NotNull @Positive @Digits(integer = 12, fraction = 2)
		BigDecimal propertyValue,

		@NotNull @PositiveOrZero @Digits(integer = 12, fraction = 2)
		BigDecimal monthlyRent,

		@NotNull PropertyType propertyType,
		@NotNull ApplicantType applicantType,
		@NotNull Location location,
		@NotNull Boolean hasLandlordExperience) {

	/** Converts the validated request into the internal domain object. */
	public MortgageApplication toApplication() {
		return new MortgageApplication(
				caseReference,
				loanAmount,
				propertyValue,
				monthlyRent,
				propertyType,
				applicantType,
				location,
				hasLandlordExperience);
	}

}
