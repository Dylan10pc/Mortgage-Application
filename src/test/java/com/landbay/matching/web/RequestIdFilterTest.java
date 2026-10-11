package com.landbay.matching.web;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
class RequestIdFilterTest {

	@Autowired
	private MockMvc mockMvc;

	// Calls an endpoint, optionally sending a request id, and returns the id that came back.
	private String idReturnedFor(String suppliedId) throws Exception {
		var request = get("/actuator/health");
		if (suppliedId != null) {
			request.header("X-Request-Id", suppliedId);
		}
		return mockMvc.perform(request).andReturn().getResponse().getHeader("X-Request-Id");
	}

	@Test
	void aRequestWithNoIdGetsAFreshOne() throws Exception {
		assertThat(idReturnedFor(null)).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
	}

	@Test
	void eachRequestGetsADifferentId() throws Exception {
		assertThat(idReturnedFor(null)).isNotEqualTo(idReturnedFor(null));
	}

	@Test
	void aSafeIdFromTheCallerIsKeptSoItCanBeTracedAcrossSystems() throws Exception {
		assertThat(idReturnedFor("trace-123.abc_DEF")).isEqualTo("trace-123.abc_DEF");
	}

	@Test
	void anIdContainingUnsafeCharactersIsReplaced() throws Exception {
		// Spaces and punctuation could be used to forge log lines, so such an id is not trusted.
		assertThat(idReturnedFor("not safe; drop table")).matches("[0-9a-f-]{36}");
	}

	@Test
	void anOverlyLongIdIsReplaced() throws Exception {
		assertThat(idReturnedFor("a".repeat(65))).matches("[0-9a-f-]{36}");
	}

	@Test
	void errorResponsesCarryTheIdToo() throws Exception {
		String id = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/matches")
						.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andReturn().getResponse().getHeader("X-Request-Id");

		assertThat(id).isNotBlank();
	}

	@Test
	void theIdIsRemovedFromTheLoggingContextWhenTheRequestEnds() throws Exception {
		idReturnedFor("trace-1");

		// Request threads are reused, so a leftover id would wrongly appear in later requests' logs.
		assertThat(MDC.get("requestId")).isNull();
	}

}
