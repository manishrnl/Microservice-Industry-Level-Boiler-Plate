# Backend Deep Dive And Troubleshooting Guide

This document explains the backend codebase deeply enough for an engineer to trace requests, find the owning service, understand configuration, and troubleshoot production or local failures.

Related docs:

- `README.md` for quick setup and ports.
- `testApi.md` for manual API test order and sample payloads.
- `backend/docs/api-contracts/README.md` for API contract notes.
- `backend/config-server-repo/README.md` for Config Server usage.

## Backend Summary

The backend is a Java 21, Spring Boot 3.4.5, Spring Cloud 2024.0.1 Maven multi-module platform. The parent build is `pom.xml` at the repository root.

Main modules:

| Module | Port | Responsibility |
| --- | ---: | --- |
| `backend/commons` | n/a | Shared DTOs, API wrappers, exceptions, base entities, enums, JWT/security utilities, cache config, ModelMapper config |
| `backend/config-server` | `8888` | Spring Cloud Config Server, loading config from local native repo or Git |
| `backend/discovery-server` | `8761` | Eureka service discovery dashboard and registry |
| `backend/api-gateway` | `8080` | Single browser/API entrypoint, route forwarding, CORS, JWT validation, rate limiting, gateway logs, observability proxy |
| `backend/auth-service` | `8081` | Signup, login, OAuth, JWT creation, refresh-token cookie, sessions, OTP email, account lock/suspend/delete, admin unlock/password |
| `backend/user-service` | `8082` | User profiles, account settings, identity/contact details, preferences, admin user and role operations |
| `backend/notification-service` | `8083` | Notifications, read state, deletion, login notification creation, basic SSE endpoint |
| `backend/payment-service` | `8084` | Demo payments, Stripe checkout creation, Stripe webhook verification, payment history |
| `backend/file-service` | `8085` | Upload, file metadata, download URL, direct download/preview, seeded README/demo files |
| `backend/ai-service` | `8086` | Chat sessions, messages, provider fallback, token estimates, AI usage |
| `backend/audit-service` | `8087` | Admin audit query/export and seeded audit records |

## Build And Runtime Shape

The root `pom.xml` is a parent POM with these modules:

```text
backend/commons
backend/config-server
backend/discovery-server
backend/api-gateway
backend/auth-service
backend/user-service
backend/notification-service
backend/payment-service
backend/file-service
backend/ai-service
backend/audit-service
```

Important build details:

- Java release: `21`.
- Spring Boot: `3.4.5`.
- Spring Cloud: `2024.0.1`.
- Test framework: TestNG through Surefire.
- JaCoCo is configured in plugin management and runs during `test`.

Common commands:

```powershell
mvn clean package
mvn test
mvn clean package -DskipTests -pl backend/auth-service -am
mvn spring-boot:run -pl backend/api-gateway -am
```

Docker Compose is the expected local runtime because services depend on PostgreSQL, Redis, Kafka, Config Server, Eureka, and optional observability tools.

```powershell
docker compose --env-file backend/.env --env-file frontend/.env --profile local-infra up --build
```

## Configuration Model

Every service imports local `.env` files and optionally imports the Config Server.

Most service-local `application.yml` files are intentionally small:

```yaml
spring:
  application:
    name: auth-service
  config:
    import:
      - "optional:file:.env[.properties]"
      - "optional:file:../.env[.properties]"
      - "optional:file:../../.env[.properties]"
      - "optional:configserver:${CONFIG_SERVER_URL:http://config-server:8888}"
```

Config defaults live in `backend/config-server-repo/`.

Important config files:

| File | Purpose |
| --- | --- |
| `backend/config-server/src/main/resources/application.yml` | Config Server runtime, native/Git backend selection, encryption key |
| `backend/config-server-repo/application.yml` | Shared service defaults: datasource pool, Redis cache, Kafka, JPA, Actuator, Eureka, logging |
| `backend/config-server-repo/api-gateway.yml` | Gateway port, CORS, routes, JWT JWKS URL, rate limit, observability |
| `backend/config-server-repo/auth-service.yml` | Auth database, Redis, mail, OAuth, JWT, app URLs, downstream service URLs |
| `backend/config-server-repo/user-service.yml` | User DB and auth DB access for admin/user account joins |
| `backend/config-server-repo/payment-service.yml` | Payment DB, Stripe keys, default currency, frontend return URL |
| `backend/config-server-repo/file-service.yml` | File DB, MinIO-style values, public/download base URLs |
| `backend/config-server-repo/ai-service.yml` | AI DB, Redis, Gemini/Groq provider settings |
| `backend/config-server-repo/audit-service.yml` | Audit DB and JPA settings |

