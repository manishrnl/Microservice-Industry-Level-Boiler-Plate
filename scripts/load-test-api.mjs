#!/usr/bin/env node

import {performance} from "node:perf_hooks";

const DEFAULTS = {
    url: "http://localhost:8080/actuator/health",
    durationSeconds: 15,
    concurrency: 50,
    rps: 0,
    tickMs: 10,
    method: "GET",
    timeoutMs: 10000,
    successMin: 200,
    successMax: 399
};

const usage = () => `
Usage:
  node scripts/load-test-api.mjs [options]

Options:
  --url <url>             API URL to bombard. Default: ${DEFAULTS.url}
  --duration <seconds>    Test duration in seconds. Default: ${DEFAULTS.durationSeconds}
  --concurrency <number>  Parallel workers. Default: ${DEFAULTS.concurrency}
  --rps <number>          Target requests per second. Default: unlimited, bounded by concurrency
  --tick-ms <ms>          Scheduler tick for --rps mode. Default: ${DEFAULTS.tickMs}
  --method <method>       HTTP method. Default: ${DEFAULTS.method}
  --header "K: V"         Request header. Can be repeated.
  --body <json/string>    Request body for POST/PUT/PATCH.
  --timeout <ms>          Per-request timeout. Default: ${DEFAULTS.timeoutMs}
  --success-min <code>    Lowest status counted as passed. Default: ${DEFAULTS.successMin}
  --success-max <code>    Highest status counted as passed. Default: ${DEFAULTS.successMax}

Examples:
  node scripts/load-test-api.mjs --url http://localhost:8080/actuator/health --duration 20 --concurrency 100
  node scripts/load-test-api.mjs --url http://localhost:8080/api/v1/auth/.well-known/jwks.json --duration 30 --concurrency 200
  node scripts/load-test-api.mjs --url http://localhost:8080/api/v1/auth/me --header "Authorization: Bearer TOKEN" --duration 60 --concurrency 500 --rps 5000
`;

const readOptionValue = (args, index, name) => {
    const value = args[index + 1];
    if (!value || value.startsWith("--")) {
        throw new Error(`Missing value for ${name}`);
    }
    return value;
};

const parseArgs = () => {
    const args = process.argv.slice(2);
    const options = {...DEFAULTS, headers: []};

    for (let index = 0; index < args.length; index += 1) {
        const arg = args[index];
        if (arg === "--help" || arg === "-h") {
            console.log(usage());
            process.exit(0);
        }
        if (arg === "--url") {
            options.url = readOptionValue(args, index, arg);
            index += 1;
        } else if (arg === "--duration") {
            options.durationSeconds = Number(readOptionValue(args, index, arg));
            index += 1;
        } else if (arg === "--concurrency") {
            options.concurrency = Number(readOptionValue(args, index, arg));
            index += 1;
        } else if (arg === "--rps") {
            options.rps = Number(readOptionValue(args, index, arg));
            index += 1;
        } else if (arg === "--tick-ms") {
            options.tickMs = Number(readOptionValue(args, index, arg));
            index += 1;
        } else if (arg === "--method") {
            options.method = readOptionValue(args, index, arg).toUpperCase();
            index += 1;
        } else if (arg === "--header") {
            options.headers.push(readOptionValue(args, index, arg));
            index += 1;
        } else if (arg === "--body") {
            options.body = readOptionValue(args, index, arg);
            index += 1;
        } else if (arg === "--timeout") {
            options.timeoutMs = Number(readOptionValue(args, index, arg));
            index += 1;
        } else if (arg === "--success-min") {
            options.successMin = Number(readOptionValue(args, index, arg));
            index += 1;
        } else if (arg === "--success-max") {
            options.successMax = Number(readOptionValue(args, index, arg));
            index += 1;
        } else {
            throw new Error(`Unknown option: ${arg}`);
        }
    }

    if (!Number.isFinite(options.durationSeconds) || options.durationSeconds <= 0) {
        throw new Error("--duration must be a positive number");
    }
    if (!Number.isInteger(options.concurrency) || options.concurrency <= 0) {
        throw new Error("--concurrency must be a positive integer");
    }
    if (!Number.isFinite(options.rps) || options.rps < 0) {
        throw new Error("--rps must be zero or a positive number");
    }
    if (!Number.isFinite(options.tickMs) || options.tickMs <= 0 || options.tickMs > 1000) {
        throw new Error("--tick-ms must be a positive number up to 1000");
    }
    if (!Number.isFinite(options.timeoutMs) || options.timeoutMs <= 0) {
        throw new Error("--timeout must be a positive number");
    }
    return options;
};

const parseHeaders = (headerArgs) => {
    const headers = {};
    for (const rawHeader of headerArgs) {
        const separatorIndex = rawHeader.indexOf(":");
        if (separatorIndex <= 0) {
            throw new Error(`Invalid header "${rawHeader}". Use "Name: value"`);
        }
        const name = rawHeader.slice(0, separatorIndex).trim();
        const value = rawHeader.slice(separatorIndex + 1).trim();
        headers[name] = value;
    }
    return headers;
};

