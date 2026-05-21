import {LogOut, ShieldCheck, UserRound} from "lucide-react";
import {useEffect, useRef} from "react";
import {Link} from "react-router-dom";
import {Avatar} from "../common/Avatar";

const ProfileMenu = ({user, onClose, onLogout}) => {
    const ref = useRef(null);
    useEffect(() => {
        const handler = (event) => {
            if (ref.current && !ref.current.contains(event.target)) {
                onClose();
            }
        };
        document.addEventListener("mousedown", handler);
        return () => document.removeEventListener("mousedown", handler);
    }, [onClose]);

    return <div
        ref={ref}
        className="fixed right-3 top-16 z-50 w-[min(360px,calc(100vw-1.5rem))] overflow-hidden rounded-md border border-slate-200 bg-white shadow-xl dark:border-white/10 dark:bg-slate-950"
    >
        <div className="border-b border-slate-200 bg-slate-50 px-4 py-4 dark:border-white/10 dark:bg-white/[0.04]">
            <div className="flex items-center gap-3">
                <Avatar src={user?.avatarUrl} name={user?.name} email={user?.email} size="md"/>
                <div className="min-w-0">
                    <p className="truncate text-sm font-semibold text-slate-950 dark:text-white">{user?.name ?? "User"}</p>
                    <p className="truncate text-xs text-slate-500 dark:text-slate-400">{user?.email}</p>
                </div>
            </div>
        </div>
        <nav className="p-2">
            <Link
                to="/app/profile"
                onClick={onClose}
                className="flex items-center gap-3 rounded-md px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-white/10"
            >
                <UserRound className="h-4 w-4"/>
                Profile settings
            </Link>
            <Link
                to="/app/sessions"
                onClick={onClose}
                className="flex items-center gap-3 rounded-md px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-white/10"
            >
                <ShieldCheck className="h-4 w-4"/>
                Active sessions
            </Link>
            <button
                type="button"
                onClick={onLogout}
                className="flex w-full items-center gap-3 rounded-md px-3 py-2 text-left text-sm font-semibold text-red-600 hover:bg-red-50 dark:text-red-300 dark:hover:bg-red-400/10"
            >
                <LogOut className="h-4 w-4"/>
                Log out
            </button>
        </nav>
    </div>;
};

export {
    ProfileMenu
};
