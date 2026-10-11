package com.landbay.matching.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.landbay.matching.catalogue.FunderCatalogue;
import com.landbay.matching.catalogue.ProductCatalogue;
import com.landbay.matching.match.MatchingService;

/**
 * Wires the plain-Java matching code into Spring. The matching classes know nothing about Spring;
 * this is the one place that hands them their data.
 */
@Configuration
public class MatchingConfiguration {

	/**
	 * The single matching service shared by every request. It is immutable and holds no per-request
	 * state, so one instance is safe to use from many threads at once.
	 *
	 * <p>For now the products and funders come from the hardcoded catalogues; a later layer swaps
	 * these two arguments for data loaded from configuration.
	 */
	@Bean
	public MatchingService matchingService() {
		return new MatchingService(ProductCatalogue.all(), FunderCatalogue.all());
	}

}
