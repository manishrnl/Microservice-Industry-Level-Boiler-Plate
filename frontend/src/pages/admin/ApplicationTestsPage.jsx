import {useEffect, useMemo, useRef, useState} from "react";
import toast from "react-hot-toast";
import {
    Activity,
    AlertTriangle,
    CheckCircle2,
    Database,
    Gauge,
    Play,
    RotateCcw,
    Server,
    ShieldCheck,
    Square,
    Timer,
    XCircle,
    Zap
} from "lucide-react";
import {PageWrapper} from "../../components/common/PageWrapper";
import {endpoints} from "../../api/endpoints";
import {env} from "../../config/env";
import {useAuthStore} from "../../store/authStore";
import {
    DEFAULT_APPLICATION_TEST_OPTIONS,
    formatMetric,
    interpretApplicationLoadTest,
    runApplicationLoadTest
} from "../../utils/applicationLoadTest";

const inputClass = "w-full rounded-md border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-950 outline-none transition focus:border-teal-500 focus:ring-2 focus:ring-teal-500/20 dark:border-white/10 dark:bg-slate-950 dark:text-white";
const labelClass = "text-xs font-bold uppercase tracking-[0.12em] text-slate-500 dark:text-slate-400";

const localServiceUrl = (port, path = "/actuator/health") => {
    if (typeof window === "undefined") {
        return `http://127.0.0.1:${port}${path}`;
    }
    return `${window.location.protocol}//${window.location.hostname}:${port}${path}`;
};

const buildTargetPresets = () => [
    {group: "Gateway", label: "Gateway health", url: `${env.apiGatewayUrl}/actuator/health`, method: "GET", auth: false},
    {group: "Gateway", label: "Auth JWKS through gateway", url: endpoints.auth.jwks, method: "GET", auth: false},
    {group: "Auth", label: "DB ping through gateway", url: `${endpoints.auth.base}/db-ping`, method: "GET", auth: false},
    {group: "Auth", label: "DB stats through gateway", url: `${endpoints.auth.base}/db-stats`, method: "GET", auth: false},
    {group: "Auth", label: "Current session", url: endpoints.auth.me, method: "GET", auth: true},
    {group: "Users", label: "Admin users", url: endpoints.users.list, method: "GET", auth: true},
    {group: "Audit", label: "Audit stream", url: `${endpoints.audit.list}?page=0&size=10`, method: "GET", auth: true},
    {group: "Ops", label: "Observability logs", url: `${endpoints.observability.logs}?limit=25`, method: "GET", auth: true},
    {group: "Direct service", label: "Auth service health", url: localServiceUrl("8081"), method: "GET", auth: false},
    {group: "Direct service", label: "User service health", url: localServiceUrl("8082"), method: "GET", auth: false},
    {group: "Direct service", label: "Notification service health", url: localServiceUrl("8083"), method: "GET", auth: false},
    {group: "Direct service", label: "Payment service health", url: localServiceUrl("8084"), method: "GET", auth: false},
    {group: "Direct service", label: "File service health", url: localServiceUrl("8085"), method: "GET", auth: false},
    {group: "Direct service", label: "AI service health", url: localServiceUrl("8086"), method: "GET", auth: false},
    {group: "Direct service", label: "Audit service health", url: localServiceUrl("8087"), method: "GET", auth: false}
];

