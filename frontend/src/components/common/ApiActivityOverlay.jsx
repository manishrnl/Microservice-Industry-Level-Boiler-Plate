npimport {Clock3, LoaderCircle} from "lucide-react";
import {useEffect, useMemo, useState} from "react";
import {useApiActivityStore} from "../../store/apiActivityStore";

const ApiActivityOverlay = () => {
    const {pendingCount, startedAt, message} = useApiActivityStore();
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
    if (pendingCount === 0) {
        return null;
    }
    const isLongWait = elapsedSeconds >= 45;
    return <div className="pointer-events-none fixed inset-x-0 top-4 z-[80] flex justify-center px-4 sm:top-5">
        <div
            className="flex max-w-sm items-center gap-3 rounded-md border border-white/45 bg-slate-950/[0.08] px-3.5 py-3 text-slate-950 shadow-[0_18px_60px_rgba(15,23,42,0.16)] backdrop-blur-2xl supports-[backdrop-filter]:bg-slate-950/[0.06] dark:border-white/10 dark:bg-white/[0.08] dark:text-white"
        >
            <span className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-teal-400/15 text-teal-700 ring-1 ring-teal-400/30 dark:bg-teal-300/10 dark:text-teal-200">
                <LoaderCircle className="h-5 w-5 animate-spin"/>
            </span>
            <div className="min-w-0">
                <p className="truncate text-sm font-semibold">{message}</p>
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
