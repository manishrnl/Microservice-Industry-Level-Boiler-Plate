import {useEffect, useState} from "react";
import {UserRound} from "lucide-react";
import {displayUserName} from "../../utils/userDisplay";

const sizeClassNames = {
    sm: "h-9 w-9 text-sm",
    md: "h-11 w-11 text-base",
    lg: "h-16 w-16 text-xl",
    xl: "h-24 w-24 text-3xl"
};
const Avatar = ({src, name, email, size = "md"}) => {
    const [imageFailed, setImageFailed] = useState(false);
    useEffect(() => setImageFailed(false), [src]);
    const usableSrc = typeof src === "string" && src.trim() && !imageFailed ? src.trim() : "";
    const label = displayUserName(name, email);
    return <span
        className={`${sizeClassNames[size]} grid shrink-0 place-items-center overflow-hidden rounded-full border border-slate-200 bg-slate-100 font-semibold text-slate-600 shadow-sm`}
        aria-label={`${label} profile image`}
        title={label}
    >
            {usableSrc ? <img src={usableSrc} alt="" className="h-full w-full object-cover" onError={() => setImageFailed(true)}/> : <span
                className="grid h-full w-full place-items-center bg-teal-50 text-teal-700">
                    {initials(name, email) || <UserRound className="h-5 w-5"/>}
                </span>}
        </span>;
};
const initials = (name, email) => {
    const source = displayUserName(name, email, "");
    return source?.trim().split(/\s+/).slice(0, 2).map((part) => part[0]?.toUpperCase()).join("");
};
export {
    Avatar
};