const ApplicationTestsPage = () => {
    const accessToken = useAuthStore((state) => state.accessToken);
    const targetPresets = useMemo(buildTargetPresets, []);
    const [form, setForm] = useState(() => ({
        ...DEFAULT_APPLICATION_TEST_OPTIONS,
        url: targetPresets[0].url,
        attachAccessToken: targetPresets[0].auth,
        method: targetPresets[0].method
    }));
    const [result, setResult] = useState(null);
    const [error, setError] = useState("");
    const [running, setRunning] = useState(false);
    const abortRef = useRef(null);

    useEffect(() => () => abortRef.current?.abort(), []);

    const updateForm = (field, value) => {
        setForm((current) => ({...current, [field]: value}));
    };

    const applyPreset = (preset) => {
        setForm((current) => ({
            ...current,
            url: preset.url,
            method: preset.method,
            attachAccessToken: preset.auth
        }));
    };

    const reset = () => {
        abortRef.current?.abort();
        setRunning(false);
        setError("");
        setResult(null);
        setForm({
            ...DEFAULT_APPLICATION_TEST_OPTIONS,
            url: targetPresets[0].url,
            attachAccessToken: targetPresets[0].auth,
            method: targetPresets[0].method
        });
    };

    const stop = () => {
        abortRef.current?.abort();
        setRunning(false);
    };

    const run = async () => {
        if (running) {
            return;
        }
        const controller = new AbortController();
        abortRef.current = controller;
        setRunning(true);
        setError("");
        setResult(null);
        try {
            const finalResult = await runApplicationLoadTest(form, {
                accessToken,
                signal: controller.signal,
                onUpdate: setResult
            });
            if (!controller.signal.aborted) {
                toast.success(`testsApp completed: ${formatMetric(finalResult.hitRatio)}% hit ratio`);
            }
        } catch (exception) {
            setError(exception.message || "Application test could not run.");
            toast.error(exception.message || "Application test could not run.");
        } finally {
            if (abortRef.current === controller) {
                abortRef.current = null;
            }
            setRunning(false);
        }
    };

    const progress = result
        ? Math.min(100, (result.elapsedSeconds / Number(form.durationSeconds || 1)) * 100)
        : 0;
    const passFailRows = [
        {label: "Passed", value: result?.passed ?? 0, tone: "bg-emerald-500"},
        {label: "Failed", value: result?.failed ?? 0, tone: "bg-rose-500"},
        {label: "Dropped", value: result?.droppedByConcurrency ?? 0, tone: "bg-amber-500"}
    ];
    const statusRows = Object.entries(result?.statusCounts ?? {}).map(([label, value]) => ({
        label,
        value,
        tone: Number(label) >= 500 ? "bg-rose-500" : Number(label) >= 400 ? "bg-amber-500" : "bg-teal-500"
    }));
    const rpsRows = (result?.perSecond ?? []).slice(-20).map((row) => ({
        label: row.label,
        value: row.sent,
        tone: "bg-sky-500"
    }));
    const interpretationRows = interpretApplicationLoadTest(result, form);

    return <PageWrapper title="testsApp">
        <div className="space-y-5">
            <section className="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-950">
                <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                    <div className="min-w-0">
                        <div className="inline-flex items-center gap-2 rounded-md bg-teal-50 px-3 py-1 text-xs font-bold uppercase tracking-[0.16em] text-teal-700 ring-1 ring-teal-200 dark:bg-teal-400/10 dark:text-teal-200 dark:ring-teal-400/20">
                            <Gauge className="h-3.5 w-3.5"/>
                            testsApp
                        </div>
                        <div className="mt-4 rounded-md border border-slate-200 bg-slate-50 p-4 text-sm font-medium text-slate-700 dark:border-white/10 dark:bg-white/[0.03] dark:text-slate-200">
                            Test applications speed, latency, throughput, status mix, timeout behavior, and auth-protected API readiness from the admin dashboard.
                        </div>
                    </div>
                    <div className="flex shrink-0 flex-wrap gap-2">
                        <button
                            type="button"
                            onClick={run}
                            disabled={running}
                            className="inline-flex h-10 items-center gap-2 rounded-md bg-slate-950 px-4 text-sm font-bold text-white shadow-sm transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60 dark:bg-teal-300 dark:text-slate-950 dark:hover:bg-teal-200"
                        >
                            <Play className="h-4 w-4"/>
                            Run
                        </button>
                        <button
                            type="button"
                            onClick={stop}
                            disabled={!running}
                            className="inline-flex h-10 items-center gap-2 rounded-md border border-slate-200 bg-white px-4 text-sm font-bold text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50 dark:border-white/10 dark:bg-white/10 dark:text-white dark:hover:bg-white/15"
                        >
                            <Square className="h-4 w-4"/>
                            Stop
                        </button>
                        <button
                            type="button"
                            onClick={reset}
                            className="grid h-10 w-10 place-items-center rounded-md border border-slate-200 bg-white text-slate-700 transition hover:bg-slate-50 dark:border-white/10 dark:bg-white/10 dark:text-white dark:hover:bg-white/15"
                            aria-label="Reset testsApp form"
                            title="Reset"
                        >
                            <RotateCcw className="h-4 w-4"/>
                        </button>
                    </div>
                </div>
                <div className="mt-5 h-2 overflow-hidden rounded-full bg-slate-100 dark:bg-white/10">
                    <div
                        className={`h-full rounded-full transition-all ${running ? "bg-teal-500" : "bg-slate-400 dark:bg-slate-500"}`}
                        style={{width: `${progress}%`}}
                    />
                </div>
            </section>

            <section className="grid gap-4 xl:grid-cols-[minmax(0,1.2fr)_minmax(360px,0.8fr)]">
                <div className="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-950">
                    <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                        <Field label="Target URL" className="md:col-span-2 xl:col-span-3">
                            <input className={inputClass} value={form.url} onChange={(event) => updateForm("url", event.target.value)}/>
                        </Field>
                        <Field label="Method">
                            <select className={inputClass} value={form.method} onChange={(event) => updateForm("method", event.target.value)}>
                                {["GET", "POST", "PUT", "PATCH", "DELETE"].map((method) => <option key={method} value={method}>{method}</option>)}
                            </select>
                        </Field>
                        <Field label="Duration seconds">
                            <input className={inputClass} type="number" min="1" max="120" value={form.durationSeconds} onChange={(event) => updateForm("durationSeconds", event.target.value)}/>
                        </Field>
                        <Field label="Concurrency">
                            <input className={inputClass} type="number" min="1" max="500" value={form.concurrency} onChange={(event) => updateForm("concurrency", event.target.value)}/>
                        </Field>
                        <Field label="Target RPS">
                            <input className={inputClass} type="number" min="0" max="10000" value={form.rps} onChange={(event) => updateForm("rps", event.target.value)}/>
                        </Field>
                        <Field label="Timeout ms">
                            <input className={inputClass} type="number" min="100" value={form.timeoutMs} onChange={(event) => updateForm("timeoutMs", event.target.value)}/>
                        </Field>
                        <Field label="Success range">
                            <div className="grid grid-cols-2 gap-2">
                                <input className={inputClass} type="number" min="100" max="599" value={form.successMin} onChange={(event) => updateForm("successMin", event.target.value)}/>
                                <input className={inputClass} type="number" min="100" max="599" value={form.successMax} onChange={(event) => updateForm("successMax", event.target.value)}/>
                            </div>
                        </Field>
                        <Field label="Request body" className="md:col-span-2 xl:col-span-3">
                            <textarea className={`${inputClass} min-h-24 resize-y font-mono text-xs`} value={form.body} onChange={(event) => updateForm("body", event.target.value)} placeholder='{"sample":true}'/>
                        </Field>
                        <Field label="Extra headers" className="md:col-span-2 xl:col-span-3">
                            <textarea className={`${inputClass} min-h-20 resize-y font-mono text-xs`} value={form.headersText} onChange={(event) => updateForm("headersText", event.target.value)} placeholder="X-Test-Run: dashboard"/>
                        </Field>
                    </div>
                    <div className="mt-4 flex flex-wrap gap-3">
                        <Toggle
                            label="Attach access token"
                            checked={form.attachAccessToken}
                            onChange={(checked) => updateForm("attachAccessToken", checked)}
                        />
                        <Toggle
                            label="Send credentials"
                            checked={form.includeCredentials}
                            onChange={(checked) => updateForm("includeCredentials", checked)}
                        />
                    </div>
                </div>

                <div className="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-950">
                    <div className="flex items-center gap-2 text-sm font-bold text-slate-950 dark:text-white">
                        <Zap className="h-4 w-4 text-teal-600 dark:text-teal-300"/>
                        Test targets
                    </div>
                    <div className="mt-4 grid gap-2">
                        {targetPresets.map((preset) => <button
                            key={preset.label}
                            type="button"
                            onClick={() => applyPreset(preset)}
                            className={`flex items-center justify-between gap-3 rounded-md border px-3 py-3 text-left transition ${form.url === preset.url ? "border-slate-950 bg-slate-950 text-white dark:border-teal-300 dark:bg-teal-300 dark:text-slate-950" : "border-slate-200 bg-white text-slate-700 hover:border-slate-300 hover:bg-slate-50 dark:border-white/10 dark:bg-white/[0.03] dark:text-slate-200 dark:hover:bg-white/10"}`}
                        >
                            <span className="min-w-0">
                                <span className="flex items-center gap-2">
                                    <span className={`grid h-6 w-6 shrink-0 place-items-center rounded-md ${preset.group === "Direct service" ? "bg-sky-100 text-sky-700 dark:bg-sky-500/15 dark:text-sky-200" : preset.group === "Auth" ? "bg-indigo-100 text-indigo-700 dark:bg-indigo-500/15 dark:text-indigo-200" : "bg-teal-100 text-teal-700 dark:bg-teal-500/15 dark:text-teal-200"}`}>
                                        {preset.group === "Direct service" ? <Server className="h-3.5 w-3.5"/> : preset.group === "Auth" ? <Database className="h-3.5 w-3.5"/> : <Gauge className="h-3.5 w-3.5"/>}
                                    </span>
                                    <span className="min-w-0">
                                        <span className="block text-[10px] font-black uppercase tracking-[0.12em] opacity-70">{preset.group}</span>
                                        <span className="block text-sm font-bold">{preset.label}</span>
                                    </span>
                                </span>
                                <span className="mt-1 block truncate text-xs opacity-75">{preset.url}</span>
                            </span>
                            {preset.auth ? <ShieldCheck className="h-4 w-4 shrink-0"/> : <Activity className="h-4 w-4 shrink-0"/>}
                        </button>)}
                    </div>
                    {error && <div className="mt-4 flex gap-2 rounded-md border border-rose-200 bg-rose-50 p-3 text-sm font-semibold text-rose-700 dark:border-rose-500/30 dark:bg-rose-500/10 dark:text-rose-200">
                        <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0"/>
                        <span>{error}</span>
                    </div>}
                </div>
            </section>

            <section className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
                <MetricTile label="Verdict" value={result?.verdict ?? (running ? "RUNNING" : "IDLE")} Icon={verdictIcon(result?.verdict, running)} tone={verdictTone(result?.verdict, running)}/>
                <MetricTile label="Hit ratio" value={`${formatMetric(result?.hitRatio)}%`} Icon={CheckCircle2} tone="text-emerald-700 bg-emerald-50 ring-emerald-200 dark:text-emerald-200 dark:bg-emerald-500/10 dark:ring-emerald-500/20"/>
                <MetricTile label="p95 latency" value={`${formatMetric(result?.p95LatencyMs)}ms`} Icon={Timer} tone="text-sky-700 bg-sky-50 ring-sky-200 dark:text-sky-200 dark:bg-sky-500/10 dark:ring-sky-500/20"/>
                <MetricTile label="Passed/sec" value={formatMetric(result?.averagePassedPerSecond)} Icon={Gauge} tone="text-indigo-700 bg-indigo-50 ring-indigo-200 dark:text-indigo-200 dark:bg-indigo-500/10 dark:ring-indigo-500/20"/>
            </section>

            <section className="grid gap-4 xl:grid-cols-3">
                <BarPanel title="Pass fail" rows={passFailRows}/>
                <BarPanel title="HTTP status" rows={statusRows}/>
                <BarPanel title="Latency buckets" rows={(result?.latencyBuckets ?? []).map((row) => ({...row, tone: "bg-cyan-500"}))}/>
            </section>

            <section className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_360px]">
                <BarPanel title="Requests per second" rows={rpsRows}/>
                <div className="space-y-4">
                    <div className="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-950">
                        <div className="text-sm font-bold text-slate-950 dark:text-white">Run details</div>
                        <dl className="mt-4 grid grid-cols-2 gap-3 text-sm">
                            <Detail label="Sent" value={result?.sent ?? 0}/>
                            <Detail label="Received" value={result?.received ?? 0}/>
                            <Detail label="Failed" value={result?.failed ?? 0}/>
                            <Detail label="Timeouts" value={result?.timeoutErrors ?? 0}/>
                            <Detail label="Network errors" value={result?.networkErrors ?? 0}/>
                            <Detail label="Dropped" value={result?.droppedByConcurrency ?? 0}/>
                            <Detail label="Bytes" value={result?.bytesReceived ?? 0}/>
                            <Detail label="Elapsed" value={`${formatMetric(result?.elapsedSeconds)}s`}/>
                        </dl>
                        {result?.verdictReason && <div className="mt-4 rounded-md bg-slate-50 p-3 text-sm font-medium text-slate-700 dark:bg-white/[0.04] dark:text-slate-200">
                            {result.verdictReason}
                        </div>}
                        {result?.errorSamples?.length > 0 && <div className="mt-4 space-y-2">
                            <div className="text-xs font-bold uppercase tracking-[0.12em] text-slate-500 dark:text-slate-400">Error samples</div>
                            {result.errorSamples.map((sample) => <div key={sample} className="rounded-md border border-rose-200 bg-rose-50 p-2 text-xs font-semibold text-rose-700 dark:border-rose-500/30 dark:bg-rose-500/10 dark:text-rose-200">
                                {sample}
                            </div>)}
                        </div>}
                    </div>
                    <DiagnosticPanel rows={interpretationRows}/>
                </div>
            </section>
        </div>
    </PageWrapper>;
};

