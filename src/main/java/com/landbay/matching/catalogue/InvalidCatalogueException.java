package com.landbay.matching.catalogue;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Thrown at startup when the catalogue file is wrong. It carries EVERY problem found, not just the
 * first, so whoever edits the file can fix them all in one go.
 */
public class InvalidCatalogueException extends RuntimeException {

	private final List<String> problems;

	public InvalidCatalogueException(List<String> problems) {
		super(describe(problems));
		this.problems = List.copyOf(problems);
	}

	/** Each problem found, with where in the file it is. */
	public List<String> problems() {
		return problems;
	}

	// Builds a readable multi-line message: a heading, then one line per problem.
	private static String describe(List<String> problems) {
		return "The product and funder catalogue is invalid (" + problems.size() + " problem"
				+ (problems.size() == 1 ? "" : "s") + "):"
				+ problems.stream().map(problem -> "\n  - " + problem).collect(Collectors.joining());
	}

}
