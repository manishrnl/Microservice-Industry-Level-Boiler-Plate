import {NavLink} from "react-router-dom";
import {usePermission} from "../../hooks/usePermission";
import {mainLinks} from "./Sidebar";
import {History, Users} from "lucide-react";

const MobileNav = () => {
    const {isAdmin} = usePermission();
    const adminLinks = isAdmin() ? [
        {label: "Users", to: "/app/admin/users", Icon: Users},
        {label: "Audit", to: "/app/admin/audit", Icon: History}
    ] : [];
    const links = [...mainLinks.slice(0, 5), ...adminLinks].slice(0, 7);

    return <nav className="fixed inset-x-0 bottom-0 z-40 border-t border-slate-200 bg-white/95 px-2 py-2 shadow-[0_-12px_30px_rgba(15,23,42,0.08)] backdrop-blur dark:border-white/10 dark:bg-slate-950/95 lg:hidden">
        <div className="mx-auto flex max-w-md justify-around gap-1">
            {links.map(({label, to, Icon}) => <NavLink
                key={to}
                to={to}
                className={({isActive}) => `flex min-w-0 flex-1 flex-col items-center justify-center rounded-md px-1 py-1.5 text-[10px] font-bold ${isActive ? "bg-slate-950 text-white dark:bg-teal-300 dark:text-slate-950" : "text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-white/10"}`}
            >
                <Icon className="h-4 w-4"/>
                <span className="mt-1 max-w-full truncate">{label}</span>
            </NavLink>)}
        </div>
    </nav>;
};

export {
    MobileNav
};
