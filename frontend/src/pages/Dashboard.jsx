import {useQuery} from "@tanstack/react-query";
import {Bell, Bot, CreditCard, FileText, ShieldCheck} from "lucide-react";
import {apiClient} from "../api/axiosInstance";
import {endpoints} from "../api/endpoints";
import {Avatar} from "../components/common/Avatar";
import {PageWrapper} from "../components/common/PageWrapper";
import {useAccountIdentity} from "../hooks/useAccountIdentity";
import {asArray, unwrapApiData} from "../utils/responseUtils";
import {firstDisplayName} from "../utils/userDisplay";

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
    const cards = [
        {
            label: "Unread notifications",
            value: (notifications.data ?? []).filter((item) => !item.read).length,
            Icon: Bell
        },
        {label: "Stored files", value: files.data?.length ?? 0, Icon: FileText},
        {label: "AI tokens", value: aiUsage.data?.totalTokens ?? 0, Icon: Bot},
        {label: "Payment workflow", value: "Ready", Icon: CreditCard},
        {label: "Session controls", value: "Ready", Icon: ShieldCheck}
    ];
    const signedInName = identityReady ? firstDisplayName(identity?.name, identity?.email).toUpperCase() : "Loading profile";
    return <PageWrapper title="Dashboard">
        <section
            className="mb-5 flex flex-col gap-4 rounded-md border border-slate-200 bg-white p-5 shadow-sm sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-center gap-4">
                <Avatar src={identity?.avatarUrl} name={signedInName} email={identity?.email} size="xl"/>
                <div>
                    <p className="text-sm text-slate-500">Signed in as</p>
                    <h2 className="mt-1 text-xl font-semibold text-slate-950">{signedInName}</h2>
                    <p className="mt-1 text-sm text-slate-600">{identity?.email}</p>
                </div>
            </div>
            <div className="flex flex-wrap gap-2">
                {(identity?.roles ?? []).map((role) => <span
                    key={role}
                    className="rounded-md border border-teal-100 bg-teal-50 px-2.5 py-1 text-xs font-semibold text-teal-700"
                >
                            {role}
                        </span>)}
            </div>
        </section>
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
            {cards.map(({label, value, Icon}) => <section
                key={label}
                className="rounded-md border border-slate-200 bg-white p-5 shadow-sm"
            >
                <Icon className="h-5 w-5 text-slate-500"/>
                <p className="mt-4 text-sm text-slate-500">{label}</p>
                <p className="mt-2 text-2xl font-semibold text-slate-950">{value}</p>
            </section>)}
        </div>
    </PageWrapper>;
};
export {
    Dashboard
};
