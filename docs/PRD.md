# Project Requirement Document — TrackMyFunds

| Field            | Value                                            |
|------------------|--------------------------------------------------|
| Project Name     | TrackMyFunds — Personal Finance Tracker          |
| Document Type    | Project Requirement Document (PRD)               |
| Version          | 1.0                                              |
| Date             | 2026-05-18                                       |
| Author           | Prachi Dnyaneshwar Parate                        |
| Audience         | Project sponsors, evaluators, future contributors |

---

## 1. Executive Summary

TrackMyFunds is a web-based personal finance tracking application that lets a single user record day-to-day expenses, analyse their spending patterns, set monthly budgets per category, and ask natural-language questions about their financial data. The system pairs a conventional Spring Boot CRUD application with a curated set of AI-powered features built on Google Gemini and the browser-native Web Speech API.

The product targets individuals who want a lightweight, self-hosted alternative to commercial finance trackers — with the bonus of an AI assistant that is grounded strictly in their own data and never sees information from outside sources.

---

## 2. Background & Problem Statement

### 2.1 The problem

Existing personal finance tools fall into two camps:

1. **Heavy commercial products** (Mint, YNAB, Money Manager) — feature-rich but require sharing bank credentials, carry subscription costs, and offer no transparency into the data flow.
2. **Spreadsheet-based tracking** — fully private but tedious to maintain, with no automated analysis, no anomaly detection, and no easy way to query history.

Neither option offers an AI-driven query layer that respects user privacy: most "AI finance" products send the entire transaction history to third-party LLMs without explicit user consent.

### 2.2 The proposed solution

TrackMyFunds offers:

- A clean web UI to record, filter, sort and export expenses
- Visual analytics (dashboard, charts, summary statistics)
- Per-category monthly budgets with progress tracking
- Rule-based anomaly detection (no AI required)
- An AI assistant that answers free-form questions grounded **only** in the user's own logged expenses — a single API call per request, with no training-data retention
- Natural-language expense entry (typed or spoken) for friction-free data capture
- Optional self-hosted deployment via Docker Compose

---

## 3. Project Objectives

1. **Functional**: Provide a complete CRUD experience for personal expense tracking with rich filtering, sorting, pagination and CSV export.
2. **Analytical**: Surface spending patterns through a dashboard (category breakdown, 6-month trend, top expenses) and a monthly budget tracker.
3. **Intelligent**: Integrate Google Gemini AI for five user-facing features without exceeding the free-tier API budget.
4. **Production-ready**: Ship with Flyway migrations, profile-based configuration (dev/prod), Docker support, deployment to Render, and comprehensive unit/integration tests.
5. **Polished**: Demonstrate engineering rigor — exception handling, prompt engineering, rate limiting, graceful fallbacks, accessible UI.

---

## 4. Scope

### 4.1 In Scope

- Single-user expense management (no multi-tenancy or user accounts)
- Six predefined expense categories and four payment methods
- Indian Rupee (₹) currency formatting
- Web UI (Thymeleaf + Bootstrap 5) and a REST API
- AI features: natural-language entry, voice input, finance chat, monthly insight, anomaly detection
- Local development on H2; production deployment on PostgreSQL (Render)
- 50 automated tests across service, controller, repository and specification layers
- Docker Compose for self-hosted deployment

### 4.2 Out of Scope

- Multi-user / authentication / authorisation
- Multi-currency support (only INR is formatted)
- Bank-account synchronisation / OAuth integration with banks
- Mobile native apps (iOS/Android) — the web UI is responsive but not packaged
- Investment tracking, income tracking, recurring transactions
- Tax categorisation or financial advice beyond the AI chat
- Notification system (email/SMS/push)
- Receipt photo upload / OCR

---

## 5. Stakeholders & User Personas

### 5.1 Stakeholders

| Stakeholder           | Interest                                                          |
|-----------------------|-------------------------------------------------------------------|
| End user (single)     | Easy expense logging, useful insights, data privacy               |
| Project evaluator     | Code quality, design choices, breadth of technologies             |
| Future contributor    | Clean architecture, documentation, tested codebase                |
| Hiring manager        | Demonstrable full-stack + AI integration skills                   |

