import {Activity, BarChart3, ExternalLink, Gauge, RadioTower, RefreshCw, Search, ServerCog, ShieldCheck, Terminal} from "lucide-react";
import {useMemo, useState} from "react";
import {useQuery} from "@tanstack/react-query";
import {apiClient} from "../../api/axiosInstance";
import {endpoints} from "../../api/endpoints";
import {env} from "../../config/env";
import {PageWrapper} from "../../components/common/PageWrapper";
import {unwrapApiData} from "../../utils/responseUtils";

const services = ["", "gateway", "auth-service", "user-service", "notification-service", "payment-service", "file-service", "ai-service", "audit-service"];
const levels = ["", "ERROR", "WARN", "INFO"];
const categories = ["", "RUNTIME", "SECURITY", "BUILD"];
const quickFilters = [
    {label: "401", value: {status: "401", level: "", category: ""}},
    {label: "Errors", value: {level: "ERROR", status: "", category: ""}},
    {label: "Info", value: {level: "INFO", status: "", category: ""}},
    {label: "Security", value: {category: "SECURITY", status: "", level: ""}},
    {label: "Build", value: {category: "BUILD", status: "", level: ""}}
];

const observabilityLinks = [
    {label: "Grafana", href: env.grafanaUrl, Icon: BarChart3},
    {label: "Grafana Explore Logs", href: `${env.grafanaUrl}/explore?left=%7B%22datasource%22:%22Loki%22,%22queries%22:%5B%7B%22expr%22:%22%7Bcompose_project%3D%5C%22microservice-industry%5C%22%7D%22%7D%5D%7D`, Icon: Terminal},
    {label: "Prometheus", href: env.prometheusUrl, Icon: Activity},
    {label: "Loki API", href: env.lokiUrl, Icon: Terminal},
    {label: "Zipkin", href: env.zipkinUrl, Icon: RadioTower},
    {label: "Eureka", href: env.discoveryUrl, Icon: ServerCog},
    {label: "Gateway Health", href: env.gatewayHealthUrl, Icon: ShieldCheck},
    {label: "Gateway Metrics", href: env.gatewayMetricsUrl, Icon: Gauge},
    {label: "Config Server", href: env.configServerUrl, Icon: ServerCog}
];

