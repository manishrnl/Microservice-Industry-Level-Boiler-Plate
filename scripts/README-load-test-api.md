# API Load Test Script

`load-test-api.mjs` is a dependency-free Node.js script for bombarding an API endpoint and printing a detailed summary.

It reports:

- Total requests sent
- Responses received
- Passed requests
- Failed requests
- Network errors
- Timeout errors
- Bytes received
- Average and peak requests per second
- Hit ratio, failure ratio, receive ratio
- Latency min, average, p50, p90, p95, p99, max
- HTTP status counts, including `429 Too Many Requests`
- Terminal bar charts for pass/fail, status codes, latency buckets, and requests per second
- A simple `GOOD`, `WARNING`, or `BAD` application verdict

## Requirement

Use Node.js 18 or newer.

Check your Node version:

```powershell
node --version
```

## Basic Command

From the project root:

```powershell
node scripts/load-test-api.mjs --url http://localhost:8080/actuator/health --duration 20 --concurrency 100
```

## Target A Specific Requests-Per-Second Rate

Use `--rps` when you want the script to try a fixed request rate instead of running as fast as possible.

```powershell
node scripts/load-test-api.mjs `
  --url http://localhost:8080/actuator/health `
  --duration 20 `
  --concurrency 1000 `
  --rps 5000
```

`--concurrency` is the maximum number of in-flight requests. If the app is too slow and the script cannot keep up, the summary will show `droppedByConcurrency`.

## About 1 Lakh Requests Per Second

`1 lakh requests per second` means `100,000 RPS`.

Do not start your first test at `100000`. On a normal laptop, one Node.js process usually cannot generate a clean 100,000 RPS by itself, and Docker Desktop may become the bottleneck before your application or database does.

If you still want to attempt it locally:

```powershell
node scripts/load-test-api.mjs `
  --url http://localhost:8080/actuator/health `
  --duration 30 `
  --concurrency 20000 `
  --rps 100000 `
  --timeout 5000
```

For real 100,000 RPS testing, use multiple load-generator machines or containers, then add the results together. One machine should not be trusted as proof that the backend can or cannot serve 100,000 RPS.

Recommended ramp-up:

```powershell
node scripts/load-test-api.mjs --url http://localhost:8080/actuator/health --duration 30 --concurrency 100 --rps 500
node scripts/load-test-api.mjs --url http://localhost:8080/actuator/health --duration 30 --concurrency 500 --rps 2000
node scripts/load-test-api.mjs --url http://localhost:8080/actuator/health --duration 30 --concurrency 1000 --rps 5000
node scripts/load-test-api.mjs --url http://localhost:8080/actuator/health --duration 30 --concurrency 3000 --rps 10000
```

For a huge-user production-style app, judge capacity by:

- `hitRatioPercent >= 99`
- `p95LatencyMs <= 500` for normal APIs
- `p99LatencyMs <= 1000` if you expect heavier DB reads
- `timeoutErrors = 0`
- no growing database CPU, connection pool exhaustion, Redis errors, or `5xx` responses

## Test Public Health Endpoint

This checks whether the API gateway process is responding.

```powershell
node scripts/load-test-api.mjs `
  --url http://localhost:8080/actuator/health `
  --duration 20 `
  --concurrency 100
```

## Test Public API Gateway Route

This is better for testing gateway filters and rate limiting than `/actuator/health`, because it goes through a normal routed API path.

```powershell
node scripts/load-test-api.mjs `
  --url http://localhost:8080/api/v1/auth/.well-known/jwks.json `
  --duration 30 `
  --concurrency 200
```

Your gateway anonymous rate limit is currently `100` requests per minute, so this command should eventually show `429` responses when the limit is exceeded.

Use this version to intentionally hit the anonymous gateway limit quickly:

```powershell
node scripts/load-test-api.mjs `
  --url http://localhost:8080/api/v1/auth/.well-known/jwks.json `
  --duration 60 `
  --concurrency 200 `
  --rps 1000
```

## Test Auth Service Directly

Use this when you want to bypass the gateway and hit the auth service container/port directly.

```powershell
node scripts/load-test-api.mjs `
  --url http://localhost:8081/actuator/health `
  --duration 20 `
  --concurrency 100
```

## Test Other Service Health URLs

```powershell
node scripts/load-test-api.mjs --url http://localhost:8082/actuator/health --duration 20 --concurrency 100
node scripts/load-test-api.mjs --url http://localhost:8083/actuator/health --duration 20 --concurrency 100
node scripts/load-test-api.mjs --url http://localhost:8084/actuator/health --duration 20 --concurrency 100
node scripts/load-test-api.mjs --url http://localhost:8085/actuator/health --duration 20 --concurrency 100
node scripts/load-test-api.mjs --url http://localhost:8086/actuator/health --duration 20 --concurrency 100
node scripts/load-test-api.mjs --url http://localhost:8087/actuator/health --duration 20 --concurrency 100
```

Service ports:

| Service | Port |
| --- | --- |
| API Gateway | `8080` |
| Auth | `8081` |
| User | `8082` |
| Notification | `8083` |
| Payment | `8084` |
| File | `8085` |
| AI | `8086` |
| Audit | `8087` |

## Test Login Endpoint

Be careful with this because it creates real login/session activity.

```powershell
node scripts/load-test-api.mjs `
  --url http://localhost:8080/api/v1/auth/login `
  --method POST `
  --header "Content-Type: application/json" `
  --body '{"email":"test@example.com","password":"Password@123","deviceId":"load-test"}' `
  --duration 10 `
  --concurrency 20
