# Frontend Deep Dive And Troubleshooting Guide

This document explains the React/Vite frontend structure, runtime configuration, authentication state, API client behavior, routes, feature pages, and debugging approach.

## Frontend Summary

The frontend lives in `frontend/` and is a React 18 + Vite 6 application.

Core libraries:

| Area | Library |
| --- | --- |
| Routing | `react-router-dom` |
| Server state | `@tanstack/react-query` |
| Local state | `zustand` |
| API | `axios` and browser `fetch` |
| Forms | `react-hook-form`, `zod` |
| Notifications | `react-hot-toast` |
| Icons | `lucide-react` |
| Styling | Tailwind CSS |
| Markdown | `react-markdown` |

Important files:

| File | Purpose |
| --- | --- |
| `frontend/package.json` | Scripts and dependencies |
| `frontend/vite.config.ts` | Vite build/dev configuration |
| `frontend/src/main.jsx` | React root setup |
| `frontend/src/App.jsx` | Route tree, page titles, auth hydration |
| `frontend/src/config/env.js` | Runtime URL/environment resolution |
| `frontend/src/api/endpoints.js` | Central backend endpoint map |
| `frontend/src/api/axiosInstance.js` | Axios client base URL and credentials |
| `frontend/src/api/axiosInterceptor.js` | Auth headers, timezone headers, activity overlay, refresh-token retry |
| `frontend/src/store/authStore.js` | Access token, current user, session hydration, logout |
| `frontend/src/store/preferencesStore.js` | User/browser preferences such as timezone |
| `frontend/src/store/apiActivityStore.js` | API activity overlay state |
| `frontend/src/components/common/ProtectedRoute.jsx` | Auth and role based route guard |
| `frontend/src/components/layout/AppLayout.jsx` | Protected app shell |
| `frontend/src/components/layout/PublicLayout.jsx` | Public site shell |

## Scripts

From `frontend/`:

```powershell
npm install
npm run dev
npm run build
npm run preview
npm run load-test:api
npm run tunnel:api
```

Script meanings:

| Script | Meaning |
| --- | --- |
| `dev` | Runs custom Vite LAN host script |
| `build` | Production Vite build |
| `preview` | Preview build through custom Vite LAN host script |
| `load-test:api` | Runs `scripts/load-test-api.mjs` |
| `tunnel:api` | Runs PowerShell tunnel script for API exposure |

## Runtime Environment

Frontend runtime config is in `frontend/src/config/env.js`.

Key behavior:

- `VITE_API_GATEWAY_URL` defaults to `http://127.0.0.1:8080`.
- If the configured gateway URL is local, `env.apiGatewayUrl` becomes an empty string. That lets Vite proxy or same-host behavior work in local browser scenarios.
- The browser can override the backend by opening the app with:

```text
?api=http://localhost:8080
?apiUrl=http://localhost:8080
?backend=http://localhost:8080
```

- The override is stored in `localStorage` under `platform.apiGatewayUrl`.
- Reset the override with:

```text
?api=reset
```

Other resolved URLs:

- `prometheusUrl`
- `grafanaUrl`
- `zipkinUrl`
- `lokiUrl`
- `discoveryUrl`
- `configServerUrl`
- `gatewayMetricsUrl`
- `gatewayHealthUrl`

Troubleshooting URL issues:

| Symptom | Check |
| --- | --- |
| Frontend calls wrong backend | `localStorage.platform.apiGatewayUrl`, query override, `VITE_API_GATEWAY_URL` |
| CORS failure | Browser origin, `VITE_FRONTEND_PUBLIC_URL`, backend `FRONTEND_PUBLIC_URL` |
| Observability links wrong | `VITE_PROMETHEUS_URL`, `VITE_GRAFANA_URL`, `VITE_ZIPKIN_URL`, `VITE_LOKI_URL` |
| OAuth redirects wrong | `VITE_FRONTEND_PUBLIC_URL`, backend `FRONTEND_PUBLIC_URL`, provider redirect URI |

## Route Tree

Routes are defined in `frontend/src/App.jsx`.

