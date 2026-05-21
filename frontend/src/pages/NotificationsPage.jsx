import {ExternalLink, LoaderCircle, Trash2} from "lucide-react";
import {useState} from "react";
import {Link} from "react-router-dom";
import {PageWrapper} from "../components/common/PageWrapper";
import {NotificationItem} from "../components/notifications/NotificationItem";
import {useNotifications} from "../components/notifications/useNotifications";

const NotificationsPage = () => {
    const [selected, setSelected] = useState(null);
    const {
        notifications,
        isLoading,
        isMarkingAllRead,
        markRead,
        markAllRead,
        deleteNotification
    } = useNotifications();
    return <PageWrapper title="Notifications">
        <div className="overflow-hidden rounded-md border border-slate-200 bg-white">
            {selected && <section className="border-b bg-slate-50 px-4 py-4">
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{selected.category}</p>
                <h2 className="mt-1 text-base font-semibold text-slate-950">{selected.title}</h2>
                <p className="mt-2 text-sm text-slate-700">{selected.message}</p>
                {selected.actionUrl && <Link
                    to={selected.actionUrl}
                    className="mt-3 inline-flex items-center gap-2 text-sm font-medium text-blue-700"
                >
                    <ExternalLink className="h-4 w-4"/>
                    Open related page
                </Link>}
            </section>}
            <div className="flex items-center justify-between border-b px-4 py-3">
                <p className="text-sm text-slate-600">{notifications.filter((notification) => !notification.read).length} unread</p>
                <button
                    onClick={() => markAllRead()}
                    disabled={isMarkingAllRead}
                    className="inline-flex items-center gap-2 rounded-md border border-slate-200 px-3 py-2 text-sm font-medium hover:bg-slate-50 disabled:cursor-wait disabled:opacity-70"
                >
                    {isMarkingAllRead && <LoaderCircle className="h-4 w-4 animate-spin"/>}
                    Mark all read
                </button>
            </div>
            {isLoading ? <div className="p-6 text-sm text-slate-500">Loading
                notifications</div> : notifications.length === 0 ?
                <div className="p-10 text-center text-sm text-slate-500">No notifications
                    yet.</div> : notifications.map((notification) => <div
                    key={notification.id}
                    className="grid grid-cols-[1fr_44px] border-b last:border-b-0"
                >
                    <NotificationItem
                        notification={notification}
                        onRead={markRead}
                        onOpen={setSelected}
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
