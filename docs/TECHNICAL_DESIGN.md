# Technical & Functional Design — TrackMyFunds

| Field            | Value                                              |
|------------------|----------------------------------------------------|
| Project Name     | TrackMyFunds — Personal Finance Tracker            |
| Document Type    | Technical & Functional Design Specification        |
| Version          | 1.0                                                |
| Date             | 2026-05-18                                         |
| Author           | Prachi Dnyaneshwar Parate                          |
| Audience         | Developers, reviewers, future maintainers          |

---

## 1. System Overview

TrackMyFunds is a monolithic Spring Boot 3 web application that combines:

- A **server-rendered MVC interface** (Thymeleaf + Bootstrap 5) for user interaction
- A **REST API** (`/api/expenses/*`) for programmatic access
- A **JPA persistence layer** (H2 in development, PostgreSQL in production)
- Five **AI features** powered by Google Gemini 2.5 Flash, fronted by an in-memory rate limiter and graceful fallbacks
- Standard **DevOps tooling** — Maven build, Docker image, GitHub-pushed source, Render-hosted deployment

The application runs as a single JVM process, listens on port 8080, and exposes both human (HTML) and machine (JSON) interfaces over HTTP.

---

## 2. Architecture

### 2.1 High-level architecture

```
            ┌──────────────────────────────────────────────────────────────┐
            │                       Browser (User)                          │
            │      Bootstrap 5 + Chart.js + Web Speech API + ES2020         │
            └────────────────────────┬─────────────────────────────────────┘
                                     │ HTTP/HTTPS
                                     ▼
            ┌──────────────────────────────────────────────────────────────┐
            │              Spring Boot 3.4.5  (Tomcat embedded)             │
            │                                                              │
            │  ┌───────────────┐  ┌────────────────┐  ┌──────────────────┐ │
            │  │  MVC Layer    │  │  REST Layer    │  │ ControllerAdvice │ │
            │  │ @Controller   │  │ @RestController│  │ @ExceptionHandler│ │
            │  └───────┬───────┘  └────────┬───────┘  └──────────────────┘ │
            │          │  Thymeleaf        │  Jackson                       │
            │          ▼                   ▼                                │
            │  ┌──────────────────────────────────────────────────────────┐ │
            │  │              Service Layer (business logic)               │ │
            │  │  ExpenseService · BudgetService · CsvExportService        │ │
            │  │  GeminiService · RateLimitService                         │ │
            │  └─────┬───────────────────────────────────────────┬────────┘ │
            │         │                                          │          │
            │         ▼                                          ▼          │
            │  ┌─────────────────┐                  ┌──────────────────────┐│
            │  │ Repository      │                  │  External AI         ││
            │  │ JpaRepository + │                  │  Google Gemini 2.5   ││
            │  │ Specifications  │                  │  REST API            ││
            │  └────────┬────────┘                  └──────────────────────┘│
            └─────────────┼───────────────────────────────────────────────┘
                          │ JDBC + Hibernate
                          ▼
            ┌──────────────────────────────────────────────────────────────┐
            │   PostgreSQL 16 (prod / Render)   or   H2 in-memory (dev)    │
            │   Schema owned by Flyway migrations in prod                  │
            └──────────────────────────────────────────────────────────────┘
```

### 2.2 Layered architecture (request flow)

A typical request flows through five layers:

1. **View / Browser** — initiates an HTTP request from a form submit, fetch call, or page navigation
2. **Controller** — Spring `@Controller` (HTML) or `@RestController` (JSON) maps the URL pattern to a method, binds request params/path variables, performs validation, calls services, and returns a view name or response body
3. **Service** — orchestrates business logic; transactional boundaries are declared here with `@Transactional`
4. **Repository** — Spring Data JPA interface; query methods are derived from method names or written with `@Query`
5. **Database** — H2 or PostgreSQL handles the actual SQL

Cross-cutting concerns sit beside this stack:
- **`@ControllerAdvice`** classes (`GlobalExceptionHandler`, `AiExceptionHandler`) centralise error handling
- **`Specification`** classes (`ExpenseSpecification`) compose dynamic filter predicates
- **Configuration** (`AppConfig`) declares `@Bean` definitions for `RestTemplate` and enables JPA auditing

### 2.3 Architectural patterns used

