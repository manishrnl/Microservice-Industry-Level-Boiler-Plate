import {
    Bell,
    Bot,
    CreditCard,
    FileText,
    Gauge,
    History,
    ShieldCheck,
    UserRound,
    Users
} from "lucide-react";
import {NavLink} from "react-router-dom";
import {usePermission} from "../../hooks/usePermission";

const links = [
    {label: "Dashboard", to: "/", Icon: Gauge},
    {label: "AI Chat", to: "/ai", Icon: Bot},
    {label: "Notifications", to: "/notifications", Icon: Bell},
    {label: "Files", to: "/files", Icon: FileText},
    {label: "Payments", to: "/payments", Icon: CreditCard},
    {label: "Sessions", to: "/sessions", Icon: ShieldCheck},
    {label: "Profile", to: "/profile", Icon: UserRound}
];
const Sidebar = () => {
    const {isAdmin} = usePermission();
    const adminLinks = isAdmin() ? [
        {label: "Users", to: "/admin/users", Icon: Users},
        {label: "Audit", to: "/admin/audit", Icon: History}
    ] : [];
    return <aside className="hidden w-64 border-r border-slate-200 bg-white p-4 md:block">
        <nav className="space-y-1">
            {[...links, ...adminLinks].map(({label, to, Icon}) => <NavLink
                key={to}
                to={to}
                className={({isActive}) => `flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium ${isActive ? "bg-slate-900 text-white" : "text-slate-700 hover:bg-slate-100"}`}
            >
                <Icon className="h-4 w-4"/>
                {label}
            </NavLink>)}
        </nav>
    </aside>;
};
export {
    Sidebar
};
