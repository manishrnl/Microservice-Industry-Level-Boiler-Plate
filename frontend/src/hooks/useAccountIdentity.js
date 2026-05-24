import {useEffect, useMemo} from "react";
import {useQuery} from "@tanstack/react-query";
import {apiClient} from "../api/axiosInstance";
import {endpoints} from "../api/endpoints";
import {useAuthStore} from "../store/authStore";
import {displayUserName, mergeAccountIdentity} from "../utils/userDisplay";
import {unwrapApiData} from "../utils/responseUtils";

const useAccountIdentity = () => {
    const user = useAuthStore((state) => state.user);
    const updateUser = useAuthStore((state) => state.updateUser);
    const enabled = Boolean(user?.userId || user?.email);
    const accountSettings = useQuery({
        queryKey: ["account-settings"],
        enabled,
        staleTime: 5 * 60 * 1000,
        queryFn: async () => unwrapApiData((await apiClient.get(endpoints.users.settings)).data)
    });

    useEffect(() => {
        if (accountSettings.data) {
            updateUser({
                email: accountSettings.data.email,
                name: displayUserName(accountSettings.data.name, accountSettings.data.email),
                username: accountSettings.data.username,
                avatarUrl: accountSettings.data.avatarUrl,
                roles: accountSettings.data.roles
            });
        }
    }, [accountSettings.data, updateUser]);

    const identity = useMemo(() => mergeAccountIdentity(user, accountSettings.data), [user, accountSettings.data]);
    const identityReady = !enabled || accountSettings.isFetched || Boolean(accountSettings.data);

    return {
        accountSettings,
        identity,
        identityReady
    };
};

export {
    useAccountIdentity
};
