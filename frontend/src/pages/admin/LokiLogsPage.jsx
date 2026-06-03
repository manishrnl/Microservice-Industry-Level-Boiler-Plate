import {
    AlertTriangle,
    Check,
    ChevronDown,
    Palette,
    RefreshCw,
    Search,
    Terminal
} from "lucide-react";
import {useMemo, useState} from "react";
import {useQuery} from "@tanstack/react-query";
import {apiClient} from "../../api/axiosInstance";
import {endpoints} from "../../api/endpoints";
import {PageWrapper} from "../../components/common/PageWrapper";

const fallbackServices = ["api-gateway", "auth-service", "user-service", "notification-service", "payment-service", "file-service", "ai-service", "audit-service", "frontend", "postgres", "redis", "kafka", "grafana", "prometheus", "loki", "promtail"];
const levels = ["", "ERROR", "WARN", "SUCCESS", "INFO", "DEBUG", "TRACE"];
const severityOrder = ["ERROR", "WARN", "SUCCESS", "INFO", "DEBUG", "TRACE", "LOG"];
const selectorBackedLevels = new Set(["ERROR", "WARN", "INFO", "DEBUG", "TRACE"]);
const LOKI_QUERY_LIMIT_MAX = 1000;
const LOKI_DEFAULT_LIMIT = "1000";
const LOKI_DEFAULT_RANGE = String(60 * 60 * 1000);
const defaultFilters = {service: "", level: "", q: "", range: LOKI_DEFAULT_RANGE, limit: LOKI_DEFAULT_LIMIT, from: "", to: ""};
const levelThemes = {
    ERROR: {
        label: "ERROR",
        row: "border-red-500/30 bg-red-950/35 hover:bg-red-950/55",
        badge: "text-red-200",
        card: "border-red-200 bg-red-50 text-red-700 dark:border-red-500/25 dark:bg-red-500/10 dark:text-red-200",
        time: "text-red-200/70",
        service: "text-red-100",
        message: "text-red-50"
    },
    WARN: {
        label: "WARN",
        row: "border-yellow-400/25 bg-yellow-950/25 hover:bg-yellow-950/40",
        badge: "text-yellow-200",
        card: "border-yellow-200 bg-yellow-50 text-yellow-700 dark:border-yellow-400/25 dark:bg-yellow-400/10 dark:text-yellow-100",
        time: "text-yellow-100/70",
        service: "text-yellow-100",
        message: "text-yellow-50"
    },
    SUCCESS: {
        label: "SUCCESS",
        row: "border-emerald-400/25 bg-emerald-950/25 hover:bg-emerald-950/40",
        badge: "text-emerald-200",
        card: "border-emerald-200 bg-emerald-50 text-emerald-700 dark:border-emerald-400/25 dark:bg-emerald-400/10 dark:text-emerald-100",
        time: "text-emerald-100/70",
        service: "text-emerald-100",
        message: "text-emerald-50"
    },
    INFO: {
        label: "INFO",
        row: "border-sky-400/20 bg-sky-950/18 hover:bg-sky-950/30",
        badge: "text-sky-200",
        card: "border-sky-200 bg-sky-50 text-sky-700 dark:border-sky-400/25 dark:bg-sky-400/10 dark:text-sky-100",
        time: "text-sky-100/65",
        service: "text-cyan-100",
        message: "text-slate-100"
    },
    DEBUG: {
        label: "DEBUG",
        row: "border-cyan-400/20 bg-cyan-950/15 hover:bg-cyan-950/25",
        badge: "text-cyan-200",
        card: "border-cyan-200 bg-cyan-50 text-cyan-700 dark:border-cyan-400/25 dark:bg-cyan-400/10 dark:text-cyan-100",
        time: "text-cyan-100/60",
        service: "text-cyan-100",
        message: "text-cyan-50/90"
    },
    TRACE: {
        label: "TRACE",
        row: "border-violet-400/20 bg-violet-950/18 hover:bg-violet-950/30",
        badge: "text-violet-200",
        card: "border-violet-200 bg-violet-50 text-violet-700 dark:border-violet-400/25 dark:bg-violet-400/10 dark:text-violet-100",
        time: "text-violet-100/60",
        service: "text-violet-100",
        message: "text-violet-50/90"
    },
    LOG: {
        label: "LOG",
        row: "border-white/10 bg-slate-950 hover:bg-slate-900",
        badge: "text-slate-300",
        card: "border-slate-200 bg-slate-50 text-slate-700 dark:border-white/10 dark:bg-white/[0.04] dark:text-slate-100",
        time: "text-slate-400",
        service: "text-cyan-200",
        message: "text-slate-100"
    }
};
const lightLevelThemes = {
    ERROR: {
        ...levelThemes.ERROR,
        row: "border-red-200 bg-red-50 hover:bg-red-100/80",
        badge: "text-red-700",
        time: "text-red-500",
        service: "text-red-800",
        message: "text-red-950"
    },
    WARN: {
        ...levelThemes.WARN,
        row: "border-amber-200 bg-amber-50 hover:bg-amber-100/80",
        badge: "text-amber-700",
        time: "text-amber-600",
        service: "text-amber-800",
        message: "text-amber-950"
    },
    SUCCESS: {
        ...levelThemes.SUCCESS,
        row: "border-emerald-200 bg-emerald-50 hover:bg-emerald-100/80",
        badge: "text-emerald-700",
        time: "text-emerald-600",
        service: "text-emerald-800",
        message: "text-emerald-950"
    },
    INFO: {
        ...levelThemes.INFO,
        row: "border-sky-200 bg-sky-50 hover:bg-sky-100/80",
        badge: "text-sky-700",
        time: "text-sky-600",
        service: "text-sky-800",
        message: "text-sky-950"
    },
    DEBUG: {
        ...levelThemes.DEBUG,
        row: "border-cyan-200 bg-cyan-50 hover:bg-cyan-100/80",
        badge: "text-cyan-700",
        time: "text-cyan-600",
        service: "text-cyan-800",
        message: "text-cyan-950"
    },
    TRACE: {
        ...levelThemes.TRACE,
        row: "border-violet-200 bg-violet-50 hover:bg-violet-100/80",
        badge: "text-violet-700",
        time: "text-violet-600",
        service: "text-violet-800",
        message: "text-violet-950"
    }
};
const logThemeOptions = [
    {value: "midnight", label: "Midnight", detail: "Deep dark", swatch: "bg-slate-950"},
    {value: "terminal", label: "Terminal", detail: "Classic green", swatch: "bg-lime-500"},
    {value: "daylight", label: "Daylight", detail: "Clean light", swatch: "bg-sky-200"},
    {value: "cloud", label: "Cloud", detail: "Soft light", swatch: "bg-indigo-200"},
    {value: "ocean", label: "Ocean", detail: "Teal dark", swatch: "bg-teal-500"},
    {value: "neon", label: "Neon", detail: "High contrast", swatch: "bg-fuchsia-500"}
];
const logPanelThemes = {
    midnight: {
        shell: "border-slate-200 shadow-sm dark:border-white/10",
        header: "bg-slate-100 text-slate-500 dark:bg-white/10 dark:text-slate-300",
        body: "bg-slate-950",
        control: "border-white/10 bg-slate-900/95 text-slate-100 ring-white/5",
        empty: "text-slate-400",
        levels: levelThemes,
        log: levelThemes.LOG
    },
    terminal: {
        shell: "border-lime-300/50 shadow-sm shadow-lime-950/20 dark:border-lime-400/30",
        header: "bg-lime-950 text-lime-200 dark:bg-black dark:text-lime-300",
        body: "bg-black",
        control: "border-lime-400/30 bg-black text-lime-200 ring-lime-400/20",
        empty: "text-lime-300/70",
        levels: levelThemes,
        log: {
            ...levelThemes.LOG,
            row: "border-lime-400/15 bg-black hover:bg-lime-950/30",
            badge: "text-lime-200",
            time: "text-lime-300/55",
            service: "text-lime-200",
            message: "text-lime-50"
        }
    },
    daylight: {
        shell: "border-slate-200 shadow-sm shadow-slate-200/60 dark:border-slate-200",
        header: "bg-white text-slate-600",
        body: "bg-white",
        control: "border-slate-200 bg-white text-slate-900 ring-slate-200",
        empty: "text-slate-500",
        levels: lightLevelThemes,
        log: {
            ...levelThemes.LOG,
            row: "border-slate-200 bg-white hover:bg-slate-50",
            badge: "text-slate-700",
            time: "text-slate-500",
            service: "text-sky-700",
            message: "text-slate-950"
        }
    },
    cloud: {
        shell: "border-indigo-200 shadow-sm shadow-indigo-100/70 dark:border-indigo-200",
        header: "bg-indigo-50 text-indigo-800",
        body: "bg-slate-50",
        control: "border-indigo-200 bg-white text-indigo-950 ring-indigo-100",
        empty: "text-indigo-500",
        levels: lightLevelThemes,
        log: {
            ...levelThemes.LOG,
            row: "border-indigo-100 bg-slate-50 hover:bg-indigo-50",
            badge: "text-indigo-700",
            time: "text-slate-500",
            service: "text-indigo-800",
            message: "text-slate-950"
        }
    },
    ocean: {
        shell: "border-teal-300/60 shadow-sm shadow-teal-950/10 dark:border-teal-400/30",
        header: "bg-teal-900 text-teal-50 dark:bg-teal-950 dark:text-teal-100",
        body: "bg-teal-950",
        control: "border-teal-300/40 bg-teal-950 text-teal-50 ring-teal-300/20",
        empty: "text-teal-100/70",
        levels: levelThemes,
        log: {
            ...levelThemes.LOG,
            row: "border-teal-400/15 bg-teal-950 hover:bg-teal-900/70",
            badge: "text-teal-100",
            time: "text-teal-100/60",
            service: "text-cyan-100",
            message: "text-teal-50"
        }
    },
    neon: {
        shell: "border-fuchsia-400/40 shadow-sm shadow-fuchsia-950/20 dark:border-fuchsia-400/35",
        header: "bg-fuchsia-950 text-fuchsia-100 dark:bg-fuchsia-950 dark:text-fuchsia-100",
        body: "bg-[#080914]",
        control: "border-fuchsia-400/35 bg-[#12091f] text-fuchsia-100 ring-fuchsia-400/25",
        empty: "text-fuchsia-200/70",
        levels: levelThemes,
        log: {
            ...levelThemes.LOG,
            row: "border-fuchsia-400/15 bg-[#080914] hover:bg-fuchsia-950/30",
            badge: "text-fuchsia-100",
            time: "text-fuchsia-200/55",
            service: "text-cyan-200",
            message: "text-fuchsia-50"
        }
    }
};
const ranges = [
    {label: "15m", value: String(15 * 60 * 1000)},
    {label: "20m", value: String(20 * 60 * 1000)},
    {label: "1h", value: String(60 * 60 * 1000)},
    {label: "6h", value: String(6 * 60 * 60 * 1000)},
    {label: "24h", value: String(24 * 60 * 60 * 1000)},
    {label: "7d", value: String(7 * 24 * 60 * 60 * 1000)},
    {label: "30d", value: String(30 * 24 * 60 * 60 * 1000)}
];
const quickFilters = [
    {label: "Last 20m", value: {range: String(20 * 60 * 1000), from: "", to: ""}},
    {label: "Last 100", value: {limit: "100"}},
    {label: "Last 1000", value: {limit: "1000"}},
    {label: "401", value: {q: "401", level: ""}},
    {label: "Errors", value: {q: "", level: "ERROR"}},
    {label: "Warnings", value: {q: "", level: "WARN"}},
    {label: "Success", value: {q: "", level: "SUCCESS"}},
    {label: "Info", value: {q: "", level: "INFO"}},
    {label: "Auth", value: {q: "auth", level: ""}},
    {label: "Gateway", value: {service: "api-gateway"}},
    {label: "Startup", value: {q: "Started", level: ""}},
    {label: "Flyway", value: {q: "Flyway", level: ""}}
];
const LokiLogsPage = () => {
    const [filters, setFilters] = useState(defaultFilters);
    const [logTheme, setLogTheme] = useState("midnight");
    const [themeMenuOpen, setThemeMenuOpen] = useState(false);
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
        limit: String(clampLogLimit(filters.limit)),
        direction: "BACKWARD",
        range: filters.range,
        from: filters.from,
        to: filters.to
    }), [filters.from, filters.limit, filters.range, filters.to, logql]);
    const logs = useQuery({
        queryKey: ["loki-logs", queryShape],
        queryFn: async () => {
            const {start, end} = resolveFilterWindow(queryShape);
            const params = {
                query: queryShape.query,
                limit: queryShape.limit,
                direction: queryShape.direction,
                start: start.toISOString(),
                end: end.toISOString()
            };
            return (await apiClient.get(endpoints.observability.lokiQueryRange, {params})).data;
        },
        refetchInterval: filters.from || filters.to ? false : 15000
    });
    const rows = useMemo(() => extractRows(logs.data).slice(0, clampLogLimit(filters.limit)), [filters.limit, logs.data]);
    const levelCounts = useMemo(() => summarizeLevels(rows), [rows]);
    const selectedLogTheme = logPanelTheme(logTheme);
    const selectedLogThemeOption = logThemeOptions.find((theme) => theme.value === logTheme) ?? logThemeOptions[0];
    const searchTerm = filters.q.trim();
    const updateFilter = (field) => (event) => setFilters((current) => ({...current, [field]: event.target.value}));
    const updateRangeFilter = (event) => setFilters((current) => ({...current, range: event.target.value, from: "", to: ""}));
    const applyQuickFilter = (value) => setFilters((current) => ({...current, ...value}));
    const clearFilters = () => setFilters(defaultFilters);

    return <PageWrapper title="Loki Logs">
        <div className="space-y-5">
            <section className="rounded-md border border-slate-200 bg-white p-4 dark:border-white/10 dark:bg-slate-950">
                <div className="flex flex-wrap items-center justify-between gap-3">
                    <div className="flex items-center gap-3">
                        <span className="grid h-10 w-10 place-items-center rounded-md bg-emerald-100 text-emerald-700 ring-1 ring-emerald-200 dark:bg-emerald-500/15 dark:text-emerald-300 dark:ring-emerald-500/25">
                            <Terminal className="h-5 w-5"/>
                        </span>
                        <div>
                            <h2 className="text-sm font-semibold text-slate-950 dark:text-white">Docker and service logs</h2>
                            <p className="text-sm text-slate-500 dark:text-slate-400">{rows.length} entries from Loki - stored on Loki disk for 30 days</p>
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

                <div className="mt-4 grid gap-3 xl:grid-cols-[170px_120px_minmax(220px,1fr)_110px_100px_190px_190px_auto]">
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
                    <Select value={filters.range} onChange={updateRangeFilter} options={ranges.map((range) => range.value)} labels={Object.fromEntries(ranges.map((range) => [range.value, range.label]))} label="Range"/>
                    <Select value={filters.limit} onChange={updateFilter("limit")} options={["50", "100", "200", "500", "1000"]} label="Limit"/>
                    <DateTimeInput label="From" value={filters.from} onChange={updateFilter("from")}/>
                    <DateTimeInput label="To" value={filters.to} onChange={updateFilter("to")}/>
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

                <div className="mt-3 grid gap-2 sm:grid-cols-2 lg:grid-cols-4 xl:grid-cols-7">
                    {severityOrder.map((level) => {
                        const theme = levelTheme(level, logTheme);
                        return <div
                            key={level}
                            className={`rounded-md border px-3 py-2 ${theme.card}`}
                        >
                            <span className="block text-[10px] font-bold uppercase tracking-wide opacity-70">{theme.label}</span>
                            <span className="mt-1 block text-lg font-black leading-none">{levelCounts[level] ?? 0}</span>
                        </div>;
                    })}
                </div>


                <div className={`mt-4 overflow-hidden rounded-md border ${selectedLogTheme.shell}`}>
                    <div className="overflow-x-auto">
                        <div className="min-w-[980px]">
                            <div className={`grid grid-cols-[170px_112px_170px_minmax(520px,1fr)] px-3 py-2 text-xs font-bold uppercase ${selectedLogTheme.header}`}>
                                <span>Time</span>
                                <span>Level</span>
                                <span>Service</span>
                                <span className="flex min-w-0 items-center justify-between gap-3">
                                    <span>Message</span>
                                    <div className="relative ml-auto">
                                        <button
                                            type="button"
                                            onClick={() => setThemeMenuOpen((open) => !open)}
                                            className={`inline-flex h-9 min-w-[156px] shrink-0 items-center justify-between gap-2 rounded-md border px-2.5 normal-case shadow-sm ring-1 transition hover:-translate-y-px hover:shadow-md ${selectedLogTheme.control}`}
                                            aria-haspopup="listbox"
                                            aria-expanded={themeMenuOpen}
                                        >
                                            <span className="inline-flex min-w-0 items-center gap-2">
                                                <Palette className="h-3.5 w-3.5 shrink-0"/>
                                                <span className={`h-2.5 w-2.5 shrink-0 rounded-full ring-1 ring-black/10 ${selectedLogThemeOption.swatch}`}/>
                                                <span className="truncate text-[11px] font-black">{selectedLogThemeOption.label}</span>
                                            </span>
                                            <ChevronDown className={`h-3.5 w-3.5 shrink-0 transition ${themeMenuOpen ? "rotate-180" : ""}`}/>
                                        </button>
                                        {themeMenuOpen && <div
                                            className="absolute right-0 top-10 z-30 w-60 overflow-hidden rounded-md border border-slate-200 bg-white p-1.5 text-slate-950 shadow-xl shadow-slate-950/15 ring-1 ring-black/5 dark:border-white/10 dark:bg-slate-950 dark:text-white"
                                            role="listbox"
                                            aria-label="Log theme"
                                        >
                                            {logThemeOptions.map((theme) => {
                                                const active = theme.value === logTheme;
                                                return <button
                                                    key={theme.value}
                                                    type="button"
                                                    onClick={() => {
                                                        setLogTheme(theme.value);
                                                        setThemeMenuOpen(false);
                                                    }}
                                                    className={`flex w-full items-center gap-3 rounded-md px-2.5 py-2 text-left transition ${active ? "bg-slate-950 text-white shadow-sm dark:bg-white dark:text-slate-950" : "hover:bg-slate-100 dark:hover:bg-white/10"}`}
                                                    role="option"
                                                    aria-selected={active}
                                                >
                                                    <span className={`h-3 w-3 shrink-0 rounded-full ring-1 ring-black/10 ${theme.swatch}`}/>
                                                    <span className="min-w-0 flex-1">
                                                        <span className="block text-xs font-black">{theme.label}</span>
                                                        <span className={`mt-0.5 block text-[11px] font-semibold normal-case ${active ? "text-white/75 dark:text-slate-700" : "text-slate-500 dark:text-slate-400"}`}>{theme.detail}</span>
                                                    </span>
                                                    {active && <Check className="h-4 w-4 shrink-0"/>}
                                                </button>;
                                            })}
                                        </div>}
                                    </div>
                                </span>
                            </div>
                            <div className={`max-h-[620px] overflow-auto font-mono text-xs ${selectedLogTheme.body}`}>
                                {rows.map((row) => {
                                    const theme = levelTheme(row.level, logTheme);
                                    return <div
                                        key={logRowKey(row)}
                                        className={`grid grid-cols-[170px_112px_170px_minmax(520px,1fr)] border-t px-3 py-2 transition-colors ${theme.row}`}
                                    >
                                        <span className={theme.time}>{formatLokiTime(row.timestamp)}</span>
                                        <span className={`text-[11px] font-black tracking-wide ${theme.badge}`}>
                                            {displayLevel(row.level)}
                                        </span>
                                        <span className={`truncate font-semibold ${theme.service}`} title={row.service}>{row.service}</span>
                                        <span className={`min-w-0 ${theme.message}`}>
                                            <LogMessage formatted={row.formattedMessage} search={searchTerm}/>
                                        </span>
                                    </div>;
                                })}
                                {!rows.length && <p className={`px-3 py-8 text-center text-sm ${selectedLogTheme.empty}`}>No matching Loki logs.</p>}
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

