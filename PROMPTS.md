# AI Usage and Significant Prompts

This file records how AI was used while building this exercise, the significant prompts, and any AI suggestions I changed or rejected. Prompts are lightly edited for grammar.

## How AI was used

I used Claude Code as a pair-programming partner. I set the way of working: build the solution **layer by layer**, so that I understand each part before the next one is added, and commit each layer myself after reading the files (the git history has one commit per layer). After each layer the AI explains what it built and why, and I can question or overrule it before moving on.

### Who did what

- **The AI** proposed the layer plan and the detailed design, wrote the tests, and implemented everything from Layer 4 onwards (the matching service, result ordering, HTTP API, configuration and operational pieces).
- **I wrote five files myself** in Layers 1 to 3, from stubs and hints the AI supplied, which the AI then reviewed against its tests: `LendingMetrics` (the LTV and ICR maths), `MaxLtvRule`, `AllowedValuesRule`, `FunderCriteria.evaluate` and `FunderCatalogue.all`.
- **I made the process decisions and chose between the options the AI put to me**, listed below. Where I gave a layer a plain "go", I was delegating the detailed design, and the entry says so.

### Where I steered the AI

| Layer | What I directed or decided |
|---|---|
| 0 | Chose **Maven over Gradle** (familiar to reviewers, and the wrapper means nothing to install). Approved the plan and the downloads. Asked for a test that proves the app really **responds**, not just that it compiles. |
| 1 | Chose **closed enums** for better error handling. Decided that **I make the commits myself**, one file at a time, so I read everything. Asked for **a comment on every snippet**. Asked to **write a practice file myself** to get used to Java again. |
| 2 | Asked to continue the practice-file approach. **Questioned whether the eligibility rules should be merged into fewer files**; the AI advised keeping them separate (merge only where the logic is identical) and I went with that. |
| 3 | Asked for the item still owed from Layer 2 (the product catalogue) to be completed in the same step. Wrote the funder logic myself. |
| 4 | Asked for **pros and cons of the ordering options** instead of accepting a default, then **chose the price-led recommendations**. Told the AI to **write the implementation from here on**, to move faster. |
| 5 | Directed the AI to build the HTTP API from the agreed plan, and delegated the detailed design. |
| 6 and 7 | Asked for **both layers together**. Asked to see the front end; on learning there is none, **told the AI not to build a demo page** and to show me how to run the application instead. |
| 8 | Asked for the README, and asked that this file make my direction visible. |

## Prompts

### 1. Starting the exercise

> I have pasted the full Landbay take-home brief (Mortgage Product and Funder Matching). I want to build the solution layer by layer so that I understand each part as it is built. Please also explain in plain English what the exercise actually requires.

**Outcome:** The AI explained the problem (two filters: product eligibility, then funder appetite, with structured "why not" explanations), listed the domain terms (LTV, ICR, HMO, SPV), listed ambiguities in the brief, and proposed nine layers (0–8).

### 2. Project location, build tool and Layer 0

> Use Maven rather than Gradle, because it is more widely known and easier for a reviewer to run. Please start Layer 0 (the project scaffold) and finish with a small test to confirm that everything works.
>
> When recording my prompts in this file, please correct the grammar and expand short decisions so that the reasoning is clear.

**Decisions made:**

- **Maven over Gradle.** Maven is more familiar to most reviewers, and the Maven wrapper (`mvnw`) means a reviewer needs only a JDK installed, not Maven itself.
- **Layered build.** One layer at a time, each ending with a verification step.

### 3. Approving the Layer 0 downloads

> Yes, go ahead with the downloads (the Spring Initializr project skeleton, Apache Maven via the wrapper, and the Spring Boot dependencies). Use `com.landbay` as the group, and include only Spring Web and Actuator for now. The "small test" at the end should confirm that the application genuinely responds as expected, not just that it compiles.

**Outcome:** Layer 0 complete. Spring Boot 4.1.1 on Java 21, built with the Maven wrapper. Verified two ways: an automated test (`HealthEndpointTests`) and a real server started from the packaged jar and called over HTTP.

### 4. Layer 1: the domain model, with a practice file

> I will make the git commits myself, so that I can read each file, understand what it does and then commit it.
>
> For Layer 1, use closed enums, because they give better error handling. Please build Layer 1 now. I also want to do some of the coding myself, for practice and to get used to Java again, so please leave me a file to write and tell me what to do and which code snippets to add. You will review it afterwards and correct anything I got wrong.

**Decisions made:**