Public routes:

| Path | Page |
| --- | --- |
| `/` | `HomePage` |
| `/gallery` | `ImageGalleryPage` |
| `/login` | `LoginPage` |
| `/signup` | `SignupPage` |
| `/forgot-password` | `ForgotPasswordPage` |
| `/oauth/callback` | `OAuthCallbackPage` |
| `/403` | `AccessDeniedPage` |
| `*` | `NotFoundPage` |

Protected app routes under `/app`:

| Path | Page |
| --- | --- |
| `/app/dashboard` | `Dashboard` |
| `/app/profile` | `ProfilePage` |
| `/app/notifications` | `NotificationsPage` |
| `/app/sessions` | `SessionsPage` |
| `/app/files` | `FilesPage` |
| `/app/payments` | `PaymentsPage` |
| `/app/premium` | `PremiumPage` |
| `/app/ai` | `AiChatPage` |

Admin routes:

| Path | Required role | Page |
| --- | --- | --- |
| `/app/admin/users` | ADMIN or SUPER_ADMIN | `UserManagementPage` |
| `/app/admin/audit` | ADMIN or SUPER_ADMIN | `AuditLogPage` |
| `/app/admin/observability` | ADMIN or SUPER_ADMIN | `ObservabilityPage` |
| `/app/admin/logs` | ADMIN or SUPER_ADMIN | `LokiLogsPage` |
| `/app/admin/applicationTests` | ADMIN or SUPER_ADMIN | `ApplicationTestsPage` |
| `/app/super-admin` | SUPER_ADMIN | `SuperAdminDashboardPage` |
| `/app/super-admin/observability` | SUPER_ADMIN | `ObservabilityPage` |
| `/app/super-admin/logs` | SUPER_ADMIN | `LokiLogsPage` |

Legacy redirects:

- `/dashboard` -> `/app/dashboard`
- `/profile` -> `/app/profile`
- `/notifications` -> `/app/notifications`
- `/sessions` -> `/app/sessions`
- `/files` -> `/app/files`
- `/payments` -> `/app/payments`
- `/premium` -> `/app/premium`
- `/ai` -> `/app/ai`
- `/admin/*` -> `/app/admin/*`
- `/super-admin/*` -> `/app/super-admin/*`

## Authentication State

Primary file: `frontend/src/store/authStore.js`.

Storage keys:

| Key | Storage | Purpose |
| --- | --- | --- |
| `platform.accessToken` | localStorage | JWT access token |
| `platform.authNotice` | sessionStorage | Login-page notice after session expiry/logout events |

Auth lifecycle:

1. App mounts.
2. `App.jsx` calls `useAuthStore().hydrate()` and `usePreferencesStore().hydrate()`.
3. `hydrate()` reads `platform.accessToken`.
4. If the token is missing or expired, it calls `POST /api/v1/auth/refresh` using browser `fetch` with `credentials: include`.
5. It calls `GET /api/v1/auth/me` with the access token.
6. It stores the refreshed access token and user.
7. Protected routes become available.

Logout:

- Calls `POST /api/v1/auth/logout` with credentials.
- Clears `platform.accessToken`.
- Resets auth state.

Session expiry:

- Clears stored auth.
- Shows toast: `Session expired. Log in again.`
- Redirect behavior also exists in Axios interceptor after refresh failure.

Troubleshooting auth state:

| Symptom | Check |
| --- | --- |
| User immediately returns to login | Refresh cookie missing/expired, `platform.accessToken` missing, `/auth/me` failing |
| Login succeeds but protected API calls 401 | Axios interceptor not loaded/imported, access token not stored, gateway URL mismatch |
| Refresh loop or repeated 401 | Refresh cookie rejected by browser due SameSite/Secure/CORS |
| Admin route redirects to `/403` | JWT user lacks role in `roles` claim |

## API Client

Files:

- `frontend/src/api/axiosInstance.js`
- `frontend/src/api/axiosInterceptor.js`
- `frontend/src/api/endpoints.js`

Axios instance:

