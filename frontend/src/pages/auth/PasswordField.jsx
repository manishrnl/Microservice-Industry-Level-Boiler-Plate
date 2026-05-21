import {Eye, EyeOff} from "lucide-react";
import {useState} from "react";

const PasswordField = ({label, placeholder, registration}) => {
    const [visible, setVisible] = useState(false);
    return <label className="mb-4 block">
            <span
                className="mb-1.5 block text-sm font-semibold text-slate-700 dark:text-slate-200"
            >{label}</span>
        <span className="relative block">
        <input
            className="h-12 w-full rounded-md border border-slate-200 bg-slate-50/80 px-3 pr-12 text-sm text-slate-950 shadow-inner shadow-slate-950/[0.03] outline-none transition placeholder:text-slate-400 focus:border-teal-500 focus:bg-white focus:ring-4 focus:ring-teal-500/10 dark:border-white/10 dark:bg-white/[0.06] dark:text-white dark:placeholder:text-slate-500 dark:focus:bg-white/[0.09]"
            placeholder={placeholder}
            type={visible ? "text" : "password"}
            {...registration}
        />
        <button
            type="button"
            aria-label={visible ? "Hide password" : "Show password"}
            onClick={() => setVisible((value) => !value)}
            className="absolute right-1.5 top-1/2 grid h-9 w-9 -translate-y-1/2 place-items-center rounded-md text-slate-500 transition hover:bg-white hover:text-slate-900 dark:text-slate-400 dark:hover:bg-white/10 dark:hover:text-white"
        >
          {visible ? <EyeOff className="h-4 w-4"/> : <Eye className="h-4 w-4"/>}
        </button>
      </span>
    </label>;
};
export {
    PasswordField
};