const DateTimeInput = ({label, value, onChange}) => <label className="relative block">
    <span className="pointer-events-none absolute left-3 top-1/2 z-10 -translate-y-1/2 text-[10px] font-black uppercase tracking-wide text-slate-400 dark:text-slate-500">{label}</span>
    <input
        type="datetime-local"
        value={value}
        onChange={onChange}
        className="h-10 w-full rounded-md border border-slate-200 bg-white pl-12 pr-2 text-xs font-semibold text-slate-800 outline-none focus:border-slate-400 dark:border-white/10 dark:bg-slate-900 dark:text-white"
    />
</label>;

const LogMessage = ({formatted, search}) => {
    const message = formatted ?? formatLogMessage("");
    return <span className="block min-w-0" title={message.raw}>
        <span className="block break-words text-[12px] leading-5">
            <HighlightedText value={message.summary} search={search}/>
        </span>
        {!!message.details.length && <span className="mt-1 flex flex-wrap gap-1.5">
            {message.details.map((detail) => <span
                key={`${detail.label}-${detail.value}`}
                className="inline-flex max-w-[260px] items-center gap-1 rounded border border-current/20 bg-white/10 px-1.5 py-0.5 text-[10px] font-semibold leading-4 opacity-90"
            >
                <span className="shrink-0 uppercase opacity-60">{detail.label}</span>
                <span className="min-w-0 truncate font-black">
                    <HighlightedText value={detail.value} search={search}/>
                </span>
            </span>)}
        </span>}
    </span>;
};

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
    if (selectorBackedLevels.has(level)) {
        selectors.push(`level="${escapeLabel(level)}"`);
    }
    const filters = [`{${selectors.join(",")}}`];
    if (level === "SUCCESS") {
        filters.push("|~ \"(?i)(success|succeeded|successful|started|completed|healthy|status[=:] ?[23][0-9][0-9]|200 OK|\\\\bUP\\\\b)\"");
    }
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
            const message = parsed.message || String(line ?? "");
            const level = classifyLogLevel(labels.level || parsed.level, `${line ?? ""} ${parsed.message ?? ""}`);
            return {
                index,
                timestamp,
                service: labels.service || labels.container || "unknown",
                level,
                message,
                formattedMessage: formatLogMessage(message),
                labels
            };
        });
    }).sort((a, b) => compareTimestamp(b.timestamp, a.timestamp));
};

