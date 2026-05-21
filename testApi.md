# API Testing Guide

Use this file as a quick Postman/curl checklist for testing the backend APIs through the API Gateway.

## Base URLs

Recommended API base URL:

```text
http://localhost:8080
```

Direct service URLs are available during local development, but API testing should normally go through the gateway so JWT validation and `X-User-*` header forwarding behave the same way as the frontend.

| Service | Local URL |
| --- | --- |
| API Gateway | `http://localhost:8080` |
| Auth Service | `http://localhost:8081` |
| User Service | `http://localhost:8082` |
| Notification Service | `http://localhost:8083` |
| Payment Service | `http://localhost:8084` |
| File Service | `http://localhost:8085` |
| AI Service | `http://localhost:8086` |
| Audit Service | `http://localhost:8087` |
| Config Server | `http://localhost:8888` |
| Eureka | `http://localhost:8761` |

## Postman Variables

Create these variables in a Postman environment:

| Variable | Example |
| --- | --- |
| `baseUrl` | `http://localhost:8080` |
| `accessToken` | Paste from login response |
| `userId` | Paste a UUID from responses if needed |
| `sessionId` | Paste an auth or AI session id |
| `paymentId` | Paste from payment creation response |
| `fileId` | Paste from file metadata response |
| `notificationId` | Paste from notification list response |

For protected endpoints, add this header:

```text
Authorization: Bearer {{accessToken}}
```

For JSON requests, add:

```text
Content-Type: application/json
```

## Suggested Test Order

1. Start the stack with `docker compose --profile local-infra up --build`.
2. Check gateway health.
3. Sign up a user.
4. Log in and copy `accessToken` from the response into `{{accessToken}}`.
5. Test `/api/v1/auth/me`.
6. Test user, notification, file, payment, and AI endpoints with the bearer token.
7. Test admin endpoints only with a token that has `ADMIN` or `SUPER_ADMIN`.

## Health And Platform Endpoints

### Gateway Health

```http
GET {{baseUrl}}/actuator/health
```

curl:

```bash
curl http://localhost:8080/actuator/health
```

### Config Server Health

```http
GET http://localhost:8888/actuator/health
```

### Eureka Dashboard

Open in browser:

```text
http://localhost:8761
```

### Service Health URLs

| Service | Endpoint |
| --- | --- |
| Auth | `GET http://localhost:8081/actuator/health` |
| User | `GET http://localhost:8082/actuator/health` |
| Notification | `GET http://localhost:8083/actuator/health` |
| Payment | `GET http://localhost:8084/actuator/health` |
| File | `GET http://localhost:8085/actuator/health` |
| AI | `GET http://localhost:8086/actuator/health` |
| Audit | `GET http://localhost:8087/actuator/health` |

## Authentication APIs

### Sign Up

Public endpoint.

```http
POST {{baseUrl}}/api/v1/auth/signup
```

Body:

```json
{
  "email": "test@example.com",
  "password": "Password@123",
  "confirmPassword": "Password@123",
  "fullName": "Test User",
  "avatarUrl": "https://example.com/avatar.png"
}
```

curl:

```bash
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Password@123","confirmPassword":"Password@123","fullName":"Test User","avatarUrl":"https://example.com/avatar.png"}'
```

### Login

Public endpoint. Copy `accessToken` from the response.

```http
POST {{baseUrl}}/api/v1/auth/login
```

Body:

```json
{
  "email": "test@example.com",
  "password": "Password@123",
  "deviceId": "postman-device"
}
```

curl:

```bash
curl -i -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Password@123","deviceId":"postman-device"}'
```

### Refresh Token

Public endpoint. Requires the `refresh_token` cookie set by login.

```http
POST {{baseUrl}}/api/v1/auth/refresh
```

curl with cookie jar:

```bash
curl -c cookies.txt -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Password@123","deviceId":"curl-device"}'

curl -b cookies.txt -i -X POST http://localhost:8080/api/v1/auth/refresh
```

### Logout

Public endpoint. Clears the refresh cookie.

```http
POST {{baseUrl}}/api/v1/auth/logout
```

### Get Current Auth User

Protected endpoint.

