import {LoaderCircle, Trash2} from "lucide-react";
import {PageWrapper} from "../components/common/PageWrapper";
import {NotificationItem} from "../components/notifications/NotificationItem";
import {useNotifications} from "../components/notifications/useNotifications";

const NotificationsPage = () => {
    const {
        notifications,
        isLoading,
        isMarkingAllRead,
        isClearingAll,
        markRead,
        markAllRead,
        deleteNotification,
        clearNotifications
    } = useNotifications();
    return <PageWrapper title="Notifications">
        <div className="overflow-hidden rounded-md border border-slate-200 bg-white">
            <div className="flex flex-col gap-3 border-b px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
                <p className="text-sm text-slate-600">{notifications.filter((notification) => !notification.read).length} unread</p>
                <div className="flex flex-wrap gap-2">
                    <button
                        onClick={() => markAllRead()}
                        disabled={isMarkingAllRead}
                        className="inline-flex items-center gap-2 rounded-md border border-slate-200 px-3 py-2 text-sm font-medium hover:bg-slate-50 disabled:cursor-wait disabled:opacity-70"
                    >
                        {isMarkingAllRead && <LoaderCircle className="h-4 w-4 animate-spin"/>}
                        Mark all read
                    </button>
                    <button
                        onClick={() => clearNotifications()}
                        disabled={isClearingAll || notifications.length === 0}
                        className="inline-flex items-center gap-2 rounded-md border border-red-200 px-3 py-2 text-sm font-medium text-red-600 hover:bg-red-50 disabled:cursor-wait disabled:opacity-70"
                    >
                        {isClearingAll && <LoaderCircle className="h-4 w-4 animate-spin"/>}
                        Clear all
                    </button>
                </div>
            </div>
            {isLoading ? <div className="p-6 text-sm text-slate-500">Loading
                notifications</div> : notifications.length === 0 ?
                <div className="p-10 text-center text-sm text-slate-500">No notifications
                    yet.</div> : notifications.map((notification) => <div
                    key={notification.id}
                    className="grid grid-cols-[minmax(0,1fr)_44px] border-b last:border-b-0"
                >
                    <NotificationItem
                        notification={notification}
                        onRead={markRead}
                    />
                    <button
                        onClick={() => deleteNotification(notification.id)}
                        className="grid place-items-center text-slate-400 hover:bg-red-50 hover:text-red-600"
                        aria-label="Delete notification"
                    >
                        <Trash2 className="h-4 w-4"/>
                    </button>
                </div>)}
        </div>
    </PageWrapper>;
};
export {
    NotificationsPage
};
