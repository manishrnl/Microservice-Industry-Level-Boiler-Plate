import {useQuery} from "@tanstack/react-query";
import {apiClient} from "../../api/axiosInstance";
import {endpoints} from "../../api/endpoints";
import {PageWrapper} from "../../components/common/PageWrapper";
import {asArray} from "../../utils/responseUtils";

const UserManagementPage = () => {
    const users = useQuery({
        queryKey: ["users"],
        queryFn: async () => asArray((await apiClient.get(endpoints.users.list)).data)
    });
    return <PageWrapper title="Users">
        <div className="overflow-hidden rounded-md border border-slate-200 bg-white">
            {(users.data ?? []).map((user) => <div
                key={user.userId}
                className="grid gap-2 border-b px-4 py-3 text-sm last:border-0 sm:grid-cols-[1fr_1.4fr_1fr]"
            >
                <span className="font-medium text-slate-950">{user.name}</span>
                <span className="min-w-0 break-words text-slate-600 sm:truncate">{user.email}</span>
                <span className="text-xs font-semibold uppercase tracking-wide text-slate-500">{user.roles.join(", ")}</span>
            </div>)}
        </div>
    </PageWrapper>;
};
export {
    UserManagementPage
};