const ObservabilityPage = () => {
    const [filters, setFilters] = useState({q: "", level: "", category: "", service: "", status: "", limit: "200"});
    const params = useMemo(() => Object.fromEntries(Object.entries(filters).filter(([, value]) => String(value).trim())), [filters]);
    const logs = useQuery({
        queryKey: ["observability-logs", params],
        queryFn: async () => unwrapApiData((await apiClient.get(endpoints.observability.logs, {params})).data),
        refetchInterval: 10000
    });
    const entries = logs.data?.items ?? [];
    const updateFilter = (field) => (event) => setFilters((value) => ({...value, [field]: event.target.value}));
    const applyQuickFilter = (value) => setFilters((current) => ({...current, ...value}));

    return <PageWrapper title="Observability">
        <div className="space-y-5">
            <section className="rounded-md border border-slate-200 bg-white p-4 dark:border-white/10 dark:bg-slate-950">
                <div className="flex flex-wrap items-center justify-between gap-3">
                    <div className="flex items-center gap-2">
                        <span className="grid h-10 w-10 place-items-center rounded-md bg-slate-100 text-slate-700 dark:bg-white/10 dark:text-teal-300">
                            <Terminal className="h-5 w-5"/>
                        </span>
                        <div>
                            <h2 className="text-sm font-semibold text-slate-950 dark:text-white">Log Center</h2>
                            <p className="text-sm text-slate-500 dark:text-slate-400">Gateway runtime logs available to admin roles.</p>
                        </div>
                    </div>
                    <button
                        type="button"
                        onClick={() => logs.refetch()}
                        className="inline-flex h-10 items-center gap-2 rounded-md border border-slate-200 px-3 text-sm font-medium text-slate-700 hover:bg-slate-50 dark:border-white/10 dark:text-slate-100 dark:hover:bg-white/10"
                    >
                        <RefreshCw className={`h-4 w-4 ${logs.isFetching ? "animate-spin" : ""}`}/>
                        Refresh
                    </button>
                </div>
                <div className="mt-4 grid gap-3 xl:grid-cols-[minmax(220px,1fr)_120px_130px_170px_110px_100px]">
                    <label className="relative block">
                        <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400"/>
                        <input
                            value={filters.q}
                            onChange={updateFilter("q")}
                            placeholder="Search logs, paths, user IDs, services"
                            className="h-10 w-full rounded-md border border-slate-200 bg-white pl-9 pr-3 text-sm outline-none focus:border-slate-400 dark:border-white/10 dark:bg-slate-900 dark:text-white"
                        />
                    </label>
                    <Select value={filters.level} onChange={updateFilter("level")} options={levels} label="All levels"/>
                    <Select value={filters.category} onChange={updateFilter("category")} options={categories} label="All logs"/>
                    <Select value={filters.service} onChange={updateFilter("service")} options={services} label="All services"/>
                    <input
                        value={filters.status}
                        onChange={updateFilter("status")}
                        placeholder="Status"
                        inputMode="numeric"
                        className="h-10 rounded-md border border-slate-200 bg-white px-3 text-sm outline-none focus:border-slate-400 dark:border-white/10 dark:bg-slate-900 dark:text-white"
                    />
                    <Select value={filters.limit} onChange={updateFilter("limit")} options={["50", "100", "200", "500"]} label="Limit"/>
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
                <div className="mt-4 overflow-hidden rounded-md border border-slate-200 dark:border-white/10">
                    <div className="grid grid-cols-[150px_82px_100px_150px_86px_minmax(260px,1fr)] bg-slate-100 px-3 py-2 text-xs font-bold uppercase text-slate-500 dark:bg-white/10 dark:text-slate-300">
                        <span>Time</span>
                        <span>Level</span>
                        <span>Status</span>
                        <span>Service</span>
                        <span>Method</span>
                        <span>Message</span>
                    </div>
                    <div className="max-h-[520px] overflow-auto bg-slate-950 font-mono text-xs text-slate-100">
                        {entries.map((entry, index) => <div
                            key={`${entry.timestamp}-${index}`}
                            className="grid grid-cols-[150px_82px_100px_150px_86px_minmax(260px,1fr)] gap-0 border-t border-white/10 px-3 py-2"
                        >
                            <span className="text-slate-400">{formatTime(entry.timestamp)}</span>
                            <span className={levelClass(entry.level)}>{entry.level}</span>
                            <span className="text-slate-300">{entry.status ?? ""}</span>
                            <span className="text-cyan-200">{entry.service}</span>
                            <span className="text-slate-300">{entry.method ?? ""}</span>
                            <span className="break-all text-slate-100">{entry.message}</span>
                        </div>)}
                        {!entries.length && <p className="px-3 py-8 text-center text-sm text-slate-400">No matching logs.</p>}
                    </div>
                </div>
                {logs.isError && <p className="mt-3 text-sm text-red-600 dark:text-red-300">Could not load logs. Admin or super-admin access is required.</p>}
            </section>

            <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
                {observabilityLinks.map(({label, href, Icon}) => <a
                    key={label}
                    href={href}
                    target="_blank"
                    rel="noreferrer"
                    className="group rounded-md border border-slate-200 bg-white p-4 transition hover:-translate-y-0.5 hover:border-slate-300 hover:shadow-lg dark:border-white/10 dark:bg-slate-950 dark:hover:border-white/20"
                >
                    <span className="flex items-start justify-between gap-4">
                        <span className="grid h-10 w-10 place-items-center rounded-md bg-slate-100 text-slate-700 dark:bg-white/10 dark:text-teal-300">
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

const Select = ({value, onChange, options, label}) => <select
    value={value}
    onChange={onChange}
    className="h-10 rounded-md border border-slate-200 bg-white px-3 text-sm outline-none focus:border-slate-400 dark:border-white/10 dark:bg-slate-900 dark:text-white"
>
    {options.map((option) => <option key={option || label} value={option}>{option || label}</option>)}
</select>;

const formatTime = (value) => {
    if (!value) {
        return "";
    }
    return new Date(value).toLocaleTimeString([], {hour: "2-digit", minute: "2-digit", second: "2-digit"});
};

const levelClass = (level) => {
    if (level === "ERROR") {
        return "font-bold text-red-300";
    }
    if (level === "WARN") {
        return "font-bold text-amber-300";
    }
    return "font-bold text-emerald-300";
};

export {
    ObservabilityPage
};
