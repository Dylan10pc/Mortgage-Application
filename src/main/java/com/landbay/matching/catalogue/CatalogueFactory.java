package com.landbay.matching.catalogue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.landbay.matching.domain.ApplicantType;
import com.landbay.matching.domain.Funder;
import com.landbay.matching.domain.Location;
import com.landbay.matching.domain.Product;
import com.landbay.matching.domain.PropertyType;
import com.landbay.matching.eligibility.AllowedValuesRule;
import com.landbay.matching.eligibility.EligibilityRule;
import com.landbay.matching.eligibility.ExperiencedLandlordRule;
import com.landbay.matching.eligibility.LoanAmountRule;
import com.landbay.matching.eligibility.MaxLtvRule;
import com.landbay.matching.eligibility.MinIcrRule;
import com.landbay.matching.eligibility.ProductCriteria;
import com.landbay.matching.funding.FunderCriteria;

/**
 * Checks the raw catalogue file and turns it into the real product and funder objects.
 *
 * <p>The checking is strict and complete: it finds EVERY problem (not just the first), says where in
 * the file each one is, and refuses to produce a catalogue at all unless the whole file is right.
 * The application therefore fails at startup with a clear list, instead of running with a half-broken
 * catalogue and giving wrong answers to real applications.
 *
 * <p>Checked here: required and unexpected parameters for each rule type, valid enum names, duplicate
 * ids, funders listing products that do not exist, and empty lists. Number ranges are checked by the
 * domain classes themselves (for example a rate must be positive), and their messages are reported
 * with the location in the file.
 */
public final class CatalogueFactory {

	private static final Logger log = LoggerFactory.getLogger(CatalogueFactory.class);

	// Every problem found so far, in file order, so they can all be reported together.
	private final List<String> problems = new ArrayList<>();

	// Use build(...): one factory instance is created per build, to hold that build's problems.
	private CatalogueFactory() {
	}

	/**
	 * Builds the catalogue, or throws {@link InvalidCatalogueException} listing every problem.
	 */
	public static Catalogue build(CatalogueProperties properties) {
		CatalogueFactory factory = new CatalogueFactory();
		Catalogue catalogue = factory.buildCatalogue(properties);

		if (!factory.problems.isEmpty()) {
			throw new InvalidCatalogueException(factory.problems);
		}

		log.info("Loaded catalogue: {} products, {} funders", catalogue.products().size(), catalogue.funders().size());
		return catalogue;
	}

	private Catalogue buildCatalogue(CatalogueProperties properties) {
		// Every product id written in the file, so a funder's list can be checked against them
		// even when that product has some other problem of its own.
		Set<String> declaredProductIds = properties.products().stream()
				.map(ProductConfig::id)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		List<ProductCriteria> products = buildProducts(properties.products());
		List<FunderCriteria> funders = buildFunders(properties.funders(), declaredProductIds);

		if (problems.isEmpty()) {
			warnAboutUnfundedProducts(products, funders);
		}
		return new Catalogue(products, funders);
	}

	// ---- Products ----

	private List<ProductCriteria> buildProducts(List<ProductConfig> configs) {
		if (configs.isEmpty()) {
			problems.add("products: at least one product is required");
		}

		List<ProductCriteria> products = new ArrayList<>();
		Set<String> seenIds = new HashSet<>();

		for (int i = 0; i < configs.size(); i++) {
			ProductConfig config = configs.get(i);
			String path = label("products", i, config.id());
			int problemsBefore = problems.size();

			if (config.id() != null && !seenIds.add(config.id())) {
				problems.add(path + ": the id '" + config.id() + "' is used by more than one product");
			}

			Product product = create(path, () -> new Product(config.id(), config.name(), config.indicativeRate()));

			// A product with no rules would accept every application, which is almost certainly a mistake.
			if (config.rules().isEmpty()) {
				problems.add(path + ": at least one rule is required");
			}
			List<EligibilityRule> rules = buildRules(config.rules(), path);

			// Keep the product only if nothing at all was wrong with it.
			if (problems.size() == problemsBefore) {
				products.add(new ProductCriteria(product, rules));
			}
		}
		return products;
	}

	// ---- Funders ----

	private List<FunderCriteria> buildFunders(List<FunderConfig> configs, Set<String> productIds) {
		if (configs.isEmpty()) {
			problems.add("funders: at least one funder is required");
		}

		List<FunderCriteria> funders = new ArrayList<>();
		Set<String> seenIds = new HashSet<>();

		for (int i = 0; i < configs.size(); i++) {
			FunderConfig config = configs.get(i);
			String path = label("funders", i, config.id());
			int problemsBefore = problems.size();

			if (config.id() != null && !seenIds.add(config.id())) {
				problems.add(path + ": the id '" + config.id() + "' is used by more than one funder");
			}

			Funder funder = create(path, () -> new Funder(config.id(), config.name(), config.rateMargin()));

			// The products it funds must be given, and each must be a real product (catches typos).
			if (config.fundsProducts().isEmpty()) {
				problems.add(path + ": 'funds-products' must list at least one product");
			}
			for (String productId : config.fundsProducts()) {
				if (!productIds.contains(productId)) {
					problems.add(path + ": 'funds-products' lists '" + productId + "', which is not a product in the catalogue");
				}
			}

			// A funder with no rules of its own is allowed: it simply accepts whatever the product accepts.
			List<EligibilityRule> rules = buildRules(config.rules(), path);

			if (problems.size() == problemsBefore) {
				funders.add(new FunderCriteria(funder, new LinkedHashSet<>(config.fundsProducts()), rules));
			}
		}
		return funders;
	}

	// ---- Rules ----