| Pattern               | Where it lives                                                  | Why                                        |
|-----------------------|-----------------------------------------------------------------|--------------------------------------------|
| Layered architecture  | Controller → Service → Repository → Entity                      | Separation of concerns                      |
| DTO pattern           | `dto/` package                                                  | Decouple wire format from JPA entities      |
| Specification pattern | `ExpenseSpecification`                                          | Dynamic filter composition with no IF-chains|
| Dependency injection  | Constructor injection everywhere (Lombok `@RequiredArgsConstructor` or explicit) | Testability, immutability      |
| Interface + impl      | `ExpenseService` + `ExpenseServiceImpl`                        | Mockable in tests, polymorphic if needed    |
| Centralised error handling | `@ControllerAdvice` classes                                | Consistent error responses                  |
| Hexagonal-ish boundary | `GeminiService.callGemini()` is the only point that talks HTTP to Gemini | External system abstraction        |

---

## 3. Technology Stack

### 3.1 Backend

| Technology               | Version       | Purpose                                                |
|--------------------------|---------------|--------------------------------------------------------|
| Java                     | 17 (target), 21 (runtime) | Application language                       |
| Spring Boot              | 3.4.5         | Application framework, embedded Tomcat                 |
| Spring MVC               | 6.2 (Spring 6)| HTTP routing, request binding, view resolution         |
| Spring Web               | 6.2           | REST endpoints                                         |
| Spring Data JPA          | 3.4           | Repository abstraction over JDBC                       |
| Hibernate                | 6.6           | JPA implementation, dialect support, criteria API      |
| Bean Validation (Hibernate Validator) | 8.0 | JSR-380 annotations on entities/DTOs              |
| Lombok                   | 1.18          | Reduce boilerplate (@Data, @Builder, @RequiredArgsConstructor) |
| Jackson                  | 2.18          | JSON serialisation/deserialisation; LocalDate via JavaTimeModule |
| RestTemplate             | 6.2           | HTTP client for outbound calls to Gemini               |

### 3.2 Frontend (server-rendered)

| Technology               | Version       | Purpose                                                |
|--------------------------|---------------|--------------------------------------------------------|
| Thymeleaf                | 3.1           | Server-side HTML template engine                       |
| Thymeleaf Layout Dialect | 3.3           | `layout:decorate`, `layout:fragment` for template inheritance |
| Bootstrap                | 5.3.3         | Responsive CSS framework                               |
| Bootstrap Icons          | 1.11.3        | Icon font used throughout                              |
| Chart.js                 | 4.4.4         | Doughnut + bar charts on dashboard                     |
| Web Speech API           | browser-native| Voice input in Chromium-based browsers                 |
| ES2020 JavaScript        | n/a           | Optional chaining, nullish coalescing, async/await     |

### 3.3 Database & Migrations

| Technology               | Version       | Purpose                                                |
|--------------------------|---------------|--------------------------------------------------------|
| H2                       | 2.x (Spring-managed) | In-memory dev database                          |
| PostgreSQL               | 16 (Render-provisioned, 18.3 observed) | Production database         |
| Flyway                   | 11.10.0       | Schema migration tool (prod only)                      |
| HikariCP                 | 5.1           | Connection pooling (Spring-default)                    |

### 3.4 AI / ML

| Technology               | Version       | Purpose                                                |
|--------------------------|---------------|--------------------------------------------------------|
| Google Gemini API        | 2.5 Flash (v1beta endpoint) | LLM for NL parsing, chat, insights       |
| Custom RateLimitService  | n/a           | In-memory `AtomicInteger` + windowed timestamp         |

### 3.5 Build & DevOps

| Technology               | Version       | Purpose                                                |
|--------------------------|---------------|--------------------------------------------------------|
| Maven                    | 3.9+          | Build, dependency management                           |
| Docker                   | 24+           | Containerisation (multi-stage build)                   |
| Docker Compose           | 2.x           | Local prod-like environment with PostgreSQL            |
| Render                   | n/a           | PaaS hosting (web service + Postgres free tier)        |
| GitHub                   | n/a           | Source control, Render's auto-deploy trigger           |
| Eclipse Temurin          | 21-alpine     | Base JRE image for the runtime layer                   |

### 3.6 Testing

| Technology               | Version       | Purpose                                                |
|--------------------------|---------------|--------------------------------------------------------|
| JUnit Jupiter            | 5.11          | Unit / integration test runner                         |
| Mockito                  | 5.14          | Mocking framework for service-layer unit tests         |
| AssertJ                  | 3.26          | Fluent assertions                                      |
| Spring Boot Test         | 3.4.5         | `@WebMvcTest`, `@DataJpaTest`, `MockMvc`, `@MockBean`  |
| Hamcrest                 | 2.2           | `Matchers.startsWith(...)` and friends                 |

### 3.7 Auxiliary libraries

| Technology               | Version       | Purpose                                                |
|--------------------------|---------------|--------------------------------------------------------|
| OpenCSV                  | 5.9           | CSV export                                             |
| SLF4J / Logback          | 2.0 / 1.5     | Logging facade + implementation                        |