const resolveFilterWindow = (filters) => {
    const now = new Date();
    const to = parseDateTimeLocal(filters.to) ?? now;
    const rangeMs = Number(filters.range || LOKI_DEFAULT_RANGE);
    const safeRangeMs = Number.isFinite(rangeMs) && rangeMs > 0 ? rangeMs : Number(LOKI_DEFAULT_RANGE);
    const from = parseDateTimeLocal(filters.from) ?? new Date(to.getTime() - safeRangeMs);
    if (from.getTime() > to.getTime()) {
        return {start: to, end: from};
    }
    return {start: from, end: to};
};

const parseDateTimeLocal = (value) => {
    const text = String(value ?? "").trim();
    if (!text) {
        return null;
    }
    const date = new Date(text);
    return Number.isNaN(date.getTime()) ? null : date;
};

const clampLogLimit = (limit) => {
    const value = Number(limit || LOKI_DEFAULT_LIMIT);
    if (!Number.isFinite(value)) {
        return Number(LOKI_DEFAULT_LIMIT);
    }
    return Math.max(1, Math.min(value, LOKI_QUERY_LIMIT_MAX));
};

const logRowKey = (row) => `${row.timestamp}|${row.service}|${row.message}`;

const parseLogLine = (line) => {
    const value = stripAnsi(line).trim();
    if (!value.startsWith("{")) {
        return {message: value, level: levelFromLine(value)};
    }
    try {
        const parsed = JSON.parse(value);
        const message = parsed.message || parsed.msg || parsed.log || value;
        return {message: stripAnsi(message).trim(), level: parsed.level || parsed.severity};
    } catch {
        return {message: value, level: levelFromLine(value)};
    }
};

