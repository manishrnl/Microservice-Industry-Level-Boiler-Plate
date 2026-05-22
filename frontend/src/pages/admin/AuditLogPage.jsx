import {useQuery} from "@tanstack/react-query";
import {Download, Laptop, MapPin, Search, UserRound} from "lucide-react";
import {useMemo, useState} from "react";
import {apiClient} from "../../api/axiosInstance";
import {endpoints} from "../../api/endpoints";
import {PageWrapper} from "../../components/common/PageWrapper";
import {usePreferencesStore} from "../../store/preferencesStore";
import {formatDateTime} from "../../utils/dateUtils";
import {asArray} from "../../utils/responseUtils";

const AuditLogPage = () => {
    const timezone = usePreferencesStore((state) => state.timezone);
    const [query, setQuery] = useState("");
    const audit = useQuery({
        queryKey: ["audit"],
        queryFn: async () => asArray((await apiClient.get(endpoints.audit.list)).data)
    });
    const events = audit.data ?? [];
    const filteredEvents = useMemo(() => {
        const term = query.trim().toLowerCase();
        if (!term) {
            return events;
        }
        return events.filter((event) => JSON.stringify(event).toLowerCase().includes(term));
    }, [events, query]);
    const exportAudit = async () => {
        const response = await apiClient.get(endpoints.audit.export);
        const blob = new Blob([typeof response.data === "string" ? response.data : JSON.stringify(response.data, null, 2)], {type: "application/json"});
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement("a");
        anchor.href = url;
        anchor.download = "audit-export.json";
        anchor.click();
        URL.revokeObjectURL(url);
    };
    return <PageWrapper title="Audit Logs">
        <div className="overflow-hidden rounded-md border border-slate-200 bg-white">
            <div className="flex flex-col gap-3 border-b px-4 py-3 lg:flex-row lg:items-center lg:justify-between">
                <p className="text-sm text-slate-600">{filteredEvents.length} of {events.length} events</p>
                <label className="relative w-full lg:max-w-md">
                    <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400"/>
                    <input
                        value={query}
                        onChange={(event) => setQuery(event.target.value)}
                        placeholder="Search actor, user, role, IP, trace..."
                        className="h-10 w-full rounded-md border border-slate-200 pl-9 pr-3 text-sm outline-none focus:border-slate-400"
                    />
                </label>
                <button
                    onClick={() => void exportAudit()}
                    className="inline-flex items-center gap-2 rounded-md border border-slate-200 px-3 py-2 text-sm font-medium hover:bg-slate-50"
                >
                    <Download className="h-4 w-4"/>
                    Export
                </button>
            </div>
            {filteredEvents.length === 0 ?
                <div className="p-8 text-sm text-slate-500">No audit events returned by the
                    backend.</div> : filteredEvents.map((event, index) => <article
                    key={`${event.traceId ?? "trace"}-${index}`}
                    className="border-b px-4 py-4 last:border-b-0"
                >
                    <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
                        <div className="min-w-0">
                            <div className="flex flex-wrap items-center gap-2">
                                <span className="rounded-md bg-slate-950 px-2 py-1 text-xs font-semibold text-white">{event.action ?? "ACTION"}</span>
                                <span className="rounded-md bg-emerald-50 px-2 py-1 text-xs font-semibold text-emerald-700">{event.status ?? "status"}</span>
                                <span className="text-xs text-slate-500">{event.resourceType ?? "resource"} / {event.resourceId ?? "unknown"}</span>
                            </div>
                            <h2 className="mt-3 text-sm font-semibold text-slate-950">{auditDescription(event)}</h2>
                            <p className="mt-1 text-sm leading-6 text-slate-600">{event.afterState?.description ?? "No description was attached to this audit event."}</p>
                        </div>
                        <time className="shrink-0 text-sm text-slate-500">{formatDateTime(event.createdAt, timezone)}</time>
                    </div>
                    <dl className="mt-4 grid gap-3 text-sm md:grid-cols-2 xl:grid-cols-4">
                        <AuditFact icon={UserRound} label="Actor" value={actorLabel(event)}/>
                        <AuditFact icon={UserRound} label="Target" value={targetLabel(event)}/>
                        <AuditFact icon={MapPin} label="IP address" value={event.ipAddress ?? "Not recorded"}/>
                        <AuditFact icon={Laptop} label="Device" value={event.userAgent ?? "Not recorded"}/>
                    </dl>
                    <details className="mt-4 rounded-md border border-slate-200 bg-slate-50">
                        <summary className="cursor-pointer px-3 py-2 text-sm font-semibold text-slate-700">Before / after details</summary>
                        <div className="grid gap-3 border-t border-slate-200 p-3 lg:grid-cols-2">
                            <StateBlock title="Before" value={event.beforeState}/>
                            <StateBlock title="After" value={event.afterState}/>
                        </div>
                        <div className="border-t border-slate-200 px-3 py-2 text-xs text-slate-500">Trace: {event.traceId ?? "Not recorded"}</div>
                    </details>
                </article>)}
        </div>
    </PageWrapper>;
};

const AuditFact = ({icon: Icon, label, value}) => <div className="flex min-w-0 gap-2 rounded-md border border-slate-200 bg-slate-50 p-3">
    <Icon className="mt-0.5 h-4 w-4 shrink-0 text-slate-500"/>
    <span className="min-w-0">
        <span className="block text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</span>
        <span className="block break-words text-slate-800">{value}</span>
    </span>
</div>;

const StateBlock = ({title, value}) => <section>
    <h3 className="mb-2 text-xs font-semibold uppercase tracking-wide text-slate-500">{title}</h3>
    <pre className="max-h-64 overflow-auto rounded-md bg-white p-3 text-xs leading-5 text-slate-700">{JSON.stringify(value ?? {}, null, 2)}</pre>
</section>;

const auditDescription = (event) => {
    const actor = actorLabel(event);
    const target = targetLabel(event);
    const action = String(event.action ?? "changed").replaceAll("_", " ").toLowerCase();
    return `${actor} performed ${action} on ${target}`;
};

const actorLabel = (event) => {
    const actor = event.afterState?.actor;
    return actor?.email ? `${actor.name ?? event.username ?? "Unknown"} <${actor.email}>` : event.username ?? event.userId ?? "System";
};

const targetLabel = (event) => {
    const target = event.afterState?.targetUser ?? event.beforeState?.targetUser;
    if (target?.email) {
        return `${target.name ?? "Unknown"} <${target.email}>`;
    }
    return event.resourceId ?? event.resourceType ?? "Unknown resource";
};
export {
    AuditLogPage
};
