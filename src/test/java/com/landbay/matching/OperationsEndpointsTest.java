package com.landbay.matching;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Checks what is, and is not, exposed for the people and platforms that run the service. */
@SpringBootTest
@AutoConfigureMockMvc
class OperationsEndpointsTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void livenessAndReadinessProbesReportUp() throws Exception {
		// Liveness: "is the process working?". Readiness: "should traffic be sent to it?".
		mockMvc.perform(get("/actuator/health/liveness"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
		mockMvc.perform(get("/actuator/health/readiness"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void healthShowsNoInternalDetails() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.components").doesNotExist());
	}

	@Test
	void endpointsThatRevealConfigurationAreNotExposed() throws Exception {
		for (String endpoint : new String[] { "env", "beans", "configprops", "mappings", "heapdump", "loggers" }) {
			mockMvc.perform(get("/actuator/" + endpoint)).andExpect(status().isNotFound());
		}
	}

}
