import {useEffect, useMemo, useRef, useState} from "react";
import {ExternalLink, LoaderCircle, X} from "lucide-react";
import {Link} from "react-router-dom";
import {NotificationItem} from "./NotificationItem";

const NotificationDropdown = ({
                                  notifications,
                                  onClose,
                                  onRead,
                                  onMarkAllRead,
                                  markAllPending
                              }) => {
    const ref = useRef(null);
    const [tab, setTab] = useState("all");
    const [selected, setSelected] = useState(null);
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
        className="fixed right-6 top-16 z-50 w-[360px] overflow-hidden rounded-md border border-slate-200 bg-white shadow-xl"
    >
        <div className="flex items-center justify-between border-b px-4 py-3">
            <h2 className="text-sm font-semibold">Notifications</h2>
            <button
                onClick={onMarkAllRead}
                disabled={markAllPending}
                className="inline-flex items-center gap-1.5 text-xs font-medium text-blue-700 disabled:cursor-wait disabled:opacity-70"
            >
                {markAllPending && <LoaderCircle className="h-3.5 w-3.5 animate-spin"/>}
                Mark all read
            </button>
        </div>
        <div className="flex border-b">
            <button
                onClick={() => setTab("all")}
                className={`flex-1 px-4 py-2 text-sm ${tab === "all" ? "border-b-2 border-blue-600 text-blue-700" : "text-slate-600"}`}
            >All
            </button>
            <button
                onClick={() => setTab("unread")}
                className={`flex-1 px-4 py-2 text-sm ${tab === "unread" ? "border-b-2 border-blue-600 text-blue-700" : "text-slate-600"}`}
            >Unread
            </button>
        </div>
        {selected && <div className="border-b bg-slate-50 px-4 py-3">
            <div className="flex items-start justify-between gap-3">
                <div>
                    <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{selected.category}</p>
                    <h3 className="mt-1 text-sm font-semibold text-slate-950">{selected.title}</h3>
                </div>
                <button
                    onClick={() => setSelected(null)}
                    className="grid h-8 w-8 place-items-center rounded-md text-slate-500 hover:bg-white hover:text-slate-900"
                    aria-label="Close notification details"
                >
                    <X className="h-4 w-4"/>
                </button>
            </div>
            <p className="mt-2 text-sm text-slate-700">{selected.message}</p>
            {selected.actionUrl && <Link
                to={selected.actionUrl}
                onClick={onClose}
                className="mt-3 inline-flex items-center gap-2 text-sm font-medium text-blue-700"
            >
                <ExternalLink className="h-4 w-4"/>
                Open related page
            </Link>}
        </div>}
        <div className="max-h-[480px] overflow-y-auto">
            {visible.length === 0 ? <div
                className="flex flex-col items-center gap-2 px-6 py-12 text-center text-sm text-slate-500"
            >
                <div className="h-16 w-16 rounded-full bg-slate-100"/>
                No notifications yet
            </div> : visible.map((item) => <NotificationItem
                key={item.id}
                notification={item}
                onRead={onRead}
                onOpen={setSelected}
            />)}
        </div>
        <Link
            to="/notifications"
            onClick={onClose}
            className="block border-t px-4 py-3 text-center text-sm font-medium text-blue-700"
        >See
            all notifications</Link>
    </div>;
};
export {
    NotificationDropdown
};
