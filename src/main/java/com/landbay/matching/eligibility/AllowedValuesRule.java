package com.landbay.matching.eligibility;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.landbay.matching.domain.ApplicantType;
import com.landbay.matching.domain.Checks;
import com.landbay.matching.domain.Location;
import com.landbay.matching.domain.MortgageApplication;
import com.landbay.matching.domain.PropertyType;
import com.landbay.matching.domain.Reason;
import com.landbay.matching.domain.ReasonCode;

/**
 * Requires one enum-valued field of the application to be in an allowed set, for example
 * "property type must be HOUSE or FLAT". One generic class covers property type, applicant
 * type and location, instead of three near-identical classes.
 *
 * <p>Reading the generics: {@code T extends Enum<T>} means "T is some enum type". The rule is told
 * how to read the field via {@code extractor}, for example the method reference
 * {@code MortgageApplication::propertyType}, so the same code works for every enum.
 *
 * @param failureCode which code to report when the value is not allowed
 * @param description what the field is called in messages, for example "Property type"
 * @param extractor   reads the field from the application
 * @param allowed     the values that pass
 * @param <T>         the enum type being checked
 */
public record AllowedValuesRule<T extends Enum<T>>(
		ReasonCode failureCode,
		String description,
		Function<MortgageApplication, T> extractor,
		Set<T> allowed) implements EligibilityRule {

	public AllowedValuesRule {
		Objects.requireNonNull(failureCode, "failureCode must not be null");
		Checks.requireText(description, "description");
		Objects.requireNonNull(extractor, "extractor must not be null");

		// Take an unmodifiable copy so the caller cannot change the set afterwards.
		allowed = Set.copyOf(allowed);

		// A rule that allows nothing would reject every application, which is surely a mistake.
		if (allowed.isEmpty()) {
			throw new IllegalArgumentException("allowed must not be empty");
		}
	}

	/** Convenience: the property type must be one of these. */
	public static AllowedValuesRule<PropertyType> propertyTypes(PropertyType... allowed) {
		return new AllowedValuesRule<>(ReasonCode.PROPERTY_TYPE_NOT_ACCEPTED, "Property type",
				MortgageApplication::propertyType, Set.copyOf(Arrays.asList(allowed)));
	}

	/** Convenience: the applicant type must be one of these. */
	public static AllowedValuesRule<ApplicantType> applicantTypes(ApplicantType... allowed) {
		return new AllowedValuesRule<>(ReasonCode.APPLICANT_TYPE_NOT_ACCEPTED, "Applicant type",
				MortgageApplication::applicantType, Set.copyOf(Arrays.asList(allowed)));
	}

	/** Convenience: the property location must be one of these. */
	public static AllowedValuesRule<Location> locations(Location... allowed) {
		return new AllowedValuesRule<>(ReasonCode.LOCATION_NOT_SUPPORTED, "Location",
				MortgageApplication::location, Set.copyOf(Arrays.asList(allowed)));
	}

	@Override
	public Optional<Reason> check(MortgageApplication application) {
		// Read the value being checked, e.g. this application's property type.
		T actual = extractor.apply(application);

		// Allowed values pass.
		if (allowed.contains(actual)) {
			return Optional.empty();
		}

		// Failed: list what would have passed. Sorted, because a Set has no guaranteed order
		// and API responses and tests need the same text every time.
		String accepted = allowed.stream()
				.map(Enum::name)
				.sorted()
				.collect(Collectors.joining(", "));

		return Optional.of(new Reason(
				failureCode,
				description + " " + actual.name() + " is not accepted",
				actual.name(),
				"one of " + accepted));
	}

}