### 5.2 Primary Persona

**"Ananya, 24 — first job"**
- Recently started working in Pune; tracks all expenses in a spreadsheet
- Wants to know "where my money goes" but spreadsheets give no insight
- Doesn't want to give bank credentials to a third party
- Curious about AI but only if her data stays under her control
- Uses Chrome on a Windows laptop, occasionally types on her phone

### 5.3 Secondary Persona

**"Rohan, 28 — engineer**
- Self-hosts apps on a home server / cloud VM
- Comfortable with Docker, command line, and reading code
- Wants to fork and extend (e.g. add recurring transactions, multi-user)
- Values architectural clarity and good test coverage

---

## 6. Functional Requirements

Each requirement has a unique ID, priority (M = must-have, S = should-have, C = could-have), and acceptance criteria.

### 6.1 Expense Management (Core CRUD)

| ID    | Pri | Requirement                                                                                          |
|-------|-----|------------------------------------------------------------------------------------------------------|
| FR-1  | M   | A user can create a new expense with title, amount, category, payment method, date, and description. |
| FR-2  | M   | All required fields are validated; the user sees inline error messages on submission failure.        |
| FR-3  | M   | A user can view a paginated list of all expenses.                                                    |
| FR-4  | M   | A user can edit an existing expense by ID.                                                           |
| FR-5  | M   | A user can delete an expense after confirming via a browser prompt.                                  |
| FR-6  | M   | A user can view a single expense's JSON representation via the REST API.                             |

### 6.2 Filtering, Sorting & Search

| ID    | Pri | Requirement                                                                                          |
|-------|-----|------------------------------------------------------------------------------------------------------|
| FR-7  | M   | A user can filter the expense list by category, payment method, date range, amount range, keyword.   |
| FR-8  | M   | A user can sort the expense list by date, title, or amount in ascending or descending order.         |
| FR-9  | M   | A user can paginate the expense list (default 10 records per page).                                  |
| FR-10 | S   | Filter dropdowns auto-submit on change; the keyword input auto-submits after a 300ms debounce.       |
| FR-11 | S   | A "Clear All" button resets every filter and submits.                                                |
| FR-12 | C   | Active filters are visually highlighted (blue border, light blue background).                        |

### 6.3 Summary Statistics & Dashboard

| ID    | Pri | Requirement                                                                                          |
|-------|-----|------------------------------------------------------------------------------------------------------|
| FR-13 | M   | The expense list shows four summary stats: total amount, record count, average, highest expense.     |
| FR-14 | M   | A dashboard page shows a doughnut chart of spending by category.                                     |
| FR-15 | M   | A dashboard page shows a bar chart of monthly spending for the last 6 months.                        |
| FR-16 | M   | A dashboard page shows the top 5 most expensive transactions.                                        |

### 6.4 Export

| ID    | Pri | Requirement                                                                                          |
|-------|-----|------------------------------------------------------------------------------------------------------|
| FR-17 | M   | A user can export the current filtered expense set as a CSV file.                                    |

### 6.5 Budgeting

| ID    | Pri | Requirement                                                                                          |
|-------|-----|------------------------------------------------------------------------------------------------------|
| FR-18 | M   | A user can set a monthly limit for any category for the current month and year.                      |
| FR-19 | M   | Setting a new limit for an existing (category, month, year) updates the existing record.             |
| FR-20 | M   | The budget page shows a card per category with progress bar, percentage used, and remaining amount.  |
| FR-21 | M   | Progress bars are green below 80%, yellow 80-100%, red above 100%.                                   |
| FR-22 | M   | An "Over budget!" red badge appears on any category that has been exceeded.                          |
| FR-23 | M   | The expense list page shows a compact red-pill strip listing every over-budget category.             |
| FR-24 | S   | Categories with no budget set yet are shown as quick-set chips on the budget page.                   |
| FR-25 | C   | A JSON endpoint exposes current-month budget statuses for external clients (`GET /budget/status`).   |

### 6.6 Anomaly Detection

