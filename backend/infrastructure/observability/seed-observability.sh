#!/bin/sh
set -eu

KAFKA_BOOTSTRAP_SERVERS="${KAFKA_BOOTSTRAP_SERVERS:-kafka:9092}"
KAFKA_BIN="${KAFKA_BIN:-/opt/kafka/bin}"
ZIPKIN_URL="${ZIPKIN_URL:-http://zipkin:9411}"
PROMETHEUS_URL="${PROMETHEUS_URL:-http://prometheus:9090}"
SAMPLE_TOPIC="${OBSERVABILITY_SAMPLE_TOPIC:-observability.sample.events}"

log() {
  printf '%s %s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$*"
}

wait_for_tcp() {
  host="$1"
  port="$2"
  for attempt in $(seq 1 60); do
    if nc -z "$host" "$port" >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done
  log "Timed out waiting for $host:$port"
  return 1
}

wait_for_http() {
  url="$1"
  for attempt in $(seq 1 60); do
    if wget -q -O /dev/null -T 3 "$url" >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done
  log "Timed out waiting for $url"
  return 1
}

wait_for_kafka() {
  for attempt in $(seq 1 60); do
    if "$KAFKA_BIN/kafka-topics.sh" --bootstrap-server "$KAFKA_BOOTSTRAP_SERVERS" --list >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done
  log "Timed out waiting for Kafka at $KAFKA_BOOTSTRAP_SERVERS"
  return 1
}

create_topic() {
  topic="$1"
  "$KAFKA_BIN/kafka-topics.sh" \
    --bootstrap-server "$KAFKA_BOOTSTRAP_SERVERS" \
    --create \
    --if-not-exists \
    --topic "$topic" \
    --partitions 3 \
    --replication-factor 1 >/dev/null
}

produce() {
  topic="$1"
  shift
  printf '%s\n' "$@" | "$KAFKA_BIN/kafka-console-producer.sh" \
    --bootstrap-server "$KAFKA_BOOTSTRAP_SERVERS" \
    --topic "$topic" >/dev/null
}

seed_kafka() {
  now="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  create_topic "$SAMPLE_TOPIC"
  for topic in auth.events user.events payment.events notification.events audit.events; do
    create_topic "$topic"
  done

  produce "$SAMPLE_TOPIC" \
    "{\"eventId\":\"sample-observability-001\",\"eventType\":\"OBSERVABILITY_SAMPLE_READY\",\"source\":\"observability-seeder\",\"createdAt\":\"$now\",\"details\":{\"prometheus\":\"sample metrics exporter is being scraped\",\"zipkin\":\"demo traces were submitted\",\"kafka\":\"demo topic messages were produced\"}}" \
    "{\"eventId\":\"sample-observability-002\",\"eventType\":\"OBSERVABILITY_KAFKA_FLOW\",\"source\":\"observability-seeder\",\"createdAt\":\"$now\",\"details\":{\"topics\":[\"auth.events\",\"user.events\",\"payment.events\",\"notification.events\",\"audit.events\"]}}"

  produce "auth.events" \
    "{\"eventId\":\"sample-auth-login-001\",\"eventType\":\"AUTH_LOGIN_SUCCEEDED\",\"userId\":\"demo-user\",\"email\":\"demo.user@example.com\",\"createdAt\":\"$now\"}" \
    "{\"eventId\":\"sample-auth-refresh-001\",\"eventType\":\"AUTH_TOKEN_REFRESHED\",\"userId\":\"demo-user\",\"createdAt\":\"$now\"}"
  produce "user.events" \
    "{\"eventId\":\"sample-user-profile-001\",\"eventType\":\"USER_PROFILE_UPDATED\",\"userId\":\"demo-user\",\"createdAt\":\"$now\"}"
  produce "payment.events" \
    "{\"eventId\":\"sample-payment-001\",\"eventType\":\"PAYMENT_CONFIRMED\",\"userId\":\"demo-user\",\"amount\":499.00,\"currency\":\"INR\",\"createdAt\":\"$now\"}"
  produce "notification.events" \
    "{\"eventId\":\"sample-notification-001\",\"eventType\":\"NOTIFICATION_CREATED\",\"userId\":\"demo-user\",\"channel\":\"email\",\"createdAt\":\"$now\"}"
  produce "audit.events" \
    "{\"eventId\":\"sample-audit-001\",\"eventType\":\"AUDIT_EVENT_RECORDED\",\"userId\":\"demo-user\",\"action\":\"OBSERVABILITY_SAMPLE_SEEDED\",\"createdAt\":\"$now\"}"
}