Critical environment files:

- `backend/.env.example`: backend service, database, security, OAuth, AI, file, mail, payment, observability values.
- `frontend/.env.example`: frontend Vite values used by the browser.

When a service cannot start because a property is missing, check these in order:

1. `backend/.env` exists and was copied from `backend/.env.example`.
2. `CONFIG_SERVER_BACKEND=native` for local Docker.
3. `CONFIG_NATIVE_SEARCH_LOCATIONS=file:/app/config-server-repo` inside Compose.
4. Service `spring.application.name` matches the corresponding config file name.
5. Config Server health is up: `http://localhost:8888/actuator/health`.

## Gateway Responsibilities

The API Gateway is the public backend entrypoint at `http://localhost:8080`.

Important files:

| File | Responsibility |
| --- | --- |
| `backend/api-gateway/src/main/resources/application.yml` | Routes, CORS, Redis, Eureka, JWT JWKS URL, rate-limit settings |
| `GatewaySecurityConfig.java` | Disables form/basic/CSRF and permits gateway exchanges |
| `JwtAuthenticationFilter.java` | Validates bearer tokens for protected paths and forwards identity headers |
| `CorrelationIdFilter.java` | Adds request correlation IDs |
| `RateLimitFilter.java` | Applies anonymous/authenticated request limits through Redis |
| `RequestLoggingFilter.java` | Records request metadata and slow requests |
| `SecurityHeadersFilter.java` | Adds defensive response headers |
| `ObservabilityLogController.java` | Admin-only gateway log and Loki proxy endpoints |

Gateway route map:

| Prefix | Destination |
| --- | --- |
| `/api/v1/auth/**` | `auth-service:8081` |
| `/api/v1/users/**` | `user-service:8082` |
| `/api/v1/notifications/**` | `notification-service:8083` |
| `/api/v1/ai/**` | `ai-service:8086` |
| `/api/v1/payments/**` | `payment-service:8084` |
| `/api/v1/files/**` | `file-service:8085` |
| `/api/v1/audit/**` | `audit-service:8087` |

Gateway JWT behavior:

- Public paths include login, signup, refresh, logout, OTP/password endpoints, OAuth paths, JWKS, actuator, Swagger paths, and payment webhooks.
- Protected paths require `Authorization: Bearer <access_token>`.
- The JWT is decoded using the auth-service JWKS endpoint configured by `AUTH_JWKS_URL`.
- On success, the gateway forwards:
  - `X-User-Id`
  - `X-User-Email`
  - `X-User-Name`
  - `X-User-Roles`
  - `X-Session-Id`
- On missing/invalid token, it returns a Problem Details style `401`.

Troubleshooting gateway failures:

| Symptom | Most likely cause | Check |
| --- | --- | --- |
| Browser CORS error | Frontend origin mismatch | `FRONTEND_PUBLIC_URL`, `VITE_FRONTEND_PUBLIC_URL`, gateway CORS allowed patterns |
| `401 Missing bearer token` | Frontend did not attach access token | `frontend/src/api/axiosInterceptor.js`, localStorage key `platform.accessToken` |
| `401 Invalid or expired token` | Expired/invalid JWT or JWKS unavailable | Auth refresh endpoint, `AUTH_JWKS_URL`, auth-service logs |
| `503` or gateway route failure | Downstream service down/unhealthy | `docker compose logs <service>`, service health endpoint |
| Rate-limit responses | Redis/rate limit config | `GATEWAY_RATE_LIMIT_*`, Redis health, `RateLimitFilter.java` |
| Admin log page fails | Missing ADMIN/SUPER_ADMIN or Loki down | `/api/v1/observability/logs`, `LOKI_URL`, JWT roles |

## Auth Service

Primary code:

- Controller: `backend/auth-service/src/main/java/com/company/platform/auth/controller/AuthController.java`
- OAuth controller: `backend/auth-service/src/main/java/com/company/platform/auth/controller/OAuthController.java`
- Service: `backend/auth-service/src/main/java/com/company/platform/auth/service/AuthService.java`
- Session service: `AuthSessionService.java`
- Token service: `JwtTokenService.java`
- RSA/JWKS service: `RsaKeyService.java`
- Cookie factory: `RefreshTokenCookieFactory.java`
- Email services: `AuthMailService.java`, `AuthEmailDeliveryService.java`, `AuthEmailTemplates.java`
- Login lockout: `LoginAttemptService.java`

Main behaviors:

