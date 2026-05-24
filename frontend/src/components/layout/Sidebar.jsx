import {
    Bell,
    Bot,
    CreditCard,
    FileText,
    Gauge,
    History,
    RadioTower,
    ShieldCheck,
    UserRound,
    Users
} from "lucide-react";
import {NavLink} from "react-router-dom";
import {usePermission} from "../../hooks/usePermission";
import {BrandMark} from "./BrandMark";

const mainLinks = [
    {label: "Dashboard", to: "/app/dashboard", Icon: Gauge},
    {label: "AI Chat", to: "/app/ai", Icon: Bot},
    {label: "Notifications", to: "/app/notifications", Icon: Bell},
    {label: "Files", to: "/app/files", Icon: FileText},
    {label: "Payments", to: "/app/payments", Icon: CreditCard},
    {label: "Sessions", to: "/app/sessions", Icon: ShieldCheck},
    {label: "Profile", to: "/app/profile", Icon: UserRound}
];
const Sidebar = () => {
    const {isAdmin} = usePermission();
    const adminLinks = isAdmin() ? [
        {label: "Users", to: "/app/admin/users", Icon: Users},
        {label: "Audit", to: "/app/admin/audit", Icon: History},
        {label: "Observability", to: "/app/admin/observability", Icon: RadioTower}
    ] : [];
    return <aside className="hidden w-64 border-r border-slate-200 bg-white p-4 dark:border-white/10 dark:bg-slate-950 md:block">
        <BrandMark className="mb-5 px-1"/>
        <nav className="space-y-1">
            {[...mainLinks, ...adminLinks].map(({label, to, Icon, end}) => <NavLink
                key={to}
                to={to}
                end={end}
                className={({isActive}) => `flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium ${isActive ? "bg-slate-900 text-white dark:bg-teal-300 dark:text-slate-950" : "text-slate-700 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-white/10"}`}
            >
                <Icon className="h-4 w-4"/>
                {label}
            </NavLink>)}
        </nav>
    </aside>;
};
export {
    mainLinks,
    Sidebar
};
