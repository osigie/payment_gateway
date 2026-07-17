# Payment Gateway

A robust payment microservice for merchant payment flows, built with Spring Boot. The service provides RESTful endpoints to authorize, capture, void, and refund payments, with support for idempotency, transactional safety, and integration with a (mock or real) bank API. The project demonstrates best practices in security, error handling, and developer experience including end-to-end tests, an OpenAPI contract, and operational endpoints via Spring Boot Actuator.

---

## Features
- **Comprehensive payment flows:** authorization, capture, void, and refund
- **API key-based merchant authentication** (x-api-key header; merchant registration is not available via the API merchants must be seeded via migrations or admin workflows)
- **Request idempotency** via the x-idempotency-key header
- **PostgreSQL as primary datastore**
- **Resilience:** exponential backoff, retries with jitter, robust error handling
- **Spring Actuator health endpoints** for readiness and liveness
- **Swagger/OpenAPI docs** auto-generated and always in sync
- **Seeded merchant support** for easy local and integration testing
- **Built and containerized with Jib**

---

## Getting Started

### Prerequisites
- Java 25
- Docker and Docker Compose
- Maven 3.9+
- Running Bank API

### Quick Start (Docker Compose)
```sh
cp .env.example .env           # copy environment example if you don't have a .env file
./mvnw compile jib:dockerBuild # build the Docker image with Jib

docker-compose up --build      # launch service and dependencies
```
This launches the service at [http://localhost:8080](http://localhost:8080) and boots a local Postgres instance.

Configure your `.env` and see all environment variables required in `.env.example`.

### Manual Run
1. Create and seed a Postgres database (settings in `application-dev.yml`).
2. Build and run with Java 25:
   ```sh
   ./mvnw spring-boot:run
   ```
   Or, to build the container using [Jib](https://github.com/GoogleContainerTools/jib):
   ```sh
   ./mvnw compile jib:dockerBuild
   ```


## API Endpoints & Documentation
- **OpenAPI/Swagger UI:**  [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI Spec:**       [http://localhost:8080/openapi.yaml](http://localhost:8080/openapi.yaml)
- **Health Check:**       [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

### Core Payment Endpoints (all under `/api/v1/payments`)
- `POST /api/v1/payments`         — authorize a payment (headers: x-api-key, x-idempotency-key)
- `POST /api/v1/payments/{id}/capture` — capture a payment
- `POST /api/v1/payments/{id}/void`    — void a payment
- `POST /api/v1/payments/{id}/refund`  — refund a captured payment
- `GET /api/v1/payments`/`{id}`   — list and fetch payments for the merchant
- All requests must include API Key header: `x-api-key: <your-key>`

> Requests missing required headers or submitting reused idempotency keys respond with 400/401/409 as appropriate.

### Seeded Merchant
A test merchant is pre-seeded in the database via Flyway migration:

| ID | Name | API Key |
|---|---|---|
| d290f1ee-6c54-4b01-90e6-d701748f0851 | Test Merchant | sk_test_merchant_1234567890 |

Use the API key in the `x-api-key` header for local development and testing:
```
x-api-key: sk_test_merchant_1234567890
```

## Testing
- Unit and integration tests cover core flows, error cases, idempotency, and authentication (JUnit + MockMvc)
- Run all tests:
```sh
./mvnw test
```

## Author
Ken Osagie (<kenosagie88@gmail.com>)
