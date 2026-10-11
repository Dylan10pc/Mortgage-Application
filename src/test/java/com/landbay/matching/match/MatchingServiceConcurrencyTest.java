package com.landbay.matching.match;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.landbay.matching.domain.MortgageApplication;

import static com.landbay.matching.ApplicationBuilder.anApplication;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The matching service is shared by every request, so it must give correct answers when many
 * requests use it at once. This hammers one instance from many threads and checks every answer.
 */
@SpringBootTest
class MatchingServiceConcurrencyTest {

	@Autowired
	private MatchingService service;

	@Test
	void manyThreadsSharingOneServiceAllGetTheCorrectAnswers() throws Exception {
		MortgageApplication standard = anApplication().build();
		MortgageApplication lowerLtv = anApplication().loan("120000").build();

		// The answers each application should always get, worked out once on a single thread.
		MatchResult expectedStandard = service.match(standard);
		MatchResult expectedLowerLtv = service.match(lowerLtv);

		try (ExecutorService pool = Executors.newFixedThreadPool(16)) {
			List<Future<MatchResult>> standardResults = new ArrayList<>();
			List<Future<MatchResult>> lowerLtvResults = new ArrayList<>();

			// 1,000 requests of two different kinds, interleaved across the threads.
			for (int i = 0; i < 500; i++) {
				standardResults.add(pool.submit(() -> service.match(standard)));
				lowerLtvResults.add(pool.submit(() -> service.match(lowerLtv)));
			}

			for (Future<MatchResult> result : standardResults) {
				assertThat(result.get()).isEqualTo(expectedStandard);
			}
			for (Future<MatchResult> result : lowerLtvResults) {
				assertThat(result.get()).isEqualTo(expectedLowerLtv);
			}
		}
	}

}
