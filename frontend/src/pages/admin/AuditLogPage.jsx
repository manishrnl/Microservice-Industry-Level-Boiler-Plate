import {useQuery} from "@tanstack/react-query";
import {Download} from "lucide-react";
import {apiClient} from "../../api/axiosInstance";
import {endpoints} from "../../api/endpoints";
import {PageWrapper} from "../../components/common/PageWrapper";
import {asArray} from "../../utils/responseUtils";

const AuditLogPage = () => {
    const audit = useQuery({
        queryKey: ["audit"],
        queryFn: async () => asArray((await apiClient.get(endpoints.audit.list)).data)
    });
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
            <div className="flex items-center justify-between border-b px-4 py-3">
                <p className="text-sm text-slate-600">{audit.data?.length ?? 0} events</p>
                <button
                    onClick={() => void exportAudit()}
                    className="inline-flex items-center gap-2 rounded-md border border-slate-200 px-3 py-2 text-sm font-medium hover:bg-slate-50"
                >
                    <Download className="h-4 w-4"/>
                    Export
                </button>
            </div>
            {(audit.data ?? []).length === 0 ?
                <div className="p-8 text-sm text-slate-500">No audit events returned by the
                    backend.</div> : audit.data?.map((event, index) => <div
                    key={`${event.traceId ?? "trace"}-${index}`}
                    className="grid gap-2 border-b px-4 py-3 text-sm last:border-b-0 sm:grid-cols-4 sm:gap-3"
                >
                    <span className="font-medium text-slate-950">{event.action ?? "ACTION"}</span>
                    <span className="text-slate-600">{event.resourceType ?? "resource"}</span>
                    <span className="text-slate-600">{event.status ?? "status"}</span>
                    <span
                        className="min-w-0 break-words text-xs text-slate-500 sm:truncate sm:text-sm"
                    >{event.traceId ?? event.createdAt}</span>
                </div>)}
        </div>
    </PageWrapper>;
};
export {
    AuditLogPage
};
