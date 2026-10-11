package com.landbay.matching.api;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.landbay.matching.match.MatchResult;
import com.landbay.matching.match.MatchingService;

/**
 * The HTTP entry point: receives a mortgage application and returns the matching products and funders.
 *
 * <p>Deliberately thin. It checks the request, hands it to {@link MatchingService} and converts the
 * answer to JSON; the real decisions all happen in the plain-Java matching code.
 */
@RestController
@RequestMapping("/api/v1/matches")
public class MatchController {

	private static final Logger log = LoggerFactory.getLogger(MatchController.class);

	private final MatchingService matchingService;

	// Spring supplies the MatchingService bean (see MatchingConfiguration) through the constructor.
	public MatchController(MatchingService matchingService) {
		this.matchingService = matchingService;
	}

	/**
	 * Works out what a proposed mortgage could get. POST because the input is a body, even though
	 * nothing is stored; the response is 200 OK (not 201 Created) for the same reason.
	 *
	 * <p>{@code @Valid} makes Spring check the request's annotations first; a bad request never
	 * reaches this method and is answered with a 400 by {@link ApiExceptionHandler}.
	 */
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public MatchResponse match(@Valid @RequestBody MatchRequest request) {
		MatchResult result = matchingService.match(request.toApplication());

		// One summary line per request. The case reference is safe to log: the request restricts its characters.
		log.info("Matched case {}: {} eligible products, {} fundable, {} ineligible",
				result.caseReference(),
				result.eligibleProducts().size(),
				result.eligibleProducts().stream().filter(product -> product.fundable()).count(),
				result.ineligibleProducts().size());

		return MatchResponse.from(result);
	}

}