- Signup creates a local auth user, assigns `USER`, sends signup verification OTP, and provisions demo data.
- Login accepts email or username, checks account status/lockout, validates password, requires verified email, creates a session, sends login email/notification, and returns an access token.
- Refresh reads the `refresh_token` cookie, validates the active session, issues a new 15 minute access token, and refreshes the cookie.
- Logout clears the refresh cookie.
- `/me` validates the active session, reloads roles, and returns user plus a refreshed access token.
- Password/account actions revoke sessions where appropriate.
- Bootstrap super admin is assigned when the user email matches `app.bootstrap-super-admin-email`.
- JWKS is cached under `authJwks`.

Auth endpoints:

| Method | Path | Notes |
| --- | --- | --- |
| `POST` | `/api/v1/auth/signup` | Public. Creates unverified local account |
| `POST` | `/api/v1/auth/login` | Public. Returns access token and sets refresh cookie |
| `POST` | `/api/v1/auth/refresh` | Public. Requires `refresh_token` cookie |
| `POST` | `/api/v1/auth/logout` | Public. Clears refresh cookie |
| `GET` | `/api/v1/auth/me` | Protected. Requires active session |
| `PUT` | `/api/v1/auth/me/password` | Protected. Current password required |
| `POST` | `/api/v1/auth/me/suspend` | Protected. Requires confirmation `SUSPEND` |
| `DELETE` | `/api/v1/auth/me` | Protected. Requires confirmation `DELETE` |
| `POST` | `/api/v1/auth/verify-email` | Public OTP verification |
| `POST` | `/api/v1/auth/resend-verification` | Public |
| `POST` | `/api/v1/auth/forgot-password` | Public |
| `POST` | `/api/v1/auth/reset-password` | Public OTP reset |
| `GET` | `/api/v1/auth/sessions` | Protected |
| `DELETE` | `/api/v1/auth/sessions/{sessionId}` | Protected |
| `DELETE` | `/api/v1/auth/sessions/all` | Protected |
| `GET` | `/api/v1/auth/.well-known/jwks.json` | Public JWKS |
| `GET` | `/api/v1/auth/db-ping` | ADMIN or SUPER_ADMIN |
| `GET` | `/api/v1/auth/db-stats` | ADMIN or SUPER_ADMIN |
| `GET` | `/api/v1/auth/admin/ping` | ADMIN or SUPER_ADMIN |
| `POST` | `/api/v1/auth/admin/users/{userId}/unlock` | SUPER_ADMIN |
| `PUT` | `/api/v1/auth/admin/users/{userId}/password` | SUPER_ADMIN |

Common auth failures:

| Error | Meaning | Fix |
| --- | --- | --- |
| `Email already registered` | Signup duplicate | Use another email or reset DB |
| `Email is not verified` | Login blocked until OTP verification | Check MailHog/Brevo and call verify-email |
| `Invalid email, username, or password` | Bad credentials | Check identifier/password, failed attempts |
| `Account locked` | Too many failed attempts or suspended/deleted status | Wait lock duration or use SUPER_ADMIN unlock |
| `Refresh token is missing` | Cookie not sent | Check `withCredentials`, CORS credentials, cookie SameSite/Secure |
| Gateway `Invalid or expired token` | Access token expired and refresh failed | Inspect refresh cookie and auth-service session row |

## User Service

Primary code:

- `backend/user-service/src/main/java/com/company/platform/user/controller/UserController.java`
- `AuthUserAdminService.java`
- `UserAccountMapper.java`
- Repositories under `backend/user-service/src/main/java/com/company/platform/user/repository/`
- Models under `backend/user-service/src/main/java/com/company/platform/user/model/`

Main behaviors:

- Reads identity from gateway headers, not from the request body.
- Owns profile, identity document, contact details, and preferences.
- Reads auth account/roles through `AuthUserAdminService`.
- Updates username by coordinating with auth-side account data.
- Normalizes Aadhaar to digits and PAN to uppercase.
- Preferences store timezone and default to valid client timezone or `UTC`.
- `/internal/demo-data` is used by auth demo provisioning.

User endpoints:

| Method | Path | Role | Notes |
| --- | --- | --- | --- |
| `GET` | `/api/v1/users/me` | USER | Current profile/account summary |
| `PUT` | `/api/v1/users/me` | USER | Update display name |
| `GET` | `/api/v1/users/me/settings` | USER | Profile, identity, contact, auth account |
| `PUT` | `/api/v1/users/me/settings` | USER | Update combined account settings |
| `GET` | `/api/v1/users/{id}` | ADMIN | User detail |
| `GET` | `/api/v1/users` | ADMIN | Search/list users |
| `DELETE` | `/api/v1/users/{id}` | SUPER_ADMIN | Currently empty implementation |
| `PUT` | `/api/v1/users/{id}/role` | ADMIN | Update roles |
| `GET` | `/api/v1/users/me/preferences` | USER | Timezone |
| `PUT` | `/api/v1/users/me/preferences` | USER | Update timezone |
| `PUT` | `/api/v1/users/me/avatar` | USER | Update avatar URL |

