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

## Suggestions changed or rejected

- **Generated parent version corrected (Layer 0).** The Spring Initializr skeleton declared the Spring Boot parent as `4.1.1.RELEASE`. Before building, the AI checked Maven Central directly and found that this version returns HTTP 404; the published version is `4.1.1`. It was corrected in `pom.xml`, otherwise the first build would have failed. Lesson: verify generated scaffolding against the real repository rather than trusting it.
- **Skeleton tidied (Layer 0).** The generated `pom.xml` contained empty placeholder `<licenses>`, `<developers>` and `<scm>` blocks, and a generic `HELP.md`. These were removed as noise.

More entries will be added as the exercise progresses.
