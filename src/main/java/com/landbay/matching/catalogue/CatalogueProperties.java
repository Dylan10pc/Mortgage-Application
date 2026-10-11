package com.landbay.matching.catalogue;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Everything under {@code landbay.catalogue} in the configuration: the products Landbay offers
 * and the funders behind them. Spring fills this in from {@code catalogue.yml}.
 *
 * <p>This is only the raw, unchecked text of the file. {@link CatalogueFactory} validates it and turns
 * it into the real product and funder objects.
 *
 * @param products every product, in the order they should be listed
 * @param funders  every funder, in the order they should be listed
 */
@ConfigurationProperties(prefix = "landbay.catalogue")
public record CatalogueProperties(List<ProductConfig> products, List<FunderConfig> funders) {

	public CatalogueProperties {
		// A missing list means "none given"; the factory then reports it clearly.
		products = products == null ? List.of() : List.copyOf(products);
		funders = funders == null ? List.of() : List.copyOf(funders);
	}

}