---

## 4. Module / Package Breakdown

```
src/main/java/com/trackmyfunds/
├── TrackMyFundsApplication.java       (1)  @SpringBootApplication entry point
│
├── config/
│   └── AppConfig.java                 (2)  @EnableJpaAuditing; RestTemplate bean
│
├── controller/
│   ├── ExpenseController.java         (3)  MVC + form endpoints  (/expenses/*)
│   ├── ExpenseRestController.java     (4)  REST API              (/api/expenses/*)
│   ├── BudgetController.java          (5)  Budget UI + JSON      (/budget/*)
│   ├── AiController.java              (6)  AI features           (/ai/*)
│   └── GlobalExceptionHandler.java    (7)  @RestControllerAdvice — JSON errors
│
├── service/
│   ├── ExpenseService.java            (8)  Interface
│   ├── ExpenseServiceImpl.java        (9)  Business logic for expenses
│   ├── BudgetService.java             (10) Budget upsert + status calc
│   ├── CsvExportService.java          (11) OpenCSV-based export
│   ├── GeminiService.java             (12) Single point of Gemini integration
│   └── RateLimitService.java          (13) 12 calls/hour cap
│
├── repository/
│   ├── ExpenseRepository.java         (14) JpaRepository + JpaSpecificationExecutor
│   └── BudgetRepository.java          (15) Derived-name queries
│
├── specification/
│   └── ExpenseSpecification.java      (16) JPA Criteria predicates
│
├── model/
│   ├── Expense.java                   (17) JPA @Entity
│   ├── Budget.java                    (18) JPA @Entity
│   └── BaseEntity.java                (19) Optional shared @MappedSuperclass
│
├── dto/
│   ├── ExpenseRequestDTO.java         (20) Create/update payload
│   ├── ExpenseFilterDTO.java          (21) Filter inputs → Specification
│   ├── SummaryStatsDTO.java           (22) Total, count, avg, highest
│   ├── DashboardDTO.java              (23) Category + month aggregates + top5
│   ├── AnomalyResult.java             (24) Anomaly check output
│   ├── BudgetStatusDTO.java           (25) Per-category month status
│   ├── NLExpenseParseResult.java      (26) Gemini JSON → typed record
│   ├── FinanceSummaryDTO.java         (27) Full snapshot for AI chat
│   ├── MonthlyInsightDTO.java         (28) Dashboard insight inputs
│   └── ErrorResponseDTO.java          (29) JSON error envelope
│
├── enums/
│   ├── Category.java                  (30) 8-value enum
│   └── PaymentMethod.java             (31) 4-value enum
│
└── exception/
    ├── GeminiApiException.java        (32) Runtime exception
    └── AiExceptionHandler.java        (33) @ControllerAdvice → friendly page
```

---

## 5. Data Model

### 5.1 Entity-Relationship Diagram

```
            ┌────────────────────────┐               ┌────────────────────────┐
            │       EXPENSE          │               │        BUDGET          │
            ├────────────────────────┤               ├────────────────────────┤
            │ id           BIGSERIAL │ PK            │ id            BIGSERIAL│ PK
            │ title        VARCHAR   │ NOT NULL      │ category      VARCHAR  │ NOT NULL
            │ amount       DECIMAL   │ NOT NULL  >0  │ monthly_limit DECIMAL  │ NOT NULL  >0
            │ category     VARCHAR   │ NOT NULL ENUM │ month_value   INTEGER  │ NOT NULL
            │ payment_method VARCHAR │ NOT NULL ENUM │ year_value    INTEGER  │ NOT NULL
            │ date         DATE      │ NOT NULL      │ created_at    TIMESTAMP│
            │ description  TEXT      │               │                        │
            │ created_at   TIMESTAMP │               │ UNIQUE(category,month, │
            │                        │               │        year)            │
            │ idx (date), idx(category)               │                        │
            └────────────────────────┘               └────────────────────────┘
```

There is no foreign-key relationship between expenses and budgets at the database level — the AI / service layer correlates them by `category + month + year` only.

### 5.2 Entity details

#### Expense

| Column          | Type            | Constraints                          | Java field      |
|-----------------|-----------------|--------------------------------------|-----------------|
| id              | BIGSERIAL       | PK                                   | `Long id`       |
| title           | VARCHAR(255)    | NOT NULL, @NotBlank                  | `String title`  |
| amount          | DECIMAL(19,2)   | NOT NULL, @Positive                  | `BigDecimal amount` |
| category        | VARCHAR(20)     | NOT NULL, @Enumerated(STRING)        | `Category category` |
| payment_method  | VARCHAR(20)     | NOT NULL, @Enumerated(STRING)        | `PaymentMethod paymentMethod` |
| date            | DATE            | NOT NULL                             | `LocalDate date` |
| description     | TEXT            | nullable                             | `String description` |
| created_at      | TIMESTAMP(6)    | nullable, @CreatedDate (Spring auditing) | `LocalDateTime createdAt` |