const Field = ({label, className = "", children}) => <label className={`block min-w-0 ${className}`}>
    <span className={labelClass}>{label}</span>
    <span className="mt-1.5 block">{children}</span>
</label>;

const Toggle = ({label, checked, onChange}) => <label className="inline-flex cursor-pointer items-center gap-2 rounded-md border border-slate-200 bg-white px-3 py-2 text-sm font-bold text-slate-700 dark:border-white/10 dark:bg-white/[0.03] dark:text-slate-200">
    <input
        type="checkbox"
        checked={checked}
        onChange={(event) => onChange(event.target.checked)}
        className="h-4 w-4 rounded border-slate-300 text-teal-600 focus:ring-teal-500"
    />
    {label}
</label>;

const MetricTile = ({label, value, Icon, tone}) => <div className="rounded-md border border-slate-200 bg-white p-4 shadow-sm dark:border-white/10 dark:bg-slate-950">
    <div className="flex items-center justify-between gap-3">
        <span className="text-xs font-bold uppercase tracking-[0.12em] text-slate-500 dark:text-slate-400">{label}</span>
        <span className={`grid h-9 w-9 place-items-center rounded-md ring-1 ${tone}`}>
            <Icon className="h-4 w-4"/>
        </span>
    </div>
    <div className="mt-3 truncate text-2xl font-semibold text-slate-950 dark:text-white">{value}</div>
