import {ArrowRight, Github, Linkedin} from "lucide-react";
import {useEffect, useRef, useState} from "react";
import {endpoints} from "../../api/endpoints";
import {env} from "../../config/env";
import {useApiActivityStore} from "../../store/apiActivityStore";
import {getPreferredTimeZone} from "../../store/preferencesStore";
import {getClientLocalTime} from "../../utils/clientContext";

const allProviders = [
    {
        provider: "google",
        label: "Google",
        Icon: "google",
        className: "border-slate-200 bg-white text-slate-800 shadow-slate-950/5 hover:border-slate-300 hover:bg-slate-50 dark:border-white/10 dark:bg-white/[0.06] dark:text-slate-100 dark:hover:bg-white/[0.1]",
        iconClassName: "bg-white text-slate-950 dark:bg-white dark:text-slate-950"
    },
    {
        provider: "github",
        label: "GitHub",
        Icon: Github,
        className: "border-slate-900 bg-slate-950 text-white shadow-slate-950/15 hover:bg-slate-800 dark:border-white/10 dark:bg-white dark:text-slate-950 dark:hover:bg-slate-200",
        iconClassName: "bg-white/10 text-white dark:bg-slate-950 dark:text-white"
    },
    {
        provider: "linkedin",
        label: "LinkedIn",
        Icon: Linkedin,
        className: "border-[#0a66c2] bg-[#0a66c2] text-white shadow-blue-900/15 hover:bg-[#0759aa] dark:border-[#79b7f2] dark:bg-[#0a66c2] dark:text-white dark:hover:bg-[#1473d1]",
        iconClassName: "bg-white/15 text-white"
    }
];
const ProviderIcon = ({icon, className}) => {
    if (icon === "google") {
        return <span
            className={`grid h-8 w-8 place-items-center rounded-md border border-slate-200 text-sm font-semibold shadow-sm ${className}`}
        >
        <span
            className="bg-[linear-gradient(90deg,#4285f4,#34a853,#fbbc05,#ea4335)] bg-clip-text text-transparent"
        >G</span>
      </span>;
    }
    const Icon = icon;
    return <span className={`grid h-8 w-8 place-items-center rounded-md ${className}`}>
      <Icon className="h-4 w-4"/>
    </span>;
};
const OAuthButtons = ({mode}) => {
    const action = mode === "login" ? "Sign in" : "Sign up";
    const providers = allProviders.filter((item) => env.oauthProviders.includes(item.provider));
    const [loadingProvider, setLoadingProvider] = useState(null);
    const startActivity = useApiActivityStore((state) => state.startActivity);
    const stopActivityRef = useRef(null);
    useEffect(() => () => stopActivityRef.current?.(), []);
    const startOAuth = (provider) => {
        const label = allProviders.find((item) => item.provider === provider)?.label ?? provider;
        const authorizeUrl = new URL(endpoints.auth.oauthAuthorize(provider), window.location.origin);
        const timeZone = getPreferredTimeZone();
        if (timeZone) {
            authorizeUrl.searchParams.set("timeZone", timeZone);
        }
        authorizeUrl.searchParams.set("localTime", getClientLocalTime(timeZone));
        setLoadingProvider(provider);
        stopActivityRef.current?.();
        stopActivityRef.current = startActivity(`Opening ${label}`);
        window.setTimeout(() => stopActivityRef.current?.(), 18e4);
        window.setTimeout(() => window.location.assign(authorizeUrl.toString()), 80);
    };
    if (providers.length === 0) {
        return null;
    }
    return <div className="grid gap-2">
        {providers.map(({provider, label, Icon, className, iconClassName}) => <button
            key={provider}
            type="button"
            onClick={() => startOAuth(provider)}
            disabled={loadingProvider !== null}
            aria-label={`${action} with ${label}`}
            className={`group flex min-h-12 w-full items-center justify-between rounded-md border px-3 text-sm font-semibold shadow-sm transition duration-200 hover:-translate-y-0.5 hover:shadow-lg focus:outline-none focus:ring-4 focus:ring-teal-500/15 disabled:cursor-wait disabled:opacity-75 ${className}`}
        >
          <span className="flex items-center gap-3">
            <ProviderIcon icon={Icon} className={iconClassName}/>
            <span>{loadingProvider === provider ? `Connecting to ${label}` : `${action} with ${label}`}</span>
          </span>
            <ArrowRight className="h-4 w-4 opacity-55 transition group-hover:translate-x-0.5 group-hover:opacity-90"/>
        </button>)}
    </div>;
};
export {
    OAuthButtons
};