Indexes: `idx_expenses_date(date)`, `idx_expenses_category(category)`.

#### Budget

| Column          | Type            | Constraints                          | Java field      |
|-----------------|-----------------|--------------------------------------|-----------------|
| id              | BIGSERIAL       | PK                                   | `Long id`       |
| category        | VARCHAR(20)     | NOT NULL, @Enumerated(STRING)        | `Category category` |
| monthly_limit   | DECIMAL(19,2)   | NOT NULL, @Positive                  | `BigDecimal monthlyLimit` |
| month_value     | INTEGER         | NOT NULL                             | `int month`     |
| year_value      | INTEGER         | NOT NULL                             | `int year`      |
| created_at      | TIMESTAMP(6)    | nullable                             | `LocalDateTime createdAt` |

Unique constraint: `uk_budgets_category_month_year(category, month_value, year_value)`.

> **Why `month_value` / `year_value`?** `month` and `year` are SQL reserved words in H2 (and date functions in most dialects). Using non-reserved names avoids quoting hacks. The Java field names stay as `month`/`year` so `JpaRepository` derived methods like `findByMonthAndYear` continue to work — only the DB column mapping differs.

### 5.3 DTO Catalog

| DTO                       | Purpose                                                                    |
|---------------------------|----------------------------------------------------------------------------|
| `ExpenseRequestDTO`       | Inbound payload for POST/PUT — validated with Bean Validation              |
| `ExpenseFilterDTO`        | Query parameters bundled into a single record; `toSpecification()` returns JPA `Specification<Expense>` |
| `SummaryStatsDTO`         | Returns total, count, average, highest for the filtered set                |
| `DashboardDTO`            | spending-by-category, spending-by-month (last 6), top-5 expenses           |
| `AnomalyResult`           | `isAnomaly`, `message`, `categoryAverage`, `multiplier`                    |
| `BudgetStatusDTO`         | `category`, `monthlyLimit`, `amountSpent`, `amountRemaining`, `percentageUsed`, `isOverBudget` |
| `NLExpenseParseResult`    | Gemini JSON deserialised into Category/PaymentMethod enums and LocalDate   |
| `FinanceSummaryDTO`       | Full data snapshot for AI chat context                                     |
| `MonthlyInsightDTO`       | Inputs for the dashboard insight card                                       |
| `ErrorResponseDTO`        | `status`, `message`, `timestamp`, `errors[]` — REST JSON error envelope    |

---

## 6. API Endpoint Catalog

### 6.1 MVC Endpoints (return HTML or redirect)

| Method | Path                            | Controller                | Purpose                                          |
|--------|---------------------------------|---------------------------|--------------------------------------------------|
| GET    | `/`                             | ExpenseController         | Redirect to `/expenses`                          |
| GET    | `/expenses`                     | ExpenseController         | Filtered/paginated expense list                  |
| GET    | `/expenses/new`                 | ExpenseController         | New-expense form                                 |
| POST   | `/expenses`                     | ExpenseController         | Create expense; redirect to list                 |
| GET    | `/expenses/{id}/edit`           | ExpenseController         | Edit form                                        |
| POST   | `/expenses/{id}`                | ExpenseController         | Update expense                                   |
| POST   | `/expenses/{id}/delete`         | ExpenseController         | Delete expense                                   |
| GET    | `/expenses/export`              | ExpenseController         | Stream CSV of filtered set                       |
| GET    | `/dashboard`                    | ExpenseController         | Analytics dashboard                              |
| GET    | `/budget`                       | BudgetController          | Budget management page                           |
| POST   | `/budget/set`                   | BudgetController          | Upsert a budget for current month                |
| GET    | `/ai`                           | AiController              | AI tools landing page                            |
| GET    | `/ai/chat`                      | AiController              | AI chat page                                     |
| POST   | `/ai/chat`                      | AiController              | Send a question, render answer                   |

### 6.2 JSON Endpoints (annotated `@ResponseBody`)

| Method | Path                            | Controller                | Returns                                          |
|--------|---------------------------------|---------------------------|--------------------------------------------------|
| POST   | `/expenses/parse-nl`            | ExpenseController         | `NLExpenseParseResult` (200) or `{error:…}` (422)|
| GET    | `/ai/insight/{month}/{year}`    | AiController              | `text/plain` insight (200)                        |
| GET    | `/budget/status`                | BudgetController          | `List<BudgetStatusDTO>` (200)                    |

