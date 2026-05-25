const DEFAULT_APPLICATION_TEST_OPTIONS = {
    url: "",
    method: "GET",
    durationSeconds: 5,
    concurrency: 5,
    rps: 5,
    tickMs: 100,
    timeoutMs: 8000,
    successMin: 200,
    successMax: 399,
    headersText: "",
    body: "",
    attachAccessToken: true,
    includeCredentials: true
};

const createStats = () => ({
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
    errorSamples: [],
    perSecond: new Map()
});

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

const bumpMap = (map, key, amount = 1) => {
    map.set(String(key), (map.get(String(key)) || 0) + amount);
};

const bumpSecond = (stats, key) => {
    const second = Math.floor(Date.now() / 1000);
    if (!stats.perSecond.has(second)) {
        stats.perSecond.set(second, {sent: 0, received: 0, passed: 0, failed: 0});
    }
    stats.perSecond.get(second)[key] += 1;
};

const percentile = (values, rank) => {
    if (!values.length) {
        return 0;
    }
    const sorted = [...values].sort((left, right) => left - right);
    const index = Math.ceil((rank / 100) * sorted.length) - 1;
    return sorted[Math.min(sorted.length - 1, Math.max(0, index))];
};

const formatMetric = (value, digits = 2) => Number(value || 0).toFixed(digits);

const sumStatusRange = (statusCounts = {}, predicate) => Object.entries(statusCounts)
    .filter(([status]) => predicate(Number(status)))
    .reduce((sum, [, count]) => sum + Number(count || 0), 0);

const interpretApplicationLoadTest = (result, options = {}) => {
    if (!result) {
        return [
            {
                tone: "info",
                title: "Ready",
                detail: "Run a target to measure application speed, latency, throughput, status mix, timeout behavior, and gateway readiness."
            }
        ];
    }

    const notes = [];
    const status401Or403 = sumStatusRange(result.statusCounts, (status) => status === 401 || status === 403);
    const status429 = sumStatusRange(result.statusCounts, (status) => status === 429);
    const status5xx = sumStatusRange(result.statusCounts, (status) => status >= 500);
    const actualSentRps = result.averageSentPerSecond || 0;
    const actualPassedRps = result.averagePassedPerSecond || 0;
    const targetRps = Number(options.rps || 0);

    if (status401Or403 > 0) {
        notes.push({
            tone: "warn",
            title: "Auth failure detected",
            detail: "401/403 responses usually mean the endpoint is protected or the token is missing/invalid; this does not measure database efficiency."
        });
    }
    if (status429 > 0) {
        notes.push({
            tone: "warn",
            title: "Rate limit detected",
            detail: "429 responses mean the gateway or application intentionally throttled the traffic."
        });
    }
    if (status5xx > 0) {
        notes.push({
            tone: "bad",
            title: "Server errors detected",
            detail: "Check service logs, database connection pools, Redis, and container CPU or memory pressure."
        });
    }
    if (result.timeoutErrors > 0 && result.received === 0) {
        notes.push({
            tone: "bad",
            title: "All requests timed out",
            detail: "The target RPS or concurrency is far above what the current local setup can answer before the timeout."
        });
    } else if (result.timeoutErrors > 0) {
        notes.push({
            tone: "warn",
            title: "Timeouts detected",
            detail: "Reduce target RPS or concurrency until timeout errors are zero, then ramp gradually."
        });
    }
    if (result.networkErrors > 0 && result.received === 0) {
        notes.push({
            tone: "bad",
            title: "No HTTP responses",
            detail: "This is usually a connectivity or readiness issue: closed port, restarting container, or localhost IPv4/IPv6 mismatch."
        });
    } else if (result.networkErrors > 0) {
        notes.push({
            tone: "warn",
            title: "Network errors detected",
            detail: "Some requests failed before an HTTP response arrived; inspect the error samples and service logs."
        });
    }
    if (result.droppedByConcurrency > 0) {
        notes.push({
            tone: "warn",
            title: "Concurrency ceiling reached",
            detail: "The requested target rate is higher than the configured in-flight concurrency can sustain at the observed latency."
        });
    }
    if (targetRps > 0 && actualSentRps < targetRps * 0.8) {
        notes.push({
            tone: "warn",
            title: "Generator below target RPS",
            detail: `Observed ${formatMetric(actualSentRps)} sent req/sec against a target of ${targetRps}; use observed passed throughput for capacity decisions.`
        });
    }
    if (result.passed > 0) {
        notes.push({
            tone: result.verdict === "GOOD" ? "good" : "info",
            title: "Observed throughput",
            detail: `This run completed about ${formatMetric(actualPassedRps)} passed req/sec with p95 ${formatMetric(result.p95LatencyMs)}ms.`
        });
    }

    return notes.length > 0 ? notes : [
        {
            tone: "good",
            title: "Healthy run",
            detail: "No auth failures, rate limits, server errors, timeouts, dropped requests, or network errors were detected."
        }
    ];
};

