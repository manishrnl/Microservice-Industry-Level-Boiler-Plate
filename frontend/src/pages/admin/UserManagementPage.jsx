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
                className="grid grid-cols-3 border-b px-4 py-3 text-sm last:border-0"
            >
                <span>{user.name}</span>
                <span>{user.email}</span>
                <span>{user.roles.join(", ")}</span>
            </div>)}
        </div>
    </PageWrapper>;
};
export {
    UserManagementPage
};
