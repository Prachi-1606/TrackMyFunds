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

## Spring Profiles

| Profile | Datasource | DDL           | Flyway  | Thymeleaf cache |
|---------|------------|---------------|---------|-----------------|
| `dev`   | H2 mem     | `create-drop` | off     | off             |
| `prod`  | MySQL 8    | `validate`    | on      | on              |
| `test`  | H2 mem     | `create-drop` | off     | off             |
