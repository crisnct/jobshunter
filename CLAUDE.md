# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

JobsHunter: a Spring Boot 4 / Java 25 backend that automatically searches jobs on behalf of a user by
sending their CV + preferences to AI providers (OpenAI/GPT, Google Gemini, xAI Grok) or SERP (Google
Jobs), validates/scores the results, and notifies the user by WhatsApp (Twilio) or email (Mailtrap).
A minimal React (Vite + Tailwind) frontend is served from the same jar as static resources. The whole
codebase runs as a two-module Maven build:

- `annotation-processor` — a compile-time annotation processor library (see below), consumed by `application`.
- `application` — the Spring Boot app (`com.jobshunter.JobshunterApplication`), everything else.

## Build & run

```bash
# Build everything (both modules)
mvn clean install

# Run the backend (defaults to the `local` Maven profile -> spring.profiles.active=local)
mvn -pl application spring-boot:run

# Run with a specific Spring profile
mvn -pl application spring-boot:run -Pprod
```

- Java 25, Maven multi-module (root `pom.xml` is the parent; `application/pom.xml` is the real app).
- `application`'s Maven build also drives the frontend via `frontend-maven-plugin`: it installs Node
  20.11.1, runs `npm install` and `npm run build` against the root `package.json`/`vite.config.js`
  (Vite root is `application/src/main/resources/static`, output goes to
  `application/target/classes/static`), so a plain `mvn clean install` also builds the UI.
- IntelliJ run configs exist under `.run/` (`JobsHunter-local.run.xml`, `JobsHunter-prod.run.xml`) —
  both invoke `com.jobshunter.JobshunterApplication`, differing only by `-Dspring.profiles.active`.
- Spring profiles: `local` (default; disables all real AI/notification providers and wires the
  `Fake*Client` beans in `service/testdata/` so nothing hits paid APIs), `prod`, `test`, `cds`.

### Frontend only

```bash
npm install
npm run dev     # Vite dev server on :5173, proxies to backend on :8443
npm run build
npm run lint
```

Frontend source lives at `application/src/main/resources/static/src/` (currently just
`App.jsx`, `main.jsx`, `config/apiConfig.js`, `index.css`) — deliberately minimal/early-stage, not a
full SPA yet.

### Tests

```bash
# All tests (application module)
mvn -pl application test

# Single test class
mvn -pl application test -Dtest=ValidationRulesTest

# Single test method
mvn -pl application test -Dtest=ValidationRulesTest#someMethodName
```

JUnit 5 + Mockito + WireMock (`wiremock-jre8-standalone`) + H2 (in-memory DB for tests) + Spring's
`spring-boot-starter-test`/`spring-boot-starter-webmvc-test`. There is no dedicated Maven `test`
profile setup beyond the standard Surefire run; tests live under
`application/src/test/java/com/jobshunter/**` and mostly cover `security/` (JWT, delegated-auth
filters) and `service/application/` (validation rules, second-level cache, an `IntegrationTests`
class). Test coverage is currently thin — most packages (hunting, clients, controllers other than
`InternalMcpController`) have no tests yet.

### Docker / deploy

```bash
docker compose --profile local up --build -d   # full local stack (app + ngrok + nginx-proxy)
docker compose --profile aws up -d             # AWS-style stack (app + MySQL + nginx-proxy)
```

`Dockerfile` is a two-stage build: Maven build of `application` module, then a runtime image with the
native library set Playwright/Chromium needs (this app drives a real headless browser). CI
(`.github/workflows/deploy.yml`) builds the image, pushes to ECR, and deploys to EC2 over SSH on every
push to `main`.

## Architecture

### Request flow (high level)

`UserController`/`CvController` collect a user's CV, prompts, and job preferences into a
`JobOrderEntity`. `JobHuntScheduler` (in `service/application/scheduler/`) periodically drains pending
orders through `JobOrderProcessor.process(orderId)`, which builds a `SearchJobOrder` and delegates to
`JobHuntService` → `HuntingOrchestrator`. The same entry point (`JobOrderProcessor`) is also invoked
synchronously from `InternalMcpController` for MCP-delegated on-demand searches.

### Hunting pipeline (`service/application/hunting/`)

This is the core of the app; a full architecture write-up (with Mermaid class/component/sequence
diagrams) lives in `service/application/hunting/architecture/hunting-architecture.md` — read it before
touching this package. Summary:

- `JobHunting` is a **sealed interface**; `HuntingOrchestrator` picks the implementation by
  `EngineType` (GPT / Grok / Gemini / SERP) and fans out in parallel via `CompletableFuture` for
  "search by prompt" and "search by company" simultaneously, then de-dupes by URL across all sources.
- `GenericJobHunting` (abstract) is the base: builds a provider-specific request, calls the
  corresponding `AiJobsClient` implementation (`service/clients/{gpt,gemini,grok,serp}/`), then pipes
  results through `JobsStateMachine`.
- `AiConversationJobHunting` (abstract, extends `GenericJobHunting`) is used by GPT and Grok: these
  providers support a stateful "conversation" where rejected jobs are fed back with a corrective
  prompt for retries (`AiConversationStateMachine`, bounded by `maxRetries`). Gemini and SERP are
  stateless/direct — no retry conversation.
