import {useQuery} from "@tanstack/react-query";
import {ArrowRight, Bell, Bot, CreditCard, FileText, ShieldCheck, Sparkles} from "lucide-react";
import {Link} from "react-router-dom";
import {apiClient} from "../api/axiosInstance";
import {endpoints} from "../api/endpoints";
import {Avatar} from "../components/common/Avatar";
import {PageWrapper} from "../components/common/PageWrapper";
import {useAccountIdentity} from "../hooks/useAccountIdentity";
import {asArray, unwrapApiData} from "../utils/responseUtils";
import {firstDisplayName} from "../utils/userDisplay";

const formatTokens = (value) => Number(value ?? 0).toLocaleString();

const Dashboard = () => {
    const {identity, identityReady} = useAccountIdentity();
    const notifications = useQuery({
        queryKey: ["notifications"],
        queryFn: async () => asArray((await apiClient.get(endpoints.notifications.list)).data)
    });
    const files = useQuery({
        queryKey: ["files"],
        queryFn: async () => asArray((await apiClient.get(endpoints.files.mine)).data)
    });
    const aiUsage = useQuery({
        queryKey: ["ai-usage"],
        queryFn: async () => unwrapApiData((await apiClient.get(endpoints.ai.usage)).data)
    });
    const usage = aiUsage.data ?? {};
    const usedTokens = Number(usage.usedTokens ?? 0);
    const totalTokens = Number(usage.totalTokens ?? 0);
    const availableTokens = Number(usage.availableTokens ?? usage.remainingTokens ?? Math.max(0, totalTokens - usedTokens));
    const freeTrialPercent = Number(usage.freeTrialPercent ?? 10);
    const usagePercent = totalTokens > 0 ? Math.min(100, Math.round((usedTokens / totalTokens) * 100)) : 0;
    const cards = [
        {
            label: "Unread notifications",
            value: (notifications.data ?? []).filter((item) => !item.read).length,
            Icon: Bell
        },
        {label: "Stored files", value: files.data?.length ?? 0, Icon: FileText},
        {label: "Payment workflow", value: "Ready", Icon: CreditCard},
        {label: "Session controls", value: "Ready", Icon: ShieldCheck}
    ];
    const signedInName = identityReady ? firstDisplayName(identity?.name, identity?.email).toUpperCase() : "Loading profile";
    return <PageWrapper title="Dashboard">
        <section
            className="mb-5 overflow-hidden rounded-md border border-slate-200 bg-white shadow-sm dark:border-white/10 dark:bg-slate-950">
            <div className="grid gap-5 p-5 lg:grid-cols-[1fr_auto] lg:items-center">
                <div className="flex min-w-0 items-center gap-4">
                    <Avatar src={identity?.avatarUrl} name={signedInName} email={identity?.email} size="xl"/>
                    <div className="min-w-0">
                        <p className="text-sm font-medium text-slate-500">Signed in as</p>
                        <h2 className="mt-1 truncate text-2xl font-semibold text-slate-950 dark:text-white">{signedInName}</h2>
                        <p className="mt-1 break-words text-sm text-slate-600 dark:text-slate-300">{identity?.email}</p>
                    </div>
                </div>
                <div className="flex flex-wrap gap-2 lg:justify-end">
                    {(identity?.roles ?? []).map((role) => <span
                        key={role}
                        className="rounded-md border border-teal-100 bg-teal-50 px-2.5 py-1 text-xs font-semibold text-teal-700 dark:border-teal-300/20 dark:bg-teal-300/10 dark:text-teal-200"
                    >
                        {role}
                    </span>)}
                </div>
            </div>
            <div className="grid border-t border-slate-200 bg-slate-50/70 dark:border-white/10 dark:bg-white/[0.03] sm:grid-cols-3">
                <HeaderStat label="Profile" value={identityReady ? "Ready" : "Loading"}/>
                <HeaderStat label="AI allowance" value={`${usagePercent}% used`}/>
                <HeaderStat label="Workspace" value="Operational"/>
            </div>
        </section>
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            {cards.map(({label, value, Icon}) => <section
                key={label}
                className="rounded-md border border-slate-200 bg-white p-5 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md dark:border-white/10 dark:bg-slate-950"
            >
                <div className="flex items-start justify-between gap-3">
                    <span className="grid h-10 w-10 place-items-center rounded-md bg-slate-100 text-slate-700 dark:bg-white/10 dark:text-slate-200"><Icon className="h-5 w-5"/></span>
                    <ArrowRight className="h-4 w-4 text-slate-300"/>
                </div>
                <p className="mt-4 text-sm font-medium text-slate-500">{label}</p>
                <p className="mt-2 text-2xl font-semibold text-slate-950 dark:text-white">{value}</p>
            </section>)}
        </div>
        <section className="mt-4 overflow-hidden rounded-md border border-slate-200 bg-white shadow-sm dark:border-white/10 dark:bg-slate-950">
            <div className="grid gap-5 p-5 lg:grid-cols-[1fr_auto] lg:items-start">
                <div className="min-w-0">
                    <div className="flex items-center gap-3">
                        <span className="grid h-10 w-10 place-items-center rounded-md bg-slate-100 text-slate-700 dark:bg-white/10 dark:text-slate-200"><Bot className="h-5 w-5"/></span>
                        <div>
                            <p className="text-sm font-semibold text-slate-950 dark:text-white">AI Tokens</p>
                            <p className="mt-1 text-sm text-slate-600 dark:text-slate-300">
                                Free trial includes {freeTrialPercent}% of the premium token pool.
                            </p>
                        </div>
                    </div>
                </div>
                <Link
                    to="/app/premium"
                    className="inline-flex w-full items-center justify-center gap-2 rounded-md bg-slate-950 px-4 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-slate-800 dark:bg-teal-300 dark:text-slate-950 dark:hover:bg-teal-200 sm:w-auto"
                >
                    <Sparkles className="h-4 w-4"/>
                    Premium features
                </Link>
            </div>
            <div className="grid gap-3 px-5 pb-5 sm:grid-cols-3">
                <TokenMetric label="Used" value={formatTokens(usedTokens)}/>
                <TokenMetric label="Available" value={formatTokens(availableTokens)}/>
                <TokenMetric label="Total" value={formatTokens(totalTokens)}/>
            </div>
            <div className="h-2 overflow-hidden bg-slate-100 dark:bg-white/10">
                <div
                    className="h-full bg-teal-500 transition-all"
                    style={{width: `${usagePercent}%`}}
                />
            </div>
        </section>
    </PageWrapper>;
};

const TokenMetric = ({label, value}) => <div className="rounded-md border border-slate-200 bg-slate-50 p-4 dark:border-white/10 dark:bg-white/5">
    <p className="text-xs font-semibold uppercase text-slate-500 dark:text-slate-400">{label}</p>
    <p className="mt-2 text-2xl font-semibold text-slate-950 dark:text-white">{value}</p>
</div>;

const HeaderStat = ({label, value}) => <div className="border-b border-slate-200 px-5 py-3 last:border-b-0 dark:border-white/10 sm:border-b-0 sm:border-r sm:last:border-r-0">
    <p className="text-xs font-semibold uppercase text-slate-500">{label}</p>
    <p className="mt-1 text-sm font-semibold text-slate-950 dark:text-white">{value}</p>
</div>;

export {
    Dashboard
};