### 6.3 REST API (`/api/expenses/*` — JSON only)

| Method | Path                            | Returns                                                          |
|--------|---------------------------------|------------------------------------------------------------------|
| GET    | `/api/expenses`                 | `Page<Expense>` with filters, pagination, sort                   |
| GET    | `/api/expenses/summary`         | `SummaryStatsDTO`                                                 |
| GET    | `/api/expenses/{id}`            | `Expense` (200) / `ErrorResponseDTO` (404)                       |
| POST   | `/api/expenses`                 | `Expense` (201) / `ErrorResponseDTO` (400 on validation)         |
| PUT    | `/api/expenses/{id}`            | `Expense` (200) / `ErrorResponseDTO` (404 / 400)                 |
| DELETE | `/api/expenses/{id}`            | 204 No Content / `ErrorResponseDTO` (404)                        |

---

## 7. Feature-by-Feature Technical Design

### 7.1 Expense CRUD

**Request flow (create):**
1. User submits `POST /expenses` with form data
2. Spring binds form fields to `ExpenseRequestDTO`
3. `@Valid` triggers JSR-380 validation; on error `BindingResult.hasErrors()` returns the form view with field errors
4. `ExpenseService.createExpense(dto)` builds an `Expense` and calls `repo.save(...)`
5. `checkAnomaly(saved)` runs; if flagged, the result is stashed in `RedirectAttributes` as a flash attribute
6. Controller redirects to `/expenses` (POST-Redirect-GET pattern)
7. The list page reads `successMessage` and (optional) `anomaly` flash attributes

### 7.2 Filtering & Specifications

Filter params are bound to `ExpenseFilterDTO`. The DTO has a `toSpecification()` method that composes JPA `Specification<Expense>` predicates by chaining `Specification.where(...).and(...)`. Each predicate is null-safe — if a parameter is `null`, the corresponding predicate returns `criteriaBuilder.conjunction()` (a no-op), effectively skipping that filter.

This pattern eliminates `if (param != null) query += "AND …"` SQL-string construction.

### 7.3 CSV Export

`CsvExportService` uses OpenCSV's `CSVWriter` to serialise the filtered expense list to a `String`. The controller sets `Content-Type: text/csv; charset=UTF-8` and `Content-Disposition: attachment; filename=expenses.csv` before streaming the bytes.

### 7.4 Dashboard

`ExpenseServiceImpl.getDashboardData()` produces a `DashboardDTO`:
- **Category totals**: `Collectors.groupingBy(Category::name, reducing(BigDecimal.ZERO, getAmount, BigDecimal::add))`, then sorted descending by value, then collected back to a `LinkedHashMap` for stable iteration order
- **Monthly totals**: pre-seed 6 keys (`YearMonth.now().minusMonths(0..5)` formatted as `"MMM yyyy"`) with `BigDecimal.ZERO`, then `computeIfPresent` for each in-range expense
- **Top 5**: stream sorted descending by amount, limited to 5

The controller splits the LinkedHashMap into two parallel `ArrayList`s (labels and values) before adding to the model — Chart.js expects arrays, not Maps.

### 7.5 Budgets

`BudgetService.setBudget(category, limit)` is an upsert: look up by `(category, currentMonth, currentYear)`, update `monthlyLimit` if found, otherwise create. The DB unique constraint is a safety net.

`getCurrentMonthBudgets()` joins each budget with the running spend via `ExpenseRepository.sumByCategoryAndDateBetween(...)` — a JPQL query using `COALESCE(SUM, 0)` to handle the no-data case without returning `null`.

### 7.6 Anomaly Detection (rule-based)

In `ExpenseServiceImpl.checkAnomaly(newExpense)`:
1. Fetch all expenses in the same category from the last 3 months (excluding the row just saved by ID)
2. If empty → `AnomalyResult.none()`
3. Compute average; if zero → `AnomalyResult.none()`
4. Compute multiplier = newAmount / average
5. If multiplier ≤ 2.0 → `isAnomaly=false`
6. Else build a friendly message and return `isAnomaly=true`

The controller passes the result through `RedirectAttributes` so the anomaly banner appears on the next page render.

### 7.7 AI: Natural-Language Parsing

`GeminiService.parseNaturalLanguageExpense(userInput)`:
1. Build a deterministic prompt with today's date and the field schema spelled out
2. Call `callGemini(prompt)`
3. Pass the raw text to `ObjectMapper.readValue(..., NLExpenseParseResult.class)`
4. Jackson deserialises strings into `Category` / `PaymentMethod` enums, the `"2026-05-18"` into `LocalDate` (via `JavaTimeModule` autoconfigured by Spring), and numbers into `BigDecimal`
5. Any parse failure → return `null`; the controller responds with HTTP 422 + `{error:"Could not parse. Please fill manually."}`

