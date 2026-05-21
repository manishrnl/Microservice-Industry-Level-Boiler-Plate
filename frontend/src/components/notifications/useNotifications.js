import {useMutation, useQuery, useQueryClient} from "@tanstack/react-query";
import {useCallback, useMemo, useState} from "react";
import toast from "react-hot-toast";
import {apiClient} from "../../api/axiosInstance";
import {endpoints} from "../../api/endpoints";
import {useSSE} from "../../hooks/useSSE";
import {useAuthStore} from "../../store/authStore";
import {unwrapApiData} from "../../utils/responseUtils";

const useNotifications = () => {
    const queryClient = useQueryClient();
    const authenticated = useAuthStore((state) => state.isAuthenticated);
    const accessToken = useAuthStore((state) => state.accessToken);
    const [shake, setShake] = useState(false);
    const notificationsQuery = useQuery({
        queryKey: ["notifications"],
        queryFn: async () => unwrapNotificationList((await apiClient.get(endpoints.notifications.list)).data),
        enabled: authenticated
    });
    const addNotification = useCallback((notification) => {
        queryClient.setQueryData(["notifications"], (old = []) => [notification, ...old]);
        setShake(true);
        window.setTimeout(() => setShake(false), 700);
        if (!isWelcomeNotification(notification)) {
            toast(notification.title);
        }
    }, [queryClient]);
    useSSE(endpoints.notifications.stream, {
        enabled: authenticated && Boolean(accessToken),
        eventName: "notification",
        headers: useMemo(() => accessToken ? {Authorization: `Bearer ${accessToken}`} : void 0, [accessToken]),
        onMessage: addNotification
    });
    const markRead = useMutation({
        mutationFn: async (id) => unwrapApiData((await apiClient.patch(endpoints.notifications.markRead(id))).data),
        onMutate: (id) => queryClient.setQueryData(["notifications"], (old = []) => old.map((item) => item.id === id ? {
            ...item,
            read: true
        } : item)),
        onSettled: () => queryClient.invalidateQueries({queryKey: ["notifications"]})
    });
    const markAllRead = useMutation({
        mutationFn: async () => unwrapNotificationList((await apiClient.patch(endpoints.notifications.markAllRead)).data),
        onMutate: () => queryClient.setQueryData(["notifications"], (old = []) => old.map((item) => ({
            ...item,
            read: true
        }))),
        onSuccess: (data) => {
            if (Array.isArray(data)) {
                queryClient.setQueryData(["notifications"], data);
            }
        },
        onSettled: () => queryClient.invalidateQueries({queryKey: ["notifications"]})
    });
    const remove = useMutation({
        mutationFn: async (id) => apiClient.delete(endpoints.notifications.delete(id)),
        onMutate: (id) => queryClient.setQueryData(["notifications"], (old = []) => old.filter((item) => item.id !== id)),
        onSettled: () => queryClient.invalidateQueries({queryKey: ["notifications"]})
    });
    const clearAll = useMutation({
        mutationFn: async () => apiClient.delete(endpoints.notifications.deleteAll),
        onMutate: () => queryClient.setQueryData(["notifications"], []),
        onSettled: () => queryClient.invalidateQueries({queryKey: ["notifications"]})
    });
    const notifications = notificationsQuery.data ?? [];
    const unreadCount = useMemo(() => notifications.filter((item) => !item.read).length, [notifications]);
    return {
        notifications,
        unreadCount,
        shake,
        isLoading: notificationsQuery.isLoading,
        isMarkingAllRead: markAllRead.isPending,
        isClearingAll: clearAll.isPending,
        markRead: markRead.mutate,
        markAllRead: markAllRead.mutate,
        deleteNotification: remove.mutate,
        clearNotifications: clearAll.mutate
    };
};
const unwrapNotificationList = (payload) => {
    const data = unwrapApiData(payload);
    return Array.isArray(data) ? data : [];
};
const isWelcomeNotification = (notification) => {
    const title = String(notification?.title ?? "").trim();
    return /^welcome\b/i.test(title);
};
export {
    useNotifications
};
