package com.landbay.matching.api;

import java.util.List;
import java.util.regex.Matcher;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.jayway.jsonpath.JsonPath;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests of the HTTP API: real requests through validation, the matching code and JSON
 * serialisation. The default request is a healthy application: 75% LTV, interest cover 1.4545, an
 * experienced individual with a house in England.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MatchApiTest {

	// The default request body. Tests change single fields with with(...).
	private static final String BASE = """
			{
			  "caseReference": "CASE-1001",
			  "loanAmount": 150000,
			  "propertyValue": 200000,
			  "monthlyRent": 1000,
			  "propertyType": "HOUSE",
			  "applicantType": "INDIVIDUAL",
			  "location": "ENGLAND",
			  "hasLandlordExperience": true
			}
			""";

	@Autowired
	private MockMvc mockMvc;

	// Replaces one field's value (given as JSON text) in the default request.
	private static String with(String body, String field, String jsonValue) {
		return body.replaceAll("\"" + field + "\": [^,\\n]*",
				Matcher.quoteReplacement("\"" + field + "\": " + jsonValue));
	}

	private static String with(String field, String jsonValue) {
		return with(BASE, field, jsonValue);
	}

	// Sends a JSON body to the endpoint. (Named post, so the request builder is referred to by its class.)
	private ResultActions post(String body) throws Exception {
		return mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/matches")
				.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	// Reads a list out of a JSON response body using a JsonPath expression.
	private static List<String> listFrom(String json, String path) {
		return JsonPath.read(json, path);
	}

	// ---- Successful matches ----

	@Test
	void aStandardApplicationGetsOneProductFundedByTwoOfTheThreeFunders() throws Exception {
		post(BASE)
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.caseReference").value("CASE-1001"))

				// One eligible product, shown with its own rate as a percentage.
				.andExpect(jsonPath("$.eligibleProducts.length()").value(1))
				.andExpect(jsonPath("$.eligibleProducts[0].productId").value("STANDARD_75"))
				.andExpect(jsonPath("$.eligibleProducts[0].productName").value("Standard 75% LTV"))
				.andExpect(jsonPath("$.eligibleProducts[0].fundable").value(true))
				.andExpect(jsonPath("$.eligibleProducts[0].indicativeRatePercent").value(4.79))

				// Two funders can fund it, cheapest first, with rate = product rate + funder margin.
				.andExpect(jsonPath("$.eligibleProducts[0].fundingOptions.length()").value(2))
				.andExpect(jsonPath("$.eligibleProducts[0].fundingOptions[0].funderId").value("ATLAS_PENSION_FUND"))
				.andExpect(jsonPath("$.eligibleProducts[0].fundingOptions[0].ratePercent").value(5.09))
				.andExpect(jsonPath("$.eligibleProducts[0].fundingOptions[1].funderId").value("CORVUS_CREDIT_FUND"))
				.andExpect(jsonPath("$.eligibleProducts[0].fundingOptions[1].ratePercent").value(5.54))

				// The bank declined, and the response says exactly why.
				.andExpect(jsonPath("$.eligibleProducts[0].declinedFunders.length()").value(1))
				.andExpect(jsonPath("$.eligibleProducts[0].declinedFunders[0].funderId").value("MERIDIAN_BANK"))
				.andExpect(jsonPath("$.eligibleProducts[0].declinedFunders[0].funderName").value("Meridian Bank"))
				.andExpect(jsonPath("$.eligibleProducts[0].declinedFunders[0].reasons[0].code").value("LTV_TOO_HIGH"))
				.andExpect(jsonPath("$.eligibleProducts[0].declinedFunders[0].reasons[0].actual").value("0.7500"))
				.andExpect(jsonPath("$.eligibleProducts[0].declinedFunders[0].reasons[0].requirement").value("<= 0.7"))
				.andExpect(jsonPath("$.eligibleProducts[0].declinedFunders[0].reasons[0].message").isNotEmpty())

				// Five products are ineligible, nearest to qualifying first.
				.andExpect(jsonPath("$.ineligibleProducts.length()").value(5))
				.andExpect(jsonPath("$.ineligibleProducts[0].productId").value("FIRST_TIME_LANDLORD_70"))
				.andExpect(jsonPath("$.ineligibleProducts[0].reasons[0].code").value("LTV_TOO_HIGH"))
				.andExpect(jsonPath("$.ineligibleProducts[4].productId").value("MULTI_UNIT_BLOCK_70"));
	}

	@Test
	void aLowerLoanOpensMoreProductsRankedByTheRateActuallyOnOffer() throws Exception {
		String body = post(with("loanAmount", "120000"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		// Cheapest best offer first.
		assertThat(listFrom(body, "$.eligibleProducts[*].productId"))
				.containsExactly("STANDARD_65", "STANDARD_75", "FIRST_TIME_LANDLORD_70");
		// The bank is the cheapest funder for the cheapest product: 4.29% + 0.10% = 4.39%.
		assertThat(listFrom(body, "$.eligibleProducts[0].fundingOptions[*].funderId"))
				.containsExactly("MERIDIAN_BANK", "ATLAS_PENSION_FUND", "CORVUS_CREDIT_FUND");
	}

	@Test
	void aProductCanBeEligibleYetUnfundableAndTheResponseExplainsEachRefusal() throws Exception {
		// A first-time-landlord company borrowing 1.8m: the product accepts it, but no funder does.
		String body = with(with(with(with(with(BASE, "applicantType", "\"LIMITED_COMPANY\""),
				"loanAmount", "1800000"), "propertyValue", "2400000"), "monthlyRent", "12000"),
				"hasLandlordExperience", "false");

		post(body)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.eligibleProducts.length()").value(1))
				.andExpect(jsonPath("$.eligibleProducts[0].productId").value("LIMITED_COMPANY_75"))
				.andExpect(jsonPath("$.eligibleProducts[0].fundable").value(false))
				.andExpect(jsonPath("$.eligibleProducts[0].fundingOptions").isEmpty())
				.andExpect(jsonPath("$.eligibleProducts[0].declinedFunders.length()").value(3))
				// Nearest to saying yes first: pension fund (1 reason), credit fund (1), bank (2).
				.andExpect(jsonPath("$.eligibleProducts[0].declinedFunders[0].funderId").value("ATLAS_PENSION_FUND"))
				.andExpect(jsonPath("$.eligibleProducts[0].declinedFunders[0].reasons[0].code").value("LANDLORD_EXPERIENCE_REQUIRED"))
				.andExpect(jsonPath("$.eligibleProducts[0].declinedFunders[1].reasons[0].code").value("LOAN_TOO_LARGE"))
				.andExpect(jsonPath("$.eligibleProducts[0].declinedFunders[2].reasons.length()").value(2));
	}

	@Test
	void northernIrelandMatchesNothingButEveryProductExplainsWhy() throws Exception {
		String body = post(with("location", "\"NORTHERN_IRELAND\""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.eligibleProducts").isEmpty())
				.andExpect(jsonPath("$.ineligibleProducts.length()").value(6))
				// Nearest to qualifying: STANDARD_75 fails only on location.
				.andExpect(jsonPath("$.ineligibleProducts[0].productId").value("STANDARD_75"))
				.andExpect(jsonPath("$.ineligibleProducts[0].reasons.length()").value(1))
				.andExpect(jsonPath("$.ineligibleProducts[0].reasons[0].code").value("LOCATION_NOT_SUPPORTED"))
				.andExpect(jsonPath("$.ineligibleProducts[0].reasons[0].actual").value("NORTHERN_IRELAND"))
				.andReturn().getResponse().getContentAsString();

		// Every one of the six products names the location as a reason.
		assertThat(listFrom(body, "$.ineligibleProducts[*].reasons[?(@.code == 'LOCATION_NOT_SUPPORTED')].code"))
				.hasSize(6);
	}

	@Test
	void theSameRequestAlwaysGivesTheSameResponse() throws Exception {
		String first = post(BASE).andReturn().getResponse().getContentAsString();
		String second = post(BASE).andReturn().getResponse().getContentAsString();

		assertThat(first).isEqualTo(second);
	}

	@Test
	void zeroRentIsAcceptedAndDeclinedWithAReasonRatherThanRejected() throws Exception {
		post(with("monthlyRent", "0"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.eligibleProducts").isEmpty());
	}

	// ---- Invalid requests: field rules ----

	@Test
	void anEmptyObjectReportsEveryMissingFieldAtOnce() throws Exception {
		String body = post("{}")
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.title").value("Invalid request"))
				.andExpect(jsonPath("$.errors.length()").value(8))
				.andReturn().getResponse().getContentAsString();

		assertThat(listFrom(body, "$.errors[*].field")).containsExactly(
				"applicantType", "caseReference", "hasLandlordExperience", "loanAmount",
				"location", "monthlyRent", "propertyType", "propertyValue");
	}

	@Test
	void severalBadValuesAreAllReportedTogetherWithTheirFields() throws Exception {
		String body = with(with(with(with(BASE, "caseReference", "\"bad ref!\""),
				"loanAmount", "-5"), "propertyValue", "0"), "monthlyRent", "-1");

		String response = post(body)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.length()").value(4))
				.andReturn().getResponse().getContentAsString();

		assertThat(listFrom(response, "$.errors[*].field"))
				.containsExactly("caseReference", "loanAmount", "monthlyRent", "propertyValue");
		assertThat(listFrom(response, "$.errors[?(@.field == 'loanAmount')].message"))
				.containsExactly("must be greater than 0");
		assertThat(listFrom(response, "$.errors[?(@.field == 'caseReference')].message"))
				.containsExactly("may only contain letters, digits and . _ / -");
	}

	@Test
	void moneyWithMoreThanTwoDecimalPlacesIsRejected() throws Exception {
		String response = post(with("loanAmount", "150000.123"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("loanAmount"))
				.andReturn().getResponse().getContentAsString();

		String message = JsonPath.read(response, "$.errors[0].message");
		assertThat(message).contains("numeric value out of bounds");
	}

	@Test
	void aCaseReferenceThatIsTooLongIsRejected() throws Exception {
		post(with("caseReference", "\"" + "A".repeat(65) + "\""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("caseReference"));
	}

	@Test
	void leavingOutTheExperienceFlagIsAnErrorNotASilentFalse() throws Exception {
		String withoutFlag = """
				{
				  "caseReference": "CASE-1001",
				  "loanAmount": 150000,
				  "propertyValue": 200000,
				  "monthlyRent": 1000,
				  "propertyType": "HOUSE",
				  "applicantType": "INDIVIDUAL",
				  "location": "ENGLAND"
				}
				""";

		post(withoutFlag)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.length()").value(1))
				.andExpect(jsonPath("$.errors[0].field").value("hasLandlordExperience"));
	}

	// ---- Invalid requests: unreadable bodies ----

	@Test
	void anUnknownEnumValueIsRejectedWithTheAcceptedValuesListed() throws Exception {
		post(with("propertyType", "\"BUNGALOW\""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Malformed request"))
				.andExpect(jsonPath("$.detail").value("'BUNGALOW' is not a valid value for 'propertyType'. "
						+ "Accepted values: HOUSE, FLAT, HMO, MULTI_UNIT_BLOCK."));
	}

	@Test
	void enumValuesMustMatchExactly() throws Exception {
		// Lower case is not accepted: the contract is the exact upper-case name.
		String response = post(with("location", "\"england\""))
				.andExpect(status().isBadRequest())
				.andReturn().getResponse().getContentAsString();

		String detail = JsonPath.read(response, "$.detail");
		assertThat(detail).contains("'england' is not a valid value for 'location'");
	}

	@Test
	void brokenJsonIsRejected() throws Exception {
		post("{ this is not json")
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Malformed request"));
	}

	@Test
	void anEmptyBodyIsRejected() throws Exception {
		post("")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Malformed request"));
	}

	@Test
	void aNumberFieldHoldingTextIsRejected() throws Exception {
		post(with("loanAmount", "\"lots\""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Malformed request"));
	}

	// ---- Invalid requests: wrong URL, method or content type ----

	@Test
	void aWrongContentTypeIsRefusedWithAProblemResponse() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/matches").contentType(MediaType.TEXT_PLAIN).content(BASE))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(415));
	}

	@Test
	void aGetIsRefusedBecauseOnlyPostIsSupported() throws Exception {
		mockMvc.perform(get("/api/v1/matches"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void anUnknownPathGivesAProblemResponse() throws Exception {
		mockMvc.perform(get("/api/v1/nothing-here"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

}