```js
axios.create({
  baseURL: env.apiGatewayUrl,
  withCredentials: true,
  headers: {
    "Content-Type": "application/json"
  }
})
```

Request interceptor behavior:

- Starts an API activity overlay message based on method/path.
- Adds `X-Client-Time-Zone` from preferences.
- Adds `X-Client-Local-Time`.
- Adds `Authorization: Bearer <token>` for non-public auth endpoints.
- Adds `X-User-Name` and `X-User-Email` from current user for non-public endpoints.

Response interceptor behavior:

- Stops the activity overlay.
- For protected requests that return `401`, it tries exactly one refresh path.
- While refresh is in progress, other failed requests are queued.
- On refresh success:
  - Saves new access token.
  - Replays queued requests.
  - Replays the original request.
- On refresh failure:
  - Clears auth state.
  - Redirects to `/login`.

Public auth paths skipped by the interceptor:

- `/api/v1/auth/signup`
- `/api/v1/auth/login`
- `/api/v1/auth/refresh`
- `/api/v1/auth/logout`
- `/api/v1/auth/verify-email`
- `/api/v1/auth/resend-verification`
- `/api/v1/auth/forgot-password`
- `/api/v1/auth/reset-password`
- `/api/v1/auth/oauth2`

## Endpoint Map

Primary file: `frontend/src/api/endpoints.js`.

Main endpoint groups:

| Group | Backend prefix |
| --- | --- |
| `auth` | `/api/v1/auth` |
| `users` | `/api/v1/users` |
| `notifications` | `/api/v1/notifications` |
| `ai` | `/api/v1/ai` |
| `files` | `/api/v1/files` |
| `payments` | `/api/v1/payments` |
| `audit` | `/api/v1/audit` |
| `observability` | `/api/v1/observability` |

When adding a new backend endpoint:

1. Add a stable URL builder to `endpoints.js`.
2. Use `apiClient` from `axiosInstance.js`.
3. Confirm whether the path should be in `publicAuthPaths`.
4. Add activity text in `axiosInterceptor.js` if the operation is user-facing.
5. Ensure backend gateway route exists for the prefix.

## Feature Pages

### Auth Pages

Files:

- `frontend/src/pages/auth/LoginPage.jsx`
- `SignupPage.jsx`
- `ForgotPasswordPage.jsx`
- `OAuthCallbackPage.jsx`
- `OAuthButtons.jsx`
- `PasswordField.jsx`
- `AuthShell.jsx`

Expected backend dependencies:

- Auth endpoints.
- Email delivery for OTP/reset flows.
- OAuth provider credentials for social login.

### Dashboard And Layout

Files:

- `frontend/src/pages/Dashboard.jsx`
- `frontend/src/components/layout/AppLayout.jsx`
- `Sidebar.jsx`
- `Topbar.jsx`
- `MobileNav.jsx`
- `ProfileMenu.jsx`

The layout renders protected navigation and profile controls. Page access is controlled by route guards, not only by hidden nav items.

### Profile And Settings

Files:

- `frontend/src/pages/ProfilePage.jsx`
- `frontend/src/utils/userDisplay.js`
- `frontend/src/hooks/useAccountIdentity.js`

Backend dependencies:

- `GET /api/v1/auth/me`
- `GET/PUT /api/v1/users/me`
- `GET/PUT /api/v1/users/me/settings`
- `PUT /api/v1/users/me/avatar`
- `PUT /api/v1/auth/me/password`
- suspend/delete auth endpoints

### Notifications

Files:

- `frontend/src/pages/NotificationsPage.jsx`
- `frontend/src/components/notifications/*`
- `frontend/src/hooks/useSSE.js`

Backend dependencies:

- `GET /api/v1/notifications`
- `GET /api/v1/notifications/stream`
- mark read/read-all/delete endpoints

Important note: the backend SSE endpoint currently only opens a stream and sends a connection comment. The frontend can connect, but real-time notification push is not fully implemented in the backend yet.

### Sessions

File:

- `frontend/src/pages/SessionsPage.jsx`

Backend dependencies:

