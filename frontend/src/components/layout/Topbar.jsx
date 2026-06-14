import {ChevronDown, FlaskConical, History, LogOut, RadioTower, ShieldCheck, Users} from "lucide-react";
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
import {LanguageSelector} from "../common/LanguageSelector";

const topNavLinkClass = (isActive) => `whitespace-nowrap rounded-md border px-2.5 py-2 text-xs font-bold shadow-sm transition duration-200 ${isActive ? "border-slate-950 bg-slate-950 text-white dark:border-teal-300 dark:bg-teal-300 dark:text-slate-950" : "border-transparent bg-white/0 text-slate-600 hover:-translate-y-0.5 hover:border-teal-300 hover:bg-teal-50 hover:text-slate-950 hover:shadow-md dark:text-slate-300 dark:hover:border-teal-300/50 dark:hover:bg-teal-300/10 dark:hover:text-white"}`;
const iconButtonClass = "grid h-9 w-9 place-items-center rounded-md border border-slate-200 bg-white text-slate-700 shadow-sm transition duration-200 hover:-translate-y-0.5 hover:border-teal-300 hover:bg-teal-50 hover:text-slate-950 hover:shadow-md focus:outline-none focus:ring-2 focus:ring-teal-500/25 dark:border-white/10 dark:bg-slate-900 dark:text-slate-100 dark:hover:border-teal-300/50 dark:hover:bg-teal-300/10 dark:hover:text-white sm:h-10 sm:w-10";

const Topbar = () => {
    const logout = useAuthStore((state) => state.logout);
    const {identity, identityReady} = useAccountIdentity();
    const [profileOpen, setProfileOpen] = useState(false);
    const [logoutConfirmOpen, setLogoutConfirmOpen] = useState(false);
    const [logoutPending, setLogoutPending] = useState(false);
    const [closedMenu, setClosedMenu] = useState("");
    const {isAdmin, isSuperAdmin} = usePermission();
    const navigate = useNavigate();
    const location = useLocation();

    const handleLogout = async () => {
        setLogoutPending(true);
        try {
            await logout();
            setProfileOpen(false);
            setLogoutConfirmOpen(false);
            navigate("/login", {replace: true});
        } finally {
            setLogoutPending(false);
        }
    };
    const requestLogout = () => {
        setProfileOpen(false);
        setLogoutConfirmOpen(true);
    };
    const greetingName = identityReady ? firstDisplayName(identity?.name, identity?.email, "") : "";
    const primaryLinks = mainLinks.slice(0, 3);
    const moreLinks = mainLinks.slice(3);
    const adminLinks = [
        ...(isSuperAdmin() ? [{label: "Super Admin", to: "/app/super-admin", Icon: ShieldCheck}] : []),
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
                    className={({isActive}) => topNavLinkClass(isActive)}
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
            <LanguageSelector compact/>
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
                    onLogout={requestLogout}
                />}
            </div>
            <button
                onClick={requestLogout}
                className={iconButtonClass}
                aria-label="Log out"
            >
                <LogOut className="h-4 w-4"/>
            </button>
        </div>
        {logoutConfirmOpen && <LogoutConfirmDialog
            pending={logoutPending}
            onCancel={() => setLogoutConfirmOpen(false)}
            onConfirm={() => void handleLogout()}
        />}
    </header>;
};
const LogoutConfirmDialog = ({pending, onCancel, onConfirm}) => <div className="fixed inset-0 z-[120] grid place-items-center bg-slate-950/35 px-4 backdrop-blur-sm">
    <div className="w-full max-w-sm overflow-hidden rounded-xl border border-slate-200/80 bg-white/95 shadow-[0_28px_90px_rgba(15,23,42,0.30)] ring-1 ring-slate-950/5 backdrop-blur-xl dark:border-white/10 dark:bg-slate-950/95 dark:text-white dark:ring-white/10">
        <div className="border-b border-slate-200 px-4 py-4 dark:border-white/10">
            <h2 className="text-base font-bold text-slate-950 dark:text-white">Confirm logout</h2>
            <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">Are you sure you want to end this session?</p>
        </div>
        <div className="flex justify-end gap-2 px-4 py-3">
            <button
                type="button"
                onClick={onCancel}
                disabled={pending}
                className="inline-flex h-10 items-center rounded-md border border-slate-200 bg-white px-4 text-sm font-semibold text-slate-700 shadow-sm transition duration-200 hover:-translate-y-0.5 hover:border-teal-300 hover:bg-teal-50 hover:text-slate-950 hover:shadow-md focus:outline-none focus:ring-2 focus:ring-teal-500/25 dark:border-white/10 dark:bg-slate-900 dark:text-slate-100 dark:hover:border-teal-300/50 dark:hover:bg-teal-300/10 dark:hover:text-white"
            >
                Cancel
            </button>
            <button
                type="button"
                onClick={onConfirm}
                disabled={pending}
                className="inline-flex h-10 items-center rounded-md border border-red-500/50 bg-red-600 px-4 text-sm font-semibold text-white shadow-sm transition duration-200 hover:-translate-y-0.5 hover:bg-red-500 hover:shadow-md focus:outline-none focus:ring-2 focus:ring-red-500/30 disabled:cursor-wait disabled:opacity-70"
            >
                {pending ? "Logging out..." : "Log out"}
            </button>
        </div>
    </div>
