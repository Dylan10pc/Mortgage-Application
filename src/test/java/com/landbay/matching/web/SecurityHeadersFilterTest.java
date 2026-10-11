package com.landbay.matching.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityHeadersFilterTest {

	@Autowired
	private MockMvc mockMvc;

	private static final String VALID_BODY = """
			{"caseReference":"CASE-1","loanAmount":150000,"propertyValue":200000,"monthlyRent":1000,
			 "propertyType":"HOUSE","applicantType":"INDIVIDUAL","location":"ENGLAND","hasLandlordExperience":true}
			""";

	private ResultActions postApi(String body) throws Exception {
		return mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/matches")
				.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	@Test
	void apiResponsesAreProtectedAndNeverCached() throws Exception {
		postApi(VALID_BODY)
				.andExpect(status().isOk())
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(header().string("X-Frame-Options", "DENY"))
				.andExpect(header().string("Referrer-Policy", "no-referrer"))
				.andExpect(header().string("Cache-Control", "no-store"));
	}

	@Test
	void apiErrorResponsesAreProtectedToo() throws Exception {
		postApi("{}")
				.andExpect(status().isBadRequest())
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(header().string("Cache-Control", "no-store"));
	}

	@Test
	void otherEndpointsGetTheProtectiveHeadersButAreNotMarkedNoStore() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(header().string("X-Frame-Options", "DENY"))
				.andExpect(header().doesNotExist("Cache-Control"));
	}

}
