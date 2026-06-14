import {Moon, Sun} from "lucide-react";
import {useEffect, useState} from "react";

const getInitialTheme = () => {
    const saved = window.localStorage.getItem("theme");
    if (saved === "light" || saved === "dark") {
        return saved;
    }
    return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
};
const ThemeToggle = () => {
    const [theme, setTheme] = useState(getInitialTheme);
    useEffect(() => {
        document.documentElement.classList.toggle("dark", theme === "dark");
        window.localStorage.setItem("theme", theme);
    }, [theme]);
    return <button
        type="button"
        aria-label="Toggle theme"
        onClick={() => setTheme((value) => value === "dark" ? "light" : "dark")}
        className="grid h-10 w-10 place-items-center rounded-md border border-slate-200 bg-white text-slate-700 shadow-sm backdrop-blur transition duration-200 hover:-translate-y-0.5 hover:border-teal-300 hover:bg-teal-50 hover:text-slate-950 hover:shadow-md focus:outline-none focus:ring-2 focus:ring-teal-500/25 dark:border-white/10 dark:bg-slate-900 dark:text-slate-100 dark:hover:border-teal-300/50 dark:hover:bg-teal-300/10 dark:hover:text-white"
    >
        {theme === "dark" ? <Sun className="h-4 w-4"/> : <Moon className="h-4 w-4"/>}
    </button>;
};
export {
    ThemeToggle
};
