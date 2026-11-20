# Library REST API

A RESTful book management service built with Java 21, Spring Boot 4.1.0, Spring Data JPA, Bean Validation, PostgreSQL, and OpenAPI / Swagger UI.

## Tech Stack

- **Language & Build**: Java 21, Maven Wrapper (`./mvnw` / `mvnw.cmd`)
- **Framework**: Spring Boot 4.1.0 (`spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`)
- **Database**: PostgreSQL (runtime), H2 in-memory PostgreSQL compatibility mode (tests)
- **API Docs**: `springdoc-openapi-starter-webmvc-ui` 3.0.0

## Security Notice

An earlier commit (`45c776b` on `main`) included a hardcoded local database password in `src/main/resources/application.properties`. All credentials have been removed from tracked configuration and replaced with environment variable placeholders. If you previously used that local database password, rotate it immediately.

## Prerequisites

- JDK 21 or newer
- PostgreSQL 14+ (for running the application outside the test suite)

## Configuration & Environment Variables

Default settings in `src/main/resources/application.properties` use environment variable placeholders:

| Variable | Description | Default |
|---|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/library_db` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | *(empty)* |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | Hibernate schema mode (`validate`, `update`, etc.) | `validate` |
| `SPRING_JPA_SHOW_SQL` | Enable SQL logging | `false` |
| `APP_CORS_ALLOWED_ORIGINS` | Comma-separated allowed CORS origins for `/api/**` | `http://localhost:3000,http://localhost:5173` |

Copy `.env.example` to `.env` (gitignored) or export variables in your shell before starting the app. For local development where Hibernate auto-updates the schema, activate the `dev` profile (`application-dev.properties`).

## Running Locally

Using environment variables and the `dev` profile on Windows PowerShell:

```powershell
$env:SPRING_DATASOURCE_PASSWORD="your-local-password"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

On macOS / Linux:

```bash
SPRING_DATASOURCE_PASSWORD="your-local-password" ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Running Tests

Tests run in complete isolation against an in-memory H2 database configured in PostgreSQL compatibility mode (`src/test/resources/application.properties`). No running PostgreSQL instance is required.

```powershell
.\mvnw.cmd clean test
```

## API Documentation (Swagger UI)

When the application is running:

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

## Endpoints

| Method | Path | Description | Success Status |
|---|---|---|---|
| `GET` | `/api/books` | List books (supports `page`, `size`, `sort`, and optional `title`, `author`, `isbn` filters) | `200 OK` |
| `GET` | `/api/books/{id}` | Retrieve a single book by positive ID | `200 OK` |
| `POST` | `/api/books` | Create a book (returns `Location` header and generated `id`) | `201 Created` |
| `PUT` | `/api/books/{id}` | Update an existing book | `200 OK` |
| `DELETE` | `/api/books/{id}` | Delete a book | `204 No Content` |

### Validation & Error Responses

- `title` and `author` are required and capped at 255 characters.
- `isbn` is required and normalized by stripping hyphens/spaces and uppercasing `X`; must be a valid 10-digit (`^\d{9}[\dX]$`) or 13-digit (`^\d{13}$`) ISBN and unique across books.
- Errors return a structured JSON `ApiError` payload (`timestamp`, `status`, `error`, `message`, `path`, and optional `fieldErrors`).

### `curl` Examples

Create a book:

```bash
curl -i -X POST http://localhost:8080/api/books \
  -H "Content-Type: application/json" \
  -d '{"title":"Test Book Title","author":"Test Author","isbn":"978-0-00-000000-1"}'
```

List / filter books:

```bash
curl "http://localhost:8080/api/books?title=Test&page=0&size=10"
```

Get by ID:

```bash
curl http://localhost:8080/api/books/1
```

Update a book:

```bash
curl -X PUT http://localhost:8080/api/books/1 \
  -H "Content-Type: application/json" \
  -d '{"title":"Updated Test Book","author":"Test Author","isbn":"9780000000001"}'
```

Delete a book:

```bash
curl -i -X DELETE http://localhost:8080/api/books/1
```
