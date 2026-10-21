# LGTM Observability Stack

This directory contains all configuration files to run the full **LGTM stack** (Loki + Grafana + Tempo + Mimir) alongside the food-delivery-backend.

## 📁 Directory Structure

```
observability/
├── alloy/
│   └── config.alloy          # Grafana Alloy pipeline (scrape metrics, receive traces)
├── loki/
│   └── loki-config.yaml      # Loki log storage config
├── mimir/
│   └── mimir.yaml            # Mimir metrics storage config
├── tempo/
│   └── tempo.yaml            # Tempo distributed tracing config
└── grafana/
    └── provisioning/
        ├── datasources/
        │   └── datasources.yaml       # Auto-provisioned Loki + Mimir + Tempo datasources
        └── dashboards/
            ├── dashboards.yaml        # Dashboard provider config
            └── food-delivery-overview.json  # Pre-built Spring Boot dashboard
```

## 🚀 Getting Started

### 1. Start the full stack

```bash
docker-compose up -d
```

### 2. Start your Spring Boot app

```bash
./mvnw spring-boot:run
```

### 3. Open Grafana

- **URL**: http://localhost:3000
- **Login**: `admin` / `admin`
- Navigate to **Dashboards → Food Delivery → Food Delivery — Application Overview**

## 🔭 Telemetry Flow

```
Spring Boot App
│
├── Logs     → Loki4j Logback appender  → Loki   (port 3100)
├── Metrics  → /actuator/prometheus     → Alloy  → Mimir  (port 9009)
└── Traces   → OTLP HTTP exporter       → Tempo  (port 4318)
                                              ↓
                                         Grafana (port 3000)
                                  correlates all three pillars
```

## 🔗 Service URLs

| Service        | URL                          | Purpose                      |
|----------------|------------------------------|------------------------------|
| Grafana        | http://localhost:3000        | Unified UI (admin/admin)     |
| Loki           | http://localhost:3100        | Log API                      |
| Mimir          | http://localhost:9009        | Metrics API                  |
| Tempo          | http://localhost:3200        | Trace API                    |
| Alloy UI       | http://localhost:12345       | Pipeline inspector           |
| App Metrics    | http://localhost:8080/actuator/prometheus | Raw Prometheus metrics |

## 📊 What's on the Dashboard

| Panel | Query | Pillar |
|---|---|---|
| HTTP Request Rate | `rate(http_server_requests_seconds_count[1m])` | Metrics |
| HTTP Error Rate (5xx) | `rate(...{status=~"5.."}[1m])` | Metrics |
| Latency p50/p95/p99 | `histogram_quantile(0.99, ...)` | Metrics |
| JVM Heap Used | `jvm_memory_used_bytes{area="heap"}` | Metrics |
| HikariCP Connections | `hikaricp_connections_active` | Metrics |
| Redis Cache Hit Rate | `cache_gets_total{result="hit"}` | Metrics |
| Application Logs | `{app="food-delivery-backend"}` | Logs |

## 🔍 Correlations

Grafana is configured so you can:
1. **Metric → Trace**: Click a spike on any metric chart → see the traces from that time window
2. **Trace → Log**: Click on a span in Tempo → jump to the correlated log line in Loki (matched by `traceId`)
3. **Log → Trace**: Click the `traceId` in a Loki log line → open the full trace in Tempo

This is the core power of the LGTM stack!

## ⚙️ Environment Variables

Add these to your `.env` or set them before running the app:

```env
# Loki — log shipper endpoint
LOKI_URL=http://localhost:3100

# OpenTelemetry — trace/metric exporter (via Alloy)
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4319

# Tracing sample rate (1.0 = 100% in dev, use 0.1 in prod)
TRACING_SAMPLE_RATE=1.0

# Environment label added to all metrics and logs
ENVIRONMENT=local
```
