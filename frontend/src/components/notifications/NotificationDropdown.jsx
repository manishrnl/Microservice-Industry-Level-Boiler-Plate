import {useEffect, useMemo, useRef, useState} from "react";
import {LoaderCircle} from "lucide-react";
import {Link} from "react-router-dom";
import {NotificationItem} from "./NotificationItem";

const NotificationDropdown = ({
                                  notifications,
                                  onClose,
                                  onRead,
                                  onMarkAllRead,
                                  onClearAll,
                                  markAllPending,
                                  clearAllPending
                              }) => {
    const ref = useRef(null);
    const [tab, setTab] = useState("all");
    const visible = useMemo(() => tab === "all" ? notifications : notifications.filter((item) => !item.read), [notifications, tab]);
    useEffect(() => {
        const handler = (event) => {
            if (ref.current && !ref.current.contains(event.target)) {
                onClose();
            }
        };
        document.addEventListener("mousedown", handler);
        return () => document.removeEventListener("mousedown", handler);
    }, [onClose]);
    return <div
        ref={ref}
        className="fixed left-3 right-3 top-16 z-50 overflow-hidden rounded-md border border-slate-200 bg-white shadow-xl dark:border-white/10 dark:bg-slate-950 sm:left-auto sm:right-6 sm:w-[360px]"
    >
        <div className="flex items-center justify-between gap-3 border-b border-slate-200 px-4 py-3 dark:border-white/10">
            <h2 className="text-sm font-semibold text-slate-950 dark:text-white">Notifications</h2>
            <div className="flex items-center gap-3">
                <button
                    onClick={onMarkAllRead}
                    disabled={markAllPending}
                    className="inline-flex items-center gap-1.5 text-xs font-medium text-blue-700 disabled:cursor-wait disabled:opacity-70 dark:text-teal-300"
                >
                    {markAllPending && <LoaderCircle className="h-3.5 w-3.5 animate-spin"/>}
                    Mark all read
                </button>
                <button
                    onClick={onClearAll}
                    disabled={clearAllPending || notifications.length === 0}
                    className="inline-flex items-center gap-1.5 text-xs font-medium text-red-600 disabled:cursor-wait disabled:opacity-60 dark:text-red-300"
                >
                    {clearAllPending && <LoaderCircle className="h-3.5 w-3.5 animate-spin"/>}
                    Clear
                </button>
            </div>
        </div>
        <div className="flex border-b border-slate-200 dark:border-white/10">
            <button
                onClick={() => setTab("all")}
                className={`flex-1 px-4 py-2 text-sm ${tab === "all" ? "border-b-2 border-blue-600 text-blue-700 dark:border-teal-300 dark:text-teal-300" : "text-slate-600 dark:text-slate-300"}`}
            >All
            </button>
            <button
                onClick={() => setTab("unread")}
                className={`flex-1 px-4 py-2 text-sm ${tab === "unread" ? "border-b-2 border-blue-600 text-blue-700 dark:border-teal-300 dark:text-teal-300" : "text-slate-600 dark:text-slate-300"}`}
            >Unread
            </button>
        </div>
        <div className="max-h-[min(480px,calc(100vh-14rem))] overflow-y-auto">
            {visible.length === 0 ? <div
                className="flex flex-col items-center gap-2 px-6 py-12 text-center text-sm text-slate-500 dark:text-slate-400"
            >
                <div className="h-16 w-16 rounded-full bg-slate-100 dark:bg-white/10"/>
                No notifications yet
            </div> : visible.map((item) => <NotificationItem
                key={item.id}
                notification={item}
                onRead={onRead}
            />)}
        </div>
        <Link
            to="/app/notifications"
            onClick={onClose}
            className="block border-t border-slate-200 px-4 py-3 text-center text-sm font-medium text-blue-700 dark:border-white/10 dark:text-teal-300"
        >See
            all notifications</Link>
    </div>;
};
export {
    NotificationDropdown
};
