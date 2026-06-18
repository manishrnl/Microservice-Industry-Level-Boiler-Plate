# End-To-End Workflow And Troubleshooting Runbook

This document connects frontend, gateway, backend services, databases, infrastructure, and observability into one working-flow guide.

Use it when an error appears in the application and you need to decide where to look first.

## System Flow At A Glance

```text
Browser
  |
  | React/Vite app, access token in localStorage, refresh token in cookie
  v
API Gateway :8080
  |
  | Validates JWT, adds X-User-* headers, routes /api/v1/*
  v
Feature service
  |
  | Owns business logic and database/cache/provider calls
  v
PostgreSQL / Redis / Kafka / MinIO-style config / Stripe / AI provider / Mail
```

Core principle: the browser should call only the gateway for backend APIs. Feature services trust identity headers from the gateway.

## Local Startup Workflow

1. Create environment files:

```powershell
Copy-Item backend/.env.example backend/.env
Copy-Item frontend/.env.example frontend/.env
```

2. Start Docker Desktop.

3. Start the full stack:

```powershell
docker compose --env-file backend/.env --env-file frontend/.env --profile local-infra up --build
```

4. Open:

| Tool | URL |
| --- | --- |
| Frontend | `http://localhost:5173` |
| Gateway health | `http://localhost:8080/actuator/health` |
| Config Server health | `http://localhost:8888/actuator/health` |
| Eureka | `http://localhost:8761` |
| MailHog | `http://localhost:8025` |
| MinIO console | `http://localhost:9001` |
| Prometheus | `http://localhost:9090` |
| Grafana | `http://localhost:3000` |
| Zipkin | `http://localhost:9411` |

5. Stop:

```powershell
docker compose --env-file backend/.env --env-file frontend/.env --profile local-infra down
```

## Normal Login Workflow

1. User submits login form.
2. Frontend calls `POST /api/v1/auth/login`.
3. Gateway treats login as public and forwards to auth-service.
4. Auth-service:
   - Finds user by email or username.
   - Unlocks account if lock duration expired.
   - Validates password.
   - Blocks deleted/locked/unverified accounts.
   - Creates user session.
   - Sends login notice and suspicious login warning if needed.
   - Calls notification/demo provisioning services.
   - Issues access JWT.
   - Sets `refresh_token` cookie.
5. Frontend stores access token in `localStorage.platform.accessToken`.
6. App redirects into `/app/dashboard`.
7. Later protected calls include `Authorization: Bearer <token>`.
8. Gateway validates JWT and forwards `X-User-*` headers.

If login fails:

| Error | Inspect |
| --- | --- |
| Invalid credentials | Auth DB user row, password, failed attempts |
| Email not verified | MailHog/Brevo OTP, `/verify-email` |
| Account locked | failed attempts and `lockedUntil`, SUPER_ADMIN unlock |
| Browser does not stay logged in | refresh cookie, CORS credentials, `SECURITY_COOKIES_*` |

## Session Refresh Workflow

1. Protected API returns `401`.
2. Axios interceptor checks that the request is not public and was not retried.
3. First failed request calls `POST /api/v1/auth/refresh`.
4. Other failed requests wait in a queue.
5. Auth-service validates `refresh_token` cookie against active session.
6. Auth-service returns a new access token and cookie.
7. Frontend updates auth store and replays queued requests.
8. If refresh fails, frontend clears auth and redirects to `/login`.

Refresh failure checklist:

- Does `/api/v1/auth/refresh` include `Cookie: refresh_token=...`?
- Does gateway permit `/api/v1/auth/refresh` as public?
- Does auth-service session still exist and remain active?
- Are cookie flags correct for local or HTTPS production?
- Is the frontend hitting the same host that set the cookie?

## Protected Feature Request Workflow

Example: user opens Files page.

1. React route `/app/files` is guarded by `ProtectedRoute`.
2. Page calls endpoint from `frontend/src/api/endpoints.js`.
3. Axios adds bearer token, timezone, local time, user name/email.
4. Gateway validates JWT.
5. Gateway adds identity headers:
   - `X-User-Id`
   - `X-User-Email`
   - `X-User-Name`
   - `X-User-Roles`
   - `X-Session-Id`
6. Gateway routes `/api/v1/files/**` to file-service.
7. File-service uses `X-User-Id` to filter rows.
8. Response returns through gateway to frontend.

