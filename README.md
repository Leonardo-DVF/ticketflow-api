# TicketFlow

TicketFlow is a REST API for event management, temporary seat reservations, simulated payments, and ticket issuing. The project is being built incrementally to explore concurrency control and event-driven architecture with Java and Spring Boot.

## Project status

The project is in its initial setup stage. Business features and infrastructure will be introduced in small, testable increments so that each technology solves a concrete problem.

## Planned features

- User registration and JWT authentication
- Event, sector, and seat management
- Temporary seat reservations
- Concurrency control to prevent double booking
- Simulated payment approval and rejection
- Asynchronous ticket, notification, and audit processing
- Reservation expiration

## Technology roadmap

- Java 21 and Spring Boot
- PostgreSQL and Flyway for persistence and database migrations
- Redis for temporary seat locks
- Apache Kafka for asynchronous domain events
- Spring Security and JWT for authentication and authorization
- JUnit, Mockito, and Testcontainers for automated tests
- Docker and Docker Compose for a reproducible environment
- OpenAPI/Swagger for API documentation

## Running the project

Requirements:

- Java 21
- Docker and Docker Compose

Start PostgreSQL:

```powershell
docker compose up -d postgres
```

The local database uses `ticketflow` as its database name, username, and password. The application provides matching development defaults. For another environment, use the `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` environment variables documented in `.env.example`.

Set a JWT signing secret containing at least 32 characters before starting the application:

```powershell
$env:JWT_SECRET = "replace-with-a-random-secret-containing-at-least-32-characters"
```

Flyway applies pending migrations automatically when the application starts. Hibernate is configured with `ddl-auto=validate`, so database changes must be introduced explicitly through migration files.

Start the application:

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

On Linux or macOS:

```bash
./mvnw spring-boot:run
```

## Running the tests

The integration test starts an isolated PostgreSQL container and verifies that the Flyway migrations were applied. Docker must be running.

```powershell
.\mvnw.cmd test
```

## Architecture

TicketFlow will start as a modular monolith. Modules will remain separated by business responsibility while sharing the same deployable application. This keeps the learning path approachable and leaves room for future service extraction where it provides real value.

## Learning goals

The project aims to demonstrate backend concepts beyond basic CRUD, especially:

- preventing two users from reserving the same seat;
- managing temporary reservations with expiration;
- publishing domain events without exposing JPA entities;
- allowing independent Kafka consumers to react to the same business event;
- testing integrations against real infrastructure with Testcontainers.
