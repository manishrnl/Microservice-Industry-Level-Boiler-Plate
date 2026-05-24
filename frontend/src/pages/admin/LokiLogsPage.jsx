import {
    Activity,
    AlertTriangle,
    BarChart3,
    Clock3,
    Database,
    ExternalLink,
    RadioTower,
    RefreshCw,
    Search,
    ServerCog,
    Terminal
} from "lucide-react";
import {useMemo, useState} from "react";
import {useQuery} from "@tanstack/react-query";
import {apiClient} from "../../api/axiosInstance";
import {endpoints} from "../../api/endpoints";
import {PageWrapper} from "../../components/common/PageWrapper";
import {env} from "../../config/env";

const fallbackServices = ["api-gateway", "auth-service", "user-service", "notification-service", "payment-service", "file-service", "ai-service", "audit-service", "frontend", "postgres", "redis", "kafka", "grafana", "prometheus", "loki", "promtail"];
const levels = ["", "ERROR", "WARN", "INFO", "DEBUG", "TRACE"];
const ranges = [
    {label: "15m", value: String(15 * 60 * 1000)},
    {label: "1h", value: String(60 * 60 * 1000)},
    {label: "6h", value: String(6 * 60 * 60 * 1000)},
    {label: "24h", value: String(24 * 60 * 60 * 1000)}
];
const quickFilters = [
    {label: "401", value: {q: "401", level: ""}},
    {label: "Errors", value: {q: "", level: "ERROR"}},
    {label: "Info", value: {q: "", level: "INFO"}},
    {label: "Auth", value: {q: "auth", level: ""}},
    {label: "Gateway", value: {service: "api-gateway"}},
    {label: "Startup", value: {q: "Started", level: ""}},
    {label: "Flyway", value: {q: "Flyway", level: ""}}
];
const grafanaExploreUrl = `${env.grafanaUrl}/explore?left=%7B%22datasource%22:%22Loki%22,%22queries%22:%5B%7B%22expr%22:%22%7Bcompose_project%3D%5C%22microservice-industry%5C%22%7D%22%7D%5D%7D`;
const grafanaSamplesUrl = `${env.grafanaUrl}/d/platform-observability-samples/platform-observability-samples?orgId=1&from=now-30m&to=now`;
const prometheusQueryUrl = (expr) => `${env.prometheusUrl}/query?g0.expr=${encodeURIComponent(expr)}&g0.tab=graph&g0.range_input=30m`;
const zipkinSamplesUrl = `${env.zipkinUrl}/zipkin/?serviceName=api-gateway&lookback=1h&limit=10`;
const quickViewLinks = [
    {
        label: "All Samples",
        context: "Grafana dashboard",
        detail: "Prometheus, Zipkin, and Kafka sample signals in one place.",
        href: grafanaSamplesUrl,
        Icon: BarChart3,
        tone: "bg-orange-100 text-orange-700 ring-orange-200 dark:bg-orange-500/15 dark:text-orange-300 dark:ring-orange-500/25"
    },
    {
        label: "Prometheus",
        context: "Sample metrics",
        detail: "Request counters, p95 latency, and seeded platform metrics.",
        href: prometheusQueryUrl("platform_sample_requests_total"),
        Icon: Activity,
        tone: "bg-red-100 text-red-700 ring-red-200 dark:bg-red-500/15 dark:text-red-300 dark:ring-red-500/25"
    },
    {
        label: "Zipkin",
        context: "Sample traces",
        detail: "Login and payment traces across gateway and services.",
        href: zipkinSamplesUrl,
        Icon: RadioTower,
        tone: "bg-violet-100 text-violet-700 ring-violet-200 dark:bg-violet-500/15 dark:text-violet-300 dark:ring-violet-500/25"
    },
    {
        label: "Kafka",
        context: "Sample events",
        detail: "Produced event counts for auth, user, payment, and audit topics.",
        href: prometheusQueryUrl("platform_kafka_sample_messages_total"),
        Icon: Database,
        tone: "bg-cyan-100 text-cyan-700 ring-cyan-200 dark:bg-cyan-500/15 dark:text-cyan-300 dark:ring-cyan-500/25"
    }
];
const relatedLinks = [
    {
        label: "Grafana Explore",
        href: grafanaExploreUrl,
        Icon: BarChart3,
        tone: "bg-orange-100 text-orange-700 ring-orange-200 dark:bg-orange-500/15 dark:text-orange-300 dark:ring-orange-500/25"
    },
    {
        label: "Loki API",
        href: env.lokiStatusUrl,
        Icon: Terminal,
        tone: "bg-emerald-100 text-emerald-700 ring-emerald-200 dark:bg-emerald-500/15 dark:text-emerald-300 dark:ring-emerald-500/25"
    },
    {
        label: "Prometheus",
        href: env.prometheusUrl,
        Icon: Database,
        tone: "bg-red-100 text-red-700 ring-red-200 dark:bg-red-500/15 dark:text-red-300 dark:ring-red-500/25"
    },
    {
        label: "Zipkin",
        href: env.zipkinUrl,
        Icon: RadioTower,
        tone: "bg-violet-100 text-violet-700 ring-violet-200 dark:bg-violet-500/15 dark:text-violet-300 dark:ring-violet-500/25"
    },
    {
        label: "Gateway Health",
        href: env.gatewayHealthUrl,
        Icon: ServerCog,
        tone: "bg-sky-100 text-sky-700 ring-sky-200 dark:bg-sky-500/15 dark:text-sky-300 dark:ring-sky-500/25"
    }
];

