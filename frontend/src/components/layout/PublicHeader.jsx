import {Menu, Network, X} from "lucide-react";
import {useState} from "react";
import {Link, NavLink} from "react-router-dom";
import {ThemeToggle} from "../common/ThemeToggle";
import {useAuthStore} from "../../store/authStore";

const links = [
    {label: "Architecture", href: "/#architecture"},
    {label: "Capabilities", href: "/#capabilities"},
    {label: "Analysis", href: "/#analysis"},
    {label: "Contact", href: "/#contact"}
];

const PublicHeader = () => {
    const [open, setOpen] = useState(false);
    const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
    const ctaPath = isAuthenticated ? "/app/dashboard" : "/login";
    const ctaLabel = isAuthenticated ? "Dashboard" : "Sign in";
    const scrollHomeTop = () => {
        if (window.location.pathname === "/") {
            window.scrollTo({top: 0, behavior: "smooth"});
        }
    };

    return <header className="sticky top-0 z-40 border-b border-slate-200/80 bg-white/90 backdrop-blur-xl dark:border-white/10 dark:bg-slate-950/90">
        <div className="mx-auto flex h-16 w-full max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
            <Link to="/" onClick={scrollHomeTop} className="flex min-w-0 items-center gap-3">
                <span className="grid h-10 w-10 shrink-0 place-items-center rounded-md bg-slate-950 text-white shadow-sm dark:bg-teal-300 dark:text-slate-950">
                    <Network className="h-5 w-5"/>
                </span>
                <span className="min-w-0">
                    <span className="block truncate text-sm font-bold text-slate-950 dark:text-white">Microservice Template</span>
                    <span className="block truncate text-xs font-medium text-slate-500 dark:text-slate-400">Production-grade Java platform</span>
                </span>
            </Link>

            <nav className="hidden items-center gap-1 md:flex">
                {links.map((item) => <a
                    key={item.href}
                    href={item.href}
                    className="rounded-md px-3 py-2 text-sm font-semibold text-slate-600 transition hover:bg-slate-100 hover:text-slate-950 dark:text-slate-300 dark:hover:bg-white/10 dark:hover:text-white"
                >
                    {item.label}
                </a>)}
            </nav>

            <div className="hidden items-center gap-2 sm:flex">
                <ThemeToggle/>
                {!isAuthenticated && <NavLink
                    to="/signup"
                    className="rounded-md px-3 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-white/10"
                >
                    Create account
                </NavLink>}
                <NavLink
                    to={ctaPath}
                    className="inline-flex h-10 items-center rounded-md bg-slate-950 px-4 text-sm font-semibold text-white shadow-sm transition hover:bg-slate-800 dark:bg-teal-300 dark:text-slate-950 dark:hover:bg-teal-200"
                >
                    {ctaLabel}
                </NavLink>
            </div>

            <button
                type="button"
                onClick={() => setOpen((value) => !value)}
                className="grid h-10 w-10 place-items-center rounded-md border border-slate-200 bg-white text-slate-800 shadow-sm sm:hidden dark:border-white/10 dark:bg-white/10 dark:text-white"
                aria-label="Toggle navigation"
            >
                {open ? <X className="h-5 w-5"/> : <Menu className="h-5 w-5"/>}
            </button>
        </div>

        {open && <div className="border-t border-slate-200 bg-white px-4 py-4 shadow-lg sm:hidden dark:border-white/10 dark:bg-slate-950">
            <div className="flex flex-col gap-2">
                {links.map((item) => <a
                    key={item.href}
                    href={item.href}
                    onClick={() => setOpen(false)}
                    className="rounded-md px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-white/10"
                >
                    {item.label}
                </a>)}
                <div className="mt-2 grid grid-cols-[auto_1fr] gap-2">
                    <ThemeToggle/>
                    <Link
                        to={ctaPath}
                        onClick={() => setOpen(false)}
                        className="inline-flex h-10 items-center justify-center rounded-md bg-slate-950 px-4 text-sm font-semibold text-white dark:bg-teal-300 dark:text-slate-950"
                    >
                        {ctaLabel}
                    </Link>
                </div>
            </div>
        </div>}
    </header>;
};

export {
    PublicHeader
};