</div>;
const NavDropdown = ({label, links, active, closedMenu, onClose, onOpen}) => <div
    className="group relative"
    onMouseEnter={onOpen}
    onFocus={onOpen}
>
    <button
        type="button"
        className={`inline-flex items-center gap-1 ${topNavLinkClass(active)} ${active ? "" : "group-hover:-translate-y-0.5 group-hover:border-teal-300 group-hover:bg-teal-50 group-hover:text-slate-950 group-hover:shadow-md dark:group-hover:border-teal-300/50 dark:group-hover:bg-teal-300/10 dark:group-hover:text-white"}`}
        aria-haspopup="menu"
    >
        {label}
        <ChevronDown className="h-3.5 w-3.5 transition group-hover:rotate-180 group-focus-within:rotate-180"/>
    </button>
    <div className={`absolute left-1/2 top-full z-50 w-60 -translate-x-1/2 pt-2 ${closedMenu === label ? "hidden" : "hidden group-hover:block group-focus-within:block"}`}>
        <div className="absolute left-1/2 top-0 h-3 w-3 -translate-x-1/2 rotate-45 border-l border-t border-slate-200/80 bg-white/95 dark:border-white/10 dark:bg-slate-950/95"/>
        <div className="overflow-hidden rounded-xl border border-slate-200/80 bg-white/95 p-2 shadow-[0_24px_70px_rgba(15,23,42,0.22)] ring-1 ring-slate-950/5 backdrop-blur-xl dark:border-white/10 dark:bg-slate-950/95 dark:shadow-[0_28px_80px_rgba(0,0,0,0.55)] dark:ring-white/10">
            {links.map(({label: itemLabel, to, Icon, end}) => <NavLink
                key={to}
                to={to}
                end={end}
                onClick={onClose}
                className={({isActive}) => `group/item flex items-center gap-2.5 rounded-md px-2 py-2 text-sm transition duration-150 ${isActive ? "bg-teal-50 text-teal-900 ring-1 ring-teal-200 dark:bg-teal-300/15 dark:text-teal-100 dark:ring-teal-300/25" : "text-slate-600 hover:bg-slate-100 hover:text-slate-950 dark:text-slate-300 dark:hover:bg-white/10 dark:hover:text-white"}`}
            >
                {Icon && <span className="grid h-7 w-7 shrink-0 place-items-center rounded-md bg-slate-100 text-slate-500 transition group-hover/item:bg-white dark:bg-white/10 dark:text-teal-300 dark:group-hover/item:bg-white/15">
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