If a protected feature fails:

| Failure | Fast isolate |
| --- | --- |
| `401` | Token/JWKS/refresh problem |
| `403` | Role mismatch or backend `@PreAuthorize` |
| `404` | Wrong endpoint path or resource belongs to another user |
| `500` | Feature service logs and DB/provider config |
| CORS | Gateway CORS and frontend origin |
| Network error | Gateway down, service down, wrong API URL |

## Signup And Demo Data Workflow

1. Frontend calls `POST /api/v1/auth/signup`.
2. Auth-service creates user and role.
3. Auth-service sends signup verification OTP.
4. Auth-service calls demo provisioning.
5. Demo provisioning calls internal endpoints across:
   - user-service
   - notification-service
   - payment-service
   - file-service
   - ai-service
   - audit-service
6. User verifies email with OTP.
7. User can log in.

If signup works but pages are empty:

- Check auth-service logs for demo provisioning errors.
- Check target service logs for `/internal/demo-data`.
- Confirm all feature services are running.
- Confirm user ID matches across seeded records.

## Role And Admin Workflow

Roles are carried in the JWT `roles` claim and forwarded by gateway as `X-User-Roles`.

Frontend route protection:

- `ProtectedRoute requiredAnyRole={["ADMIN", "SUPER_ADMIN"]}`
- `ProtectedRoute requiredRole="SUPER_ADMIN"`

Backend method protection:

- Auth admin DB probes: ADMIN or SUPER_ADMIN.
- Auth unlock/password: SUPER_ADMIN.
- User search/detail/role update: ADMIN.
- User delete: SUPER_ADMIN.
- Audit query: ADMIN.
- Audit export: SUPER_ADMIN.
- Gateway observability endpoints: ADMIN or SUPER_ADMIN.

If admin UI shows access denied:

1. Call `GET /api/v1/auth/me`.
2. Check returned user roles.
3. Decode the access token and inspect `roles`.
4. Log out/in after role changes so the token is refreshed.
5. Confirm backend method role requirement.

## Feature Workflow Map

| UI area | Frontend files | Backend owner | Data/provider |
| --- | --- | --- | --- |
| Login/signup/OTP/reset | `src/pages/auth/*` | auth-service | auth DB, mail provider |
| Profile/settings/avatar | `ProfilePage.jsx`, account hooks | user-service plus auth-service | user DB, auth DB |
| Sessions | `SessionsPage.jsx` | auth-service | auth DB sessions |
| Notifications | `NotificationsPage.jsx`, notification components | notification-service | notification DB, Redis cache |
| Files | `FilesPage.jsx` | file-service | file DB; bytes stored as Base64 |
| Payments | `PaymentsPage.jsx`, `PremiumPage.jsx` | payment-service | payment DB, Stripe optional |
| AI chat | `pages/ai/AiChatPage.jsx` | ai-service | ai DB, Gemini/Groq optional |
| Audit | `AuditLogPage.jsx` | audit-service | audit DB |
| Observability/logs | `ObservabilityPage.jsx`, `LokiLogsPage.jsx` | api-gateway | in-memory gateway logs, Loki |

## Error Troubleshooting Matrix

### Browser shows CORS error

Likely layer: gateway config.

Check:

- Browser origin: `http://localhost:5173` or `http://localhost:5174`.
- `frontend/.env`: `VITE_FRONTEND_PUBLIC_URL`.
- `backend/.env`: `FRONTEND_PUBLIC_URL`.
- Gateway `globalcors` in `backend/api-gateway/src/main/resources/application.yml`.
- Whether request includes credentials and backend allows credentials.

### Browser shows network error

Likely layer: frontend API URL or gateway down.

Check:

```js
localStorage.getItem("platform.apiGatewayUrl")
```

