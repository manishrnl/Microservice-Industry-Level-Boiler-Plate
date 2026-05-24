import {Network} from "lucide-react";
import {Link} from "react-router-dom";

const COMPANY_NAME = "Microservice Template";
const COMPANY_TAGLINE = "Production-grade Java platform";

const BrandMark = ({to = "/", compact = false, className = "", ...props}) => {
    const content = <>
        <span className="grid h-10 w-10 shrink-0 place-items-center rounded-md bg-slate-950 text-white shadow-sm dark:bg-teal-300 dark:text-slate-950">
            <Network className="h-5 w-5"/>
        </span>
        {!compact && <span className="min-w-0">
            <span className="block truncate text-sm font-bold text-slate-950 dark:text-white">{COMPANY_NAME}</span>
            <span className="block truncate text-xs font-medium text-slate-500 dark:text-slate-400">{COMPANY_TAGLINE}</span>
        </span>}
    </>;

    if (!to) {
        return <div className={`flex min-w-0 items-center gap-3 ${className}`} {...props}>{content}</div>;
    }
    return <Link to={to} className={`flex min-w-0 items-center gap-3 ${className}`} {...props}>{content}</Link>;
};

export {
    BrandMark,
    COMPANY_NAME,
    COMPANY_TAGLINE
};