- **Closed enums over free-text strings** for property type, applicant type and location. An unknown value is rejected at the edge of the system with a clear error, instead of travelling into the eligibility rules, and the compiler checks every use.
- **Manual commits, one file at a time,** so that I read and understand everything that goes into the repository.
- **A hands-on practice file.** The AI wrote the tests first and left `LendingMetrics` as three stubbed methods (loan-to-value, annual rent, interest cover ratio) for me to implement. Its tests are `@Disabled` until I am ready, so the build stays green in the meantime. The AI then reviews my version.

**Outcome:** Layer 1 complete: three enums, `MortgageApplication`, `Product`, `Funder`, a shared validation helper, and tests. The AI proved its tests correct by running them against a reference solution in a throwaway copy of the project (all 10 passed) while leaving my stubs untouched.

### 5. Review of my Layer 1 code, then Layer 2: product eligibility rules

> I believe my `LendingMetrics` implementation worked. Please review it, then remove the exercise comments from that file and tell me whether it is clear, so that I can commit it. After that, move on to Layer 2 using the same approach: leave me a file or two to write myself, and correct anything I get wrong.

**Review result:** My implementation passed all 10 tests first time and matched the AI's reference answer. The only change was removing a stray blank line. The exercise instructions (TODOs and hints) were replaced by short comments that say what each method does and why it rounds the way it does, because the standing rule is that every snippet is commented.

**Decisions made for Layer 2:**

- **One small rule object per lending criterion** (`EligibilityRule`), instead of one large method full of `if` statements. Each rule can be tested alone, reused across products, and later built from configuration. A product's eligibility is just a list of rules.
- **Every failing rule is reported, not only the first.** An integrating system then sees everything wrong with an application in one response.
- **Failures are structured, not just text.** Each failure is a `Reason` with a closed-enum `ReasonCode`, a readable message, the actual value and what would have passed. This is what the brief means by "enough information for another system to understand why".
- **Compare money with `compareTo`, never `equals`,** because `BigDecimal` treats 0.75 and 0.7500 as different numbers. The `MaxLtvRule` tests are written to catch this mistake.
- **My practice files this time:** `MaxLtvRule` and `AllowedValuesRule` (a generic rule covering property type, applicant type and location). Their tests are `@Disabled` until I implement them.

**Outcome:** Layer 2 scaffolding complete: 63 tests, 10 of them waiting on my two files. The AI verified those 10 against a reference solution in a throwaway copy before handing them over.

### 6. Review of my two rule files, and a design question about file count

> For the eligibility rules, should we have fewer Java files, or is it better to keep them all separate? Please also check my two files, remove the exercise comments and tell me whether they are correct.

**Review result:** `MaxLtvRule` and `AllowedValuesRule` both passed all 10 of their tests first time (63 tests, none skipped). The AI cleaned up formatting only: spacing such as `if(` to `if (`, a trailing-whitespace line, and an inline `java.util.stream.Collectors` replaced by a normal import. The exercise instructions were replaced by short comments explaining what and why.

**My question and the answer I went with:** I asked whether fewer files would be better. The AI advised keeping one file per rule and merging only where the logic is genuinely identical (which is why the three "must be one of these values" checks are already one generic class). I accepted that.

### 7. Layer 3: funder appetite, plus the product catalogue still owed from Layer 2

> Please start Layer 3 (funder appetite), and also complete the item still owed from Layer 2: the product catalogue with an end-to-end test.

**Decisions made:**

- **Funders reuse the eligibility rules.** A funder saying "no HMOs" or "maximum 70% LTV" is the same kind of check as a product's own criteria, so a funder is a list of the same `EligibilityRule` objects plus the set of products it funds. A funder can only narrow what a product allows (product eligibility is checked first), never widen it.
- **A funder that does not fund a product at all gives one reason, `PRODUCT_NOT_FUNDED`,** and its other rules are ignored. Listing LTV or location problems for a product it would never fund is noise.
- **Known limitation, to document:** a funder's rules apply to every product it funds. Different limits per product for the same funder would need the rules held per product instead.
- **Three invented funders with deliberately different appetites,** to demonstrate the problem: a cautious bank (strict LTV, interest cover, loan size, no HMOs, no Scotland), a pension fund (experienced landlords only, larger loans) and a credit fund (funds everything, wider limits). Between them they exercise product, lending-type and risk appetite.
- **Catalogues are hardcoded for now** (`ProductCatalogue`, `FunderCatalogue`) as a stepping stone; a later layer replaces them with configuration files. A test checks that every product id a funder lists really exists, so a typo cannot silently mean "funds nothing".
- **The system must handle "eligible but unfunded".** A test builds a case where a product accepts the application but no funder will fund it, because that is a result the API has to explain.

