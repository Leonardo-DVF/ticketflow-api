# TicketFlow

TicketFlow is a REST API for event ticketing. It manages events, sectors, seats, time-limited reservations, simulated payments, and ticket issuance.

The project is a learning-focused backend portfolio piece built as a modular monolith. Its main goal is to demonstrate real business rules beyond CRUD: preventing double booking under concurrent requests, expiring reservations safely, and reacting asynchronously to payment events with Apache Kafka.

## The problem

Ticket sales have a critical consistency requirement: two people must never buy the same seat. A simple “check if available, then reserve” approach is unsafe because two simultaneous requests can both read the same available seat before either one writes a change.

TicketFlow combines PostgreSQL row locks, Redis locks with TTL, and transactional state changes to ensure that only one reservation can win. A successful payment is then handled asynchronously so ticket issuance, notification, and audit work can evolve independently.

## Architecture

The application is a modular monolith: modules are separated by business responsibility but deployed as one Spring Boot application. This keeps the system approachable while preserving natural boundaries for a future service extraction.

```text
                         +----------------------+
                         |    Spring Boot API   |
                         |  auth / events /     |
                         |  reservation /       |
                         |  payment / ticket    |
                         +----------+-----------+
                                    |
              +---------------------+---------------------+
              |                     |                     |
       PostgreSQL                 Redis                 Kafka
   durable business data     temporary seat locks    domain events
              |                                           |
              |                                 +---------+---------+
              |                                 |         |         |
              |                              Ticket  Notification  Audit
              |                              consumer  consumer   consumer
```

## Main flow

```text
User selects seats
        |
        v
POST /reservations
        |
        +--> Redis temporarily locks each seat (10-minute TTL)
        +--> PostgreSQL locks the corresponding rows and records the reservation
        +--> seats become RESERVED
        |
        v
POST /payments/{reservationId}/approve
        |
        v
payment-approved topic
        |
        +--> Ticket consumer: confirms reservation, marks seats SOLD, creates tickets
        +--> Notification consumer: records a simulated notification
        +--> Audit consumer: persists an audit log
```

## Concurrency and temporary locks

PostgreSQL is the source of truth for seat and reservation state. While a reservation is created, the application fetches seat rows using pessimistic locking in a deterministic order. That serializes conflicting database writes and prevents a seat from being sold twice.

Redis adds a fast, short-lived distributed lock before the database work. Each key follows this format:

```text
seat:lock:{eventId}:{seatId}
```

The lock is acquired atomically with `SET IF ABSENT` and has a 10-minute TTL. It is transferred to the reservation after persistence, then released after cancellation, expiration, or confirmation. Owner-checked Lua scripts ensure that one reservation cannot remove another reservation’s lock. The TTL is a final safety net if cleanup cannot reach Redis.

When a reservation expires, a scheduled job releases its seats, marks the reservation as `EXPIRED`, removes the related Redis locks, and publishes `reservation-expired`.

## Why Kafka?

Kafka is used where one business event needs independent reactions. When payment approval occurs, the payment module publishes a `PaymentApprovedEvent` to `payment-approved`; it does not need direct knowledge of tickets, notifications, or audit records.

```text
Payment service -> payment-approved -> ticket consumer
                                  -> notification consumer
                                  -> audit consumer
```

- A **producer** publishes an event, such as `PaymentApprovedEvent`.
- A **topic** is the named event stream, such as `payment-approved`.
- A **consumer** reads events and performs one responsibility.
- A **consumer group** lets every responsibility receive the event once, while allowing multiple instances of the same responsibility to share processing work.
- An **offset** is Kafka’s position marker for a consumer group, so it can resume from the next event after a restart.

Events are separate DTO records rather than JPA entities. This avoids coupling the message contract to persistence mappings.

Currently configured topics:

| Topic | Event | Producer |
| --- | --- | --- |
| `reservation-created` | `ReservationCreatedEvent` | Reservation service |
| `reservation-expired` | `ReservationExpiredEvent` | Expiration service |
| `payment-approved` | `PaymentApprovedEvent` | Payment service |
| `payment-rejected` | `PaymentRejectedEvent` | Payment service |
| `ticket-issued` | `TicketIssuedEvent` | Ticket confirmation flow |

## Technology

- Java 21, Spring Boot, Spring Web MVC
- Spring Data JPA, PostgreSQL, Flyway
- Spring Security with JWT
- Apache Kafka and Spring for Apache Kafka
- Redis for temporary seat locks
- Bean Validation and global API error handling
- OpenAPI / Swagger UI
- JUnit, Mockito, and Testcontainers
- Docker and Docker Compose

## Running with Docker

Requirements: Docker Desktop and Docker Compose.

1. Create a local environment file:

   ```powershell
   Copy-Item .env.example .env
   ```