const formatLogMessage = (message) => {
    const raw = stripAnsi(message).trim();
    const fields = parseKeyValuePairs(raw);
    const gateway = formatGatewayRequest(raw, fields);
    if (gateway) {
        return gateway;
    }
    if (Object.keys(fields).length >= 2) {
        return formatKeyValueMessage(raw, fields);
    }
    return {
        summary: ensurePeriod(humanizePlainText(raw)),
        details: [],
        raw
    };
};

const formatGatewayRequest = (raw, fields) => {
    if (!raw.includes("gateway_request") && !(fields.method && fields.path && (fields.status || fields.durationMs))) {
        return null;
    }
    const status = raw.match(/\bstatus=(\d{3})(?:\s+([A-Z]+))?/i);
    const statusText = status ? [status[1], status[2]].filter(Boolean).join(" ") : fields.status;
    const duration = fields.durationMs ? `${fields.durationMs}ms` : fields.duration;
    const summary = `${fields.method || "Request"} ${fields.path || "route"} returned ${statusText || "a response"}${duration ? ` in ${duration}` : ""}.`;
    return {
        summary,
        details: compactDetails([
            ["Method", fields.method],
            ["Path", fields.path],
            ["Status", statusText],
            ["Duration", duration],
            ["User", shortenValue(fields.userId, 18)]
        ]),
        raw
    };
};