**Outcome:** Layer 3 scaffolding and the Layer 2 catalogue are complete: 87 tests, 14 of them waiting on my two files. The AI verified those 14 against a reference solution in a throwaway copy before handing them over.

### 8. Review of my two funder files, and the pros and cons of how to order results

> Please check my two files and tell me whether they are good, so that I can commit and we can start Layer 4. Can you also give me the pros and cons for the decisions on how we should order results?

**Review result:** `FunderCriteria.evaluate` and `FunderCatalogue.all` were both correct and matched the AI's reference solution. The exercise instructions were replaced by short what-and-why comments. One housekeeping fix: `FunderCatalogueTest` had been left `@Disabled`, so my catalogue had not actually been exercised yet; once enabled, all 87 tests passed with none skipped.

**Decision requested:** how Layer 4 should order products, funders and unavailable options. The AI laid out the options with pros and cons for four separate decisions (the order of eligible products, the order of funders within a product, how to present unavailable options, and tie-breaks and where the ordering code lives) and recommended a price-led approach with a cheaper catalogue-order fallback.

### 9. Layer 4: the matching service, with price-led ordering

> Yes, let's go with your recommendations, and start Layer 4.

**My call:** I chose the price-led recommendations over the cheaper alternatives (catalogue order everywhere, an explicit funder priority number, or ranking by headroom or funder count).

**Decisions that follow from that choice:**

- **Price-led ordering.** The brief says a higher-risk funder needs a higher interest rate, so price is the one idea that drives all ordering. Each product has an indicative rate, each funder has a margin, and the rate on offer is the product's rate plus the funder's margin (a cautious bank is cheapest, a credit fund dearest).
- **Eligible products are ranked by the best rate actually on offer, not by their own headline rate.** A product that looks cheap but whose only funder charges a large margin ranks below a dearer product offered at a lower rate. A product nobody can fund has no offer, so it comes after all fundable products, ordered by its own rate.
- **Two sections instead of one mixed list:** eligible products (each with funding options and the funders that declined, with reasons) and ineligible products (nearest to qualifying first, meaning fewest reasons). "Fewest reasons" is a rough measure, which is worth stating as a limitation.
- **Every funder appears for every eligible product,** either as a funding option with its rate or as a decline with its reasons. This answers "why not funder X?" directly.
- **Every ordering ends with an id tie-break,** so the same input always gives the same output.
- **The ordering lives in one separate class (`ResultOrdering`),** apart from the matching logic, so a changed ordering requirement touches one file.
- **The margin sits on `Funder`, not `FunderCriteria`** (a change from what the AI first suggested). It is the funder's own commercial property, and this kept `FunderCriteria` and its tests untouched.
- **`MatchingService` is plain Java, with no Spring.** It is handed its products and funders, so it can be tested with any data and wired into the web layer later.
- **My practice files this time:** `ResultOrdering` (four comparators) and `MatchingService.fundingFor` (the funder loop).

**Outcome:** Layer 4 scaffolding complete: 110 tests, 16 of them waiting on my two files.



### 10. Layer 5: the HTTP API

> Okay, start Layer 5.

**My instruction:** Build the HTTP API from the plan I approved in entry 1 (an endpoint, validation, error handling), with the AI writing it, as agreed in entry 10. I gave no further detail, so I was **delegating the design**; the choices below are the AI's proposals, which I accepted and committed.

**The AI's design proposals:**

- **One endpoint: `POST /api/v1/matches`, returning 200 OK.** POST because the input is a body; 200 rather than 201 because nothing is stored. The version in the path lets the contract evolve later.
- **The API contract is separate from the internal model** (`MatchRequest` and `MatchResponse` versus `MortgageApplication` and `MatchResult`), so either can change without breaking the other.
- **Validation reports every bad field at once,** using Bean Validation annotations on the request. Domain checks remain as a second line of defence, so a rule drifting out of step would show up as a server error rather than a silent wrong answer.
- **`hasLandlordExperience` is a `Boolean`, not a `boolean`,** so leaving it out is an error instead of silently meaning "no experience". For an eligibility decision, a wrong default is worse than a rejected request.
- **Enum values must match exactly** (upper case). An unknown value is rejected with a message listing the accepted values.
- **Defensive limits on input:** the case reference is limited to 64 letters, digits and `. _ / -` so it is safe to log and echo; money allows at most 12 whole digits and 2 decimal places. Both are assumptions to confirm with the business.
- **Errors use RFC 9457 problem responses** (`application/problem+json`), including 404, 405 and 415, extending Spring's standard handler. A catch-all handler returns a generic message and logs the real exception, so internals never leak.
- **Rates are shown as percentages** (`5.09` meaning 5.09%) by moving the decimal point on the exact `BigDecimal`, never by floating-point multiplication.
- **`MatchingService` is wired in one small `@Configuration` class,** so the matching code stays plain Java and the next layer only has to change where its data comes from.

