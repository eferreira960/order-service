# HACOM Orders Processing Backend

A Spring Boot reactive backend for order processing with MongoDB, gRPC, Akka Classic Actors, SMPP SMS integration, and Prometheus metrics.

## Technologies Used

- **Java 17** - Language
- **Spring Boot 3.2.5** with **Spring WebFlux** - Reactive REST API
- **Spring Data MongoDB Reactive** - Reactive MongoDB access
- **gRPC** - Inter-service communication for order creation
- **Akka Classic Actors** - Asynchronous order processing
- **CloudHopper SMPP** - SMS sending via SMPP protocol
- **Log4j2** with YAML configuration - Logging
- **Spring Actuator** with **Micrometer Prometheus** - Metrics
- **SpringDoc OpenAPI (Swagger)** - API documentation
- **GitHub Actions** - CI/CD pipeline
- **Gradle 8.7** - Build tool

## Architecture

The project follows **Clean Architecture** principles with three layers:

```
com.hacom.orders
├── application/        → Configuration, use cases, metrics
│   ├── config/         → Programmatic MongoDB, WebFlux, Akka, OpenAPI configs
│   └── metrics/        → Custom Prometheus counters
├── domain/             → Enterprise business rules
│   ├── model/          → Order entity
│   ├── port/           → Repository and SMS sender interfaces
│   └── service/        → Domain services (if needed)
├── infrastructure/     → Frameworks, drivers, external tools
│   ├── persistence/    → MongoDB reactive repository implementation
│   ├── grpc/           → gRPC service implementation
│   ├── actor/          → Akka Classic Actor for order processing
│   ├── smpp/           → CloudHopper SMPP client
│   └── rest/           → REST API controllers
├── bootstrap/          → Spring Boot application entry point
└── shared/dto/         → Shared DTOs (future use)
```

## Flow

1. **gRPC Client** sends `CreateOrder` request → `GrpcOrderService`
2. `GrpcOrderService` creates an **Akka Actor** and sends a `ProcessOrderMessage`
3. **Akka Actor** (`OrderProcessorActor`) processes the order:
   - Saves order to **MongoDB** with status `PROCESSED`
   - Sends **SMS** notification via **SMPP** client
   - Increments **Prometheus counter** metric
   - Returns gRPC response with order ID and status
4. **REST API** allows querying:
   - `GET /api/orders/{orderId}/status` - Check order status
   - `GET /api/orders/count?from=...&to=...` - Count orders by date range

## Prerequisites

- Java 17 (JDK 17+)
- MongoDB 7.0+ (running on localhost:27017 or configured URI)
- Gradle 8.7+ (or use the Gradle wrapper)

## Configuration

### application.yml

The main configuration is in `src/main/resources/application.yml`:

```yaml
mongodbDatabase: exampleDb
mongodbUri: "mongodb://127.0.0.1:27017"
apiPort: 9898
```

> **Note:** MongoDB and WebFlux port are configured **programmatically** via `MongoConfig` and `WebFluxConfig` classes, not via Spring Boot auto-configuration.

### SMPP Configuration

```yaml
smpp:
  host: localhost       # SMSC host
  port: 2775            # SMSC port (default for smppsim)
  system-id: smppclient # SMPP system ID
  password: password    # SMPP password
```

## Build and Run

### Using Gradle Wrapper

```bash
# Generate Gradle wrapper (first time)
gradle wrapper --gradle-version 8.7

# Build the project
./gradlew clean build

# Run tests
./gradlew test

# Run the application
./gradlew bootRun
```

### Using Docker (Multi-Stage Build)

The project includes a **multi-stage Docker build** for optimized production images:

#### Build Stage:
- Uses `gradle:8.7-jdk17` to compile and package the application
- Source code is copied and built with Gradle

#### Runtime Stage:
- Uses `eclipse-temurin:17-jre-alpine` (minimal JRE image)
- Runs as a **non-root user** for security
- Includes **health check** via Actuator endpoint
- Exposes ports 9898 (REST API) and 9090 (gRPC)

#### Build and Run with Docker Compose

```bash
# Build and start all services (MongoDB + App)
docker-compose up --build

# Run in background
docker-compose up --build -d

# Include SMPP simulator service
docker-compose --profile smpp up --build

# Stop all services
docker-compose down

# View logs
docker-compose logs -f app
```

#### Build and Run Manually

```bash
# Build the Docker image
docker build -t hacom-orders-backend .

# Run with MongoDB (adjust network as needed)
docker run -d --name hacom-mongodb -p 27017:27017 mongo:7.0

docker run -d --name hacom-orders-app \
  --link hacom-mongodb \
  -p 9898:9898 \
  -p 9090:9090 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e mongodbUri=mongodb://hacom-mongodb:27017 \
  hacom-orders-backend
```

#### Docker Architecture

```
MongoDB:27017 <-  hacom-orders-app:9898 (REST API):9090 (gRPC)
SMPP Server:2775
```

## API Endpoints

### REST API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/orders/{orderId}/status` | Get order status |
| GET | `/api/orders/count?from={iso}&to={iso}` | Count orders by date range |

**Example:**
```bash
# Get order status
curl http://localhost:9898/api/orders/ORD-001/status

# Count orders in date range
curl "http://localhost:9898/api/orders/count?from=2024-01-01T00:00:00Z&to=2024-12-31T23:59:59Z"
```

### gRPC Service

The gRPC server runs on port **9090**.

**Service Definition:**
```protobuf
service OrderService {
  rpc CreateOrder (OrderRequest) returns (OrderResponse);
}

message OrderRequest {
  string order_id = 1;
  string customer_id = 2;
  string customer_phone_number = 3;
  repeated string items = 4;
}

message OrderResponse {
  string order_id = 1;
  string status = 2;
}
```

### Swagger UI

Access the interactive API documentation at:
```
http://localhost:9898/swagger-ui.html
```

### Actuator Endpoints

| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | Health check |
| `/actuator/info` | Application info |
| `/actuator/metrics` | Application metrics |
| `/actuator/prometheus` | Prometheus metrics |

### Custom Prometheus Metrics

| Metric | Description |
|--------|-------------|
| `hacom_orders_processed_total{status="success"}` | Total successfully processed orders |
| `hacom_orders_processed_total{status="failed"}` | Total failed orders |

## Testing

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "*OrderControllerTest"
```

Tests use:
- **JUnit 5** - Test framework
- **Mockito** - Mocking framework
- **Flapdoodle Embedded MongoDB** - In-memory MongoDB for integration tests
- **WebTestClient** - Reactive REST client for controller tests