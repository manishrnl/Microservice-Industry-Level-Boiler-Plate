import {AlertTriangle, CreditCard, Settings, Shield} from "lucide-react";
import {relativeTime} from "../../utils/dateUtils";

const iconMap = {
    AUTH: Shield,
    PAYMENT: CreditCard,
    SYSTEM: Settings,
    ALERT: AlertTriangle
};
const NotificationItem = ({notification, onRead, onOpen}) => {
    const Icon = iconMap[notification.category];
    const open = () => {
        onRead(notification.id);
        onOpen?.(notification);
    };
    return <button
        onClick={open}
        className={`grid w-full grid-cols-[32px_1fr] gap-3 border-l-4 px-3 py-3 text-left transition hover:bg-slate-50 ${notification.read ? "border-transparent bg-white" : "border-blue-600 bg-blue-50"}`}
    >
        <Icon className="mt-1 h-5 w-5 text-slate-600"/>
        <span className="min-w-0">
        <span className="block text-sm font-medium text-slate-950">{notification.title}</span>
        <span
            className="line-clamp-2 block text-sm text-slate-600"
        >{notification.message}</span>
        <span
            className="mt-1 block text-xs text-slate-500"
        >{relativeTime(notification.createdAt)}</span>
      </span>
    </button>;
};
export {
    NotificationItem
};