```

## Test With Bearer Token

Use this for protected APIs.

```powershell
node scripts/load-test-api.mjs `
  --url http://localhost:8080/api/v1/auth/me `
  --header "Authorization: Bearer YOUR_ACCESS_TOKEN" `
  --duration 20 `
  --concurrency 50
```

## Test A DB-Backed API Read

`GET /api/v1/auth/me` is a good DB-backed read test because it:

- validates the access token
- checks the active session
- updates session last-active time
- fetches the user from Postgres

First login and copy the access token:

```powershell
$login = Invoke-RestMethod `
  -Method POST `
  -Uri http://localhost:8080/api/v1/auth/login `
  -ContentType "application/json" `
  -Body '{"email":"test@example.com","password":"Password@123","deviceId":"db-load-test"}'

$token = $login.accessToken
```

Then bombard the DB-backed endpoint:

```powershell
node scripts/load-test-api.mjs `
  --url http://localhost:8080/api/v1/auth/me `
  --header "Authorization: Bearer $token" `
  --duration 30 `
  --concurrency 200 `
  --rps 1000
```

Heavier DB-backed session-list read:

```powershell
node scripts/load-test-api.mjs `
  --url http://localhost:8080/api/v1/auth/sessions `
  --header "Authorization: Bearer $token" `
  --duration 30 `
  --concurrency 200 `
  --rps 1000
```

If these DB-backed endpoints fail much earlier than `/actuator/health`, your bottleneck is likely auth service logic, database connections, database CPU/IO, or Redis/session work.

## Test Database Directly With pgbench

This tests Postgres itself, without HTTP, gateway, JWT, Redis, or Spring Boot overhead.

Initialize pgbench tables inside `auth_db` once:

```powershell
docker compose exec postgres pgbench -U platform -i -s 10 auth_db
```

Run a read-only database benchmark:

```powershell
docker compose exec postgres pgbench -U platform -S -c 50 -j 4 -T 60 auth_db
```

Increase clients gradually:

```powershell
docker compose exec postgres pgbench -U platform -S -c 100 -j 8 -T 60 auth_db
docker compose exec postgres pgbench -U platform -S -c 200 -j 8 -T 60 auth_db
```

Read the important pgbench numbers:

- `tps` means database transactions per second
- `latency average` shows average DB response time
- if latency rises sharply when clients increase, DB saturation has started

To test your real auth tables with a simple repeated query:

```powershell
docker compose exec postgres psql -U platform -d auth_db -c "EXPLAIN ANALYZE SELECT count(*) FROM users;"
```

## All Options

```text
--url <url>             API URL to bombard.
--duration <seconds>    Test duration in seconds. Default: 15
--concurrency <number>  Parallel workers. Default: 50
--rps <number>          Target requests per second. Default: unlimited, bounded by concurrency
--method <method>       HTTP method. Default: GET
--header "K: V"         Request header. Can be repeated.
--body <json/string>    Request body for POST/PUT/PATCH.
--timeout <ms>          Per-request timeout. Default: 10000
--success-min <code>    Lowest status counted as passed. Default: 200
--success-max <code>    Highest status counted as passed. Default: 399
```

## Reading The Summary

| Field | Meaning |
| --- | --- |
| `sent` | Total requests attempted by the script. |
| `received` | Requests that got an HTTP response. |
| `passed` | Responses with status between `success-min` and `success-max`. Default is `200-399`. |
| `failed` | Requests that timed out, failed at network level, or returned a status outside the success range. |
| `networkErrors` | Failed before receiving an HTTP response. |
| `timeoutErrors` | Requests aborted after `--timeout`. |
| `droppedByConcurrency` | Requests skipped because target `--rps` was higher than the configured in-flight concurrency could handle. |
| `averageSentPerSecond` | Average attempted request rate. |
| `averagePassedPerSecond` | Average successful request rate. |
| `peakSentPerSecond` | Highest attempted request count in one second. |
| `peakPassedPerSecond` | Highest passed request count in one second. |
| `hitRatioPercent` | `passed / sent * 100`. |
| `failureRatioPercent` | `failed / sent * 100`. |
| `receiveRatioPercent` | `received / sent * 100`. |
| `p95LatencyMs` | 95% of measured requests finished within this time. |
| `p99LatencyMs` | 99% of measured requests finished within this time. |

## Reading The Charts

The script prints simple terminal charts like:

```text
Pass / fail chart
passed             | #################################### | 9900
failed             | #................................... | 100
dropped            | .................................... | 0
```

Normal users can look for:

- Mostly full `passed` bar
- Tiny or empty `failed` bar
- Empty `dropped` bar
- `Status: GOOD` in the application verdict
- No big `429`, `500`, `502`, `503`, or timeout counts

## Recommended Tests

Start small:

```powershell
node scripts/load-test-api.mjs --url http://localhost:8080/api/v1/auth/.well-known/jwks.json --duration 10 --concurrency 10
```

Then increase pressure:

```powershell
node scripts/load-test-api.mjs --url http://localhost:8080/api/v1/auth/.well-known/jwks.json --duration 30 --concurrency 100
```

Then intentionally exceed the anonymous gateway limit:

```powershell
node scripts/load-test-api.mjs --url http://localhost:8080/api/v1/auth/.well-known/jwks.json --duration 60 --concurrency 200
```

Expected result: after enough requests, the HTTP status counts should include `429`.

## Notes

- `/actuator/health` is useful for basic uptime testing.
- `/api/v1/auth/.well-known/jwks.json` is better for testing the API gateway because it is public and does not require a token.
- For protected endpoints, pass `Authorization: Bearer YOUR_ACCESS_TOKEN`.
- High concurrency can overwhelm your local machine, Docker, Redis, database, or services. Increase load gradually.