const parseHeaderLines = (headersText = "") => headersText
    .split("\n")
    .map((line) => line.trim())
    .filter(Boolean)
    .reduce((headers, line) => {
        const separatorIndex = line.indexOf(":");
        if (separatorIndex <= 0) {
            throw new Error(`Invalid header "${line}". Use "Name: value".`);
        }
        headers[line.slice(0, separatorIndex).trim()] = line.slice(separatorIndex + 1).trim();
        return headers;
    }, {});

const cloneStats = (stats, startedAt = performance.now(), finishedAt = null) => {
    const elapsedMs = (finishedAt ?? performance.now()) - startedAt;
    const elapsedSeconds = Math.max(0.001, elapsedMs / 1000);
    const sortedLatencies = [...stats.latencies].sort((left, right) => left - right);
    const averageLatency = sortedLatencies.length
        ? sortedLatencies.reduce((sum, value) => sum + value, 0) / sortedLatencies.length
        : 0;
    const statusCounts = Object.fromEntries([...stats.statusCounts.entries()].sort(([left], [right]) => Number(left) - Number(right)));
    const errorCounts = Object.fromEntries([...stats.errorCounts.entries()].sort(([left], [right]) => left.localeCompare(right)));
    const perSecond = [...stats.perSecond.entries()]
        .sort(([left], [right]) => left - right)
        .map(([second, row]) => ({
            second,
            label: new Date(second * 1000).toLocaleTimeString(),
            ...row
        }));
    const p95 = percentile(sortedLatencies, 95);
    const hitRatio = stats.sent ? (stats.passed / stats.sent) * 100 : 0;
    const verdict = hitRatio >= 99 && p95 <= 500 && stats.timeoutErrors === 0
        ? "GOOD"
        : hitRatio >= 95 && p95 <= 1000
            ? "WARNING"
            : "BAD";

    return {
        ...stats,
        statusCounts,
        errorCounts,
        perSecond,
        elapsedMs,
        elapsedSeconds,
        averageSentPerSecond: stats.sent / elapsedSeconds,
        averagePassedPerSecond: stats.passed / elapsedSeconds,
        peakSentPerSecond: Math.max(0, ...perSecond.map((row) => row.sent)),
        peakPassedPerSecond: Math.max(0, ...perSecond.map((row) => row.passed)),
        hitRatio,
        failureRatio: stats.sent ? (stats.failed / stats.sent) * 100 : 0,
        receiveRatio: stats.sent ? (stats.received / stats.sent) * 100 : 0,
        minLatencyMs: sortedLatencies[0] ?? 0,
        avgLatencyMs: averageLatency,
        p50LatencyMs: percentile(sortedLatencies, 50),
        p90LatencyMs: percentile(sortedLatencies, 90),
        p95LatencyMs: p95,
        p99LatencyMs: percentile(sortedLatencies, 99),
        maxLatencyMs: sortedLatencies.at(-1) ?? 0,
        latencyBuckets: latencyBuckets(sortedLatencies),
        verdict,
        verdictReason: `${formatMetric(hitRatio)}% hit ratio, ${formatMetric(stats.passed / elapsedSeconds)} passed req/sec, p95 ${formatMetric(p95)}ms.`
    };
};

const latencyBuckets = (latencies) => {
    const buckets = [
        {label: "0-50ms", value: 0},
        {label: "51-100ms", value: 0},
        {label: "101-250ms", value: 0},
        {label: "251-500ms", value: 0},
        {label: "501-1000ms", value: 0},
        {label: ">1000ms", value: 0}
    ];
    for (const latency of latencies) {
        if (latency <= 50) {
            buckets[0].value += 1;
        } else if (latency <= 100) {
            buckets[1].value += 1;
        } else if (latency <= 250) {
            buckets[2].value += 1;
        } else if (latency <= 500) {
            buckets[3].value += 1;
        } else if (latency <= 1000) {
            buckets[4].value += 1;
        } else {
            buckets[5].value += 1;
        }
    }
    return buckets;
};

const errorKey = (error) => error?.cause?.code
    || error?.code
    || error?.cause?.name
    || error?.name
    || "network_error";

const errorSummary = (error) => {
    const parts = [error?.name, error?.message, error?.cause?.code, error?.cause?.message]
        .filter(Boolean);
    return [...new Set(parts)].join(": ") || "network_error";
};

const normalizeOptions = (options) => {
    const normalized = {
        ...DEFAULT_APPLICATION_TEST_OPTIONS,
        ...options,
        method: (options.method || "GET").toUpperCase(),
        durationSeconds: Number(options.durationSeconds),
        concurrency: Number(options.concurrency),
        rps: Number(options.rps),
        tickMs: Number(options.tickMs || DEFAULT_APPLICATION_TEST_OPTIONS.tickMs),
        timeoutMs: Number(options.timeoutMs),
        successMin: Number(options.successMin),
        successMax: Number(options.successMax)
    };
    if (!normalized.url?.trim()) {
        throw new Error("Target URL is required.");
    }
    if (!Number.isFinite(normalized.durationSeconds) || normalized.durationSeconds <= 0 || normalized.durationSeconds > 120) {
        throw new Error("Duration must be between 1 and 120 seconds.");
    }
    if (!Number.isInteger(normalized.concurrency) || normalized.concurrency <= 0 || normalized.concurrency > 500) {
        throw new Error("Concurrency must be between 1 and 500.");
    }
    if (!Number.isFinite(normalized.rps) || normalized.rps < 0 || normalized.rps > 10000) {
        throw new Error("Target RPS must be between 0 and 10000.");
    }
    if (!Number.isFinite(normalized.timeoutMs) || normalized.timeoutMs <= 0) {
        throw new Error("Timeout must be a positive number.");
    }
    if (normalized.successMin > normalized.successMax) {
        throw new Error("Success min cannot be greater than success max.");
    }
    return normalized;
};