### 7.8 AI: Voice Input

Pure client-side in `static/js/expense-voice.js`:
- Feature-detect `window.SpeechRecognition || window.webkitSpeechRecognition`; on miss, hide the mic and show the "requires Chrome or Edge" hint
- `lang='en-IN'`, `continuous=false`, `interimResults=true` for live transcription feedback
- `recognition.onresult` updates the NL input with the running transcript
- `recognition.onend` programmatically clicks the **Parse with AI** button, kicking off the existing fetch flow

### 7.9 AI: Finance Chat (grounded)

`GeminiService.answerFinanceQuestion(question, expenses)`:
1. `buildSummary(expenses)` produces a multi-line plaintext block: date range, total, average, highest, most-used payment, category totals
2. Prompt template forbids outside knowledge and asks the model to say *"cannot be determined"* if data is insufficient
3. `callGemini(prompt)` returns the raw narrative

This is the **anti-hallucination** design: the AI cannot legally invent numbers because the prompt restricts its scope to the data block.

### 7.10 AI: Monthly Insight

`AiController.monthlyInsight(month, year)`:
1. `ExpenseService.getMonthlyInsightData(month, year)` returns a `MonthlyInsightDTO`
2. `GeminiService.generateMonthlyInsight(...)` builds a prompt asking for "top category, positive observation, area to improve, actionable tip"
3. Returns `text/plain` to the client
4. The dashboard's JS shows a 4-line shimmer skeleton while fetching, replaces with the text on success, shows a red error on failure

If `overallTotal == 0`, the service short-circuits without an API call and returns *"No spending recorded for X yet…"*.

---

## 8. AI Integration — Deep Dive

### 8.1 Why Gemini?

- **Free tier is generous** (15 RPM / 1500 RPD on `gemini-2.5-flash`) — sufficient for personal use
- **Simple HTTP API** — no SDK required; works with `RestTemplate`
- **JSON-first** — easy to integrate with Jackson when structured output is needed

### 8.2 The single `callGemini(String prompt)` method

This private method is the **only point** in the codebase that talks to Gemini. Every AI feature ultimately funnels through it.

Sequence:
1. **API key check** — if `apiKey` is null/blank, throw `GeminiApiException` (caught by `AiExceptionHandler`, renders friendly page)
2. **Rate-limit check** — `rateLimitService.tryAcquire()` returns false if 12 calls already made this hour → return `"Daily AI limit reached. Try again later."`
3. **HTTP call** — POST to `apiUrl?key=apiKey` with body `{contents: [{parts: [{text: prompt}]}]}`
4. **Response parse** — extract `candidates[0].content.parts[0].text`
5. **Catch-all** — any exception inside the try block returns `"AI service unavailable. Please try again."` and logs the underlying error at `ERROR` level

### 8.3 Prompt-engineering principles applied

| Goal                          | Technique                                                                       |
|-------------------------------|---------------------------------------------------------------------------------|
| Strict JSON output (parsing)  | "Return ONLY a valid JSON object with no extra text, no markdown, no backticks" |
| No hallucination (chat)       | "Do not use outside knowledge. If the answer cannot be determined from the data, say so clearly." |
| Stable schema mapping         | Explicit enumeration of allowed values for `category` and `paymentMethod`       |
| Date grounding                | Embed today's date in every prompt so relative phrases ("yesterday") resolve correctly |
| Predictable insight shape     | "Include: top spending category, one positive observation, one area to improve, one specific actionable tip" |

### 8.4 Rate-limit design

`RateLimitService` is a synchronised in-memory counter:

```java
synchronized boolean tryAcquire() {
    if (windowStart + 1h < now) { windowStart = now; counter.set(0); }
    if (counter.get() >= 12) return false;
    counter.incrementAndGet();
    return true;
}
```

- **In-process** — restart resets the counter (acceptable for single-instance personal app)
- **Synchronised** — atomic check + increment, safe under concurrent requests
- **12 calls/hour** — chosen conservatively to leave ample headroom on the 15 RPM Gemini quota

For multi-instance deployments, swap the `AtomicInteger` for Redis `INCR` with `EXPIRE`.

### 8.5 Error handling strategy

