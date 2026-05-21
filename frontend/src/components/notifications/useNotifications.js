import {useMutation, useQuery, useQueryClient} from "@tanstack/react-query";
import {useCallback, useMemo, useState} from "react";
import toast from "react-hot-toast";
import {apiClient} from "../../api/axiosInstance";
import {endpoints} from "../../api/endpoints";
import {useSSE} from "../../hooks/useSSE";
import {useAuthStore} from "../../store/authStore";

const useNotifications = () => {
    const queryClient = useQueryClient();
    const authenticated = useAuthStore((state) => state.isAuthenticated);
    const accessToken = useAuthStore((state) => state.accessToken);
    const [shake, setShake] = useState(false);
    const notificationsQuery = useQuery({
        queryKey: ["notifications"],
        queryFn: async () => (await apiClient.get(endpoints.notifications.list)).data,
        enabled: authenticated
    });
    const addNotification = useCallback((notification) => {
        queryClient.setQueryData(["notifications"], (old = []) => [notification, ...old]);
        setShake(true);
        window.setTimeout(() => setShake(false), 700);
        toast(notification.title);
    }, [queryClient]);
    useSSE(endpoints.notifications.stream, {
        enabled: authenticated && Boolean(accessToken),
        eventName: "notification",
        headers: useMemo(() => accessToken ? {Authorization: `Bearer ${accessToken}`} : void 0, [accessToken]),
        onMessage: addNotification
    });
    const markRead = useMutation({
        mutationFn: async (id) => apiClient.patch(endpoints.notifications.markRead(id)),
        onMutate: (id) => queryClient.setQueryData(["notifications"], (old = []) => old.map((item) => item.id === id ? {
            ...item,
            read: true
        } : item))
    });
    const markAllRead = useMutation({
        mutationFn: async () => apiClient.patch(endpoints.notifications.markAllRead),
        onMutate: () => queryClient.setQueryData(["notifications"], (old = []) => old.map((item) => ({
            ...item,
            read: true
        })))
    });
    const remove = useMutation({
        mutationFn: async (id) => apiClient.delete(endpoints.notifications.delete(id)),
        onMutate: (id) => queryClient.setQueryData(["notifications"], (old = []) => old.filter((item) => item.id !== id))
    });
    const notifications = notificationsQuery.data ?? [];
    const unreadCount = useMemo(() => notifications.filter((item) => !item.read).length, [notifications]);
    return {
        notifications,
        unreadCount,
        shake,
        isLoading: notificationsQuery.isLoading,
        isMarkingAllRead: markAllRead.isPending,
        markRead: markRead.mutate,
        markAllRead: markAllRead.mutate,
        deleteNotification: remove.mutate
    };
};
export {
    useNotifications
};
