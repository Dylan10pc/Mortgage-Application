package com.landbay.matching.catalogue;

import java.util.List;

import com.landbay.matching.eligibility.ProductCriteria;
import com.landbay.matching.funding.FunderCriteria;

/**
 * The validated products and funders the matching service works with, built from the catalogue file.
 *
 * @param products every product with its criteria, in display order
 * @param funders  every funder with its appetite, in display order
 */
public record Catalogue(List<ProductCriteria> products, List<FunderCriteria> funders) {

	public Catalogue {
		// Take unmodifiable copies so the catalogue cannot be changed after it is built.
		products = List.copyOf(products);
		funders = List.copyOf(funders);
	}

}
