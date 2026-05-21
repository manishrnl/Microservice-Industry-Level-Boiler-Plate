import {UserRound} from "lucide-react";

const sizeClassNames = {
    sm: "h-9 w-9 text-sm",
    md: "h-11 w-11 text-base",
    lg: "h-16 w-16 text-xl",
    xl: "h-24 w-24 text-3xl"
};
const Avatar = ({src, name, email, size = "md"}) => {
    const label = name || email || "User";
    return <span
        className={`${sizeClassNames[size]} grid shrink-0 place-items-center overflow-hidden rounded-full border border-slate-200 bg-slate-100 font-semibold text-slate-600 shadow-sm`}
        aria-label={`${label} profile image`}
        title={label}
    >
            {src ? <img src={src} alt="" className="h-full w-full object-cover"/> : <span
                className="grid h-full w-full place-items-center bg-teal-50 text-teal-700">
                    {initials(name, email) || <UserRound className="h-5 w-5"/>}
                </span>}
        </span>;
};
const initials = (name, email) => {
    const source = name && !name.includes("@") ? name : email?.split("@")[0];
    return source?.trim().split(/\s+/).slice(0, 2).map((part) => part[0]?.toUpperCase()).join("");
};
export {
    Avatar
};