const LokiLogsPage = () => {
    const [filters, setFilters] = useState({service: "", level: "", q: "", range: ranges[1].value, limit: "200"});
    const services = useQuery({
        queryKey: ["loki-services"],
        queryFn: async () => {
            const payload = (await apiClient.get(endpoints.observability.lokiServices)).data;
            return Array.isArray(payload?.data) ? payload.data : [];
        },
        refetchInterval: 30000
    });
    const serviceOptions = useMemo(() => {
        const values = Array.from(new Set([...fallbackServices, ...(services.data ?? [])])).filter(Boolean).sort();
        return ["", ...values];
    }, [services.data]);
    const logql = useMemo(() => buildLogQl(filters), [filters]);
    const queryShape = useMemo(() => ({
        query: logql,
        limit: filters.limit,
        direction: "BACKWARD",
        range: filters.range
    }), [filters.limit, filters.range, logql]);
    const logs = useQuery({
        queryKey: ["loki-logs", queryShape],
        queryFn: async () => {
            const end = new Date();
            const start = new Date(end.getTime() - Number(queryShape.range || ranges[1].value));
            const params = {
                query: queryShape.query,
                limit: queryShape.limit,
                direction: queryShape.direction,
                start: start.toISOString(),
                end: end.toISOString()
            };
            return (await apiClient.get(endpoints.observability.lokiQueryRange, {params})).data;
        },
        refetchInterval: 10000
    });
    const rows = useMemo(() => extractRows(logs.data), [logs.data]);
    const searchTerm = filters.q.trim();
    const updateFilter = (field) => (event) => setFilters((current) => ({...current, [field]: event.target.value}));
    const applyQuickFilter = (value) => setFilters((current) => ({...current, ...value}));
    const clearFilters = () => setFilters((current) => ({...current, service: "", level: "", q: ""}));

    return <PageWrapper title="Loki Logs">
        <div className="space-y-5">
            <section className="rounded-md border border-slate-200 bg-white p-4 dark:border-white/10 dark:bg-slate-950">
                <div className="flex flex-wrap items-center justify-between gap-3">
                    <div>
                        <h2 className="text-sm font-semibold text-slate-950 dark:text-white">Observability quick views</h2>
                        <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">Open the seeded Prometheus, Zipkin, and Kafka views directly.</p>
                    </div>
                    <a
                        href={grafanaSamplesUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="inline-flex h-10 items-center gap-2 rounded-md bg-slate-950 px-3 text-sm font-semibold text-white hover:bg-slate-800 dark:bg-teal-300 dark:text-slate-950 dark:hover:bg-teal-200"
                    >
                        <BarChart3 className="h-4 w-4"/>
                        Open sample dashboard
                    </a>
                </div>
                <div className="mt-4 grid gap-3 md:grid-cols-2 xl:grid-cols-4">
                    {quickViewLinks.map(({label, context, detail, href, Icon, tone}) => <a
                        key={label}
                        href={href}
                        target="_blank"
                        rel="noreferrer"
                        className="group rounded-md border border-slate-200 p-4 transition hover:-translate-y-0.5 hover:border-slate-300 hover:bg-slate-50 hover:shadow-md dark:border-white/10 dark:hover:border-white/20 dark:hover:bg-white/[0.04]"
                    >
                        <span className="flex items-start justify-between gap-4">
                            <span className={`grid h-10 w-10 place-items-center rounded-md ring-1 ${tone}`}>
                                <Icon className="h-5 w-5"/>
                            </span>
                            <ExternalLink className="h-4 w-4 text-slate-400 transition group-hover:text-slate-700 dark:group-hover:text-white"/>
                        </span>
                        <span className="mt-4 block text-[11px] font-bold uppercase tracking-wide text-slate-400">{context}</span>
                        <span className="mt-1 block text-base font-semibold text-slate-950 dark:text-white">{label}</span>
                        <span className="mt-2 block min-h-10 text-sm text-slate-500 dark:text-slate-400">{detail}</span>
                    </a>)}
                </div>
            </section>

            <section className="rounded-md border border-slate-200 bg-white p-4 dark:border-white/10 dark:bg-slate-950">
                <div className="flex flex-wrap items-center justify-between gap-3">
                    <div className="flex items-center gap-3">
                        <span className="grid h-10 w-10 place-items-center rounded-md bg-emerald-100 text-emerald-700 ring-1 ring-emerald-200 dark:bg-emerald-500/15 dark:text-emerald-300 dark:ring-emerald-500/25">
                            <Terminal className="h-5 w-5"/>
                        </span>
                        <div>
                            <h2 className="text-sm font-semibold text-slate-950 dark:text-white">Docker and service logs</h2>
                            <p className="text-sm text-slate-500 dark:text-slate-400">{rows.length} entries from Loki</p>
                        </div>
                    </div>
                    <button
                        type="button"
                        onClick={() => logs.refetch()}
                        className="inline-flex h-10 items-center gap-2 rounded-md border border-slate-200 px-3 text-sm font-semibold text-slate-700 hover:bg-slate-50 dark:border-white/10 dark:text-slate-100 dark:hover:bg-white/10"
                    >
                        <RefreshCw className={`h-4 w-4 ${logs.isFetching ? "animate-spin" : ""}`}/>
                        Refresh
                    </button>
                </div>

                <div className="mt-4 grid gap-3 xl:grid-cols-[170px_120px_minmax(220px,1fr)_110px_100px_auto]">
                    <Select value={filters.service} onChange={updateFilter("service")} options={serviceOptions} label="All services"/>
                    <Select value={filters.level} onChange={updateFilter("level")} options={levels} label="All levels"/>
                    <label className="relative block">
                        <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400"/>
                        <input
                            value={filters.q}
                            onChange={updateFilter("q")}
                            placeholder="Search text, 401, route, exception, user"
                            className="h-10 w-full rounded-md border border-slate-200 bg-white pl-9 pr-3 text-sm outline-none focus:border-slate-400 dark:border-white/10 dark:bg-slate-900 dark:text-white"
                        />
                    </label>
                    <Select value={filters.range} onChange={updateFilter("range")} options={ranges.map((range) => range.value)} labels={Object.fromEntries(ranges.map((range) => [range.value, range.label]))} label="Range"/>
                    <Select value={filters.limit} onChange={updateFilter("limit")} options={["50", "100", "200", "500", "1000"]} label="Limit"/>
                    <button
                        type="button"
                        onClick={clearFilters}
                        className="h-10 rounded-md border border-slate-200 px-3 text-sm font-semibold text-slate-700 hover:bg-slate-50 dark:border-white/10 dark:text-slate-100 dark:hover:bg-white/10"
                    >
                        Clear
                    </button>
                </div>

                <div className="mt-3 flex flex-wrap gap-2">
                    {quickFilters.map((filter) => <button
                        key={filter.label}
                        type="button"
                        onClick={() => applyQuickFilter(filter.value)}
                        className="h-8 rounded-md border border-slate-200 px-3 text-xs font-semibold text-slate-600 hover:bg-slate-50 dark:border-white/10 dark:text-slate-200 dark:hover:bg-white/10"
                    >
                        {filter.label}
                    </button>)}
                </div>

                <div className="mt-4 flex items-start gap-2 rounded-md border border-slate-200 bg-slate-50 p-3 text-xs text-slate-600 dark:border-white/10 dark:bg-white/[0.04] dark:text-slate-300">
                    <Clock3 className="mt-0.5 h-4 w-4 shrink-0 text-slate-400"/>
                    <code className="min-w-0 break-all">{logql}</code>
                </div>

                <div className="mt-4 overflow-hidden rounded-md border border-slate-200 dark:border-white/10">
                    <div className="overflow-x-auto">
                        <div className="min-w-[980px]">
                            <div className="grid grid-cols-[170px_90px_160px_minmax(520px,1fr)] bg-slate-100 px-3 py-2 text-xs font-bold uppercase text-slate-500 dark:bg-white/10 dark:text-slate-300">
                                <span>Time</span>
                                <span>Level</span>
                                <span>Service</span>
                                <span>Message</span>
                            </div>
                            <div className="max-h-[620px] overflow-auto bg-slate-950 font-mono text-xs text-slate-100">
                                {rows.map((row) => <div
                                    key={`${row.timestamp}-${row.service}-${row.index}`}
                                    className="grid grid-cols-[170px_90px_160px_minmax(520px,1fr)] border-t border-white/10 px-3 py-2"
                                >
                                    <span className="text-slate-400">{formatLokiTime(row.timestamp)}</span>
                                    <span className={levelClass(row.level)}>{row.level || "LOG"}</span>
                                    <span className="truncate text-cyan-200" title={row.service}>{row.service}</span>
                                    <span className="break-all text-slate-100">
                                        <HighlightedText value={row.message} search={searchTerm}/>
                                    </span>
                                </div>)}
                                {!rows.length && <p className="px-3 py-8 text-center text-sm text-slate-400">No matching Loki logs.</p>}
                            </div>
                        </div>
                    </div>
                </div>
                {(logs.isError || logs.data?.status === "error") &&
                    <p className="mt-3 flex items-center gap-2 text-sm text-red-600 dark:text-red-300">
                        <AlertTriangle className="h-4 w-4"/>
                        Loki query failed. Check that Loki and Promtail are running.
                    </p>}
            </section>

            <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
                {relatedLinks.map(({label, href, Icon, tone}) => <a
                    key={label}
                    href={href}
                    target="_blank"
                    rel="noreferrer"
                    className="group rounded-md border border-slate-200 bg-white p-4 transition hover:-translate-y-0.5 hover:border-slate-300 hover:shadow-lg dark:border-white/10 dark:bg-slate-950 dark:hover:border-white/20"
                >
                    <span className="flex items-start justify-between gap-4">
                        <span className={`grid h-10 w-10 place-items-center rounded-md ring-1 ${tone}`}>
                            <Icon className="h-5 w-5"/>
                        </span>
                        <ExternalLink className="h-4 w-4 text-slate-400 transition group-hover:text-slate-700 dark:group-hover:text-white"/>
                    </span>
                    <span className="mt-4 block text-sm font-semibold text-slate-950 dark:text-white">{label}</span>
                    <span className="mt-3 block break-all text-xs text-slate-500 dark:text-slate-400">{href}</span>
                </a>)}
            </section>
        </div>
    </PageWrapper>;
};

const Select = ({value, onChange, options, label, labels = {}}) => <select
    value={value}
    onChange={onChange}
    className="h-10 rounded-md border border-slate-200 bg-white px-3 text-sm outline-none focus:border-slate-400 dark:border-white/10 dark:bg-slate-900 dark:text-white"
>
    {options.map((option) => <option key={option || label} value={option}>{option ? labels[option] || option : label}</option>)}
</select>;

const HighlightedText = ({value, search}) => {
    const parts = highlightParts(value, search);
    return parts.map((part, index) => part.match
        ? <mark
            key={`${index}-${part.text}`}
            className="rounded-sm bg-amber-300 px-0.5 font-bold text-slate-950 ring-1 ring-amber-200"
        >{part.text}</mark>
        : <span key={`${index}-${part.text}`}>{part.text}</span>);
};

const highlightParts = (value, search) => {
    const text = String(value ?? "");
    const needle = String(search ?? "").trim();
    if (!needle) {
        return [{text, match: false}];
    }
    const lowerText = text.toLowerCase();
    const lowerNeedle = needle.toLowerCase();
    const parts = [];
    let cursor = 0;
    let nextMatch = lowerText.indexOf(lowerNeedle, cursor);
    while (nextMatch !== -1) {
        if (nextMatch > cursor) {
            parts.push({text: text.slice(cursor, nextMatch), match: false});
        }
        parts.push({text: text.slice(nextMatch, nextMatch + needle.length), match: true});
        cursor = nextMatch + needle.length;
        nextMatch = lowerText.indexOf(lowerNeedle, cursor);
    }
    if (cursor < text.length) {
        parts.push({text: text.slice(cursor), match: false});
    }
    return parts.length ? parts : [{text, match: false}];
};

const buildLogQl = ({service, level, q}) => {
    const selectors = [service ? `service="${escapeLabel(service)}"` : "compose_project=\"microservice-industry\""];
    if (level) {
        selectors.push(`level="${escapeLabel(level)}"`);
    }
    const filters = [`{${selectors.join(",")}}`];
    const search = q?.trim();
    if (search) {
        filters.push(`|~ "(?i)${escapeRegex(search)}"`);
    }
    return filters.join(" ");
};

const escapeLabel = (value) => String(value).replace(/\\/g, "\\\\").replace(/"/g, "\\\"");

const escapeRegex = (value) => String(value)
    .replace(/[\\^$.*+?()[\]{}|]/g, "\\$&")
    .replace(/"/g, "\\\"");

const extractRows = (payload) => {
    const streams = payload?.data?.result ?? [];
    return streams.flatMap((stream) => {
        const labels = stream.stream ?? {};
        return (stream.values ?? []).map(([timestamp, line], index) => {
            const parsed = parseLogLine(line);
            return {
                index,
                timestamp,
                service: labels.service || labels.container || "unknown",
                level: labels.level || parsed.level || levelFromLine(line),
                message: parsed.message || String(line ?? ""),
                labels
            };
        });
    }).sort((a, b) => compareTimestamp(b.timestamp, a.timestamp));
};

const parseLogLine = (line) => {
    const value = String(line ?? "").trim();
    if (!value.startsWith("{")) {
        return {message: value, level: levelFromLine(value)};
    }
    try {
        const parsed = JSON.parse(value);
        const message = parsed.message || parsed.msg || parsed.log || value;
        return {message: String(message).trim(), level: parsed.level || parsed.severity};
    } catch {
        return {message: value, level: levelFromLine(value)};
    }
};

const levelFromLine = (line) => {
    const match = String(line ?? "").match(/\b(ERROR|WARN|INFO|DEBUG|TRACE)\b/i);
    return match?.[1]?.toUpperCase() ?? "";
};

const compareTimestamp = (left, right) => {
    try {
        return BigInt(left) > BigInt(right) ? 1 : BigInt(left) < BigInt(right) ? -1 : 0;
    } catch {
        return Number(left) - Number(right);
    }
};

const formatLokiTime = (value) => {
    const date = timestampToDate(value);
    return date.toLocaleString([], {month: "short", day: "2-digit", hour: "2-digit", minute: "2-digit", second: "2-digit"});
};

const timestampToDate = (value) => {
    try {
        return new Date(Number(BigInt(value) / 1000000n));
    } catch {
        return new Date(Number(value) * 1000);
    }
};

const levelClass = (level) => {
    if (level === "ERROR") {
        return "font-bold text-red-300";
    }
    if (level === "WARN") {
        return "font-bold text-amber-300";
    }
    if (level === "INFO") {
        return "font-bold text-emerald-300";
    }
    if (level === "DEBUG") {
        return "font-bold text-sky-300";
    }
    if (level === "TRACE") {
        return "font-bold text-violet-300";
    }
    return "font-bold text-slate-300";
};

export {
    LokiLogsPage
};
