# AI Usage and Significant Prompts

This file records how AI was used while building this exercise, the significant prompts, and any AI suggestions I changed or rejected. Prompts are lightly edited for grammar.

## How AI was used

I used Claude Code as a pair-programming partner. The agreed way of working was to build the solution **layer by layer**, so that I understand each part before the next one is added. After each layer the AI explains what was built and why, and I can ask questions or challenge decisions before moving on.

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

> Please check my two files, remove the exercise comments and tell me whether they are correct.

**Review result:** `MaxLtvRule` and `AllowedValuesRule` both passed all 10 of their tests first time (63 tests, none skipped). The AI cleaned up formatting only: spacing such as `if(` to `if (`, a trailing-whitespace line, and an inline `java.util.stream.Collectors` replaced by a normal import. The exercise instructions were replaced by short comments explaining what and why.


## Suggestions changed or rejected

- **Generated parent version corrected (Layer 0).** The Spring Initializr skeleton declared the Spring Boot parent as `4.1.1.RELEASE`. Before building, the AI checked Maven Central directly and found that this version returns HTTP 404; the published version is `4.1.1`. It was corrected in `pom.xml`, otherwise the first build would have failed. Lesson: verify generated scaffolding against the real repository rather than trusting it.
- **Skeleton tidied (Layer 0).** The generated `pom.xml` contained empty placeholder `<licenses>`, `<developers>` and `<scm>` blocks, and a generic `HELP.md`. These were removed as noise.

More entries will be added as the exercise progresses.
