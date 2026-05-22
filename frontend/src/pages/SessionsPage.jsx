import {Clock, Globe2, Laptop, Monitor, ShieldCheck, ShieldX} from "lucide-react";
import {useMutation, useQuery, useQueryClient} from "@tanstack/react-query";
import {useNavigate} from "react-router-dom";
import {apiClient} from "../api/axiosInstance";
import {endpoints} from "../api/endpoints";
import {PageWrapper} from "../components/common/PageWrapper";
import {useAuthStore} from "../store/authStore";
import {usePreferencesStore} from "../store/preferencesStore";
import {asArray, unwrapApiData} from "../utils/responseUtils";
import {formatDateTime, relativeTime} from "../utils/dateUtils";

const SessionsPage = () => {
    const queryClient = useQueryClient();
    const navigate = useNavigate();
    const clearAuth = useAuthStore((state) => state.clearAuth);
    const timezone = usePreferencesStore((state) => state.timezone);
    const currentHost = window.location.hostname || "current device";
    const currentBrowser = navigator.userAgent || "Current browser";
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
    const sessionList = sessions.data ?? [];
    return <PageWrapper title="Sessions">
        <div className="overflow-hidden rounded-md border border-slate-200 bg-white dark:border-white/10 dark:bg-slate-900">
            <div className="flex flex-col gap-3 border-b border-slate-200 px-4 py-3 dark:border-white/10 sm:flex-row sm:items-center sm:justify-between">
                <p className="text-sm text-slate-600 dark:text-slate-300">Manage devices signed in to your
                    account.</p>
                <button
                    onClick={() => revokeAll.mutate()}
                    disabled={revokeAll.isPending}
                    className="rounded-md border border-slate-200 px-3 py-2 text-sm font-medium hover:bg-slate-50 disabled:cursor-wait disabled:opacity-70 dark:border-white/10 dark:text-slate-100 dark:hover:bg-white/10"
                >
                    Revoke all
                </button>
            </div>
            {sessions.isLoading ? <div className="flex items-center gap-3 p-8 text-sm text-slate-500 dark:text-slate-400">
                <Clock className="h-5 w-5 animate-pulse"/>
                Loading active sessions from the database...
            </div> : sessionList.length === 0 ?
                <div className="grid grid-cols-[40px_1fr] gap-3 p-6 sm:p-8">
                    <span className="grid h-10 w-10 place-items-center rounded-md bg-emerald-50 text-emerald-700 dark:bg-emerald-300/10 dark:text-emerald-200">
                        <ShieldCheck className="h-5 w-5"/>
                    </span>
                    <div className="min-w-0">
                        <p className="text-sm font-semibold text-slate-950 dark:text-white">Current browser session</p>
                        <p className="mt-1 text-sm leading-6 text-slate-600 dark:text-slate-300">
                            Session details are syncing from the database. You are currently viewing this app from <strong>{currentHost}</strong>.
                        </p>
                        <div className="mt-4 grid gap-2 text-xs text-slate-600 dark:text-slate-300 md:grid-cols-2">
                            <span className="inline-flex items-center gap-2"><Laptop className="h-3.5 w-3.5"/> Browser session</span>
                            <span className="inline-flex items-center gap-2"><Globe2 className="h-3.5 w-3.5"/> Host {currentHost}</span>
                            <span className="inline-flex items-center gap-2 md:col-span-2"><Monitor className="h-3.5 w-3.5"/> <span className="truncate">{currentBrowser}</span></span>
                        </div>
                    </div>
                </div> : sessionList.map((session) => {
                    const id = session.sessionId ?? session.id ?? "";
                    return <div
                        key={id}
                        className="grid grid-cols-[32px_1fr] items-start gap-3 border-b border-slate-200 px-4 py-4 last:border-b-0 dark:border-white/10 sm:grid-cols-[40px_1fr_auto]"
                    >
                        <Monitor className="h-5 w-5 text-slate-500 dark:text-slate-400"/>
                        <div className="min-w-0 space-y-2">
                            <div>
                                <p className="text-sm font-medium text-slate-950 dark:text-white">
                                    {session.browser || "Browser"} on {session.operatingSystem || "Unknown OS"}
                                    {session.current ? <span className="ml-2 inline-flex items-center gap-1 rounded bg-emerald-50 px-2 py-0.5 text-xs font-medium text-emerald-700"><ShieldCheck className="h-3 w-3"/> Current</span> : null}
                                </p>
                                <p className="truncate text-xs text-slate-500 dark:text-slate-400">{session.userAgent || "User agent not reported"}</p>
                            </div>
                            <div className="grid gap-2 text-xs text-slate-600 dark:text-slate-300 md:grid-cols-2">
                                <span className="inline-flex items-center gap-2"><Laptop className="h-3.5 w-3.5"/> {session.deviceType || "Device"} · {session.deviceId || "Browser session"}</span>
                                <span className="inline-flex items-center gap-2"><Globe2 className="h-3.5 w-3.5"/> IP {session.ipAddress || "not reported"}</span>
                                <span className="inline-flex items-center gap-2"><Clock className="h-3.5 w-3.5"/> Login {session.createdAt ? `${formatDateTime(session.createdAt, timezone)} (${relativeTime(session.createdAt)})` : "not reported"}</span>
                                <span className="inline-flex items-center gap-2"><Clock className="h-3.5 w-3.5"/> Last active {session.lastActive ? `${formatDateTime(session.lastActive, timezone)} (${relativeTime(session.lastActive)})` : "not reported"}</span>
                            </div>
                        </div>
                        <button
                            onClick={() => revoke.mutate(id)}
                            disabled={!id || revoke.isPending}
                            className="col-span-2 inline-flex items-center justify-center gap-2 rounded-md border border-slate-200 px-3 py-2 text-sm hover:bg-slate-50 disabled:cursor-wait disabled:opacity-70 dark:border-white/10 dark:text-slate-100 dark:hover:bg-white/10 sm:col-span-1"
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
