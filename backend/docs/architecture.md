# Microservice Platform Architecture

This starter kit contains a Java 21 Spring Boot 3.4 microservice backend, a React 18 TypeScript
frontend, Docker Compose infrastructure, and Kubernetes base manifests.

Services use Eureka for discovery, Spring Cloud Config for centralized configuration,
PostgreSQL for source-of-truth storage, Redis as cache/session acceleration, Kafka for async
events, and Micrometer/Prometheus/Zipkin/Grafana for observability.