| Failure mode              | Where it surfaces                  | What the user sees                       |
|---------------------------|------------------------------------|------------------------------------------|
| API key blank             | Throws `GeminiApiException`         | Friendly *"AI service is unavailable"* page |
| Rate limit exceeded       | Returns string, no exception        | Inline message inside the AI bubble       |
| Network failure / HTTP error / parse error | Caught, logged, returns FALLBACK | Inline *"AI service unavailable. Please try again."* |
| Gemini returns unparseable JSON (parse-nl only) | Returns `null` from `parseNaturalLanguageExpense` | Red banner *"Could not parse. Please fill manually."* |

---

## 9. Security Considerations

| Concern             | Mitigation                                                                                |
|---------------------|-------------------------------------------------------------------------------------------|
| Secret in source    | `gemini.api.key=${GEMINI_API_KEY:}` — never hardcoded; `.gitignore` excludes `.env`         |
| XSS                 | Thymeleaf `th:text` HTML-escapes by default; `th:utext` is never used for user content      |
| SQL injection       | Parameterised queries via JPA / `@Query` with `@Param`                                     |
| CSRF                | Not yet implemented — single-user portfolio app; would add Spring Security CSRF for prod  |
| Authentication      | Not implemented — explicitly out of scope                                                   |
| Sensitive data in logs | API key is never logged; only the exception type and message in the catch block         |
| Dependency CVEs     | Versions pinned via Spring Boot BOM; periodic `mvn versions:display-dependency-updates`     |

---

## 10. Deployment

### 10.1 Local development (H2)

```bash
$env:GEMINI_API_KEY = "AIzaSy…"
./mvnw spring-boot:run
```

The `dev` profile is the default; H2 schema is auto-created by Hibernate (`ddl-auto=create-drop`); Flyway is disabled.

### 10.2 Docker Compose (local prod-like)

`docker-compose.yml` defines two services:

```
postgres ── PostgreSQL 16 with health check + named volume
app      ── multi-stage Dockerfile, depends_on postgres
```

App env vars: `SPRING_PROFILES_ACTIVE=prod`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`. Flyway runs `V1__create_expenses_table.sql` and `V2__create_budgets_table.sql` on first start.

### 10.3 Production (Render)

- **Web Service**: Render builds the Dockerfile, exposes port 8080
- **PostgreSQL**: Render free-tier managed instance, connection string passed via env vars
- **Auto-deploy**: every `git push origin main` triggers a new Render build

Environment variables to set in Render:
```
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://<host>:5432/<db>
DB_USERNAME=<user>
DB_PASSWORD=<password>
GEMINI_API_KEY=<key>
```

### 10.4 Dockerfile (multi-stage)

```
Stage 1 (build):   maven:3.9-eclipse-temurin-21-alpine → mvn package -DskipTests
Stage 2 (runtime): eclipse-temurin:21-jre-alpine        → java -jar app.jar
                   Non-root user 'tmf' for security
                   EXPOSE 8080
```

---

## 11. Testing Strategy

The suite uses Spring Boot's test slices for fast, focused tests.

### 11.1 Test classes (50 tests total)

| Class                          | Type                       | Count | Strategy                                              |
|--------------------------------|----------------------------|-------|-------------------------------------------------------|
| `ExpenseSpecificationTest`     | `@DataJpaTest` + H2        | 12    | Real Hibernate + H2, per-test rollback                |
| `ExpenseServiceTest`           | `@ExtendWith(MockitoExtension)` | 14    | Pure unit, mocks repository                          |
| `ExpenseControllerTest`        | `@WebMvcTest`              | 11    | MVC slice, MockMvc, Thymeleaf rendering verified      |
| `ExpenseRestControllerTest`    | `@WebMvcTest`              | 12    | REST slice, JSON assertions via `jsonPath`            |
| `TrackMyFundsApplicationTests` | `@SpringBootTest`          | 1     | Context-load smoke test                               |

### 11.2 Key techniques

- **`@MockBean(JpaMetamodelMappingContext.class)`** in `@WebMvcTest` classes — necessary because `AppConfig` declares `@EnableJpaAuditing` but the web slice has no JPA infrastructure
- **`PageImpl<>(List.of(...))`** to mock paginated service responses
- **`jsonPath("$.errors").doesNotExist()`** to verify `@JsonInclude(NON_NULL)` is working
- **`@ActiveProfiles("test")`** on `@DataJpaTest` to prevent the `DataSeeder` (`@Profile("dev")`) from running

### 11.3 Run the suite

```bash
./mvnw test
```

Expected result:
```
Tests run: 50, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## 12. File Structure (project root)