	private List<EligibilityRule> buildRules(List<RuleConfig> configs, String owner) {
		List<EligibilityRule> rules = new ArrayList<>();

		for (int i = 0; i < configs.size(); i++) {
			EligibilityRule rule = buildRule(configs.get(i), owner + " > rules[" + i + "]");
			if (rule != null) {
				rules.add(rule);
			}
		}
		return rules;
	}

	// Turns one rule from the file into a rule object, or records what is wrong and returns null.
	private EligibilityRule buildRule(RuleConfig rule, String path) {
		if (rule.type() == null) {
			problems.add(path + ": 'type' is required");
			return null;
		}
		String where = path + " (" + fileName(rule.type()) + ")";

		return switch (rule.type()) {
			case MAX_LTV -> expect(where, rule, "value")
					? create(where, () -> new MaxLtvRule(rule.value())) : null;

			case MIN_ICR -> expect(where, rule, "value", "stress-rate")
					? create(where, () -> new MinIcrRule(rule.value(), rule.stressRate())) : null;

			case LOAN_AMOUNT -> expect(where, rule, "min", "max")
					? create(where, () -> new LoanAmountRule(rule.min(), rule.max())) : null;

			case EXPERIENCED_LANDLORD -> expect(where, rule)
					? new ExperiencedLandlordRule() : null;

			case PROPERTY_TYPES -> {
				List<PropertyType> allowed = enums(PropertyType.class, rule, where);
				yield allowed == null ? null
						: create(where, () -> AllowedValuesRule.propertyTypes(allowed.toArray(PropertyType[]::new)));
			}

			case APPLICANT_TYPES -> {
				List<ApplicantType> allowed = enums(ApplicantType.class, rule, where);
				yield allowed == null ? null
						: create(where, () -> AllowedValuesRule.applicantTypes(allowed.toArray(ApplicantType[]::new)));
			}

			case LOCATIONS -> {
				List<Location> allowed = enums(Location.class, rule, where);
				yield allowed == null ? null
						: create(where, () -> AllowedValuesRule.locations(allowed.toArray(Location[]::new)));
			}
		};
	}

	// Checks that a rule has exactly the parameters its type needs: none missing, none left over.
	// A leftover is usually a typo or a parameter put on the wrong rule, so it is reported, not ignored.
	private boolean expect(String where, RuleConfig rule, String... expected) {
		Set<String> given = parametersGiven(rule);
		Set<String> wanted = new LinkedHashSet<>(Arrays.asList(expected));
		boolean ok = true;

		for (String name : wanted) {
			if (!given.contains(name)) {
				problems.add(where + ": requires '" + name + "'");
				ok = false;
			}
		}
		for (String name : given) {
			if (!wanted.contains(name)) {
				problems.add(where + ": does not use '" + name + "'");
				ok = false;
			}
		}
		return ok;
	}

	// The names of the parameters actually written for this rule.
	private static Set<String> parametersGiven(RuleConfig rule) {
		Set<String> names = new LinkedHashSet<>();
		if (rule.value() != null) {
			names.add("value");
		}
		if (rule.min() != null) {
			names.add("min");
		}
		if (rule.max() != null) {
			names.add("max");
		}
		if (rule.stressRate() != null) {
			names.add("stress-rate");
		}
		if (!rule.values().isEmpty()) {
			names.add("values");
		}
		return names;
	}

	// Converts a rule's 'values' list into enum constants, reporting each unknown name along with
	// the accepted ones. Returns null if anything is wrong.
	private <E extends Enum<E>> List<E> enums(Class<E> type, RuleConfig rule, String where) {
		if (!expect(where, rule, "values")) {
			return null;
		}

		List<E> parsed = new ArrayList<>();
		boolean valid = true;
		for (String text : rule.values()) {
			try {
				parsed.add(Enum.valueOf(type, text));
			} catch (IllegalArgumentException e) {
				problems.add(where + ": '" + text + "' is not a valid " + type.getSimpleName()
						+ "; accepted values: " + acceptedValues(type));
				valid = false;
			}
		}
		return valid ? parsed : null;
	}

	private static String acceptedValues(Class<? extends Enum<?>> type) {
		return Arrays.stream(type.getEnumConstants()).map(Enum::name).collect(Collectors.joining(", "));
	}

	// ---- Helpers ----

	// Runs a constructor from the domain. If it rejects the values (a negative rate, a blank name...)
	// the reason is recorded against the place in the file, and null is returned.
	private <T> T create(String path, Supplier<? extends T> constructor) {
		try {
			return constructor.get();
		} catch (IllegalArgumentException | NullPointerException e) {
			problems.add(path + ": " + e.getMessage());
			return null;
		}
	}

	// Where an item is in the file, for error messages: products[2] (HMO_70)
	private static String label(String section, int index, String id) {
		return section + "[" + index + "]" + (id == null ? "" : " (" + id + ")");
	}

	// A rule type as it is spelt in the file: MAX_LTV becomes max-ltv.
	private static String fileName(RuleType type) {
		return type.name().toLowerCase(Locale.ROOT).replace('_', '-');
	}

	// A product that no funder lists can never have a funding option. That is allowed (it shows up as
	// "eligible but unfunded"), but it is worth a warning in case it is a mistake.
	private void warnAboutUnfundedProducts(List<ProductCriteria> products, List<FunderCriteria> funders) {
		Set<String> funded = funders.stream()
				.flatMap(funder -> funder.fundedProductIds().stream())
				.collect(Collectors.toSet());

		for (ProductCriteria criteria : products) {
			if (!funded.contains(criteria.product().id())) {
				log.warn("Product {} is not listed by any funder, so it can never be funded", criteria.product().id());
			}
		}
	}

}