const formatKeyValueMessage = (raw, fields) => {
    const actor = formatActor(fields);
    let summary;
    if (fields.msg) {
        summary = formatMessageWithContext(actor, fields);
    } else if (fields.caller?.includes("metrics.go") || fields.query_type || fields.returned_lines || fields.total_lines) {
        summary = formatLokiMetric(actor, fields);
    } else if (fields.status || fields.duration) {
        summary = `${actor} completed${fields.query_type ? ` ${humanizeToken(fields.query_type)}` : ""}${fields.duration ? ` in ${fields.duration}` : ""}${fields.status ? ` with status ${fields.status}` : ""}.`;
    } else {
        summary = ensurePeriod(humanizePlainText(raw.split(/\s+/).slice(0, 14).join(" ")));
    }
    return {
        summary: ensurePeriod(summary),
        details: buildReadableDetails(fields),
        raw
    };
};

const formatMessageWithContext = (actor, fields) => {
    const cleanMessage = humanizePlainText(fields.msg);
    if (/executing query/i.test(cleanMessage)) {
        const queryKind = fields.type || fields.query_type || "log";
        return `${actor} started ${humanizeToken(queryKind)} query${fields.length ? ` for ${fields.length}` : ""}${fields.step ? ` with ${fields.step} steps` : ""}.`;
    }
    if (/completed recalculate owned streams job/i.test(cleanMessage)) {
        return `${actor} completed recalculating owned streams.`;
    }
    return `${actor} ${lowerFirst(cleanMessage)}`;
};

