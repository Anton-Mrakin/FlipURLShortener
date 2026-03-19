# Full URL Shortener

A high-performance, resilient URL shortening service built with Spring Boot, leveraging Hexagonal Architecture and modern observability tools.

## Table of Contents
- [Features](#features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Observability](#observability)
- [Performance & Benchmarks](#performance--benchmarks)
- [Configuration](#configuration)
- [Getting Started](#getting-started)
- [API Reference](#api-reference)

## Features
- **Deterministic Shortening**: Same URL always yields the same short code using SHA-256.
- **Multiple Strategies**: Supports SHA-256, Random String, and Base62 generators.
- **High Throughput**: Optimized for high concurrent read/write loads with Redis caching.
- **Resilience**: Built-in Rate Limiting and Retry mechanisms using Resilience4j.
- **Event-Driven**: Integration with Kafka and ActiveMQ for URL access tracking using the Transactional Outbox pattern.
- **Self-Maintenance**: Automatic asynchronous LRU eviction and periodic cleanup to maintain storage limits.
- **Observability**: Full instrumentation with Prometheus metrics and Loki logs.
- **Cloud Native**: Kubernetes-ready with Liveness and Readiness probes.

## Architecture
The project follows **Hexagonal Architecture** (Ports and Adapters) to ensure business logic is decoupled from infrastructure:
- `domain`: Core models and business rules. Zero external dependencies.
- `usecases`: Implementation of application logic (Shorten, Resolve).
- `infra`: Adapters for Database (PostgreSQL), Cache (Redis), Messaging (Kafka/ActiveMQ), and REST API.

### Transactional Outbox
Ensures reliable message delivery to Kafka/ActiveMQ even during database or network failures. Uses `transaction-outbox` library for robust implementation.

## Tech Stack
- **Framework**: Spring Boot 3.3.4
- **Language**: Java 21
- **Database**: PostgreSQL 16 (with Flyway migrations)
- **Cache**: Redis 7
- **Messaging**: Apache Kafka / ActiveMQ
- **Resilience**: Resilience4j (Retry, RateLimiter)
- **Observability**: Prometheus, Grafana, Loki, Micrometer
- **Testing**: JUnit 5, Testcontainers, Mockito, JMH

## Observability
The service is fully instrumented for production monitoring:
- **Metrics**: Available at `/actuator/prometheus`. Tracks `url_count`, request counters, and latencies.
- **Logging**: Integrated with Grafana Loki for centralized log aggregation.
- **Health Checks**: Standard Spring Boot Actuator `/health` (Liveness/Readiness) and `/info` endpoints.

## Performance & Benchmarks
The service has been benchmarked using JMH for generator performance.

### Generator Benchmarks (JMH)
| Benchmark | Mode | Score (ops/s) |
|-----------|------|---------------|
| Base62    | thrpt| ~41,600,000   |
| Random    | thrpt| ~113,000,000  |
| SHA-256   | thrpt| ~10,400,000   |

### Integration Load Test Results
- **Avg Latency**: ~430 ms
- **Throughput**: ~2,260 req/sec
*(Tested on local environment with concurrent clients)*

## Configuration
Key properties in `application.yml`:
- `app.url-limit`: Maximum storage capacity.
- `app.max-url-length`: Maximum length of input URL (default 2048).
- `app.generator.name`: Selected generator (`sha256Generator`, `randomStringGenerator`, `base62Generator`).
- `app.short-code-length`: Configurable length for short codes.

## Getting Started

### Prerequisites
- Docker & Docker Compose
- JDK 21
- Maven

### Build & Run
```bash
# Build the project
./mvnw clean package

# Run with all infrastructure
docker-compose up -d
```

## API Reference

### Shorten URL
`POST /api/v1/urls/shorten`
- **Request**: `String` (Plain text URL)
- **Response**: `200 OK` with short code.

### Resolve URL
`GET /api/v1/urls/{shortCode}`
- **Response**: `200 OK` with original URL.

---
Developed as a high-performance demonstration project.
