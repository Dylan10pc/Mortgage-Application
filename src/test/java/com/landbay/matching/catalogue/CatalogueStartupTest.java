package com.landbay.matching.catalogue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.landbay.matching.config.MatchingConfiguration;
import com.landbay.matching.match.MatchingService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Starts a real Spring application context with deliberately wrong configuration and checks that the
 * application REFUSES TO START with a useful message, instead of running with a broken catalogue.
 */
class CatalogueStartupTest {

	// Starts only the matching configuration, with whatever properties each test supplies.
	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withUserConfiguration(MatchingConfiguration.class);

	// A minimal valid catalogue, written as the flat properties Spring reads from the YAML file.
	private static final String[] VALID = {
			"landbay.catalogue.products[0].id=P1",
			"landbay.catalogue.products[0].name=Product one",
			"landbay.catalogue.products[0].indicative-rate=0.05",
			"landbay.catalogue.products[0].rules[0].type=experienced-landlord",
			"landbay.catalogue.funders[0].id=F1",
			"landbay.catalogue.funders[0].name=Funder one",
			"landbay.catalogue.funders[0].rate-margin=0.001",
			"landbay.catalogue.funders[0].funds-products[0]=P1"
	};

	@Test
	void aValidCatalogueStartsAndWiresTheMatchingService() {
		runner.withPropertyValues(VALID).run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context).hasSingleBean(MatchingService.class);
			assertThat(context.getBean(Catalogue.class).products()).hasSize(1);
		});
	}

	@Test
	void aFunderListingAnUnknownProductStopsStartupAndSaysWhich() {
		runner.withPropertyValues(VALID)
				.withPropertyValues("landbay.catalogue.funders[0].funds-products[0]=NOPE")
				.run(context -> assertThat(context).hasFailed().getFailure()
						.rootCause().isInstanceOf(InvalidCatalogueException.class)
						.hasMessageContaining("funders[0] (F1)")
						.hasMessageContaining("'NOPE'"));
	}

	@Test
	void aMisspeltRuleTypeStopsStartup() {
		runner.withPropertyValues(VALID)
				.withPropertyValues("landbay.catalogue.products[0].rules[0].type=max-ltvv")
				.run(context -> assertThat(context).hasFailed().getFailure()
						.hasStackTraceContaining("max-ltvv"));
	}

	@Test
	void aMissingCatalogueStopsStartup() {
		runner.run(context -> assertThat(context).hasFailed().getFailure()
				.rootCause().isInstanceOf(InvalidCatalogueException.class)
				.hasMessageContaining("products: at least one product is required")
				.hasMessageContaining("funders: at least one funder is required"));
	}

	@Test
	void aRateThatIsNotANumberStopsStartup() {
		runner.withPropertyValues(VALID)
				.withPropertyValues("landbay.catalogue.products[0].indicative-rate=cheap")
				.run(context -> assertThat(context).hasFailed());
	}

}
