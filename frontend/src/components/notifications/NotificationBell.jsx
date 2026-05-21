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
            className={`relative grid h-10 w-10 place-items-center rounded-full border border-slate-200 bg-white text-slate-700 shadow-sm hover:bg-slate-50 dark:border-white/10 dark:bg-white/10 dark:text-slate-100 dark:hover:bg-white/15 ${shake ? "animate-shake" : ""}`}
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
