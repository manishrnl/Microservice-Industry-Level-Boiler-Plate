import {LogOut} from "lucide-react";
import {NavLink, useNavigate} from "react-router-dom";
import {useState} from "react";
import {ThemeToggle} from "../common/ThemeToggle";
import {Avatar} from "../common/Avatar";
import {NotificationBell} from "../notifications/NotificationBell";
import {useAuthStore} from "../../store/authStore";
import {usePermission} from "../../hooks/usePermission";
import {ProfileMenu} from "./ProfileMenu";
import {mainLinks} from "./Sidebar";

const Topbar = () => {
    const user = useAuthStore((state) => state.user);
    const logout = useAuthStore((state) => state.logout);
    const [profileOpen, setProfileOpen] = useState(false);
    const {isAdmin} = usePermission();
    const navigate = useNavigate();
    const handleLogout = async () => {
        await logout();
        setProfileOpen(false);
        navigate("/login", {replace: true});
    };
    const firstName = firstDisplayName(user?.name, user?.email).toUpperCase();
    const headerLinks = [
        ...mainLinks,
        ...(isAdmin() ? [
            {label: "Users", to: "/app/admin/users"},
            {label: "Audit", to: "/app/admin/audit"}
        ] : [])
    ];
    return <header className="flex min-h-16 items-center justify-between gap-3 border-b border-slate-200 bg-white px-4 py-2 dark:border-white/10 dark:bg-slate-950 sm:px-6">
        <div className="flex min-w-0 flex-1 items-center gap-4">
            <span className="shrink-0 truncate font-semibold text-slate-950 dark:text-white">Platform</span>
            <nav className="hidden min-w-0 items-center gap-1 overflow-x-auto lg:flex">
                {headerLinks.map(({label, to}) => <NavLink
                    key={to}
                    to={to}
                    className={({isActive}) => `whitespace-nowrap rounded-md px-2.5 py-2 text-xs font-bold transition ${isActive ? "bg-slate-950 text-white dark:bg-teal-300 dark:text-slate-950" : "text-slate-600 hover:bg-slate-100 hover:text-slate-950 dark:text-slate-300 dark:hover:bg-white/10 dark:hover:text-white"}`}
                >
                    {label}
                </NavLink>)}
            </nav>
        </div>
        <div className="flex shrink-0 items-center gap-2 sm:gap-4">
            <span className="hidden text-sm font-semibold text-slate-700 dark:text-slate-200 sm:inline">Welcome {firstName}</span>
            <ThemeToggle/>
            <NotificationBell/>
            <div className="relative">
                <button
                onClick={() => setProfileOpen((value) => !value)}
                className="rounded-full outline-none ring-offset-2 transition focus:ring-2 focus:ring-teal-500"
                aria-label="Toggle profile menu"
                aria-expanded={profileOpen}
            >
                <Avatar src={user?.avatarUrl} name={user?.name} email={user?.email} size="sm"/>
            </button>
                {profileOpen && <ProfileMenu
                    user={user}
                    onClose={() => setProfileOpen(false)}
                    onLogout={handleLogout}
                />}
            </div>
            <button
                onClick={handleLogout}
                className="grid h-9 w-9 place-items-center rounded-md border border-slate-200 bg-white text-slate-700 shadow-sm hover:bg-slate-50 dark:border-white/10 dark:bg-white/10 dark:text-slate-100 dark:hover:bg-white/15 sm:h-10 sm:w-10"
                aria-label="Log out"
            >
                <LogOut className="h-4 w-4"/>
            </button>
        </div>
    </header>;
};
const firstDisplayName = (name, email) => {
    const candidate = name && !name.includes("@") ? name : email?.split("@")[0];
    return candidate?.trim().split(/\s+/)[0] || "User";
};
export {
    Topbar
};