- `GET /api/v1/auth/sessions`
- `DELETE /api/v1/auth/sessions/{sessionId}`
- `DELETE /api/v1/auth/sessions/all`

If the user revokes the current session or all sessions, the backend clears the refresh cookie.

### Files

File:

- `frontend/src/pages/FilesPage.jsx`

Backend dependencies:

- `POST /api/v1/files/upload` with multipart field `file`
- `GET /api/v1/files/my-files`
- `GET /api/v1/files/{id}/metadata`
- `GET /api/v1/files/{id}/download-url`
- `GET /api/v1/files/{id}/download`
- `DELETE /api/v1/files/{id}`

Common frontend-side issue: do not send JSON content type manually for file upload. Let the browser build multipart boundaries.

### Payments

Files:

- `frontend/src/pages/PaymentsPage.jsx`
- `frontend/src/pages/PremiumPage.jsx`

Backend dependencies:

- `POST /api/v1/payments`
- `GET /api/v1/payments`
- `POST /api/v1/payments/{paymentId}/confirm`
- `GET /api/v1/payments/admin/users/{userId}` for SUPER_ADMIN

Payment return URLs include query parameters such as `paymentId`, `status`, and `session_id`.

### AI Chat

File:

- `frontend/src/pages/ai/AiChatPage.jsx`

Backend dependencies:

- `POST /api/v1/ai/chat`
- `GET /api/v1/ai/sessions`
- `GET /api/v1/ai/sessions/{sessionId}/messages`
- `POST /api/v1/ai/sessions`
- `PATCH /api/v1/ai/sessions/{sessionId}`
- `POST /api/v1/ai/sessions/{sessionId}/messages`
- `DELETE /api/v1/ai/sessions/{sessionId}`
- `GET /api/v1/ai/usage`

The backend stream endpoint is currently placeholder-level; normal chat response uses `POST /chat`.

### Admin Pages

Files:

- `UserManagementPage.jsx`
- `AuditLogPage.jsx`
- `ObservabilityPage.jsx`
- `LokiLogsPage.jsx`
- `ApplicationTestsPage.jsx`
- `SuperAdminDashboardPage.jsx`

Backend dependencies:

- User admin endpoints require ADMIN for listing/role updates and SUPER_ADMIN for destructive/unlock/password actions.
- Audit query requires ADMIN.
- Audit export requires SUPER_ADMIN.
- Observability gateway endpoints require ADMIN or SUPER_ADMIN and a valid bearer token.

## Frontend Troubleshooting Decision Tree

1. Is the app using the right backend?

Open DevTools console:

```js
localStorage.getItem("platform.apiGatewayUrl")
localStorage.getItem("platform.accessToken")
```

Reset backend override if needed:

```text
http://localhost:5173/?api=reset
```

2. Is the user authenticated?

- Check `platform.accessToken`.
- Check Network tab for `/api/v1/auth/refresh`.
- Check Network tab for `/api/v1/auth/me`.

3. Are cookies sent?

For refresh/logout calls, Network tab must show:

- `credentials: include` behavior.
- `Cookie: refresh_token=...` on same-site/local-compatible requests.
- Backend response `Set-Cookie` on login/refresh/logout.

4. Is the authorization header sent?

Protected Axios calls should include:

```text
Authorization: Bearer <token>
```

If missing, inspect:

- `useAuthStore.getState().accessToken`
- Whether the URL is incorrectly classified as public.
- Whether `axiosInterceptor.js` is imported during app startup.

5. Is this a role issue?

- Inspect decoded token roles.
- Confirm `ProtectedRoute` requirement.
- Confirm backend `@PreAuthorize` requirement.

6. Is this a CORS issue?

The browser origin must match gateway CORS config. Typical local values:

```text
VITE_FRONTEND_PUBLIC_URL=http://localhost:5173
FRONTEND_PUBLIC_URL=http://localhost:5173
```

If Vite moved to `5174`, update config or free port `5173`.

7. Is UI data stale?

- React Query may cache page data.
- Zustand stores auth/preferences locally.
- Backend Redis may cache service data.
- Force reload, logout/login, or inspect backend cache eviction paths.