2. Replace `JWT_SECRET` in `.env` with a random value containing at least 32 characters.

   To start with an administrator for local development, also set the following values. The bootstrap is disabled by default and should not use a shared or production password.

   ```text
   ADMIN_BOOTSTRAP_ENABLED=true
   ADMIN_BOOTSTRAP_NAME=TicketFlow Admin
   ADMIN_BOOTSTRAP_EMAIL=admin@example.com
   ADMIN_BOOTSTRAP_PASSWORD=a-strong-local-password
   ```

3. Start the complete environment:

   ```powershell
   docker compose up -d --build
   ```

The API waits for PostgreSQL, Redis, and Kafka health checks. Flyway applies pending migrations on startup and Hibernate validates the schema; it never creates the schema automatically.

Useful URLs:

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Health: `http://localhost:8080/actuator/health`

Only health endpoints are exposed by Actuator. Docker Compose uses this endpoint to report the application container as healthy after its dependencies are available.

To stop the environment without deleting database data:

```powershell
docker compose down
```

## Running locally

Start the infrastructure first:

```powershell
docker compose up -d postgres redis kafka
```

Set the JWT secret and start the application:

```powershell
$env:JWT_SECRET = "replace-with-a-random-secret-containing-at-least-32-characters"
.\mvnw.cmd spring-boot:run
```

On Linux or macOS, use `./mvnw spring-boot:run`.

Local defaults and all configurable variables are documented in `.env.example`. In Docker, the application uses internal service addresses (`postgres`, `redis`, and `kafka:19092`); on the host it uses `localhost`.

## API overview

Public endpoints:

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/auth/register` | Register a user |
| `POST` | `/auth/login` | Authenticate and obtain a JWT |
| `GET` | `/events` | List available events |
| `GET` | `/events/{id}` | Get an available event |
| `GET` | `/events/{eventId}/seats` | List event seats |
| `GET` | `/events/{eventId}/seats/available` | List available event seats |

Authenticated-user endpoints require `Authorization: Bearer <token>`:

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/reservations` | Create a temporary reservation |
| `GET` | `/reservations/{id}` | Get an owned reservation |
| `GET` | `/reservations/my` | List the caller’s reservations |
| `DELETE` | `/reservations/{id}` | Cancel a pending reservation |
| `POST` | `/payments/{reservationId}/approve` | Simulate payment approval |
| `POST` | `/payments/{reservationId}/reject` | Simulate payment rejection |
| `GET` | `/tickets/my` | List the caller’s tickets |
| `GET` | `/tickets/{id}` | Get an owned ticket |

Administrative endpoints require the `ADMIN` role:

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/events` | Create an event |
| `PUT` | `/events/{id}` | Update an event |
| `DELETE` | `/events/{id}` | Cancel an event |
| `POST` | `/events/{eventId}/sectors` | Create a sector |
| `GET` | `/events/{eventId}/sectors` | List sectors |
| `POST` | `/sectors/{sectorId}/seats` | Create a seat |

New registrations receive the `USER` role. To bootstrap an administrator in local development, set the `ADMIN_BOOTSTRAP_*` variables shown above before starting the application. When enabled, the bootstrap creates the configured administrator or promotes an existing user with that email; it does not overwrite an existing password.

The bootstrap is disabled by default. Never commit its password or enable it in an untrusted environment.

## Example reservation request

```http
POST /reservations
Authorization: Bearer <token>
Content-Type: application/json

{
  "eventId": 10,
  "seatIds": [100, 101]
}
```

Seats must be unique, belong to the selected available event, and be in `AVAILABLE` status. A conflicting reservation returns a `409 Conflict` response using the standard API error format:

```json
{
  "timestamp": "2026-10-19T12:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Seat A10 is already reserved",
  "path": "/reservations"
}
```

## Tests

Run the test suite with Docker running:

```powershell
.\mvnw.cmd test
```

The suite covers service rules, controllers, Kafka serialization and consumers, Flyway persistence, Redis locking, and end-to-end payment processing. Testcontainers is used for integration tests with real PostgreSQL and Kafka infrastructure. A dedicated concurrency integration test issues simultaneous reservation attempts for the same seat and verifies that exactly one succeeds.

## Project structure

```text
src/main/java/com/leonardo/ticketflow
├── auth            authentication and JWT
├── event           events
├── sector          event sectors and pricing
├── seat            seats and availability
├── reservation     reservation, expiration, locks
├── payment         simulated payments
├── ticket          ticket generation and queries
├── notification    simulated notifications
├── audit           audit log persistence
├── kafka           topics, producers, consumers, event DTOs
├── config          application configuration
└── exception       standardized API error handling
```

## Next improvements

- Add idempotent consumption and a transactional outbox for stronger delivery guarantees.
- Add operational health checks and metrics.
- Evolve selected modules into separate services only when operational needs justify it.