</div>;

const BarPanel = ({title, rows}) => {
    const max = Math.max(1, ...rows.map((row) => Number(row.value) || 0));
    return <div className="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-950">
        <div className="text-sm font-bold text-slate-950 dark:text-white">{title}</div>
        <div className="mt-4 space-y-3">
            {rows.length === 0 && <div className="rounded-md bg-slate-50 p-3 text-sm font-medium text-slate-500 dark:bg-white/[0.04] dark:text-slate-400">No samples yet</div>}
            {rows.map((row) => <div key={row.label} className="grid grid-cols-[88px_minmax(0,1fr)_56px] items-center gap-3 text-sm">
                <div className="truncate font-semibold text-slate-600 dark:text-slate-300">{row.label}</div>
                <div className="h-2 overflow-hidden rounded-full bg-slate-100 dark:bg-white/10">
                    <div className={`h-full rounded-full ${row.tone}`} style={{width: `${Math.max(2, (Number(row.value) / max) * 100)}%`}}/>
                </div>
                <div className="text-right font-bold text-slate-900 dark:text-white">{row.value}</div>
            </div>)}
        </div>
    </div>;
};

const diagnosticTone = {
    good: "border-emerald-200 bg-emerald-50 text-emerald-800 dark:border-emerald-500/30 dark:bg-emerald-500/10 dark:text-emerald-200",
    warn: "border-amber-200 bg-amber-50 text-amber-800 dark:border-amber-500/30 dark:bg-amber-500/10 dark:text-amber-200",
    bad: "border-rose-200 bg-rose-50 text-rose-800 dark:border-rose-500/30 dark:bg-rose-500/10 dark:text-rose-200",
    info: "border-sky-200 bg-sky-50 text-sky-800 dark:border-sky-500/30 dark:bg-sky-500/10 dark:text-sky-200"
};

