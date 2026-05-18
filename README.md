# TrackMyFunds

A personal finance tracking application built with Spring Boot 3.4.5, Thymeleaf, and Chart.js.

## Tech Stack

| Layer       | Technology                              |
|-------------|-----------------------------------------|
| Backend     | Spring Boot 3.4.5, Spring Data JPA      |
| Frontend    | Thymeleaf, Bootstrap 5, Chart.js 4      |
| Database    | H2 (dev) / MySQL 8 (prod)               |
| Migrations  | Flyway (prod only)                      |
| Build       | Maven (Maven Wrapper included)          |

## Prerequisites

- **JDK 17+** (JDK 21 recommended)
- **Maven** — or use the included `./mvnw` wrapper
- **Docker + Docker Compose** — required only for the containerised setup

---

## Quick Start (local dev — H2)

The default profile uses an in-memory H2 database. No setup required.

```bash
./mvnw spring-boot:run
```

Open [http://localhost:8080/dashboard](http://localhost:8080/dashboard)

The H2 console is available at [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
(JDBC URL: `jdbc:h2:mem:trackmyfundsdb`, username: `sa`, password: *(empty)*).

---

## Running Tests

```bash
./mvnw test
```

The test suite contains 50 tests across four classes:

| Test class                  | Strategy                              |
|-----------------------------|---------------------------------------|
| `ExpenseSpecificationTest`  | `@DataJpaTest` + H2                   |
| `ExpenseServiceTest`        | `@ExtendWith(MockitoExtension.class)` |
| `ExpenseControllerTest`     | `@WebMvcTest` (MVC slice)             |
| `ExpenseRestControllerTest` | `@WebMvcTest` (REST slice)            |

---

## Production Setup (MySQL)

### Environment Variables

| Variable      | Description                                      | Example                                                                |
|---------------|--------------------------------------------------|------------------------------------------------------------------------|
| `DB_URL`      | Full JDBC connection URL for the MySQL database  | `jdbc:mysql://localhost:3306/trackmyfundsdb?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true` |
| `DB_USERNAME` | MySQL username                                   | `tmf_user`                                                             |
| `DB_PASSWORD` | MySQL password                                   | `tmf_password`                                                         |

Activate the production profile with:

```bash
SPRING_PROFILES_ACTIVE=prod \
DB_URL=jdbc:mysql://localhost:3306/trackmyfundsdb?useSSL=false&serverTimezone=UTC \
DB_USERNAME=tmf_user \
DB_PASSWORD=tmf_password \
./mvnw spring-boot:run
```

On first startup Flyway will automatically apply
`src/main/resources/db/migration/V1__create_expenses_table.sql`
and create the `expenses` table. Subsequent starts are no-ops unless new
migration files are added.

> **Note:** `spring.jpa.hibernate.ddl-auto=validate` is set in prod — Hibernate
> checks the schema matches the entity mappings but never alters it. All DDL is
> owned by Flyway.

---

## Docker Compose

The easiest way to run the full stack locally or on a server.

### 1. Build and start

```bash
docker compose up --build
```

This will:
1. Build the application image (multi-stage Maven + JRE build)
2. Start a MySQL 8 container and wait for it to be healthy
3. Start the Spring Boot app connected to MySQL

Open [http://localhost:8080/dashboard](http://localhost:8080/dashboard)

### 2. Stop

```bash
docker compose down
```

MySQL data is persisted in the `mysql_data` named volume and survives restarts.
To destroy the volume as well:

```bash
docker compose down -v
```

### 3. Changing credentials for production

Edit the `environment` section of the `app` service and the `mysql` service in
[docker-compose.yml](docker-compose.yml) before deploying. Do **not** commit
real credentials — use a `.env` file or a secrets manager instead.

```dotenv
# .env  (never commit this file)
MYSQL_ROOT_PASSWORD=your_root_password
MYSQL_PASSWORD=your_app_password
DB_PASSWORD=your_app_password
```

Then reference them in `docker-compose.yml`:

```yaml
environment:
  MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
  MYSQL_PASSWORD:      ${MYSQL_PASSWORD}
  DB_PASSWORD:         ${DB_PASSWORD}
```

---

## Project Structure

```
src/
├── main/
│   ├── java/com/trackmyfunds/
│   │   ├── controller/      # MVC + REST controllers
│   │   ├── dto/             # Request / response / filter DTOs
│   │   ├── enums/           # Category, PaymentMethod
│   │   ├── exception/       # GlobalExceptionHandler
│   │   ├── model/           # Expense entity
│   │   ├── repository/      # Spring Data JPA repository
│   │   ├── service/         # Business logic
│   │   └── specification/   # JPA Criteria API specifications
│   └── resources/
│       ├── db/migration/    # Flyway SQL migrations
│       ├── static/js/       # expenses.js (filter UX)
│       └── templates/       # Thymeleaf HTML templates
└── test/
    └── java/com/trackmyfunds/
        ├── controller/      # Web layer slice tests
        ├── service/         # Unit tests
        └── specification/   # JPA integration tests
```

## AI Features

TrackMyFunds ships with five AI-powered features built on the **Google Gemini 1.5-flash** API. They're all reachable from the **AI Tools** link in the navbar (`/ai`).

| # | Feature                  | Where                         | What it does                                                                                  |
|---|--------------------------|-------------------------------|-----------------------------------------------------------------------------------------------|
| 1 | **Natural-language entry** | `/expenses/new` form          | Type *"spent 200 on metro yesterday"* → Gemini parses it into title/amount/category/etc. and auto-fills the form |
| 2 | **Voice expense entry**  | mic button on `/expenses/new` | Speak the same sentence — browser's Web Speech API transcribes it, then hands off to feature #1 |
| 3 | **Finance Chat**         | `/ai/chat`                    | Free-form Q&A grounded **only** in your logged expenses — the prompt forbids outside knowledge |
| 4 | **Monthly Insight**      | `/dashboard` card             | Auto-generated 4-5 line spending report on page load: top category, positive observation, area to improve, actionable tip |
| 5 | **Anomaly Detection**    | inline banner on `/expenses`  | Rule-based check (no AI): flags any new expense > 2× the 3-month category average             |

### Getting a free Gemini API key

1. Sign in to [Google AI Studio](https://aistudio.google.com/app/apikey) with any Google account
2. Click **Create API key**
3. Choose a Google Cloud project (Studio offers to create one for you)
4. Copy the key — it starts with `AIzaSy...`

The free tier on `gemini-1.5-flash` is generous (15 RPM / 1500 RPD at time of writing) and is more than enough for personal use.

### Configuring the key

The app reads the key from the `gemini.api.key` property, which is wired to a **`GEMINI_API_KEY`** environment variable:

```properties
# application.properties
gemini.api.key=${GEMINI_API_KEY:}
gemini.api.url=https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent
```

Set it locally:

```powershell
$env:GEMINI_API_KEY = "AIzaSy...your-key"
./mvnw spring-boot:run
```

Or on Render: Dashboard → your service → **Environment** → add `GEMINI_API_KEY`. The next deploy picks it up.

### Built-in safety rails

- **Rate limiting**: an in-memory `RateLimitService` caps Gemini calls at **12 per hour app-wide**. Beyond that, the feature returns *"Daily AI limit reached. Try again later."* instead of hitting the API — keeping you well inside free-tier quotas.
- **Missing-key handling**: if `GEMINI_API_KEY` isn't set, a `GeminiApiException` is thrown and the user lands on a friendly *"AI service is unavailable"* page instead of a stack trace.
- **Graceful per-feature fallbacks**: transient errors (network, parsing) fall back to *"AI service unavailable. Please try again."* so the UI never breaks.

### Browser support

Voice input depends on the [Web Speech API](https://developer.mozilla.org/en-US/docs/Web/API/Web_Speech_API), which is only available in Chromium-based browsers — **Chrome and Edge**. On Firefox or Safari the mic button is hidden automatically and a small *"Voice input requires Chrome or Edge"* hint is shown. All other AI features (text NL entry, chat, monthly insight, anomaly detection) work in every modern browser.

---

## Spring Profiles

| Profile | Datasource | DDL           | Flyway  | Thymeleaf cache |
|---------|------------|---------------|---------|-----------------|
| `dev`   | H2 mem     | `create-drop` | off     | off             |
| `prod`  | MySQL 8    | `validate`    | on      | on              |
| `test`  | H2 mem     | `create-drop` | off     | off             |