const formatLokiMetric = (actor, fields) => {
    const queryType = humanizeToken(fields.query_type || fields.type || "log");
    if (fields.query_type === "labels") {
        return `${actor} loaded ${fields.label || "labels"} in ${fields.duration || "a short time"}${fields.status ? ` with status ${fields.status}` : ""}.`;
    }
    if (fields.query_type === "stats") {
        return `${actor} calculated log stats in ${fields.duration || "a short time"}${fields.status ? ` with status ${fields.status}` : ""}.`;
    }
    const returned = fields.returned_lines ?? fields.total_entries;
    const scanned = fields.total_lines;
    const lineSummary = returned || scanned
        ? ` Returned ${formatNumber(returned ?? 0)}${scanned ? ` of ${formatNumber(scanned)} scanned lines` : " entries"}.`
        : "";
    return `${actor} completed ${queryType} query${fields.duration ? ` in ${fields.duration}` : ""}${fields.status ? ` with status ${fields.status}` : ""}.${lineSummary}`;
};

const buildReadableDetails = (fields) => compactDetails([
    ["Status", fields.status],
    ["Duration", fields.duration || (fields.durationMs ? `${fields.durationMs}ms` : "")],
    ["Returned", fields.returned_lines],
    ["Total", fields.total_lines],
    ["Type", fields.query_type || fields.type],
    ["Range", fields.length],
    ["Step", fields.step],
    ["Latency", fields.latency],
    ["Path", fields.path],
    ["Method", fields.method],
    ["Throughput", fields.throughput],
    ["Caller", fields.caller],
    ["Trace", shortenValue(fields.traceID || fields.traceId, 18)]
], 8);

