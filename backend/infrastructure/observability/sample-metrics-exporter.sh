#!/bin/sh
set -eu

METRICS_DIR="${METRICS_DIR:-/www}"
METRICS_PORT="${SAMPLE_METRICS_PORT:-9109}"
INTERVAL_SECONDS="${SAMPLE_METRICS_INTERVAL_SECONDS:-15}"

mkdir -p "$METRICS_DIR"

write_metrics() {
  epoch="$(date +%s)"
  auth_success="$((1200 + epoch % 200))"
  auth_failure="$((18 + epoch % 12))"
  payment_success="$((320 + epoch % 80))"
  payment_failed="$((7 + epoch % 9))"
  kafka_auth="$((2500 + epoch % 400))"
  kafka_user="$((2100 + epoch % 300))"
  kafka_payment="$((900 + epoch % 150))"
  kafka_audit="$((1800 + epoch % 250))"
  p95_gateway="$((180 + epoch % 60))"
  p95_auth="$((120 + epoch % 45))"
  p95_payment="$((240 + epoch % 90))"

  cat > "$METRICS_DIR/metrics" <<EOF
# HELP platform_sample_requests_total Demo request volume for the platform observability dashboard.
# TYPE platform_sample_requests_total counter
platform_sample_requests_total{service="api-gateway",route="/api/v1/auth/login",status="2xx"} $auth_success
platform_sample_requests_total{service="api-gateway",route="/api/v1/auth/login",status="4xx"} $auth_failure
platform_sample_requests_total{service="payment-service",route="/api/v1/payments",status="2xx"} $payment_success
platform_sample_requests_total{service="payment-service",route="/api/v1/payments",status="5xx"} $payment_failed
# HELP platform_sample_request_p95_milliseconds Demo p95 latency in milliseconds.
# TYPE platform_sample_request_p95_milliseconds gauge
platform_sample_request_p95_milliseconds{service="api-gateway"} $p95_gateway
platform_sample_request_p95_milliseconds{service="auth-service"} $p95_auth
platform_sample_request_p95_milliseconds{service="payment-service"} $p95_payment
# HELP platform_kafka_sample_messages_total Demo Kafka messages produced by topic.
# TYPE platform_kafka_sample_messages_total counter
platform_kafka_sample_messages_total{topic="auth.events",event_type="AUTH_LOGIN_SUCCEEDED"} $kafka_auth
platform_kafka_sample_messages_total{topic="user.events",event_type="USER_PROFILE_UPDATED"} $kafka_user
platform_kafka_sample_messages_total{topic="payment.events",event_type="PAYMENT_CONFIRMED"} $kafka_payment
platform_kafka_sample_messages_total{topic="audit.events",event_type="AUDIT_EVENT_RECORDED"} $kafka_audit
# HELP platform_zipkin_sample_spans_total Demo Zipkin spans seeded into the tracing backend.
# TYPE platform_zipkin_sample_spans_total counter
platform_zipkin_sample_spans_total{trace="demo-login-flow"} 4
platform_zipkin_sample_spans_total{trace="demo-payment-flow"} 5
EOF
}

write_metrics
while true; do
  write_metrics
  sleep "$INTERVAL_SECONDS"
done &

exec httpd -f -p "$METRICS_PORT" -h "$METRICS_DIR"
