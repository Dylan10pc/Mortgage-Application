package com.landbay.matching.domain;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductAndFunderTest {

	// A valid rate and margin to use wherever the test is about something else.
	private static final BigDecimal RATE = new BigDecimal("0.0479");
	private static final BigDecimal MARGIN = new BigDecimal("0.0030");

	@Test
	void createsAProduct() {
		Product product = new Product("STANDARD_75", "Standard Buy-to-Let 75%", RATE);

		assertThat(product.id()).isEqualTo("STANDARD_75");
		assertThat(product.name()).isEqualTo("Standard Buy-to-Let 75%");
		assertThat(product.indicativeRate()).isEqualByComparingTo("0.0479");
	}

	@Test
	void createsAFunder() {
		Funder funder = new Funder("HIGH_STREET_BANK", "High Street Bank", MARGIN);

		assertThat(funder.id()).isEqualTo("HIGH_STREET_BANK");
		assertThat(funder.name()).isEqualTo("High Street Bank");
		assertThat(funder.rateMargin()).isEqualByComparingTo("0.0030");
	}

	// Covers null, "" and "   " (whitespace only).
	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = "   ")
	void productRejectsABlankIdOrName(String blank) {
		assertThatThrownBy(() -> new Product(blank, "A name", RATE))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("id must not be blank");
		assertThatThrownBy(() -> new Product("AN_ID", blank, RATE))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("name must not be blank");
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = "   ")
	void funderRejectsABlankIdOrName(String blank) {
		assertThatThrownBy(() -> new Funder(blank, "A name", MARGIN))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("id must not be blank");
		assertThatThrownBy(() -> new Funder("AN_ID", blank, MARGIN))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("name must not be blank");
	}

	@Test
	void productRejectsARateThatIsNotPositive() {
		assertThatThrownBy(() -> new Product("AN_ID", "A name", BigDecimal.ZERO))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("indicativeRate must be greater than zero");
		assertThatThrownBy(() -> new Product("AN_ID", "A name", null))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("indicativeRate must be greater than zero");
	}

	@Test
	void funderRejectsANegativeMarginButAcceptsZero() {
		assertThatThrownBy(() -> new Funder("AN_ID", "A name", new BigDecimal("-0.0001")))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("rateMargin must not be negative");
		assertThatThrownBy(() -> new Funder("AN_ID", "A name", null))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("rateMargin must not be negative");
		assertThatCode(() -> new Funder("AN_ID", "A name", BigDecimal.ZERO)).doesNotThrowAnyException();
	}

}