const DiagnosticPanel = ({rows}) => <div className="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-950">
    <div className="text-sm font-bold text-slate-950 dark:text-white">Result interpretation</div>
    <div className="mt-4 space-y-2">
        {rows.map((row) => <div key={`${row.title}-${row.detail}`} className={`rounded-md border p-3 ${diagnosticTone[row.tone] ?? diagnosticTone.info}`}>
            <div className="text-sm font-bold">{row.title}</div>
            <div className="mt-1 text-xs font-semibold leading-5 opacity-90">{row.detail}</div>
        </div>)}
    </div>
</div>;

const Detail = ({label, value}) => <div className="rounded-md bg-slate-50 p-3 dark:bg-white/[0.04]">
    <dt className="text-xs font-bold uppercase tracking-[0.12em] text-slate-500 dark:text-slate-400">{label}</dt>
    <dd className="mt-1 truncate text-base font-semibold text-slate-950 dark:text-white">{value}</dd>
</div>;

const verdictIcon = (verdict, running) => {
    if (running) {
        return Activity;
    }
    if (verdict === "GOOD") {
        return CheckCircle2;
    }
    if (verdict === "WARNING") {
        return AlertTriangle;
    }
    if (verdict === "BAD") {
        return XCircle;
    }
    return Gauge;
};

const verdictTone = (verdict, running) => {
    if (running) {
        return "text-sky-700 bg-sky-50 ring-sky-200 dark:text-sky-200 dark:bg-sky-500/10 dark:ring-sky-500/20";
    }
    if (verdict === "GOOD") {
        return "text-emerald-700 bg-emerald-50 ring-emerald-200 dark:text-emerald-200 dark:bg-emerald-500/10 dark:ring-emerald-500/20";
    }
    if (verdict === "WARNING") {
        return "text-amber-700 bg-amber-50 ring-amber-200 dark:text-amber-200 dark:bg-amber-500/10 dark:ring-amber-500/20";
    }
    if (verdict === "BAD") {
        return "text-rose-700 bg-rose-50 ring-rose-200 dark:text-rose-200 dark:bg-rose-500/10 dark:ring-rose-500/20";
    }
    return "text-slate-700 bg-slate-50 ring-slate-200 dark:text-slate-200 dark:bg-white/10 dark:ring-white/10";
};

export {
    ApplicationTestsPage
};
