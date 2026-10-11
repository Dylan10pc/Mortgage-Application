package com.landbay.matching.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReasonTest {

	@Test
	void createsAReason() {
		Reason reason = new Reason(ReasonCode.LTV_TOO_HIGH, "LTV is too high", "0.8000", "<= 0.75");

		assertThat(reason.code()).isEqualTo(ReasonCode.LTV_TOO_HIGH);
		assertThat(reason.actual()).isEqualTo("0.8000");
		assertThat(reason.requirement()).isEqualTo("<= 0.75");
	}

	@Test
	void rejectsAMissingCode() {
		assertThatThrownBy(() -> new Reason(null, "message", "actual", "requirement"))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("code must not be null");
	}

	@Test
	void rejectsBlankDetails() {
		// A reason without detail would defeat its purpose, so each text part is required.
		assertThatThrownBy(() -> new Reason(ReasonCode.LTV_TOO_HIGH, " ", "actual", "requirement"))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("message must not be blank");
		assertThatThrownBy(() -> new Reason(ReasonCode.LTV_TOO_HIGH, "message", "", "requirement"))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("actual must not be blank");
		assertThatThrownBy(() -> new Reason(ReasonCode.LTV_TOO_HIGH, "message", "actual", null))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("requirement must not be blank");
	}

}
