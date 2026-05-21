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

## Find The Maximum Smooth RPS

Do not start at `1 lakh` or `10,000` RPS. Start small, then increase until the result changes from `GOOD` to `WARNING` or `BAD`.

For your local machine, a practical ramp looks like this:

```powershell
node scripts/load-test-api.mjs --url http://localhost:8080/actuator/health --duration 30 --concurrency 50 --rps 100
node scripts/load-test-api.mjs --url http://localhost:8080/actuator/health --duration 30 --concurrency 100 --rps 250
node scripts/load-test-api.mjs --url http://localhost:8080/actuator/health --duration 30 --concurrency 200 --rps 500
node scripts/load-test-api.mjs --url http://localhost:8080/actuator/health --duration 30 --concurrency 400 --rps 1000
node scripts/load-test-api.mjs --url http://localhost:8080/actuator/health --duration 30 --concurrency 800 --rps 2000
```

The maximum smooth RPS is the highest command where:

- verdict is `GOOD`
- `hitRatioPercent >= 99`
- `timeoutErrors = 0`
- `droppedByConcurrency = 0`
- `p95LatencyMs` is acceptable for your app

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

## Test DB Through Your Application Without Token

Use this endpoint to measure app plus DB together with a very light query:

```text
GET /api/v1/auth/db-ping
```

It goes through:

```text
client -> API gateway -> auth-service -> Postgres auth_db -> auth-service -> API gateway -> client
```

It runs a real DB query:

- `select 1`
- return DB elapsed time

First verify one request:

```powershell
Invoke-RestMethod http://localhost:8080/api/v1/auth/db-ping
```

Then run a smooth DB load test:

```powershell
node scripts/load-test-api.mjs `
  --url http://localhost:8080/api/v1/auth/db-ping `
  --duration 30 `
  --concurrency 50 `
  --rps 100
