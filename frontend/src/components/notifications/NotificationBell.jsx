import {Bell} from "lucide-react";
import {useState} from "react";
import {NotificationBadge} from "./NotificationBadge";
import {NotificationDropdown} from "./NotificationDropdown";
import {useNotifications} from "./useNotifications";

const NotificationBell = () => {
    const [open, setOpen] = useState(false);
    const {
        notifications,
        unreadCount,
        shake,
        isMarkingAllRead,
        isClearingAll,
        markRead,
        markAllRead,
        clearNotifications
    } = useNotifications();
    return <div className="relative">
        <button
            aria-label="Notifications"
            onClick={() => setOpen((value) => !value)}
            className={`relative grid h-10 w-10 place-items-center rounded-md border border-slate-200 bg-white text-slate-700 shadow-sm transition duration-200 hover:-translate-y-0.5 hover:border-teal-300 hover:bg-teal-50 hover:text-slate-950 hover:shadow-md focus:outline-none focus:ring-2 focus:ring-teal-500/25 dark:border-white/10 dark:bg-slate-900 dark:text-slate-100 dark:hover:border-teal-300/50 dark:hover:bg-teal-300/10 dark:hover:text-white ${shake ? "animate-shake" : ""}`}
        >
            <Bell className="h-5 w-5"/>
            <NotificationBadge count={unreadCount}/>
        </button>
        {open && <NotificationDropdown
            notifications={notifications}
            onClose={() => setOpen(false)}
            onRead={markRead}
            onMarkAllRead={markAllRead}
            onClearAll={clearNotifications}
            markAllPending={isMarkingAllRead}
            clearAllPending={isClearingAll}
        />}
    </div>;
};
export {
    NotificationBell
};
