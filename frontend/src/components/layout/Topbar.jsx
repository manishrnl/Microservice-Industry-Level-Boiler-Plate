import {ChevronDown, FlaskConical, History, LogOut, RadioTower, Users} from "lucide-react";
import {NavLink, useLocation, useNavigate} from "react-router-dom";
import {useState} from "react";
import {ThemeToggle} from "../common/ThemeToggle";
import {Avatar} from "../common/Avatar";
import {NotificationBell} from "../notifications/NotificationBell";
import {useAuthStore} from "../../store/authStore";
import {usePermission} from "../../hooks/usePermission";
import {firstDisplayName} from "../../utils/userDisplay";
import {useAccountIdentity} from "../../hooks/useAccountIdentity";
import {ProfileMenu} from "./ProfileMenu";
import {mainLinks} from "./Sidebar";
import {BrandMark} from "./BrandMark";

const Topbar = () => {
    const logout = useAuthStore((state) => state.logout);
    const {identity, identityReady} = useAccountIdentity();
    const [profileOpen, setProfileOpen] = useState(false);
    const [closedMenu, setClosedMenu] = useState("");
    const {isAdmin} = usePermission();
    const navigate = useNavigate();
    const location = useLocation();

    const handleLogout = async () => {
        await logout();
        setProfileOpen(false);
        navigate("/login", {replace: true});
    };
    const greetingName = identityReady ? firstDisplayName(identity?.name, identity?.email, "") : "";
    const primaryLinks = mainLinks.slice(0, 3);
    const moreLinks = mainLinks.slice(3);
    const adminLinks = [
        {label: "Users", to: "/app/admin/users", Icon: Users},
        {label: "Audit", to: "/app/admin/audit", Icon: History},
        {label: "Observability", to: "/app/admin/observability", Icon: RadioTower},
        {label: "Loki Logs", to: "/app/admin/logs", Icon: RadioTower},
        {label: "testsApp", to: "/app/admin/applicationTests", Icon: FlaskConical}
    ];
    return <header className="grid min-h-16 grid-cols-[1fr_auto] items-center gap-3 border-b border-slate-200 bg-white px-4 py-2 dark:border-white/10 dark:bg-slate-950 sm:px-6 lg:grid-cols-[240px_minmax(0,1fr)_auto]">
        <div className="flex min-w-0 items-center">
            <BrandMark className="max-w-full"/>
        </div>
        <nav className="hidden min-w-0 items-center justify-center gap-1 lg:flex">
                {primaryLinks.map(({label, to, end}) => <NavLink
                    key={to}
                    to={to}
                    end={end}
                    className={({isActive}) => `whitespace-nowrap rounded-md px-2.5 py-2 text-xs font-bold transition ${isActive ? "bg-slate-950 text-white dark:bg-teal-300 dark:text-slate-950" : "text-slate-600 hover:bg-slate-100 hover:text-slate-950 dark:text-slate-300 dark:hover:bg-white/10 dark:hover:text-white"}`}
                >
                    {label}
                </NavLink>)}
                <NavDropdown
                    label="More"
                    links={moreLinks}
                    active={moreLinks.some((link) => location.pathname === link.to)}
                    closedMenu={closedMenu}
                    onClose={() => setClosedMenu("More")}
                    onOpen={() => setClosedMenu("")}
                />
                {isAdmin() && <NavDropdown
                    label="Admin"
                    links={adminLinks}
                    active={adminLinks.some((link) => location.pathname === link.to)}
                    closedMenu={closedMenu}
                    onClose={() => setClosedMenu("Admin")}
                    onOpen={() => setClosedMenu("")}
                />}
        </nav>
        <div className="flex min-w-0 shrink-0 items-center justify-end gap-2 sm:gap-4">
            {greetingName && <span className="hidden text-sm font-semibold text-slate-700 dark:text-slate-200 sm:inline">Welcome {greetingName}</span>}
            <ThemeToggle/>
            <NotificationBell/>
            <div className="relative">
                <button
                onClick={() => setProfileOpen((value) => !value)}
                className="rounded-full outline-none ring-offset-2 transition focus:ring-2 focus:ring-teal-500"
                aria-label="Toggle profile menu"
                aria-expanded={profileOpen}
            >
                <Avatar src={identity?.avatarUrl} name={identity?.name} email={identity?.email} size="sm"/>
            </button>
                {profileOpen && <ProfileMenu
                    user={identity}
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
const NavDropdown = ({label, links, active, closedMenu, onClose, onOpen}) => <div
    className="group relative"
    onMouseEnter={onOpen}
    onFocus={onOpen}
>
    <button
        type="button"
        className={`inline-flex items-center gap-1 whitespace-nowrap rounded-md px-2.5 py-2 text-xs font-bold transition ${active ? "bg-slate-950 text-white dark:bg-teal-300 dark:text-slate-950" : "text-slate-600 hover:bg-slate-100 hover:text-slate-950 group-hover:bg-slate-100 group-hover:text-slate-950 dark:text-slate-300 dark:hover:bg-white/10 dark:hover:text-white dark:group-hover:bg-white/10 dark:group-hover:text-white"}`}
        aria-haspopup="menu"
    >
        {label}
        <ChevronDown className="h-3.5 w-3.5 transition group-hover:rotate-180 group-focus-within:rotate-180"/>
    </button>
    <div className={`absolute left-1/2 top-full z-50 w-56 -translate-x-1/2 pt-2 ${closedMenu === label ? "hidden" : "hidden group-hover:block group-focus-within:block"}`}>
        <div className="absolute left-1/2 top-0 h-3 w-3 -translate-x-1/2 rotate-45 border-l border-t border-slate-200 bg-white dark:border-white/10 dark:bg-slate-950"/>
        <div className="overflow-hidden rounded-md border border-slate-200 bg-white py-1.5 shadow-[0_18px_55px_rgba(15,23,42,0.18)] ring-1 ring-slate-950/5 dark:border-white/10 dark:bg-slate-950 dark:ring-white/10">
            {links.map(({label: itemLabel, to, Icon, end}) => <NavLink
                key={to}
                to={to}
                end={end}
                onClick={onClose}
                className={({isActive}) => `flex items-center gap-2.5 px-3 py-2 text-sm transition ${isActive ? "bg-slate-100 text-slate-950 dark:bg-white/10 dark:text-white" : "text-slate-600 hover:bg-slate-50 hover:text-slate-950 dark:text-slate-300 dark:hover:bg-white/10 dark:hover:text-white"}`}
            >
                {Icon && <span className="grid h-7 w-7 shrink-0 place-items-center rounded-md bg-slate-100 text-slate-600 dark:bg-white/10 dark:text-teal-300">
                    <Icon className="h-4 w-4"/>
                </span>}
                <span className="min-w-0 truncate font-semibold">{itemLabel}</span>
            </NavLink>)}
        </div>
    </div>
</div>;
export {
    Topbar
};
