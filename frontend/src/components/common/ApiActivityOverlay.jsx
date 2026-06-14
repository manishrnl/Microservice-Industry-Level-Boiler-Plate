import {Clock3, Database, LoaderCircle, X} from "lucide-react";
import {useEffect, useMemo, useState} from "react";
import {useApiActivityStore} from "../../store/apiActivityStore";

const ApiActivityOverlay = () => {
    const {pendingCount, startedAt, message, detail, overlayDismissed, dismissOverlay} = useApiActivityStore();
    const [now, setNow] = useState(Date.now());
    useEffect(() => {
        if (pendingCount === 0) {
            return void 0;
        }
        setNow(Date.now());
        const interval = window.setInterval(() => setNow(Date.now()), 1e3);
        return () => window.clearInterval(interval);
    }, [pendingCount]);
    const elapsedSeconds = useMemo(() => {
        if (!startedAt) {
            return 0;
        }
        return Math.max(0, Math.floor((now - startedAt) / 1e3));
    }, [now, startedAt]);
    if (pendingCount === 0 || overlayDismissed) {
        return null;
    }
    const isLongWait = elapsedSeconds >= 45;
    return <div className="fixed bottom-4 right-4 z-[80] max-w-[calc(100vw-2rem)]">
        <div
            className="relative flex w-[min(30rem,calc(100vw-2rem))] items-start gap-3 rounded-md border border-white/45 bg-white/70 px-3.5 py-3 pr-12 text-slate-950 shadow-[0_18px_60px_rgba(15,23,42,0.16)] backdrop-blur-2xl dark:border-white/10 dark:bg-slate-950/70 dark:text-white"
        >
            <button
                type="button"
                aria-label="Close backend sync message"
                onClick={dismissOverlay}
                className="absolute right-2 top-2 grid h-8 w-8 place-items-center rounded-md border-2 border-red-600 bg-white text-red-600 shadow-sm transition hover:bg-red-50 hover:text-red-700 focus:outline-none focus:ring-2 focus:ring-red-500/50 active:scale-95 dark:bg-slate-950 dark:hover:bg-red-950/40"
            >
                <X className="h-4 w-4 stroke-[3]"/>
            </button>
            <span className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-teal-400/15 text-teal-700 ring-1 ring-teal-400/30 dark:bg-teal-300/10 dark:text-teal-200">
                <LoaderCircle className="h-5 w-5 animate-spin"/>
            </span>
            <div className="min-w-0">
                <p className="text-sm font-semibold leading-5">{message}</p>
                <p className="mt-0.5 flex items-start gap-1.5 text-xs leading-5 text-slate-600 dark:text-slate-300">
                    <Database className="mt-0.5 h-3.5 w-3.5 shrink-0"/>
                    <span>{detail}</span>
                </p>
                {pendingCount > 1 &&
                    <p className="mt-1 text-xs font-medium text-slate-500 dark:text-slate-400">
                        {pendingCount} backend requests are still running
                    </p>}
                {isLongWait &&
                    <p className="mt-1 inline-flex items-center gap-1.5 text-xs font-medium text-amber-700 dark:text-amber-200">
                        <Clock3 className="h-3.5 w-3.5"/>
                        Still waiting, about {elapsedSeconds}s elapsed
                    </p>}
            </div>
        </div>
    </div>;
};
export {
    ApiActivityOverlay
};