const makeRequestInit = (options, headers, signal) => {
    const init = {
        method: options.method,
        headers,
        signal,
        credentials: options.includeCredentials ? "include" : "same-origin"
    };
    if (options.body && !["GET", "HEAD"].includes(options.method)) {
        init.body = options.body;
    }
    return init;
};

const requestOnce = async (options, headers, stats, notify, externalSignal) => {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort("timeout"), options.timeoutMs);
    const abortFromExternal = () => controller.abort("stopped");
    externalSignal?.addEventListener("abort", abortFromExternal, {once: true});
    const startedAt = performance.now();
    stats.sent += 1;
    bumpSecond(stats, "sent");
    notify();

    try {
        const response = await fetch(options.url, makeRequestInit(options, headers, controller.signal));
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
        if (externalSignal?.aborted || controller.signal.reason === "stopped") {
            return;
        }
        const elapsedMs = performance.now() - startedAt;
        stats.latencies.push(elapsedMs);
        stats.failed += 1;
        if (error?.name === "AbortError") {
            stats.timeoutErrors += 1;
            bumpMap(stats.errorCounts, "timeout");
        } else {
            stats.networkErrors += 1;
            bumpMap(stats.errorCounts, errorKey(error));
            if (stats.errorSamples.length < 5) {
                stats.errorSamples.push(errorSummary(error));
            }
        }
        bumpSecond(stats, "failed");
    } finally {
        clearTimeout(timeout);
        externalSignal?.removeEventListener("abort", abortFromExternal);
        notify();
    }
};

const runWorkerPool = async (options, headers, stats, notify, endAt, signal) => {
    const worker = async () => {
        while (performance.now() < endAt && !signal?.aborted) {
            await requestOnce(options, headers, stats, notify, signal);
        }
    };
    await Promise.all(Array.from({length: options.concurrency}, worker));
};

const runTargetRps = async (options, headers, stats, notify, endAt, signal) => {
    let carry = 0;
    let inFlight = 0;
    let lastTickAt = performance.now();
    const running = new Set();

    while (performance.now() < endAt && !signal?.aborted) {
        const tickStartedAt = performance.now();
        const elapsedSinceLastTickMs = Math.max(0, tickStartedAt - lastTickAt);
        lastTickAt = tickStartedAt;
        carry += options.rps * (elapsedSinceLastTickMs / 1000);
        const toSend = Math.floor(carry);
        carry -= toSend;

        for (let index = 0; index < toSend; index += 1) {
            if (inFlight >= options.concurrency) {
                stats.droppedByConcurrency += 1;
                notify();
                continue;
            }
            inFlight += 1;
            const promise = requestOnce(options, headers, stats, notify, signal)
                .finally(() => {
                    inFlight -= 1;
                    running.delete(promise);
                });
            running.add(promise);
        }

        await sleep(Math.max(0, options.tickMs - (performance.now() - tickStartedAt)));
    }

    await Promise.allSettled([...running]);
};

const runApplicationLoadTest = async (inputOptions, {accessToken, signal, onUpdate} = {}) => {
    const options = normalizeOptions(inputOptions);
    const headers = parseHeaderLines(options.headersText);
    if (options.attachAccessToken && accessToken) {
        headers.Authorization = `Bearer ${accessToken}`;
    }
    if (options.body && !headers["Content-Type"] && !headers["content-type"]) {
        headers["Content-Type"] = "application/json";
    }

    const stats = createStats();
    const startedAt = performance.now();
    const endAt = startedAt + options.durationSeconds * 1000;
    let lastNotificationAt = 0;
    const notify = (force = false) => {
        const now = performance.now();
        if (!force && now - lastNotificationAt < 200) {
            return;
        }
        lastNotificationAt = now;
        onUpdate?.(cloneStats(stats, startedAt));
    };

    notify(true);
    if (options.rps > 0) {
        await runTargetRps(options, headers, stats, notify, endAt, signal);
    } else {
        await runWorkerPool(options, headers, stats, notify, endAt, signal);
    }
    const finishedAt = performance.now();
    const result = cloneStats(stats, startedAt, finishedAt);
    onUpdate?.(result);
    return result;
};

export {
    DEFAULT_APPLICATION_TEST_OPTIONS,
    formatMetric,
    interpretApplicationLoadTest,
    runApplicationLoadTest
};
