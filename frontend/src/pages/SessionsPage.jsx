import {Clock, Globe2, Laptop, Monitor, ShieldCheck, ShieldX} from "lucide-react";
import {useMutation, useQuery, useQueryClient} from "@tanstack/react-query";
import {useNavigate} from "react-router-dom";
import {apiClient} from "../api/axiosInstance";
import {endpoints} from "../api/endpoints";
import {PageWrapper} from "../components/common/PageWrapper";
import {useAuthStore} from "../store/authStore";
import {asArray, unwrapApiData} from "../utils/responseUtils";
import {relativeTime} from "../utils/dateUtils";

const SessionsPage = () => {
    const queryClient = useQueryClient();
    const navigate = useNavigate();
    const clearAuth = useAuthStore((state) => state.clearAuth);
    const handleRevokeSuccess = (result) => {
        if (result.revokedCurrent) {
            clearAuth();
            navigate("/login", {replace: true});
            return;
        }
        queryClient.invalidateQueries({queryKey: ["auth-sessions"]});
    };
    const sessions = useQuery({
        queryKey: ["auth-sessions"],
        queryFn: async () => asArray((await apiClient.get(endpoints.auth.sessions)).data)
    });
    const revoke = useMutation({
        mutationFn: async (sessionId) => unwrapApiData((await apiClient.delete(endpoints.auth.revokeSession(sessionId))).data),
        onSuccess: handleRevokeSuccess
    });
    const revokeAll = useMutation({
        mutationFn: async () => unwrapApiData((await apiClient.delete(endpoints.auth.revokeAllSessions)).data),
        onSuccess: handleRevokeSuccess
    });
    return <PageWrapper title="Sessions">
        <div className="overflow-hidden rounded-md border border-slate-200 bg-white">
            <div className="flex items-center justify-between border-b px-4 py-3">
                <p className="text-sm text-slate-600">Manage devices signed in to your
                    account.</p>
                <button
                    onClick={() => revokeAll.mutate()}
                    disabled={revokeAll.isPending}
                    className="rounded-md border border-slate-200 px-3 py-2 text-sm font-medium hover:bg-slate-50 disabled:cursor-wait disabled:opacity-70"
                >
                    Revoke all
                </button>
            </div>
            {(sessions.data ?? []).length === 0 ?
                <div className="p-8 text-sm text-slate-500">No active sessions returned by
                    the backend.</div> : sessions.data?.map((session) => {
                    const id = session.sessionId ?? session.id ?? "";
                    return <div
                        key={id}
                        className="grid grid-cols-[40px_1fr_auto] items-start gap-3 border-b px-4 py-4 last:border-b-0"
                    >
                        <Monitor className="h-5 w-5 text-slate-500"/>
                        <div className="min-w-0 space-y-2">
                            <div>
                                <p className="text-sm font-medium text-slate-950">
                                    {session.browser || "Browser"} on {session.operatingSystem || "Unknown OS"}
                                    {session.current ? <span className="ml-2 inline-flex items-center gap-1 rounded bg-emerald-50 px-2 py-0.5 text-xs font-medium text-emerald-700"><ShieldCheck className="h-3 w-3"/> Current</span> : null}
                                </p>
                                <p className="truncate text-xs text-slate-500">{session.userAgent || "User agent not reported"}</p>
                            </div>
                            <div className="grid gap-2 text-xs text-slate-600 md:grid-cols-2">
                                <span className="inline-flex items-center gap-2"><Laptop className="h-3.5 w-3.5"/> {session.deviceType || "Device"} · {session.deviceId || "Browser session"}</span>
                                <span className="inline-flex items-center gap-2"><Globe2 className="h-3.5 w-3.5"/> IP {session.ipAddress || "not reported"}</span>
                                <span className="inline-flex items-center gap-2"><Clock className="h-3.5 w-3.5"/> Login {session.createdAt ? relativeTime(session.createdAt) : "not reported"}</span>
                                <span className="inline-flex items-center gap-2"><Clock className="h-3.5 w-3.5"/> Last active {session.lastActive ? relativeTime(session.lastActive) : "not reported"}</span>
                            </div>
                        </div>
                        <button
                            onClick={() => revoke.mutate(id)}
                            disabled={!id || revoke.isPending}
                            className="inline-flex items-center gap-2 rounded-md border border-slate-200 px-3 py-2 text-sm hover:bg-slate-50 disabled:cursor-wait disabled:opacity-70"
                        >
                            <ShieldX className="h-4 w-4"/>
                            Revoke
                        </button>
                    </div>;
                })}
        </div>
    </PageWrapper>;
};
export {
    SessionsPage
};