```http
GET {{baseUrl}}/api/v1/auth/me
Authorization: Bearer {{accessToken}}
```

curl:

```bash
curl http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### Update Auth Profile

Protected endpoint.

```http
PUT {{baseUrl}}/api/v1/auth/me
Authorization: Bearer {{accessToken}}
```

Body:

```json
{
  "name": "Updated Test User"
}
```

### Update Auth Avatar

Protected endpoint.

```http
PUT {{baseUrl}}/api/v1/auth/me/avatar
Authorization: Bearer {{accessToken}}
```

Body:

```json
{
  "avatarUrl": "https://example.com/new-avatar.png"
}
```

### Verify Email

Public endpoint. Use the OTP delivered by email/local MailHog if email delivery is configured.

```http
POST {{baseUrl}}/api/v1/auth/verify-email
```

Body:

```json
{
  "email": "test@example.com",
  "otp": "123456"
}
```

### Resend Verification

Public endpoint.

```http
POST {{baseUrl}}/api/v1/auth/resend-verification
```

Body:

```json
{
  "email": "test@example.com"
}
```

### Forgot Password

Public endpoint.

```http
POST {{baseUrl}}/api/v1/auth/forgot-password
```

Body:

```json
{
  "email": "test@example.com"
}
```

### Reset Password

Public endpoint. Use the OTP delivered by email/local MailHog if email delivery is configured.

```http
POST {{baseUrl}}/api/v1/auth/reset-password
```

Body:

```json
{
  "email": "test@example.com",
  "otp": "123456",
  "password": "NewPassword@123",
  "confirmPassword": "NewPassword@123"
}
```

### List Auth Sessions

Protected endpoint.

```http
GET {{baseUrl}}/api/v1/auth/sessions
Authorization: Bearer {{accessToken}}
```

### Revoke One Session

Protected endpoint.

```http
DELETE {{baseUrl}}/api/v1/auth/sessions/{{sessionId}}
Authorization: Bearer {{accessToken}}
```

### Revoke All Sessions

Protected endpoint. This also clears the refresh cookie.

```http
DELETE {{baseUrl}}/api/v1/auth/sessions/all
Authorization: Bearer {{accessToken}}
```

### JWKS

Public endpoint.

```http
GET {{baseUrl}}/api/v1/auth/.well-known/jwks.json
```

### Admin Ping

Requires an `ADMIN` token.

```http
GET {{baseUrl}}/api/v1/auth/admin/ping
Authorization: Bearer {{accessToken}}
```

## OAuth APIs

These are browser redirect endpoints, not normal JSON APIs.

### Start OAuth Login

Public endpoint.

```http
GET {{baseUrl}}/api/v1/auth/oauth2/authorize/google
GET {{baseUrl}}/api/v1/auth/oauth2/authorize/github
GET {{baseUrl}}/api/v1/auth/oauth2/authorize/linkedin
```

Open one in the browser:

```text
http://localhost:8080/api/v1/auth/oauth2/authorize/google
```

### OAuth Callback

Public endpoint. OAuth providers call this with a real `code`.

```http
GET {{baseUrl}}/api/v1/auth/oauth2/callback/{provider}?code={providerCode}
```

Example:

```text
http://localhost:8080/api/v1/auth/oauth2/callback/google?code=REAL_PROVIDER_CODE
```

## User APIs

All user APIs should be tested with:

```text
Authorization: Bearer {{accessToken}}
```

The gateway reads the JWT and forwards `X-User-Id`, `X-User-Email`, `X-User-Name`, and `X-User-Roles` to the service.

### Get Current User

```http
GET {{baseUrl}}/api/v1/users/me
Authorization: Bearer {{accessToken}}
```

### Update Current User

```http
PUT {{baseUrl}}/api/v1/users/me
Authorization: Bearer {{accessToken}}
```

Body:

```json
{
  "name": "Updated Test User"
}
```

### Get User By ID

Requires an `ADMIN` token.

```http
GET {{baseUrl}}/api/v1/users/{{userId}}
Authorization: Bearer {{accessToken}}
```

### List Users

Requires an `ADMIN` token.

```http
GET {{baseUrl}}/api/v1/users
Authorization: Bearer {{accessToken}}
```

### Delete User

Requires a `SUPER_ADMIN` token.

```http
DELETE {{baseUrl}}/api/v1/users/{{userId}}
Authorization: Bearer {{accessToken}}
```

### Update User Role

Requires an `ADMIN` token.

```http
PUT {{baseUrl}}/api/v1/users/{{userId}}/role
Authorization: Bearer {{accessToken}}
```

Body:

```json
{
  "role": "ADMIN"
}
```

### Get Current User Preferences

```http
GET {{baseUrl}}/api/v1/users/me/preferences
Authorization: Bearer {{accessToken}}
```

### Update Current User Preferences

```http
PUT {{baseUrl}}/api/v1/users/me/preferences
Authorization: Bearer {{accessToken}}
```

Body:

```json
{
  "timezone": "Asia/Kolkata",
  "theme": "dark",
  "language": "en"
}
```

### Upload Current User Avatar

The current controller returns a simple upload status. No request body is required by the backend method.

```http
POST {{baseUrl}}/api/v1/users/me/avatar
Authorization: Bearer {{accessToken}}
```

## Notification APIs

All notification APIs should be tested with:

```text
Authorization: Bearer {{accessToken}}
```

### List Notifications

```http
GET {{baseUrl}}/api/v1/notifications
Authorization: Bearer {{accessToken}}
```

### Notification Stream

Server-Sent Events endpoint.

```http
GET {{baseUrl}}/api/v1/notifications/stream
Authorization: Bearer {{accessToken}}
Accept: text/event-stream
```

curl:

```bash
curl -N http://localhost:8080/api/v1/notifications/stream \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Accept: text/event-stream"
```

### Mark Notification Read

```http
PATCH {{baseUrl}}/api/v1/notifications/{{notificationId}}/read
Authorization: Bearer {{accessToken}}
```

### Mark All Notifications Read

```http
PATCH {{baseUrl}}/api/v1/notifications/read-all
Authorization: Bearer {{accessToken}}
```

### Delete Notification

```http
DELETE {{baseUrl}}/api/v1/notifications/{{notificationId}}
Authorization: Bearer {{accessToken}}
```

## Payment APIs

Payment APIs require a bearer token except the Stripe webhook endpoint.

### Create Payment

```http
POST {{baseUrl}}/api/v1/payments
Authorization: Bearer {{accessToken}}
```

Body for demo checkout:

```json
{
  "amount": 49.99,
  "currency": "usd",
  "method": "DEMO",
  "description": "Test platform payment"
}
```

Body for Stripe checkout:

```json
{
  "amount": 49.99,
  "currency": "usd",
  "method": "STRIPE",
  "description": "Test Stripe payment"
}
```

Note: Stripe checkout requires `STRIPE_SECRET_KEY` or `STRIPE_API_KEY` to be configured depending on your environment mapping.

### List Payments

```http
GET {{baseUrl}}/api/v1/payments
Authorization: Bearer {{accessToken}}
```

### Confirm Payment

```http
POST {{baseUrl}}/api/v1/payments/{{paymentId}}/confirm
Authorization: Bearer {{accessToken}}
```

Body:

```json
{
  "sessionId": "demo-session",
  "status": "success"
}
```

Use `success` or `paid` for a succeeded payment. Other statuses become cancelled.

### Stripe Webhook

Public endpoint. Requires a valid `Stripe-Signature` header if `STRIPE_WEBHOOK_SECRET` is configured.

```http
POST {{baseUrl}}/api/v1/payments/webhook
Stripe-Signature: t=TIMESTAMP,v1=SIGNATURE
```

Example payload:

```json
{
  "id": "evt_test",
  "type": "checkout.session.completed",
  "data": {
    "object": {
      "id": "cs_test",
      "metadata": {
        "payment_id": "00000000-0000-0000-0000-000000000000"
      }
    }
  }
}
```

## File APIs

All file APIs should be tested with:

```text
Authorization: Bearer {{accessToken}}
```

### Upload File

The current controller method does not read multipart content yet; it returns generated metadata.

```http
POST {{baseUrl}}/api/v1/files/upload
Authorization: Bearer {{accessToken}}
```

### Get Download URL

```http
GET {{baseUrl}}/api/v1/files/{{fileId}}/download-url
Authorization: Bearer {{accessToken}}
```

### Get File Metadata

```http
GET {{baseUrl}}/api/v1/files/{{fileId}}/metadata
Authorization: Bearer {{accessToken}}
```

### List My Files

```http
GET {{baseUrl}}/api/v1/files/my-files
Authorization: Bearer {{accessToken}}
```

### Delete File

```http
DELETE {{baseUrl}}/api/v1/files/{{fileId}}
Authorization: Bearer {{accessToken}}
```

## AI APIs

All AI APIs should be tested with:

```text
Authorization: Bearer {{accessToken}}
```

AI chat requires at least one provider key, such as `GEMINI_API_KEY` or `GROQ_API_KEY`, depending on your configured provider.

### Send Chat Message

```http
POST {{baseUrl}}/api/v1/ai/chat
Authorization: Bearer {{accessToken}}
```

Body:

```json
{
  "sessionId": null,
  "message": "Explain this microservice project in simple words.",
  "stream": false,
  "systemPrompt": "You are a helpful platform assistant.",
  "location": {
    "latitude": 28.6139,
    "longitude": 77.2090,
    "accuracy": 50
  },
  "context": {
    "locale": "en-IN",
    "timezone": "Asia/Kolkata",
    "localTime": "2026-05-21T10:30:00+05:30"
  }
}
```

Minimal body:

```json
{
  "message": "Hello"
}
```

### Stream Chat

Server-Sent Events style streaming endpoint.

```http
GET {{baseUrl}}/api/v1/ai/chat/stream/{{sessionId}}
Authorization: Bearer {{accessToken}}
Accept: text/event-stream
```

curl:

```bash
curl -N http://localhost:8080/api/v1/ai/chat/stream/YOUR_SESSION_ID \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Accept: text/event-stream"
```

### List AI Sessions

```http
GET {{baseUrl}}/api/v1/ai/sessions
Authorization: Bearer {{accessToken}}
```

### Get AI Session Messages

`sessionId` must be a UUID for this endpoint.

```http
GET {{baseUrl}}/api/v1/ai/sessions/{{sessionId}}/messages
Authorization: Bearer {{accessToken}}
```

### Create AI Session

```http
POST {{baseUrl}}/api/v1/ai/sessions
Authorization: Bearer {{accessToken}}
```

Body:

```json
{
  "title": "API test chat",
  "model": "gemini-or-groq"
}
```

### Delete AI Session

`sessionId` must be a UUID for this endpoint.

```http
DELETE {{baseUrl}}/api/v1/ai/sessions/{{sessionId}}
Authorization: Bearer {{accessToken}}
```

### Get AI Usage

```http
GET {{baseUrl}}/api/v1/ai/usage
Authorization: Bearer {{accessToken}}
```

### Update System Prompt

Admin-oriented endpoint. Use an admin token.

```http
POST {{baseUrl}}/api/v1/ai/admin/system-prompt
Authorization: Bearer {{accessToken}}
```

Body:

```json
{
  "systemPrompt": "Always answer as a concise platform assistant."
}
```

## Audit APIs

Audit APIs require elevated roles.

### List Audit Events

Requires an `ADMIN` token.

```http
GET {{baseUrl}}/api/v1/audit
Authorization: Bearer {{accessToken}}
```

### Export Audit Events

Requires a `SUPER_ADMIN` token.

```http
GET {{baseUrl}}/api/v1/audit/export
Authorization: Bearer {{accessToken}}
```

## All Application Endpoints

| Method | Endpoint | Auth |
| --- | --- | --- |
| `POST` | `/api/v1/auth/signup` | Public |
| `POST` | `/api/v1/auth/login` | Public |
| `POST` | `/api/v1/auth/refresh` | Public, refresh cookie |
| `POST` | `/api/v1/auth/logout` | Public, refresh cookie |
| `GET` | `/api/v1/auth/me` | Bearer token |
| `PUT` | `/api/v1/auth/me` | Bearer token |
| `PUT` | `/api/v1/auth/me/avatar` | Bearer token |
| `POST` | `/api/v1/auth/verify-email` | Public |
| `POST` | `/api/v1/auth/resend-verification` | Public |
| `POST` | `/api/v1/auth/forgot-password` | Public |
| `POST` | `/api/v1/auth/reset-password` | Public |
| `GET` | `/api/v1/auth/sessions` | Bearer token |
| `DELETE` | `/api/v1/auth/sessions/{sessionId}` | Bearer token |
| `DELETE` | `/api/v1/auth/sessions/all` | Bearer token |
| `GET` | `/api/v1/auth/.well-known/jwks.json` | Public |
| `GET` | `/api/v1/auth/admin/ping` | Admin bearer token |
| `GET` | `/api/v1/auth/oauth2/authorize/{provider}` | Public browser redirect |
| `GET` | `/api/v1/auth/oauth2/callback/{provider}` | Public provider callback |
| `GET` | `/api/v1/users/me` | Bearer token |
| `PUT` | `/api/v1/users/me` | Bearer token |
| `GET` | `/api/v1/users/{id}` | Admin bearer token |
| `GET` | `/api/v1/users` | Admin bearer token |
| `DELETE` | `/api/v1/users/{id}` | Super admin bearer token |
| `PUT` | `/api/v1/users/{id}/role` | Admin bearer token |
| `GET` | `/api/v1/users/me/preferences` | Bearer token |
| `PUT` | `/api/v1/users/me/preferences` | Bearer token |
| `POST` | `/api/v1/users/me/avatar` | Bearer token |
| `GET` | `/api/v1/notifications` | Bearer token |
| `GET` | `/api/v1/notifications/stream` | Bearer token |
| `PATCH` | `/api/v1/notifications/{id}/read` | Bearer token |
| `PATCH` | `/api/v1/notifications/read-all` | Bearer token |
| `DELETE` | `/api/v1/notifications/{id}` | Bearer token |
| `POST` | `/api/v1/payments` | Bearer token |
| `GET` | `/api/v1/payments` | Bearer token |
| `POST` | `/api/v1/payments/{paymentId}/confirm` | Bearer token |
| `POST` | `/api/v1/payments/webhook` | Public, Stripe signature |
| `POST` | `/api/v1/files/upload` | Bearer token |
| `GET` | `/api/v1/files/{id}/download-url` | Bearer token |
| `GET` | `/api/v1/files/{id}/metadata` | Bearer token |
| `GET` | `/api/v1/files/my-files` | Bearer token |
| `DELETE` | `/api/v1/files/{id}` | Bearer token |
| `POST` | `/api/v1/ai/chat` | Bearer token |
| `GET` | `/api/v1/ai/chat/stream/{sessionId}` | Bearer token |
| `GET` | `/api/v1/ai/sessions` | Bearer token |
| `GET` | `/api/v1/ai/sessions/{sessionId}/messages` | Bearer token |
| `POST` | `/api/v1/ai/sessions` | Bearer token |
| `DELETE` | `/api/v1/ai/sessions/{sessionId}` | Bearer token |
| `GET` | `/api/v1/ai/usage` | Bearer token |
| `POST` | `/api/v1/ai/admin/system-prompt` | Admin bearer token |
| `GET` | `/api/v1/audit` | Admin bearer token |
| `GET` | `/api/v1/audit/export` | Super admin bearer token |

## Common Problems

### 401 Missing bearer token

Login first, then send:

```text
Authorization: Bearer {{accessToken}}
```

### 401 Invalid or expired token

Call `/api/v1/auth/login` again, or call `/api/v1/auth/refresh` if Postman still has the `refresh_token` cookie.

### 403 Forbidden

Your token is valid, but the endpoint requires `ADMIN` or `SUPER_ADMIN`.

### 404 Not Found for UUID endpoints

Use real IDs returned by create/list endpoints. For placeholder tests, this UUID format is valid:

```text
00000000-0000-0000-0000-000000000000
```

### AI endpoint fails

Set an AI provider key in `.env`, then restart the AI service:

```text
GEMINI_API_KEY=
GROQ_API_KEY=
```

### Stripe webhook fails

If `STRIPE_WEBHOOK_SECRET` is set, the webhook must include a valid Stripe signature. Use the Stripe CLI or your Stripe dashboard webhook tester for real signature generation.