| ID    | Pri | Requirement                                                                                          |
|-------|-----|------------------------------------------------------------------------------------------------------|
| FR-26 | M   | When a new expense exceeds 2× the 3-month average for its category, a warning banner is shown.       |
| FR-27 | M   | The banner displays: amount, category, multiplier, average, and asks "Was this intentional?".        |
| FR-28 | M   | When there is no prior data for the category, no anomaly is reported.                                |

### 6.7 AI Features

| ID    | Pri | Requirement                                                                                          |
|-------|-----|------------------------------------------------------------------------------------------------------|
| FR-29 | M   | A user can type a natural-language description ("spent 200 on metro yesterday") and have the form auto-filled by AI. |
| FR-30 | S   | A user can speak the same sentence using a microphone button (Chrome/Edge only).                     |
| FR-31 | M   | A user can chat with an AI assistant grounded only in their logged expenses.                         |
| FR-32 | M   | The chat page provides 4 suggested questions as clickable chips.                                     |
| FR-33 | M   | The dashboard displays an auto-generated 4-5 line monthly insight on page load.                      |
| FR-34 | M   | A landing page at `/ai` indexes all AI features as cards.                                            |
| FR-35 | M   | AI features fail gracefully with a fallback message when the API is unreachable.                     |
| FR-36 | M   | AI API calls are rate-limited to 12/hour to stay within free-tier quota.                             |
| FR-37 | M   | A user-friendly error page is shown when the Gemini API key is unconfigured.                         |

### 6.8 Deployment & Operations

| ID    | Pri | Requirement                                                                                          |
|-------|-----|------------------------------------------------------------------------------------------------------|
| FR-38 | M   | The application can be built and packaged as a single executable JAR.                                |
| FR-39 | M   | The application supports two Spring profiles: `dev` (H2) and `prod` (PostgreSQL).                    |
| FR-40 | M   | Database schema is created/updated via Flyway migrations in production.                              |
| FR-41 | M   | The application can be containerised via the included multi-stage Dockerfile.                       |
| FR-42 | S   | A Docker Compose file brings up the application and a PostgreSQL container with one command.         |
| FR-43 | S   | The application is deployable to Render (Free tier) for a public demo URL.                           |

---

## 7. Non-Functional Requirements

### 7.1 Performance

- **NFR-1**: Any non-AI page renders within 500 ms on a 2-vCPU, 1 GB RAM instance with up to 10 000 expenses.
- **NFR-2**: AI features respond within 5 seconds end-to-end under normal Gemini latency.
- **NFR-3**: Database queries use indexes on `date` and `category` (most filtered columns).

### 7.2 Usability

- **NFR-4**: All pages are responsive — usable on a 360 px mobile viewport up to a 1920 px desktop.
- **NFR-5**: Form errors are visible inline next to the field, not in a banner only.
- **NFR-6**: Currency is formatted with Indian numbering convention (`₹ 1,00,000.00`).

### 7.3 Reliability

- **NFR-7**: A Gemini API failure must not prevent non-AI functionality from working.
- **NFR-8**: Schema migrations run automatically on startup in production; the app refuses to start on schema mismatch (`ddl-auto=validate`).

### 7.4 Security

- **NFR-9**: The Gemini API key is never committed to source control; it is read from the `GEMINI_API_KEY` environment variable.
- **NFR-10**: User input is validated server-side regardless of client-side checks.
- **NFR-11**: Output is HTML-escaped by default (Thymeleaf `th:text` is used, never `th:utext` for untrusted data).
- **NFR-12**: SQL injection is prevented by exclusive use of JPA / parameterised queries.

### 7.5 Maintainability

- **NFR-13**: The codebase has at least 50 automated tests covering all service-layer methods and HTTP endpoints.
- **NFR-14**: Project follows standard Spring Boot package conventions: `controller`, `service`, `repository`, `model`, `dto`, `enums`, `exception`, `specification`, `config`.
- **NFR-15**: All public service methods carry a Javadoc comment explaining intent.

### 7.6 Compatibility

- **NFR-16**: Java 17 source level (build target) running on JDK 21.
- **NFR-17**: Browser compatibility — last two versions of Chrome, Edge, Firefox, Safari. Voice input degrades gracefully on non-Chromium browsers.

---

## 8. Constraints & Assumptions

