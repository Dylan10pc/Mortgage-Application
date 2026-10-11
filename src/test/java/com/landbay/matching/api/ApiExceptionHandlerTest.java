package com.landbay.matching.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

	@Test
	void anUnexpectedErrorGivesAGenericMessageThatLeaksNothing() {
		ProblemDetail problem = new ApiExceptionHandler()
				.handleUnexpected(new RuntimeException("connection to db.internal failed with password hunter2"));

		assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
		assertThat(problem.getTitle()).isEqualTo("Internal error");
		// The caller must never see the exception's own text.
		assertThat(problem.getDetail()).doesNotContain("db.internal").doesNotContain("hunter2");
	}

}
