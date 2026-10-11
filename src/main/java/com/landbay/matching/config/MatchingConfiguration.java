package com.landbay.matching.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.landbay.matching.catalogue.Catalogue;
import com.landbay.matching.catalogue.CatalogueFactory;
import com.landbay.matching.catalogue.CatalogueProperties;
import com.landbay.matching.match.MatchingService;

/**
 * Wires the plain-Java matching code into Spring. The matching classes know nothing about Spring;
 * this is the one place that hands them their data.
 *
 * <p>The products and funders come from {@code catalogue.yml}. They are checked once, at startup:
 * if the file is wrong the application does not start (see {@link CatalogueFactory}).
 */
@Configuration
@EnableConfigurationProperties(CatalogueProperties.class)
public class MatchingConfiguration {

	/**
	 * The validated products and funders. Spring has already read the raw text of the file into
	 * {@code properties}; the factory checks it and builds the real objects.
	 */
	@Bean
	public Catalogue catalogue(CatalogueProperties properties) {
		return CatalogueFactory.build(properties);
	}

	/**
	 * The single matching service shared by every request. It is immutable and holds no per-request
	 * state, so one instance is safe to use from many threads at once.
	 */
	@Bean
	public MatchingService matchingService(Catalogue catalogue) {
		return new MatchingService(catalogue.products(), catalogue.funders());
	}

}