const parseKeyValuePairs = (value) => {
    const fields = {};
    const matcher = /([A-Za-z_][\w.-]*)=("(?:\\.|[^"])*"|[^\s]*)/g;
    let match = matcher.exec(value);
    while (match) {
        fields[match[1]] = cleanFieldValue(match[2]);
        match = matcher.exec(value);
    }
    return fields;
};

const cleanFieldValue = (value) => {
    const text = String(value ?? "");
    const unquoted = text.startsWith("\"") && text.endsWith("\"") ? text.slice(1, -1) : text;
    return unquoted.replace(/\\"/g, "\"").replace(/\\\\/g, "\\").trim();
};

const formatActor = (fields) => {
    const source = fields.component || sourceFromCaller(fields.caller) || "service";
    const actor = humanizeToken(source);
    return isLokiMetric(fields) ? `Loki ${actor}` : actor;
};

const sourceFromCaller = (caller) => String(caller ?? "").split(":")[0].replace(/\.[^.]+$/, "");

const isLokiMetric = (fields) => Boolean(fields.org_id || fields.query_hash || fields.caller?.endsWith(".go") || fields.caller?.includes(".go:"));

const humanizePlainText = (value) => String(value ?? "")
    .replace(/^\d{4}-\d{2}-\d{2}T\S+\s+/, "")
    .replace(/\s+/g, " ")
    .trim();