const percentile = (sortedValues, rank) => {
    if (sortedValues.length === 0) {
        return 0;
    }
    const index = Math.ceil((rank / 100) * sortedValues.length) - 1;
    return sortedValues[Math.min(sortedValues.length - 1, Math.max(0, index))];
};

const formatNumber = (value, digits = 2) => Number(value || 0).toFixed(digits);

const makeStats = () => ({
    sent: 0,
    received: 0,
    passed: 0,
    failed: 0,
    networkErrors: 0,
    timeoutErrors: 0,
    droppedByConcurrency: 0,
    bytesReceived: 0,
    latencies: [],
    statusCounts: new Map(),
    errorCounts: new Map(),
    perSecond: new Map()
});

const bumpMap = (map, key, amount = 1) => {
    map.set(key, (map.get(key) || 0) + amount);
};

const bumpSecond = (stats, key) => {
    const second = Math.floor(Date.now() / 1000);
    if (!stats.perSecond.has(second)) {
        stats.perSecond.set(second, {sent: 0, received: 0, passed: 0, failed: 0});
    }
    stats.perSecond.get(second)[key] += 1;
};

const requestOnce = async (options, headers, stats) => {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), options.timeoutMs);
    const startedAt = performance.now();
    stats.sent += 1;
    bumpSecond(stats, "sent");

    try {
        const response = await fetch(options.url, {
            method: options.method,
            headers,
            body: options.body,
            signal: controller.signal
        });
        const body = await response.arrayBuffer();
        const elapsedMs = performance.now() - startedAt;
        const passed = response.status >= options.successMin && response.status <= options.successMax;

        stats.received += 1;
        stats.bytesReceived += body.byteLength;
        stats.latencies.push(elapsedMs);
        bumpMap(stats.statusCounts, response.status);
        bumpSecond(stats, "received");

        if (passed) {
            stats.passed += 1;
            bumpSecond(stats, "passed");
        } else {
            stats.failed += 1;
            bumpSecond(stats, "failed");
        }
    } catch (error) {
        const elapsedMs = performance.now() - startedAt;
        stats.latencies.push(elapsedMs);
        stats.failed += 1;
        if (error?.name === "AbortError") {
            stats.timeoutErrors += 1;
            bumpMap(stats.errorCounts, "timeout");
        } else {
            stats.networkErrors += 1;
            bumpMap(stats.errorCounts, error?.code || error?.name || "network_error");
        }
        bumpSecond(stats, "failed");
    } finally {
        clearTimeout(timeout);
    }
};

const worker = async (options, headers, stats, endAt) => {
    while (performance.now() < endAt) {
        await requestOnce(options, headers, stats);
    }
};

const runTargetRps = async (options, headers, stats, endAt) => {
    const intervalMs = options.tickMs;
    const requestsPerTick = options.rps / (1000 / intervalMs);
    let carry = 0;
    let inFlight = 0;
    const running = new Set();

    while (performance.now() < endAt) {
        const tickStartedAt = performance.now();
        carry += requestsPerTick;
        const toSend = Math.floor(carry);
        carry -= toSend;

        for (let index = 0; index < toSend; index += 1) {
            if (inFlight >= options.concurrency) {
                stats.droppedByConcurrency += 1;
                continue;
            }
            inFlight += 1;
            const promise = requestOnce(options, headers, stats)
                .finally(() => {
                    inFlight -= 1;
                    running.delete(promise);
                });
            running.add(promise);
        }

        const elapsedMs = performance.now() - tickStartedAt;
        const waitMs = Math.max(0, intervalMs - elapsedMs);
        await new Promise((resolve) => setTimeout(resolve, waitMs));
    }

    await Promise.allSettled([...running]);
};

const bar = (label, value, maxValue, width = 36) => {
    const safeMax = Math.max(1, maxValue);
    const count = Math.round((value / safeMax) * width);
    const filled = "#".repeat(Math.max(0, count));
    const empty = ".".repeat(Math.max(0, width - count));
    return `${label.padEnd(18)} | ${filled}${empty} | ${value}`;
};

const printBarChart = (title, rows) => {
    if (rows.length === 0) {
        return;
    }
    const maxValue = Math.max(...rows.map(([, value]) => value));
    console.log(`\n${title}`);
    for (const [label, value] of rows) {
        console.log(bar(String(label), value, maxValue));
    }
};

const bucketLatency = (latencies) => {
    const buckets = [
        ["0-50ms", 0],
        ["51-100ms", 0],
        ["101-250ms", 0],
        ["251-500ms", 0],
        ["501-1000ms", 0],
        [">1000ms", 0]
    ];
    for (const latency of latencies) {
        if (latency <= 50) {
            buckets[0][1] += 1;
        } else if (latency <= 100) {
            buckets[1][1] += 1;
        } else if (latency <= 250) {
            buckets[2][1] += 1;
        } else if (latency <= 500) {
            buckets[3][1] += 1;
        } else if (latency <= 1000) {
            buckets[4][1] += 1;
        } else {
            buckets[5][1] += 1;
        }
    }
    return buckets;
};