```

Ramp the DB-backed API gradually:

```powershell
node scripts/load-test-api.mjs --url http://localhost:8080/api/v1/auth/db-ping --duration 30 --concurrency 50 --rps 100
node scripts/load-test-api.mjs --url http://localhost:8080/api/v1/auth/db-ping --duration 30 --concurrency 100 --rps 250
node scripts/load-test-api.mjs --url http://localhost:8080/api/v1/auth/db-ping --duration 30 --concurrency 200 --rps 500
node scripts/load-test-api.mjs --url http://localhost:8080/api/v1/auth/db-ping --duration 30 --concurrency 400 --rps 1000
```

If `/actuator/health` is `GOOD` at high RPS but `/api/v1/auth/db-ping` becomes `WARNING` or `BAD`, your bottleneck is probably database, connection pool, or auth-service DB logic.

## Test Heavier DB Table Counts

Use this endpoint when you want to test heavier DB reads through your application:

```text
GET /api/v1/auth/db-stats
```

It runs:

- count users
- count sessions

Verify one request:

```powershell
Invoke-RestMethod http://localhost:8080/api/v1/auth/db-stats
```

Load test table counts gently:

```powershell
node scripts/load-test-api.mjs --url http://localhost:8080/api/v1/auth/db-stats --duration 30 --concurrency 20 --rps 25
node scripts/load-test-api.mjs --url http://localhost:8080/api/v1/auth/db-stats --duration 30 --concurrency 50 --rps 50
node scripts/load-test-api.mjs --url http://localhost:8080/api/v1/auth/db-stats --duration 30 --concurrency 100 --rps 100
```

Do not use table-count endpoints as your only DB benchmark if tables are large. `count(*)` can be much heavier than normal indexed business queries.

## What Your Previous Results Mean

Direct auth-service DB test:

```text
URL: http://localhost:8081/api/v1/auth/db-ping
RPS target: 100
Passed: 299
Failed: 4
Dropped: 1569
p95: 6109ms
Verdict: BAD
```

Meaning:

- the request reached auth-service and returned some `200` responses
- p95 latency above 6 seconds is too high
- `droppedByConcurrency` means the app/generator could not keep up with the target RPS
- the observed smooth throughput was only about 8 passed requests per second
- this is not stable for 100 RPS

Gateway DB test before the gateway public-path patch:

```text
URL: http://localhost:8080/api/v1/auth/db-ping
Status: 401
Latency: very low
```

Meaning:

- the gateway rejected the request before it reached auth-service
- it did not test the database at all
- after the gateway patch, this endpoint should return `200`

When interpreting results:

- `401` or `403`: auth/config issue, not DB efficiency
- `429`: gateway rate limit, not DB failure
- `timeoutErrors`: app, DB, or local machine is overloaded
- `droppedByConcurrency`: target RPS is too high for the configured concurrency and observed latency
- `200` with low p95 latency: healthy path
- `200` but high p95 latency: app works but cannot handle that load smoothly

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
  --header "Authorization: Bearer eyJraWQiOiJwbGF0Zm9ybS1rZXktMS05NTQ1ODI2My1jNzIwLTQ5ODAtYWZjZC03NmQ5NDVkNDA3ZGYiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI2ZGM1NTdiNC0zMmQyLTRiNWUtOGI3Yy0zNTdhODhiOWRlMTQiLCJyb2xlcyI6WyJVU0VSIl0sImlzcyI6InBsYXRmb3JtLWF1dGgtc2VydmljZSIsIm5hbWUiOiJNYW5pc2ggU2FodSIsInNlc3Npb25JZCI6ImIyYWIyYWZmLTNkN2UtNDI1OS1hYTI1LTI2YmVhYWJjZjNlYiIsImV4cCI6MTc3OTMzOTQ5MSwiaWF0IjoxNzc5MzM4NTkxLCJqdGkiOiI3ZGRkYzUyYi1hYjJhLTQ0ZjgtYjkyZS02MjVkOTUzOTAyNzQiLCJlbWFpbCI6Im1hbmlzaHJhanJubEBnbWFpbC5jb20ifQ.TSEVDx61XD9Qqqj0Pt0B15AEQ2cpzdpYXck2YgxBIrgsaQ3WJG1Ha-3xo7Lin8I0e4vIfZHPMkO-lyEq-AOfR-UyPmySnBY-UJAq6o505JJCKcXgzrUxJAw7-u7UpGTA3ddFyohMKBF3uP5pUPY4lylploKCVuZKHO87Nj_NxcdJyTFvsxFdmFJFlH4F9bDspsnmMl4ZuD9wpqioLSGfF-N7vuSE90XO2tau3LUSWjOt-KuRhMBnYl_v3-hCEcV-4fKNTnxBRjHFhjU6lSmBXF3zhM3JnyNRKc25JSj9uRPA1y2ufcZSY-kjwOWhu8MD4QgtM3d19NT9SwWamql2gA" `
  --duration 30 `
  --concurrency 200 `
  --rps 1000

```
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
--tick-ms <ms>          Scheduler tick for --rps mode. Default: 10
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

The script also prints `Result interpretation`. Use that section as the plain-English explanation of what happened.

## Recommended Tests

Start small with public gateway:

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

Start small with DB-backed app testing:

```powershell
node scripts/load-test-api.mjs --url http://localhost:8080/api/v1/auth/db-ping --duration 30 --concurrency 50 --rps 100
```

If that is not `GOOD`, reduce the target:

```powershell
node scripts/load-test-api.mjs --url http://localhost:8080/api/v1/auth/db-ping --duration 30 --concurrency 10 --rps 10
node scripts/load-test-api.mjs --url http://localhost:8080/api/v1/auth/db-ping --duration 30 --concurrency 20 --rps 25
node scripts/load-test-api.mjs --url http://localhost:8080/api/v1/auth/db-ping --duration 30 --concurrency 50 --rps 50
```

## Notes

- `/actuator/health` is useful for basic uptime testing.
- `/api/v1/auth/.well-known/jwks.json` is better for testing the API gateway because it is public and does not require a token.
- For protected endpoints, pass `Authorization: Bearer YOUR_ACCESS_TOKEN`.
- High concurrency can overwhelm your local machine, Docker, Redis, database, or services. Increase load gradually.