Then:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
docker compose --env-file backend/.env --env-file frontend/.env ps
```

### API returns 401

Likely layer: auth token/JWKS/session.

Check:

- Request has `Authorization: Bearer`.
- Token not expired.
- Gateway can reach `AUTH_JWKS_URL`.
- `POST /api/v1/auth/refresh` succeeds.
- Refresh cookie is present.
- Auth session is still active.

### API returns 403

Likely layer: role or account status.

Check:

- User roles in `/api/v1/auth/me`.
- JWT `roles` claim.
- Frontend route requirement.
- Backend `@PreAuthorize`.
- Account status locked/suspended/deleted.

### API returns 404

Likely layer: route mismatch or resource ownership.

Check:

- URL from `frontend/src/api/endpoints.js`.
- Gateway route prefix.
- Controller mapping.
- Resource ID and current `X-User-Id`.

### API returns 500

Likely layer: feature service logic, database, provider config.

Check:

```powershell
docker compose --env-file backend/.env --env-file frontend/.env logs -f <service-name>
```

Then inspect:

- Required environment variables.
- Database connection.
- External provider credentials.
- Redis/Kafka availability.

### Frontend redirects to login after refresh

Likely layer: refresh cookie/session.

Check:

- `refresh_token` cookie exists after login.
- Cookie domain/path/samesite/secure.
- Browser request to `/refresh` sends the cookie.
- Auth-service session row is active.
- `SECURITY_COOKIES_SECURE=false` for local HTTP.

### Admin logs page fails

Likely layer: role or observability backend.

Check:

- User has ADMIN or SUPER_ADMIN.
- Gateway receives bearer token.
- `/api/v1/observability/logs` works.
- Loki is running for Loki pages.
- `LOKI_URL` points to the Loki service from gateway container.

## Service Ownership Checklist

When you see a failing URL, map it by prefix:

| URL prefix | Inspect first |
| --- | --- |
| `/api/v1/auth` | `auth-service` |
| `/api/v1/users` | `user-service` |
| `/api/v1/notifications` | `notification-service` |
| `/api/v1/payments` | `payment-service` |
| `/api/v1/files` | `file-service` |
| `/api/v1/ai` | `ai-service` |
| `/api/v1/audit` | `audit-service` |
| `/api/v1/observability` | `api-gateway` |
| `/actuator` | the service receiving the request |

## Commands For Fast Diagnosis

Check running containers:

```powershell
docker compose --env-file backend/.env --env-file frontend/.env ps
```

Follow gateway logs:

```powershell
docker compose --env-file backend/.env --env-file frontend/.env logs -f api-gateway
```

Follow one service:

```powershell
docker compose --env-file backend/.env --env-file frontend/.env logs -f auth-service
```

Health:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-RestMethod http://localhost:8888/actuator/health
```

Build backend:

```powershell
mvn clean package
```

Build one module:

```powershell
mvn clean package -DskipTests -pl backend/auth-service -am
```

Build frontend:

```powershell
cd frontend
npm run build
```

## Add A New Backend Feature Safely

1. Pick the owning service by domain.
2. Add DTOs locally or in `backend/commons` only if shared across services.
3. Add controller mapping under `/api/v1/<domain>`.
4. Add service logic and repository/entity if persistent.
5. Add validation and explicit error responses.
6. Add/confirm security:
   - Public path in gateway only if truly public.
   - Otherwise require gateway JWT headers.
   - Add `@PreAuthorize` for role-specific methods.
7. Add config to `backend/config-server-repo/<service>.yml` if needed.
8. Add tests under the service test tree.
9. Add endpoint entry in `frontend/src/api/endpoints.js`.
10. Add frontend page/hook behavior.
11. Verify through gateway, not by calling service port directly.

## Add A New Frontend Feature Safely

1. Add endpoint in `frontend/src/api/endpoints.js`.
2. Use `apiClient` unless you specifically need raw `fetch`.
3. Add route in `App.jsx`.
4. Guard the route with `ProtectedRoute` if needed.
5. Add nav item in layout if user-facing.
6. Handle loading, empty, error, and success states.
7. Confirm request headers in DevTools.
8. Confirm gateway route and backend controller mapping.
9. Build with `npm run build`.

## Known Implementation Notes

- Gateway `GatewaySecurityConfig` permits exchanges, while actual JWT enforcement is implemented in `JwtAuthenticationFilter`.
- Notification SSE is currently a connection endpoint, not a full push broadcaster.
- AI streaming endpoint currently returns a simple done event; normal AI response is non-streaming `POST /api/v1/ai/chat`.
- File service stores bytes in the database as Base64 rather than using MinIO directly in the current controller implementation.
- Audit service mostly exposes stored/seeded audit records; it is not yet wired as a universal async audit consumer.
- User-service delete endpoint exists but has no implementation body.
- Payment service works in demo mode without Stripe keys and switches to Stripe checkout when configured.