const printHealthVerdict = (stats, p95Latency, totalSeconds) => {
    const hitRatio = stats.sent === 0 ? 0 : (stats.passed / stats.sent) * 100;
    const passedRps = stats.passed / totalSeconds;
    const status = hitRatio >= 99 && p95Latency <= 500 && stats.timeoutErrors === 0
        ? "GOOD"
        : hitRatio >= 95 && p95Latency <= 1000
            ? "WARNING"
            : "BAD";
    console.log("\nApplication verdict");
    console.log(`Status: ${status}`);
    console.log(`Reason: ${formatNumber(hitRatio)}% hit ratio, ${formatNumber(passedRps)} passed req/sec, p95 latency ${formatNumber(p95Latency)}ms.`);
};

const printSummary = (options, stats, totalMs) => {
    const sortedLatencies = [...stats.latencies].sort((a, b) => a - b);
    const totalSeconds = totalMs / 1000;
    const passRatio = stats.sent === 0 ? 0 : (stats.passed / stats.sent) * 100;
    const failRatio = stats.sent === 0 ? 0 : (stats.failed / stats.sent) * 100;
    const receiveRatio = stats.sent === 0 ? 0 : (stats.received / stats.sent) * 100;
    const avgLatency = sortedLatencies.length === 0
        ? 0
        : sortedLatencies.reduce((sum, value) => sum + value, 0) / sortedLatencies.length;
    const perSecondRows = [...stats.perSecond.entries()]
        .sort(([left], [right]) => left - right)
        .map(([, row]) => row);
    const peakSentPerSecond = Math.max(0, ...perSecondRows.map((row) => row.sent));
    const peakPassedPerSecond = Math.max(0, ...perSecondRows.map((row) => row.passed));
    const p95Latency = percentile(sortedLatencies, 95);

    console.log("\nLoad test summary");
    console.table({
        url: options.url,
        method: options.method,
        concurrency: options.concurrency,
        targetRequestsPerSecond: options.rps || "unlimited",
        durationSeconds: formatNumber(totalSeconds),
        sent: stats.sent,
        received: stats.received,
        passed: stats.passed,
        failed: stats.failed,
        networkErrors: stats.networkErrors,
        timeoutErrors: stats.timeoutErrors,
        droppedByConcurrency: stats.droppedByConcurrency,
        bytesReceived: stats.bytesReceived,
        averageSentPerSecond: formatNumber(stats.sent / totalSeconds),
        averagePassedPerSecond: formatNumber(stats.passed / totalSeconds),
        peakSentPerSecond,
        peakPassedPerSecond,
        hitRatioPercent: formatNumber(passRatio),
        failureRatioPercent: formatNumber(failRatio),
        receiveRatioPercent: formatNumber(receiveRatio),
        minLatencyMs: formatNumber(sortedLatencies[0]),
        avgLatencyMs: formatNumber(avgLatency),
        p50LatencyMs: formatNumber(percentile(sortedLatencies, 50)),
        p90LatencyMs: formatNumber(percentile(sortedLatencies, 90)),
        p95LatencyMs: formatNumber(p95Latency),
        p99LatencyMs: formatNumber(percentile(sortedLatencies, 99)),
        maxLatencyMs: formatNumber(sortedLatencies.at(-1))
    });

    console.log("\nHTTP status counts");
    console.table(Object.fromEntries([...stats.statusCounts.entries()].sort(([left], [right]) => left - right)));

    if (stats.errorCounts.size > 0) {
        console.log("\nError counts");
        console.table(Object.fromEntries(stats.errorCounts.entries()));
    }

    printBarChart("Pass / fail chart", [
        ["passed", stats.passed],
        ["failed", stats.failed],
        ["dropped", stats.droppedByConcurrency]
    ]);
    printBarChart("HTTP status chart", [...stats.statusCounts.entries()].sort(([left], [right]) => left - right));
    printBarChart("Latency distribution", bucketLatency(stats.latencies));
    printBarChart("Requests per second", [...stats.perSecond.entries()]
        .sort(([left], [right]) => left - right)
        .map(([second, row]) => [new Date(second * 1000).toLocaleTimeString(), row.sent]));
    printHealthVerdict(stats, p95Latency, totalSeconds);
};

const main = async () => {
    const options = parseArgs();
    const headers = parseHeaders(options.headers);
    const stats = makeStats();
    const startedAt = performance.now();
    const endAt = startedAt + options.durationSeconds * 1000;

    console.log(`Bombarding ${options.method} ${options.url}`);
    console.log(`Duration: ${options.durationSeconds}s | Concurrency: ${options.concurrency} | Target RPS: ${options.rps || "unlimited"} | Timeout: ${options.timeoutMs}ms`);

    if (options.rps > 0) {
        await runTargetRps(options, headers, stats, endAt);
    } else {
        await Promise.all(Array.from({length: options.concurrency}, () => worker(options, headers, stats, endAt)));
    }

    const totalMs = performance.now() - startedAt;
    printSummary(options, stats, totalMs);
};

main().catch((error) => {
    console.error(error.message);
    console.error(usage());
    process.exit(1);
});