**Outcome:** Layer 5 complete: 130 tests, all passing. The AI also started the real packaged application and called it with `curl`, checking a successful match, an unknown enum value, several bad values at once and missing fields.

### 11. Layers 6 and 7 together: configuration and production polish

> Can you do both Layer 6 and Layer 7 together, please?
>

- **Products and funders now live in `catalogue.yml`,** not in Java. A rate, a limit or a whole product can change without recompiling. The two hardcoded catalogue classes were deleted, and every existing test now runs against the YAML file, so the unchanged scenarios prove the file behaves exactly as the old code did.
- **The file is checked completely at startup, and the application refuses to start if it is wrong.** The checker reports every problem together with where it is (for example `funders[1] (ATLAS_PENSION_FUND): 'funds-products' lists 'STANDARD_757'...`), so one edit can fix them all. Running with a half-valid catalogue would give wrong answers to real applications, which is worse than not starting.
- **It catches mistakes a simple parser would miss:** a missing parameter, a parameter a rule does not use (usually a typo or one on the wrong rule), an unknown enum name (with the accepted values listed), duplicate ids, a funder listing a product that does not exist, and a product with no rules (it would accept everyone).
- **A product no funder lists is a warning, not an error,** because "eligible but unfunded" is a legitimate outcome the system is designed to explain.
- **Rule types are a closed enum,** consistent with the earlier decision to use closed enums, so a misspelt rule type stops startup instead of being ignored.
- **The catalogue can be replaced without rebuilding** by starting the application with `--spring.config.additional-location=file:./catalogue.yml`. This was tested: changing STANDARD_75's rate in an external copy changed the rates in the response.
- **Known quirk:** YAML drops trailing zeros, so a limit written `0.70` is read as `0.7` and appears as `0.7` in messages. The numeric behaviour is identical; only the displayed text differs.


- **Every request gets an id** (`X-Request-Id`), returned in the response and written on every log line, so one request can be followed through the logs. A caller-supplied id is only trusted if it is short and made of plain characters, because it ends up in log files and an id containing a line break could forge log lines.
- **Safe response headers** (no sniffing, no framing, no referrer) on everything, and `Cache-Control: no-store` on the API, since responses describe someone's mortgage application. This is sensible defaults rather than a security layer: authentication was out of scope in the brief.
- **Only the health endpoint is exposed,** with separate liveness and readiness probes. A test confirms that `env`, `beans`, `configprops`, `mappings`, `heapdump` and `loggers` return 404, so nothing reveals configuration.
- **Graceful shutdown** (in-flight requests finish, up to 20 seconds) and a connection timeout.
- **Thread-safety is tested, not just asserted:** 1,000 requests of two kinds run in parallel against the single shared matching service, and every answer is checked.

**Outcome:** Layers 6 and 7 complete: 166 tests, all passing. The AI tested the real application three ways: with an edited external catalogue, with a deliberately broken catalogue (refused to start, listing three problems), and using the exact run commands given to me.

## Suggestions changed or rejected

- **Demo page proposed, then dropped (Layer 7).** The AI began to plan a small web page so I could see the application working. I stopped it, because the brief says no user interface is needed and what I needed was instructions for running it. Lesson: when asked for something that does not exist ("the link to the front end"), explain what exists and ask what is actually needed, rather than building an extra.
- **Practice-file approach ended at my request (Layer 4).** The AI's way of working was to leave me a file to write each layer.
- **Ordering: I chose price-led over the simpler alternatives (Layer 4).** The AI offered catalogue order everywhere as a lower-effort fallback; I took the fuller recommendation so ordering has a real justification (the brief's link between risk and rate).
- **Generated parent version corrected (Layer 0).** The Spring Initializr skeleton declared the Spring Boot parent as `4.1.1.RELEASE`. Before building, the AI checked Maven Central directly and found that this version returns HTTP 404; the published version is `4.1.1`. It was corrected in `pom.xml`, otherwise the first build would have failed. Lesson: verify generated scaffolding against the real repository rather than trusting it.
- **Skeleton tidied (Layer 0).** The generated `pom.xml` contained empty placeholder `<licenses>`, `<developers>` and `<scm>` blocks, and a generic `HELP.md`. These were removed as noise.
- **The AI changed its own first design for the funder margin (Layer 4).** It first suggested putting the margin on `FunderCriteria`, then moved it to `Funder`, which kept an already-committed class and its tests untouched.
