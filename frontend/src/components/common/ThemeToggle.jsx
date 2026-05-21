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
        className="grid h-10 w-10 place-items-center rounded-full border border-white/20 bg-white/75 text-slate-800 shadow-sm backdrop-blur transition hover:bg-white dark:border-white/10 dark:bg-white/10 dark:text-white dark:hover:bg-white/15"
    >
        {theme === "dark" ? <Sun className="h-4 w-4"/> : <Moon className="h-4 w-4"/>}
    </button>;
};
export {
    ThemeToggle
};