const humanizeToken = (value) => String(value ?? "")
    .replace(/[_-]+/g, " ")
    .replace(/\b\w/g, (letter) => letter.toUpperCase())
    .trim();

const lowerFirst = (value) => {
    const text = String(value ?? "").trim();
    return text ? `${text.charAt(0).toLowerCase()}${text.slice(1)}` : text;
};

const ensurePeriod = (value) => {
    const text = String(value ?? "").trim();
    return /[.!?]$/.test(text) ? text : `${text}.`;
};

const compactDetails = (items, limit = 8) => items
    .filter(([, value]) => value !== undefined && value !== null && String(value).trim() !== "")
    .slice(0, limit)
    .map(([label, value]) => ({label, value: String(value)}));

const shortenValue = (value, maxLength) => {
    const text = String(value ?? "").trim();
    if (text.length <= maxLength) {
        return text;
    }
    return `${text.slice(0, maxLength - 1)}...`;
};

const formatNumber = (value) => {
    const number = Number(value);
    return Number.isFinite(number) ? number.toLocaleString() : String(value ?? "");
};

const stripAnsi = (value) => String(value ?? "").replace(/\x1B(?:[@-Z\\-_]|\[[0-?]*[ -/]*[@-~])/g, "");

const classifyLogLevel = (explicitLevel, line) => {
    const normalized = normalizeLevel(explicitLevel);
    const text = stripAnsi(line);
    if (/\b(ERROR|SEVERE|FATAL)\b/i.test(text) || /status[=:]\s*5\d\d/i.test(text) || /\b(exception|failed|failure|unhealthy|timeout|refused)\b/i.test(text)) {
        return "ERROR";
    }
    if (/\b(WARN|WARNING)\b/i.test(text) || /status[=:]\s*4\d\d/i.test(text)) {
        return "WARN";
    }
    if (/\b(success|succeeded|successful|started|completed|healthy)\b/i.test(text) || /status[=:]\s*[23]\d\d/i.test(text) || /\b200\s+OK\b/i.test(text) || /\bUP\b/.test(text)) {
        return "SUCCESS";
    }
    return normalized || levelFromLine(text);
};

const levelFromLine = (line) => {
    const match = String(line ?? "").match(/\b(ERROR|SEVERE|FATAL|WARN|WARNING|INFO|DEBUG|TRACE)\b/i);
    return normalizeLevel(match?.[1]) ?? "";
};

const normalizeLevel = (level) => {
    const value = String(level ?? "").trim().toUpperCase();
    if (!value) {
        return "";
    }
    if (["ERROR", "SEVERE", "FATAL"].includes(value)) {
        return "ERROR";
    }
    if (["WARN", "WARNING"].includes(value)) {
        return "WARN";
    }
    if (["SUCCESS", "OK", "UP"].includes(value)) {
        return "SUCCESS";
    }
    if (["INFO", "DEBUG", "TRACE"].includes(value)) {
        return value;
    }
    return "";
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

const logPanelTheme = (theme) => logPanelThemes[theme] ?? logPanelThemes.midnight;

const levelTheme = (level, logTheme = "midnight") => {
    const panelTheme = logPanelTheme(logTheme);
    const normalized = normalizeLevel(level);
    if (!normalized) {
        return panelTheme.log;
    }
    return panelTheme.levels?.[normalized] ?? levelThemes[normalized] ?? panelTheme.log;
};

const displayLevel = (level) => levelTheme(level).label;

const summarizeLevels = (rows) => rows.reduce((counts, row) => {
    const level = normalizeLevel(row.level) || "LOG";
    counts[level] = (counts[level] ?? 0) + 1;
    return counts;
}, Object.fromEntries(severityOrder.map((level) => [level, 0])));

export {
    LokiLogsPage
};
