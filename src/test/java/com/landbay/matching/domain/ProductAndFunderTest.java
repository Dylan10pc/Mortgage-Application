package com.landbay.matching.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductAndFunderTest {

	@Test
	void createsAProduct() {
		Product product = new Product("STANDARD_75", "Standard Buy-to-Let 75%");

		assertThat(product.id()).isEqualTo("STANDARD_75");
		assertThat(product.name()).isEqualTo("Standard Buy-to-Let 75%");
	}

	@Test
	void createsAFunder() {
		Funder funder = new Funder("HIGH_STREET_BANK", "High Street Bank");

		assertThat(funder.id()).isEqualTo("HIGH_STREET_BANK");
		assertThat(funder.name()).isEqualTo("High Street Bank");
	}

	// Covers null, "" and "   " (whitespace only).
	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = "   ")
	void productRejectsABlankIdOrName(String blank) {
		assertThatThrownBy(() -> new Product(blank, "A name"))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("id must not be blank");
		assertThatThrownBy(() -> new Product("AN_ID", blank))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("name must not be blank");
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = "   ")
	void funderRejectsABlankIdOrName(String blank) {
		assertThatThrownBy(() -> new Funder(blank, "A name"))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("id must not be blank");
		assertThatThrownBy(() -> new Funder("AN_ID", blank))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("name must not be blank");
	}

}