```
TrackMyFunds/
├── .dockerignore
├── .gitignore
├── docker-compose.yml
├── Dockerfile
├── pom.xml
├── README.md
├── docs/
│   ├── PRD.md
│   └── TECHNICAL_DESIGN.md
└── src/
    ├── main/
    │   ├── java/com/trackmyfunds/   (see section 4 for full breakdown)
    │   └── resources/
    │       ├── application.properties
    │       ├── application-prod.properties
    │       ├── db/migration/
    │       │   ├── V1__create_expenses_table.sql
    │       │   └── V2__create_budgets_table.sql
    │       ├── static/js/
    │       │   ├── expenses.js
    │       │   └── expense-voice.js
    │       └── templates/
    │           ├── layout/base.html       (master layout with navbar + sidebar)
    │           ├── expenses/
    │           │   ├── list.html
    │           │   └── form.html
    │           ├── budget/dashboard.html
    │           ├── ai/
    │           │   ├── index.html
    │           │   └── chat.html
    │           ├── dashboard.html
    │           └── error/ai-error.html
    └── test/java/com/trackmyfunds/        (see section 11)
```

---

## 13. Configuration Reference

### 13.1 application.properties (base / dev)

| Property                                 | Value                                                    | Notes                                          |
|------------------------------------------|----------------------------------------------------------|------------------------------------------------|
| `spring.profiles.active`                 | `dev`                                                    | Default profile                                |
| `spring.datasource.url`                  | `jdbc:h2:mem:trackmyfundsdb;DB_CLOSE_DELAY=-1`           | In-memory H2                                   |
| `spring.jpa.hibernate.ddl-auto`          | `create-drop`                                            | Hibernate manages schema in dev                |
| `spring.flyway.enabled`                  | `false`                                                  | Flyway disabled in dev                         |
| `spring.thymeleaf.cache`                 | `false`                                                  | Templates reloaded on edit                     |
| `gemini.api.key`                         | `${GEMINI_API_KEY:}`                                     | From environment variable                       |
| `gemini.api.url`                         | `…/v1beta/models/gemini-2.5-flash:generateContent`       | Pinned model version                            |

### 13.2 application-prod.properties

| Property                                 | Value                                                    | Notes                                          |
|------------------------------------------|----------------------------------------------------------|------------------------------------------------|
| `spring.datasource.url`                  | `${DB_URL}`                                              | Provided by Render or Docker Compose           |
| `spring.datasource.driver-class-name`    | `org.postgresql.Driver`                                  |                                                |
| `spring.jpa.database-platform`           | `org.hibernate.dialect.PostgreSQLDialect`                |                                                |
| `spring.jpa.hibernate.ddl-auto`          | `validate`                                               | Hibernate verifies schema; Flyway owns DDL     |
| `spring.flyway.enabled`                  | `true`                                                   |                                                |
| `spring.flyway.locations`                | `classpath:db/migration`                                 |                                                |
| `spring.thymeleaf.cache`                 | `true`                                                   | Production template caching                     |

---

## 14. Build & Run Reference

| Task                                      | Command                                                                        |
|-------------------------------------------|--------------------------------------------------------------------------------|
| Compile + run tests                       | `./mvnw clean test`                                                            |
| Run locally (dev)                         | `./mvnw spring-boot:run`                                                       |
| Package JAR                               | `./mvnw clean package -DskipTests`                                             |
| Run packaged JAR                          | `java -jar target/trackmyfunds-0.0.1-SNAPSHOT.jar`                             |
| Build Docker image                        | `docker build -t trackmyfunds .`                                               |
| Start full stack with Compose             | `docker compose up --build`                                                    |
| Stop and clean Compose                    | `docker compose down -v`                                                       |
| Push to GitHub (triggers Render deploy)   | `git push origin main`                                                         |

---

## 15. Future Enhancements

| Idea                                                | Effort | Value |
|-----------------------------------------------------|--------|-------|
| Multi-user authentication (Spring Security + OAuth) | High   | High  |
| Recurring transactions (monthly rent, subscriptions)| Med    | High  |
| Receipt photo upload + OCR via Google Vision        | Med    | Med   |
| Push notifications when a budget hits 80%           | Med    | Med   |
| Mobile-first PWA with offline support               | High   | Med   |
| Investment + income tracking                        | High   | High  |
| Replace in-memory rate limiter with Redis           | Low    | Med   |
| Switch RestTemplate → WebClient (reactive)          | Low    | Low   |
| Add Swagger / OpenAPI documentation                 | Low    | Med   |
| GitHub Actions CI: run tests + SCA on every PR      | Low    | High  |

---

## 16. Revision History

| Version | Date       | Author                    | Notes                                            |
|---------|------------|---------------------------|--------------------------------------------------|
| 1.0     | 2026-05-18 | Prachi Dnyaneshwar Parate | Initial release covering v1 architecture & code  |