### 8.1 Constraints

- The Gemini free tier (15 RPM / 1500 RPD) is the only AI provider; no paid tier or alternative LLM is configured.
- Render free tier (web service + Postgres) is the only target cloud environment.
- Single-user application — no support for concurrent multi-user scenarios.
- All currency values are stored and displayed as Indian Rupees (₹).

### 8.2 Assumptions

- The user has Java 17+ (or 21), Maven (or uses the project's lack of `mvnw` and installs Maven), and Docker Desktop installed for local development.
- The user has or can create a free Google account for AI Studio API key generation.
- The user has a free Render and GitHub account for public deployment.
- The user's browser is modern enough to support Bootstrap 5 and ES2020 JavaScript.

---

## 9. Success Criteria

The project is considered successful when:

1. All M-priority functional requirements (FR-1 through FR-43 where Pri = M) pass manual smoke testing.
2. The automated test suite reports `Tests run: 50, Failures: 0, Errors: 0`.
3. The application is deployed to Render with a publicly accessible URL.
4. The AI chat answers a question of the form *"Which category did I spend the most on?"* correctly based on test data.
5. The Monthly Insight card on the dashboard renders within 10 seconds of page load.
6. The README documents how a new developer can run the app and obtain an API key in under 15 minutes.

---

## 10. Risks & Mitigations

| Risk                                                  | Likelihood | Impact | Mitigation                                                                                              |
|-------------------------------------------------------|------------|--------|---------------------------------------------------------------------------------------------------------|
| Gemini API key leaked to GitHub                       | Medium     | High   | Key read from env var; `.gitignore` excludes `.env`; README explicitly warns                            |
| Gemini free-tier quota exhausted                      | Medium     | Medium | In-memory `RateLimitService` caps usage at 12/hr; graceful fallback message                             |
| Gemini model deprecated mid-deployment                | Medium     | High   | Demonstrated by `gemini-1.5-flash → 2.5-flash` rotation during build; model URL is in `application.properties` and trivially updated |
| Render free tier sleeps the app after 15 min idle     | High       | Low    | Documented in README; cold start is ~30s — acceptable for portfolio project                             |
| Render Postgres free tier expires after 90 days       | Certain    | Medium | Documented in README; users can upgrade or migrate                                                      |
| Web Speech API unsupported in user's browser          | Medium     | Low    | Mic button hidden + "requires Chrome or Edge" hint shown automatically                                  |
| H2 reserved-word collision in entity column names     | Realised   | Medium | Fixed during build by overriding column names (`month_value`, `year_value`)                             |
| Browser caches stale JS preventing UI bug fixes       | High       | Low    | Documented `Ctrl+F5` / `Ctrl+Shift+Delete` workaround; future improvement: cache-busting query params   |

---

## 11. Glossary

| Term          | Definition                                                                                         |
|---------------|----------------------------------------------------------------------------------------------------|
| Expense       | A single financial transaction recorded by the user                                                |
| Category      | One of eight predefined groupings: FOOD, TRANSPORT, ENTERTAINMENT, HEALTH, UTILITIES, SHOPPING, EDUCATION, OTHER |
| Payment Method| One of four channels: CASH, CARD, UPI, NETBANKING                                                  |
| Budget        | A per-category monthly spending limit set by the user                                              |
| Anomaly       | A new expense that exceeds 2× the 3-month average for its category                                 |
| AI Chat       | The Gemini-backed natural-language Q&A feature at `/ai/chat`                                       |
| Insight       | The Gemini-generated 4-5 line monthly spending summary on the dashboard                            |
| Fallback      | The string `"AI service unavailable. Please try again."` shown when a Gemini call fails            |
| Rate Limit    | The 12-calls-per-hour cap enforced before each Gemini API request                                  |
| Flyway        | The database migration tool that owns all production DDL                                           |
| Render        | The PaaS provider hosting the live deployment                                                      |

---

## 12. Revision History

| Version | Date       | Author                       | Notes                                |
|---------|------------|------------------------------|--------------------------------------|
| 1.0     | 2026-05-18 | Prachi Dnyaneshwar Parate    | Initial release covering v1 features |