Troubleshooting user-service:

- If `X-User-Id` is missing, the request did not pass gateway JWT validation.
- If admin pages show `403`, check JWT roles and backend `@PreAuthorize`.
- If profile changes do not appear immediately, inspect cache eviction in `AuthUserAdminService.evictProfileCaches()`.
- If timezone looks wrong, inspect `X-Client-Time-Zone` from the frontend interceptor.

## Notification Service

Primary code:

- `NotificationController.java`
- `NotificationService.java`
- `NotificationRepository.java`
- `Notification.java`

Main behaviors:

- Lists notifications by current user.
- Caches notifications per `userId`.
- Marks single/all notifications read.
- Deletes single/all notifications.
- Records login notifications through `/internal/login`.
- Seeds demo notifications through `/internal/demo-data`.
- Exposes `/stream` as `text/event-stream`; current implementation sends a connection comment but does not yet broadcast stored notifications.

Troubleshooting:

- Empty list after login usually means auth-service could not call notification-service or demo provisioning failed.
- Stale notification list points to Redis cache; write operations evict the user cache.
- SSE connection can be healthy without delivering events because the service currently only opens the stream.

## Payment Service

Primary code:

- `PaymentController.java`
- `PaymentService.java`
- `PaymentRepository.java`
- DTOs under `backend/payment-service/src/main/java/com/company/platform/payment/dto/`

Main behaviors:

- `POST /api/v1/payments` creates a payment.
- If `method=STRIPE` and `payment.stripe.secret-key` is configured, it calls Stripe Checkout.
- Otherwise, it creates a `DEMO` payment with a local checkout URL.
- `POST /{paymentId}/confirm` marks payment as `SUCCEEDED` for status `success`/`paid`, otherwise `CANCELLED`.
- `POST /webhook` verifies Stripe signature and updates completed checkout sessions.
- Payment lists are cached per user.

Troubleshooting:

| Symptom | Check |
| --- | --- |
| Demo checkout only | `STRIPE_SECRET_KEY`/`payment.stripe.secret-key` is blank |
| Webhook returns service unavailable | `STRIPE_WEBHOOK_SECRET` is blank |
| Webhook `Invalid Stripe signature` | Header format, timestamp, wrong webhook secret |
| Payment not visible after create | Cache eviction, user ID, payment DB |
| Admin user payments fail | Caller must have `SUPER_ADMIN` in `X-User-Roles` |

## File Service

Primary code:

- `FileController.java`
- `FileMetadata.java`
- `FileMetadataRepository.java`
- `FileMetadataCacheService.java`

Main behaviors:

- Stores uploaded file bytes as Base64 in the database.
- Records original filename, content type, size, owner, and visibility.
- Resolves content type by extension first, then supplied content type, then Java URLConnection guess.
- `/my-files` seeds a default `README.md` if one does not exist.
- `/download` streams bytes and supports `disposition=inline` for browser preview.
- `/download-url` returns a gateway URL based on `file.download-base-url`.

Troubleshooting:

- If upload says `Choose a file to upload`, the multipart field must be named `file`.
- If downloads point to the wrong host, check `FILE_DOWNLOAD_BASE_URL` and `BACKEND_PUBLIC_URL`.
- If previews download instead of opening, check `disposition=inline` and content type.
- If file content is corrupt, inspect whether stored content is Base64 or plain text; the controller supports both but upload stores Base64.

## AI Service

Primary code:

- `AiController.java`
- `AiChatService.java`
- Providers under `backend/ai-service/src/main/java/com/company/platform/ai/provider/`
- Models/repositories for `ChatSession` and `ChatMessage`

Main behaviors:

- Chat sessions are owned by user ID.
- `/chat` creates or loads a session, saves the user message, builds a system prompt, sends request to provider factory, saves assistant response, updates token totals.
- Provider factory uses configured primary/fallback providers from config.
- On provider failure, it saves and returns a friendly fallback response with `error=true`.
- Usage is estimated from saved token counts.
- Session and message lists are cached and evicted on writes.

Important config:

