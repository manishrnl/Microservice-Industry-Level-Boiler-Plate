import {AlertTriangle, ChevronDown, CreditCard, ExternalLink, Settings, Shield} from "lucide-react";
import {useState} from "react";
import {Link} from "react-router-dom";
import {relativeTime} from "../../utils/dateUtils";

const iconMap = {
    AUTH: Shield,
    PAYMENT: CreditCard,
    SYSTEM: Settings,
    ALERT: AlertTriangle
};
const NotificationItem = ({notification, onRead, onOpen}) => {
    const [expanded, setExpanded] = useState(false);
    const Icon = iconMap[notification.category] ?? Settings;
    const open = () => {
        onRead(notification.id);
        setExpanded((value) => !value);
        onOpen?.(notification);
    };
    return <div className={`border-l-4 transition ${notification.read ? "border-transparent bg-white dark:bg-slate-950" : "border-blue-600 bg-blue-50 dark:border-teal-300 dark:bg-teal-300/10"}`}>
        <button
            onClick={open}
            className="grid w-full grid-cols-[32px_1fr_auto] gap-3 px-3 py-3 text-left transition hover:bg-slate-50 dark:hover:bg-white/10"
            aria-expanded={expanded}
        >
            <Icon className="mt-1 h-5 w-5 text-slate-600 dark:text-slate-300"/>
            <span className="min-w-0">
                <span className="block text-sm font-medium text-slate-950 dark:text-white">{notification.title}</span>
                <span className={`block text-sm text-slate-600 dark:text-slate-300 ${expanded ? "" : "line-clamp-2"}`}>{notification.message}</span>
                <span className="mt-1 block text-xs text-slate-500 dark:text-slate-400">{relativeTime(notification.createdAt)}</span>
            </span>
            <ChevronDown className={`mt-1 h-4 w-4 text-slate-400 transition dark:text-slate-500 ${expanded ? "rotate-180" : ""}`}/>
        </button>
        {expanded && <section className="border-t border-slate-200 bg-slate-50 px-4 py-3 dark:border-white/10 dark:bg-white/[0.04]">
            <div className="grid gap-2 text-xs text-slate-600 dark:text-slate-300 sm:grid-cols-2">
                <span><strong className="text-slate-900 dark:text-white">Category:</strong> {notification.category ?? "GENERAL"}</span>
                <span><strong className="text-slate-900 dark:text-white">Status:</strong> {notification.read ? "Read" : "Unread"}</span>
                <span className="sm:col-span-2"><strong className="text-slate-900 dark:text-white">Created:</strong> {notification.createdAt ? new Date(notification.createdAt).toLocaleString() : "Not reported"}</span>
            </div>
            <p className="mt-3 text-sm leading-6 text-slate-700 dark:text-slate-200">{notification.message || "No additional details were sent with this notification."}</p>
            {notification.actionUrl && <Link
                to={notification.actionUrl}
                className="mt-3 inline-flex items-center gap-2 text-sm font-medium text-blue-700 dark:text-teal-300"
            >
                <ExternalLink className="h-4 w-4"/>
                Open related page
            </Link>}
        </section>}
    </div>;
};
export {
    NotificationItem
};
