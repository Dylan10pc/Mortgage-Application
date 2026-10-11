package com.landbay.matching.eligibility;

import org.junit.jupiter.api.Test;

import com.landbay.matching.domain.Reason;
import com.landbay.matching.domain.ReasonCode;

import static com.landbay.matching.ApplicationBuilder.anApplication;
import static org.assertj.core.api.Assertions.assertThat;

class ExperiencedLandlordRuleTest {

	private final ExperiencedLandlordRule rule = new ExperiencedLandlordRule();

	@Test
	void passesForAnExperiencedLandlord() {
		assertThat(rule.check(anApplication().experienced(true).build())).isEmpty();
	}

	@Test
	void failsForAFirstTimeLandlordWithAReasonThatExplainsWhy() {
		Reason reason = rule.check(anApplication().experienced(false).build()).orElseThrow();

		assertThat(reason.code()).isEqualTo(ReasonCode.LANDLORD_EXPERIENCE_REQUIRED);
		assertThat(reason.actual()).isEqualTo("false");
		assertThat(reason.requirement()).isEqualTo("true");
	}

}
