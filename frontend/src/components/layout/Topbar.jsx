import {LogOut} from "lucide-react";
import {useNavigate} from "react-router-dom";
import {Avatar} from "../common/Avatar";
import {NotificationBell} from "../notifications/NotificationBell";
import {useAuthStore} from "../../store/authStore";

const Topbar = () => {
    const user = useAuthStore((state) => state.user);
    const logout = useAuthStore((state) => state.logout);
    const navigate = useNavigate();
    const handleLogout = async () => {
        await logout();
        navigate("/login", {replace: true});
    };
    const firstName = firstDisplayName(user?.name, user?.email).toUpperCase();
    return <header
        className="flex h-16 items-center justify-between border-b border-slate-200 bg-white px-6"
    >
        <span className="font-semibold text-slate-950">Platform</span>
        <div className="flex items-center gap-4">
            <span className="text-sm font-semibold text-slate-700">Welcome {firstName}</span>
            <NotificationBell/>
            <button
                onClick={() => navigate("/profile")}
                className="rounded-full outline-none ring-offset-2 transition focus:ring-2 focus:ring-teal-500"
                aria-label="Open profile"
            >
                <Avatar src={user?.avatarUrl} name={user?.name} email={user?.email} size="sm"/>
            </button>
            <button
                onClick={handleLogout}
                className="grid h-10 w-10 place-items-center rounded-md border border-slate-200 bg-white text-slate-700 shadow-sm hover:bg-slate-50"
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