seed_zipkin() {
  timestamp="$(date +%s)000000"
  payload="[
    {\"traceId\":\"463ac35c9f6413ad48485a3953bb6124\",\"id\":\"a2fb4a1d1a96d312\",\"name\":\"demo-login-flow\",\"timestamp\":$timestamp,\"duration\":145000,\"localEndpoint\":{\"serviceName\":\"api-gateway\"},\"tags\":{\"sample\":\"true\",\"component\":\"gateway\"}},
    {\"traceId\":\"463ac35c9f6413ad48485a3953bb6124\",\"parentId\":\"a2fb4a1d1a96d312\",\"id\":\"5f2d6f8f5f2d6f8f\",\"name\":\"POST /api/v1/auth/login\",\"timestamp\":$((timestamp + 1000)),\"duration\":72000,\"localEndpoint\":{\"serviceName\":\"auth-service\"},\"tags\":{\"http.method\":\"POST\",\"http.route\":\"/api/v1/auth/login\",\"sample\":\"true\"}},
    {\"traceId\":\"463ac35c9f6413ad48485a3953bb6124\",\"parentId\":\"5f2d6f8f5f2d6f8f\",\"id\":\"0f28590523a46541\",\"name\":\"seed demo data\",\"timestamp\":$((timestamp + 12000)),\"duration\":38000,\"localEndpoint\":{\"serviceName\":\"user-service\"},\"tags\":{\"sample\":\"true\",\"db.system\":\"postgresql\"}},
    {\"traceId\":\"463ac35c9f6413ad48485a3953bb6124\",\"parentId\":\"a2fb4a1d1a96d312\",\"id\":\"6b221d5bc9e6496c\",\"name\":\"publish auth.events\",\"timestamp\":$((timestamp + 55000)),\"duration\":26000,\"localEndpoint\":{\"serviceName\":\"notification-service\"},\"tags\":{\"messaging.system\":\"kafka\",\"messaging.destination\":\"auth.events\",\"sample\":\"true\"}},
    {\"traceId\":\"5af7183fb1d4cf5f2f1347f5a1b2c3d4\",\"id\":\"bb1f6bdfd96a9f42\",\"name\":\"demo-payment-flow\",\"timestamp\":$((timestamp + 200000)),\"duration\":188000,\"localEndpoint\":{\"serviceName\":\"api-gateway\"},\"tags\":{\"sample\":\"true\",\"component\":\"gateway\"}},
    {\"traceId\":\"5af7183fb1d4cf5f2f1347f5a1b2c3d4\",\"parentId\":\"bb1f6bdfd96a9f42\",\"id\":\"c4f2b61be1ac7a31\",\"name\":\"POST /api/v1/payments\",\"timestamp\":$((timestamp + 207000)),\"duration\":96000,\"localEndpoint\":{\"serviceName\":\"payment-service\"},\"tags\":{\"http.method\":\"POST\",\"http.route\":\"/api/v1/payments\",\"sample\":\"true\"}},
    {\"traceId\":\"5af7183fb1d4cf5f2f1347f5a1b2c3d4\",\"parentId\":\"c4f2b61be1ac7a31\",\"id\":\"c8d2b9f5e4a1c703\",\"name\":\"publish payment.events\",\"timestamp\":$((timestamp + 245000)),\"duration\":31000,\"localEndpoint\":{\"serviceName\":\"payment-service\"},\"tags\":{\"messaging.system\":\"kafka\",\"messaging.destination\":\"payment.events\",\"sample\":\"true\"}},
    {\"traceId\":\"5af7183fb1d4cf5f2f1347f5a1b2c3d4\",\"parentId\":\"c4f2b61be1ac7a31\",\"id\":\"e1b3f7a98521d441\",\"name\":\"write audit event\",\"timestamp\":$((timestamp + 270000)),\"duration\":45000,\"localEndpoint\":{\"serviceName\":\"audit-service\"},\"tags\":{\"sample\":\"true\",\"db.system\":\"postgresql\"}}
  ]"

  wget -q -O - \
    --header "Content-Type: application/json" \
    --post-data "$payload" \
    "$ZIPKIN_URL/api/v2/spans" >/dev/null
}

warm_prometheus_targets() {
  for url in \
    "http://api-gateway:8080/actuator/health" \
    "http://auth-service:8081/actuator/health" \
    "http://user-service:8082/actuator/health" \
    "http://notification-service:8083/actuator/health" \
    "http://payment-service:8084/actuator/health" \
    "http://file-service:8085/actuator/health" \
    "http://ai-service:8086/actuator/health" \
    "http://audit-service:8087/actuator/health" \
    "http://observability-sample-exporter:9109/metrics"; do
    wget -q -O /dev/null -T 3 "$url" 2>/dev/null || true
  done
  wget -q -O /dev/null -T 3 "$PROMETHEUS_URL/api/v1/query?query=up" 2>/dev/null || true
}

log "Waiting for observability dependencies"
wait_for_kafka
wait_for_http "$ZIPKIN_URL/health"
wait_for_http "$PROMETHEUS_URL/-/ready"
wait_for_tcp "observability-sample-exporter" "9109"

log "Seeding Kafka sample messages"
seed_kafka
log "Seeding Zipkin sample spans"
seed_zipkin
log "Warming Prometheus scrape targets"
warm_prometheus_targets
log "Observability sample data seeded"