- `JobsStateMachine` (`service/application/processors/`) runs every candidate `Job` through an
  ordered pipeline of `PipelineStep`s, each on its own `Executor` (see
  `processors-architecture-flow.puml` in `processors/architecture/`):
  `BASIC_CHECK` → `FETCH` (real HTTP/Playwright fetch of the job page) → `BODY_EXTRACTION` →
  `VALIDATION` (rule-based, see `processors/validation/rules/`) → `SCORING` (an AI call that scores
  match against the user's CV, engine chosen by `JobScoringProcessor.ENGINE_SELECTION`).
- Real page fetching goes through `service/clients/browser/` (`PlaywrightManager`,
  `PlaywrightFetchPage`, `HttpFetcher`) — Playwright drives a real Chromium instance, which is why the
  Docker runtime image needs the full native browser dependency set.
- `service/testdata/Fake*Client` classes are alternate Spring beans (active on the `local` profile)
  that fabricate deterministic responses for every external integration (GPT/Gemini/Grok/SERP/Twilio/
  Mailtrap/IpInfo/Scraper) so the whole pipeline can run end-to-end with zero API keys/cost.

### Resilience

Every outbound integration (GPT, Grok, Gemini, SERP, Twilio, Mailtrap, TinyURL, IpInfo, the internal
scraper) is wrapped with Resilience4j rate limiter + circuit breaker + bulkhead instances, configured
per-provider in `application.yml` (see the heavily-commented `resilience4j.*` sections) and overridden
more permissively in `application-local.yml`. `service/retry/` adds an additional custom retry layer
on top for HTTP calls. Virtual threads are enabled (`spring.threads.virtual.enabled: true`); dedicated
named executors (playwright fetch, per-provider search, job processing, orders, notifications) are
configured in `service/ExecutorsConfig.java` and sized via `jobshunter.threads.*`.

### Security (`security/`)

Two independent Spring Security filter chains in `SecurityConfig`, ordered:

1. `/api/internal/**` (`@Order(1)`) — stateless OAuth2 resource server validating a **delegated** JWT
   issued by an external MCP Authorization Server (not this app's own JWT). Validated by issuer,
   audience, and a custom `token_use` claim, all configured via `DelegatedAuthProperties`
   (`jobshunter.security.delegated-auth.*` / `DELEGATED_AUTH_*` env vars — see README's "MCP Internal
   AS delegated auth" section). This is how the JobsHunter MCP server calls back into this app on a
   user's behalf (`InternalMcpController`).
2. Everything else (`@Order(2)`) — normal session-less app auth: this app's own JWT
   (`JwtAuthenticationFilter`/`JwtService`, HS-signed, `jobshunter.security.jwt.*`), Google OAuth2
   login (`oauth2Login`, success/failure handlers persist/merge the user via `OAuth2UserDBService`),
   CSRF via a double-submit cookie (`XSRF-TOKEN`) except on `/api/auth/**`, `/api/internal/**` and
   OAuth2 endpoints, plus `DeviceIdFilter`/`CookieService` for device-bound refresh tokens
   (`RefreshTokenService`).

Note: at the time of writing, `DelegatedTokenAuthenticationFilter` and its three tests show as staged
deletions in git status — the delegated-auth internal filter chain is mid-refactor; check `git log`/
`git diff` before assuming the filter still exists.

Both chains share `SecurityHeadersFilter`, `AdditionalHeadersFilter`, `RateLimitingFilter` (in-memory,
bucket4j-backed, `security/rateLimitBucket4J/`), and `CorrelationIdFilter` (correlation ID propagated
via `MdcContextPropagationConfig` for log tracing across virtual threads/executors).

### Annotation processor module

Two independent compile-time processors, applied to `application` via
`<annotationProcessorPaths>` in `application/pom.xml`:

- `@PackageExpected(value = "...")` (`PackageUsageProcessor`) — fails the build if the annotated class
  isn't declared in one of the listed packages. Used to pin architectural boundaries.
- `@SqlInjectionSafe` (`SqlInjectionValidatorProcessor`) — fails the build if an annotated `String`
  field/parameter's literal (or default) value matches a known SQL-injection pattern.

### Database

MySQL in prod/AWS profiles, H2 in-memory for tests; schema managed by Liquibase
(`db/changelog/db.changelog-master.xml`, `liquibase.enabled` toggle). Hibernate 2nd-level cache is
enabled via Ehcache/JCache (`ehcache.xml`, `hibernate.cache.*` in `application.yml`) — see
`SecondLevelCacheTest`. `database/architecture/database-schema.puml` documents the schema. Entities
live in `database/entities/`, Spring Data repositories in `database/repository/`, and a thin
`*DBService` layer in `database/service/` sits between controllers/processors and repositories.

### AI provider integration pattern

Each provider (`service/clients/{gpt,gemini,grok,serp}/`) implements the shared `AiJobsClient`
contract plus provider-specific request/response DTOs under `dto/{gptRequest,gptResponse,...}`.
Prompts are Mustache templates (`resources/prompts/*.mustache`, rendered by `TemplateRenderer`) and
JSON response schemas are static files under `resources/schema/`. `service/application/cost/` tracks
token usage/cost per request (`TokenEstimationGuard`, `AiRequestCostEvent` + listener,
`RequestPriceService`) — an AI call is pre-checked against an estimated cost/token budget before being
sent.