- `GEMINI_BASE_URL`
- `GEMINI_MODEL`
- `GEMINI_API_KEY`
- `GROQ_BASE_URL`
- `GROQ_MODEL`
- `GROQ_API_KEY`
- `AI_CONTEXT_MAX_MESSAGES`
- `AI_RATE_LIMIT_PER_MINUTE`

Troubleshooting:

| Symptom | Check |
| --- | --- |
| AI returns fallback error text | Provider key/base URL/model invalid or network blocked |
| Chat history missing | Session ID mismatch, archived session, Redis stale cache |
| Usage looks approximate | Tokens are estimated as `text.length / 4`, not provider-reported billing tokens |
| Stream endpoint returns only done | Current stream implementation is a placeholder |

## Audit Service

Primary code:

- `AuditController.java`
- `AuditRecordService.java`
- `AuditRecord.java`
- `AuditRecordRepository.java`

Main behaviors:

- `GET /api/v1/audit` returns top 100 latest audit records and requires ADMIN.
- `GET /api/v1/audit/export` returns all records and requires SUPER_ADMIN.
- Demo data creates user/account/login/role/observability sample events.
- Before/after state is persisted as JSON text and parsed back to maps.
- Query/export are cached and evicted when demo data is seeded.

Troubleshooting:

- If admin audit page gets `403`, check token roles first.
- If records are empty, seed demo data or perform actions that create audit rows. Current code mainly has demo seeding; it is not a full cross-service audit event consumer yet.
- If JSON states are empty, malformed JSON is safely converted to `{}`.

## Observability

Local observability components:

| Component | Port | Purpose |
| --- | ---: | --- |
| Actuator | service-specific | Health and Prometheus metrics |
| Prometheus | `9090` | Scrapes service metrics |
| Grafana | `3000` | Dashboards |
| Zipkin | `9411` | Distributed tracing |
| Loki | `3100` | Logs |
| Promtail | n/a | Log shipping |

Gateway observability endpoints:

| Method | Path | Role |
| --- | --- | --- |
| `GET` | `/api/v1/observability/logs` | ADMIN or SUPER_ADMIN |
| `GET` | `/api/v1/observability/loki/labels` | ADMIN or SUPER_ADMIN |
| `GET` | `/api/v1/observability/loki/services` | ADMIN or SUPER_ADMIN |
| `GET` | `/api/v1/observability/loki/query-range` | ADMIN or SUPER_ADMIN |

Service health checks:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-RestMethod http://localhost:8888/actuator/health
Invoke-RestMethod http://localhost:8761
```

For service logs:

```powershell
docker compose --env-file backend/.env --env-file frontend/.env logs -f api-gateway
docker compose --env-file backend/.env --env-file frontend/.env logs -f auth-service
```

## Data And Persistence

PostgreSQL databases are initialized by:

```text
backend/infrastructure/postgres/init.sql
```

Expected databases:

- `auth_db`
- `user_db`
- `notification_db`
- `payment_db`
- `file_db`
- `ai_db`
- `audit_db`

Hibernate DDL is controlled by:

```text
DATABASE_AUTO_DDL=update
FLYWAY_ENABLED=false
```

For destructive local reset only:

```powershell
docker compose --env-file backend/.env --env-file frontend/.env --profile local-infra down -v
docker compose --env-file backend/.env --env-file frontend/.env --profile local-infra up --build
```

## Backend Troubleshooting Decision Tree

1. Is the gateway reachable?

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

If not, inspect `api-gateway` logs and config.

2. Is Config Server reachable?

```powershell
Invoke-RestMethod http://localhost:8888/actuator/health
```

If not, services may start with local defaults only or fail missing properties.

3. Is the target service registered/running?

```powershell
docker compose --env-file backend/.env --env-file frontend/.env ps
docker compose --env-file backend/.env --env-file frontend/.env logs -f <service-name>
```

4. Is the request authenticated?

- Browser localStorage should contain `platform.accessToken`.
- Request should include `Authorization: Bearer ...`.
- Refresh cookie should be sent for `/api/v1/auth/refresh`.

5. Is it an authorization issue?

- Decode JWT roles or call `/api/v1/auth/me`.
- ADMIN screens require ADMIN or SUPER_ADMIN.
- SUPER_ADMIN actions require SUPER_ADMIN.

6. Is it data/cache?

- Check service database.
- Check Redis cache if reads are stale.
- Confirm write methods evict matching cache names.

7. Is it third-party config?

- Email: `MAIL_*`, `BREVO_*`.
- OAuth: provider client ID/secret and redirect URI.
- Stripe: `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`.
- AI: `GEMINI_API_KEY` or `GROQ_API_KEY`.

