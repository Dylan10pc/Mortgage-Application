# Mortgage Product and Funder Matching

A Spring Boot 4 HTTP API for Landbay's buy-to-let lending. Given the details of a proposed mortgage, it answers three questions:

1. **Which Landbay products could the application be eligible for?**
2. **Which funders could fund each of those products, and at what rate?**
3. **Where an option is not available, why not?** Every refusal comes back as structured data (a stable code, the actual value, and what would have passed), so another system can act on it.

The products, funders and every figure are **invented for this exercise**. They are realistic enough to demonstrate the problem; they are not Landbay's real commercial arrangements.

**Contents:** [Run it](#1-run-it) · [The API](#2-the-api) · [Approach](#3-approach) · [The catalogue](#4-the-catalogue) · [Ordering](#5-how-results-are-ordered) · [Assumptions](#6-assumptions) · [Questions](#7-questions-i-would-ask-before-building-this-for-real) · [Left out](#8-what-i-left-out-and-what-i-would-add-before-production) · [Operations](#9-operating-it) · [Testing](#10-testing) · [AI use](#11-how-i-used-ai) · [Where to start reading](#12-where-to-start-reading-the-code)

---

## 1. Run it

**You need:** JDK 21 or newer (check with `java -version`) and an internet connection the first time, so the build can download its dependencies. You do **not** need Maven installed: the project includes the Maven wrapper.

### Windows (PowerShell)

```powershell
cd path\to\Landbay
.\mvnw.cmd spring-boot:run
```

(The leading `.\` is required in PowerShell.) When you see `Started ProductFunderMatchingApplication`, it is running on <http://localhost:8080>. Leave that window open.

In a **second** terminal, in the same folder, send the sample application:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/matches -ContentType "application/json" -InFile samples\match-request.json | ConvertTo-Json -Depth 10
```

### macOS / Linux

```bash
cd path/to/Landbay
./mvnw spring-boot:run
```

```bash
curl -s -X POST http://localhost:8080/api/v1/matches \
  -H "Content-Type: application/json" \
  -d @samples/match-request.json
```

If `./mvnw` says *permission denied*, the script has lost its executable flag in transit; run `sh mvnw spring-boot:run` instead.

### Other commands

| Task | Windows | macOS / Linux |
|---|---|---|
| Run all tests | `.\mvnw.cmd verify` | `./mvnw verify` |
| Build the jar | `.\mvnw.cmd package` | `./mvnw package` |
| Run the jar | `java -jar target\product-funder-matching-0.0.1-SNAPSHOT.jar` | `java -jar target/product-funder-matching-0.0.1-SNAPSHOT.jar` |
| Use another port | add `--server.port=8081` to the jar command, or `-Dspring-boot.run.arguments=--server.port=8081` to `spring-boot:run` | same |

Stop the app with `Ctrl+C`. If it will not start with *port 8080 already in use*, an earlier run is still going.

There is no user interface (the brief did not ask for one). The only page a browser can open is <http://localhost:8080/actuator/health>.

---

## 2. The API

### `POST /api/v1/matches`

Send a mortgage application as JSON; get back **200 OK** with the full answer. (It is a POST because the input is a body, and 200 rather than 201 because nothing is stored.)

| Field | Type | Rules |
|---|---|---|
| `caseReference` | text | required; 1 to 64 characters from `A-Z a-z 0-9 . _ / -` |
| `loanAmount` | number | required; greater than 0; pounds, at most 2 decimal places |
| `propertyValue` | number | required; greater than 0 |
| `monthlyRent` | number | required; 0 or more |
| `propertyType` | text | required; one of `HOUSE`, `FLAT`, `HMO`, `MULTI_UNIT_BLOCK` |
| `applicantType` | text | required; one of `INDIVIDUAL`, `LIMITED_COMPANY` |
| `location` | text | required; one of `ENGLAND`, `WALES`, `SCOTLAND`, `NORTHERN_IRELAND` |
| `hasLandlordExperience` | true / false | required (leaving it out is an error, not "false") |

Enum values must match exactly, in upper case. An unknown value is rejected with the accepted values listed.

### The response (abridged)

```json
{
  "caseReference": "CASE-1001",
  "eligibleProducts": [
    {
      "productId": "STANDARD_75",
      "productName": "Standard 75% LTV",
      "fundable": true,
      "indicativeRatePercent": 4.79,
      "fundingOptions": [
        { "funderId": "ATLAS_PENSION_FUND", "funderName": "Atlas Pension Fund", "ratePercent": 5.09 },
        { "funderId": "CORVUS_CREDIT_FUND", "funderName": "Corvus Credit Fund", "ratePercent": 5.54 }
      ],
      "declinedFunders": [
        {
          "funderId": "MERIDIAN_BANK",
          "funderName": "Meridian Bank",
          "reasons": [
            { "code": "LTV_TOO_HIGH", "message": "Loan-to-value 0.7500 is above the maximum 0.7",
              "actual": "0.7500", "requirement": "<= 0.7" }
          ]
        }
      ]
    }
  ],
  "ineligibleProducts": [
    { "productId": "FIRST_TIME_LANDLORD_70", "productName": "First-time Landlord 70% LTV",
      "reasons": [ { "code": "LTV_TOO_HIGH", "message": "...", "actual": "0.7500", "requirement": "<= 0.7" } ] }
  ]
}
```

- **`eligibleProducts`**: products whose own criteria the application meets, best first. For each, `fundingOptions` lists the funders able to fund it (cheapest first, with the rate on offer) and `declinedFunders` lists those that cannot, each with every reason. A product can be **eligible but unfundable**: `fundable` is `false` and `fundingOptions` is empty.
- **`ineligibleProducts`**: products the application does not meet, nearest to qualifying first, each with every reason.
- **Reasons** have a `code` to switch on (a closed list: `LTV_TOO_HIGH`, `ICR_TOO_LOW`, `LOAN_TOO_SMALL`, `LOAN_TOO_LARGE`, `PROPERTY_TYPE_NOT_ACCEPTED`, `APPLICANT_TYPE_NOT_ACCEPTED`, `LOCATION_NOT_SUPPORTED`, `LANDLORD_EXPERIENCE_REQUIRED`, `PRODUCT_NOT_FUNDED`), a human `message` (wording may change; do not parse it), the `actual` value found and the `requirement` that would have passed.
- Rates are percentages: `5.09` means 5.09%.

### Errors

Every error is an [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) `application/problem+json` response; no stack traces are ever returned. A request with several bad fields reports **all** of them at once:

```json
{
  "title": "Invalid request",
  "status": 400,
  "detail": "The request is not valid. See 'errors' for the fields to correct.",
  "instance": "/api/v1/matches",
  "errors": [
    { "field": "caseReference", "message": "may only contain letters, digits and . _ / -" },
    { "field": "loanAmount", "message": "must be greater than 0" }
  ]
}
```

Unreadable bodies and unknown enum values give a 400 titled `Malformed request` (for example: `'BUNGALOW' is not a valid value for 'propertyType'. Accepted values: HOUSE, FLAT, HMO, MULTI_UNIT_BLOCK.`). Wrong method, content type or path give 405, 415 and 404 in the same format.

A loan larger than the property value, or zero rent, is **not** an error: it is a valid request that the products decline, with reasons.

---

## 3. Approach

Matching is **two filters, in order**:

1. **Product eligibility.** Does the application meet each product's own lending criteria?
2. **Funder appetite.** For each product that passed, which funders will fund it?

A funder can only *narrow* what a product allows, never widen it.

### The main design decisions

- **Every criterion is a small rule object.** A product's criteria, and a funder's appetite, are each just a list of the same `EligibilityRule`s (max LTV, minimum interest cover, loan size range, allowed property/applicant types and locations, experienced landlord). Rules are tested alone, reused by both products and funders, and built from configuration. A new kind of criterion is one new rule class, a new rule type, and one case in the catalogue reader.
- **All failures are reported, not just the first.** The brief asks for "enough information for another system to understand why", so every rule runs and every failure comes back with a stable code, the actual value and the requirement.
- **Conservative rounding.** LTV is rounded **up** and interest cover **down** (to 4 decimal places), the cautious direction in each case, so rounding can never let a borderline loan pass. Money is `BigDecimal` throughout and compared with `compareTo`, never `equals` (`0.75` and `0.7500` are different to `equals`).
- **Closed enums everywhere** (property type, applicant type, location, rule type, reason code), so an unknown value is rejected at the edge or at startup instead of being silently mishandled.
- **A plain-Java core.** The matching logic (`domain`, `eligibility`, `funding`, `match`) has no Spring in it and is tested directly with any data. Spring appears only at the edges: the web layer, and one configuration class that supplies the data.
- **The API contract is separate from the internal model**, so either can change without breaking the other.
- **Configuration is validated at startup.** Products and funders live in `catalogue.yml`. If the file is wrong the application refuses to start and lists *every* problem with its location, rather than running with a half-broken catalogue and giving wrong answers.
- **Deterministic output.** The same request always gives the same response: every ordering ends with an id tie-break.

### Layout

```
src/main/java/com/landbay/matching/
  domain/        MortgageApplication, Product, Funder, enums, LendingMetrics (LTV, ICR), Reason, ReasonCode
  eligibility/   EligibilityRule and its rules; ProductCriteria (a product + its rules)
  funding/       FunderCriteria (a funder + its product list + its rules)
  match/         MatchingService (the two filters), ResultOrdering, result types
  catalogue/     reads and validates catalogue.yml into the objects above
  config/        MatchingConfiguration (hands the catalogue to the matching service)
  api/           MatchController, MatchRequest, MatchResponse, ApiExceptionHandler
  web/           request-id and security-header filters
src/main/resources/
  catalogue.yml  the products and funders
  application.properties
samples/match-request.json
PROMPTS.md       how AI was used, and the prompts
```

---

## 4. The catalogue

Six products and three funders, all in [`src/main/resources/catalogue.yml`](src/main/resources/catalogue.yml). Every product also requires house/flat (or the type named), England, Wales or Scotland, and interest cover tested at a 5.5% stress rate.

| Product | Rate | Key criteria |
|---|---|---|
| `STANDARD_65` | 4.29% | LTV up to 65%, cover 125%, £25k to £1m. First-time landlords welcome |
| `STANDARD_75` | 4.79% | LTV up to 75%, cover 140%, £25k to £1m. **Experienced landlords only** |
| `FIRST_TIME_LANDLORD_70` | 4.99% | Individuals only, LTV up to 70%, cover 145%, £25k to £500k |
| `LIMITED_COMPANY_75` | 4.89% | Limited companies only, LTV up to 75%, cover 125%, £50k to £2m |
| `HMO_70` | 5.49% | HMOs only, experienced, LTV up to 70%, cover 145%, £50k to £1m |
| `MULTI_UNIT_BLOCK_70` | 5.29% | Blocks only, experienced, LTV up to 70%, cover 130%, £100k to £3m |

| Funder | Adds | Appetite |
|---|---|---|
| Meridian Bank (cautious) | +0.10% | Funds the two standard products, first-time landlord and limited company. Max LTV 70%, cover 130%, £50k to £750k, houses and flats, **England and Wales only**. No HMOs or blocks |
| Atlas Pension Fund (long-term) | +0.30% | Funds the two standard products, limited company and multi-unit block. **Experienced landlords only**, £100k to £3m, houses, flats and blocks, England, Wales, Scotland |
| Corvus Credit Fund (risk-tolerant) | +0.75% | Funds **all six** products. £25k to £1.5m, England, Wales, Scotland. Wider appetite, higher price |

Between them the funders differ on **which products** they fund, **what kind of lending** (property type, location, applicant experience) and **how much risk** (LTV, cover, size), as the brief describes.

### Changing it without rebuilding

Edit `catalogue.yml` (the file's header lists every rule type and its parameters), or run with a copy:

```
java -jar target/product-funder-matching-0.0.1-SNAPSHOT.jar --spring.config.additional-location=file:./catalogue.yml
```

The file is checked as the application starts. A product with an unknown rule parameter, a funder listing a product that does not exist, a duplicate id and so on stops startup with all problems listed, for example:

```
The product and funder catalogue is invalid (3 problems):
  - products[0] (STANDARD_65) > rules[2] (max-ltv): does not use 'min'
  - funders[1] (ATLAS_PENSION_FUND): 'funds-products' lists 'STANDARD_757', which is not a product in the catalogue
  - funders[1] (ATLAS_PENSION_FUND) > rules[2] (property-types): 'BUNGALOW' is not a valid PropertyType; accepted values: ...
```

---

## 5. How results are ordered

The brief leaves ordering to us. The brief also says higher-risk funders need higher rates, so **price** drives every ordering.

- **Rate on offer** = the product's indicative rate + the funder's margin. (The cautious bank is cheapest, the credit fund dearest.)
- **Funding options** for a product: cheapest rate first.
- **Eligible products**: products a funder can fund come first, ranked by the **best rate actually on offer**, not the product's own headline rate (a product that looks cheap but whose only funder charges a large margin ranks below a dearer product offered more cheaply). Products nobody can fund come last, ordered by their own rate.
- **Declined funders** and **ineligible products**: nearest to qualifying first, meaning fewest reasons. This is a rough measure (a 1% LTV overshoot counts the same as any other single failure), so it is a presentation choice rather than a score.
- **Every ordering ends with an id tie-break**, so output is always repeatable.

All of it lives in one class, `ResultOrdering`, so a different ordering requirement is a one-file change.

---

## 6. Assumptions

The brief is deliberately incomplete. These are the assumptions I made, all visible in the code or catalogue:

1. **UK, pounds sterling.** All money is GBP and there is no currency field. Rates and ratios are fractions internally (`0.0479`) and percentages in the API.
2. **Location is country-level** (England, Wales, Scotland, Northern Ireland). Northern Ireland is in the list so it can be declined with a clear reason.
3. **Interest cover is interest-only:** annual rent ÷ (loan × stress rate), with one stress rate per rule (5.5%) and no tax-band distinction. The application has no term, repayment type, product fee or tax band, so none can affect the result.
4. **Funders' appetite means appetite only:** whether a funder *would* fund a case. I do not model its capacity, remaining allocation or exposure limits.
5. **A funder's rules apply to every product it funds.** Different limits for the same funder on different products would need the rules held per product. A funder can narrow a product but never widen it.
6. **One indicative rate per product**, plus a fixed margin per funder. The result is an indication, not an offer: real pricing also depends on fees, term, revert rate and more.
7. **The first-time landlord product also accepts experienced individuals**; it is simply more cautious, so experienced landlords will see better options too.
8. **A loan larger than the property is a valid request** (over 100% LTV) that products decline with a reason, and **zero rent is accepted** and declined with a reason. Only structurally invalid input (negative or missing values) is a 400.
9. **Input limits are my own:** case reference is 1 to 64 characters from `A-Z a-z 0-9 . _ / -` (so it is safe to log and echo), and money has at most 12 whole digits and 2 decimal places.
10. **All products, funders and figures are invented.**
11. **Stateless.** Nothing is stored, and there is no authentication.

---

## 7. Questions I would ask before building this for real

Things I noticed the brief does not say, and would want to clarify with the business:

1. **How exactly is interest cover calculated?** Which stress rate, does it differ for basic- and higher-rate taxpayers and for limited companies, is top-slicing allowed, and is rent treated differently for HMOs and blocks?
2. **What else belongs in the application?** Term, repayment type (interest-only or repayment), fixed-rate period, fees, borrower income and tax band, existing portfolio size, credit history. Each would change eligibility or price.
3. **Is "previous landlord experience" a yes/no?** Real criteria are usually years of experience or number of properties, and "portfolio landlord" rules often apply above four properties.
4. **How fine should location be?** Country-level hides real differences (London, and the Scottish legal process that some funders avoid). Does Landbay lend in Northern Ireland?
5. **Is funder appetite per product, and what about capacity?** Do funders set limits per product, and do they have allocations, concentration limits or pipeline caps that change availability over time? Is a funder's answer here binding?
6. **Who decides the priority?** Is lowest rate the right ordering, or do funder preference, speed or certainty of completion matter? Does the consumer of this API want one recommended option or the full list?
7. **Who sees the reasons?** If they reach customers or brokers, the wording needs review (compliance, tone, no leaking of funder criteria that are commercially sensitive).
8. **Is an audit trail required?** Regulated lending may need each request, the catalogue version used and the result stored and reproducible later. Today the response does not say which catalogue version produced it.
9. **How should rounding at the boundary work?** I round LTV up and cover down. Does Landbay round, truncate or compare exact values?
10. **Is a funder's margin constant?** It may vary by LTV tier, product or volume.
11. **Who owns the catalogue, and how often does it change?** That decides whether it needs approval workflows, effective dates, versioning or hot reload (today a change needs a restart).
12. **What are the volume, latency and availability expectations?**
13. **What is a real case reference?** I restricted the characters; the real format and whether it must be unique or idempotent are unknown.

---

## 8. What I left out, and what I would add before production

Deliberately out of scope for a take-home, in rough priority order:

- **Authentication, authorisation and rate limiting.** The brief excludes them. This service handles personal financial details, so it needs them (API keys, OAuth 2 or mTLS) plus limits at a gateway, including request-size limits.
- **Audit and persistence.** Store each request, the result and the catalogue version used, for regulatory traceability and complaints. Add the catalogue version to the response.
- **Observability.** Metrics (Micrometer to Prometheus), distributed tracing (OpenTelemetry), dashboards and alerts, and structured JSON logging. Today there is a per-request id on every log line, an INFO summary per request, and health, liveness and readiness probes.
- **Catalogue governance.** Versioning, approval, effective dates, hot reload, and a validation step in CI. Today the file is validated at startup only.
- **Real pricing and per-product funder limits** (see assumptions 5 and 6), and funder capacity.
- **API documentation as a contract:** an OpenAPI specification, consumer-driven contract tests and a versioning policy.
- **Delivery.** A Dockerfile, a CI pipeline, dependency and vulnerability scanning, and a code-coverage gate.
- **Load testing.** There is a concurrency-correctness test but no performance test.
- **Property-based tests** for the rounding boundaries, and compliance review of the reason wording.

---

## 9. Operating it

- **Health:** `/actuator/health`, plus `/actuator/health/liveness` and `/actuator/health/readiness` for a platform that restarts or routes around an unhealthy instance. Nothing else is exposed (tests confirm `env`, `beans`, `configprops`, `mappings`, `heapdump` and `loggers` are not reachable).
- **Request id:** every response carries an `X-Request-Id` header, and the same id appears on every log line for that request. A caller may send its own (letters, digits and `. _ -`, up to 64 characters); anything else is replaced, because ids end up in log files.
- **Response headers:** `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`, and `Cache-Control: no-store` on the API.
- **Shutdown:** graceful, finishing in-flight requests for up to 20 seconds.
- **Concurrency:** one `MatchingService` is shared by all requests. It is immutable, and a test runs 1,000 parallel requests against it and checks every answer.

---

## 10. Testing

`./mvnw verify` runs **166 tests**, none skipped.

- **Unit tests** for every rule, including the boundaries (exactly on a limit passes; just over fails), the rounding direction, and the `0.75` versus `0.7500` trap.
- **Scenario tests** against the real catalogue: a standard case, a lower-LTV case with more products, an eligible-but-unfundable company loan, Northern Ireland declined by everything, and the proof that price beats headline rate.
- **API tests** through the whole stack: the happy paths and every kind of bad input (missing fields, several bad values at once, too many decimals, broken JSON, unknown enum, wrong method, content type and path).
- **Startup tests** that boot a real application context with a deliberately wrong catalogue and check it refuses to start with a clear message.
- **Operations tests** for the request id, headers, probes and the endpoints that must *not* be exposed, plus the concurrency test.

I also ran the packaged jar and called it with real HTTP requests, including with an edited and with a deliberately broken external catalogue.

---

## 11. How I used AI

I used Claude Code as a pair-programming partner, working **one layer at a time** so that I understood each part before the next was added. I committed each layer myself after reading the files. In brief:

- **I set the process and made the key calls:** Maven over Gradle, closed enums, layer-by-layer with a commit per layer, and a comment on every snippet. I asked whether the rule classes should be merged into fewer files (they stayed separate), and I chose price-led ordering from options the AI laid out with pros and cons. I also declined an extra the AI proposed (a demo web page).
- **I wrote five files myself** from stubs and hints the AI supplied, which the AI then reviewed against its own tests: `LendingMetrics` (the LTV and ICR maths), `MaxLtvRule`, `AllowedValuesRule`, `FunderCriteria.evaluate` and `FunderCatalogue.all`.
- **From Layer 4 onwards I asked the AI to write the implementation**, so the matching service, ordering, API, configuration and operational pieces were AI-written to the design above, then read and committed by me.
- **The AI wrote the tests**, and proved its own hand-over tests correct by running them against a reference solution in a throwaway copy before giving me the stubs. I would then also run the tests against my own implementation before committing them.

[`PROMPTS.md`](PROMPTS.md) lists the significant prompts, the decisions made at each step, and the suggestions I changed or rejected.

---

## 12. Where to start reading the code

1. [`MatchingService.match`](src/main/java/com/landbay/matching/match/MatchingService.java): the two filters, in about 20 lines.
2. [`ProductCriteria.evaluate`](src/main/java/com/landbay/matching/eligibility/ProductCriteria.java) and [`FunderCriteria.evaluate`](src/main/java/com/landbay/matching/funding/FunderCriteria.java): run every rule, keep every failure.
3. [`MinIcrRule`](src/main/java/com/landbay/matching/eligibility/MinIcrRule.java) and [`LendingMetrics`](src/main/java/com/landbay/matching/domain/LendingMetrics.java): a typical rule, and the maths behind it.
4. [`ResultOrdering`](src/main/java/com/landbay/matching/match/ResultOrdering.java): the whole ordering policy.
5. [`CatalogueFactory`](src/main/java/com/landbay/matching/catalogue/CatalogueFactory.java) and [`catalogue.yml`](src/main/resources/catalogue.yml): configuration and its checking.
6. [`MatchController`](src/main/java/com/landbay/matching/api/MatchController.java) and [`ApiExceptionHandler`](src/main/java/com/landbay/matching/api/ApiExceptionHandler.java): the thin web layer.
