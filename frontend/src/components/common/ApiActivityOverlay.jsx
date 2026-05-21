import {Clock3, LoaderCircle, Server} from "lucide-react";
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
    const isColdStart = elapsedSeconds >= 3;
    const isLongWait = elapsedSeconds >= 45;
    return <div
        className="pointer-events-none fixed inset-x-0 bottom-5 z-[80] flex justify-center px-4"
    >
        <div
            className="flex max-w-md items-start gap-3 rounded-lg border border-white/80 bg-white/95 p-4 text-slate-900 shadow-[0_18px_55px_rgba(15,23,42,0.2)] backdrop-blur-xl dark:border-white/10 dark:bg-slate-950/95 dark:text-white"
        >
        <span
            className="mt-0.5 grid h-9 w-9 shrink-0 place-items-center rounded-md bg-teal-50 text-teal-700 dark:bg-teal-400/10 dark:text-teal-300"
        >
          {isColdStart ? <Server className="h-5 w-5"/> :
              <LoaderCircle className="h-5 w-5 animate-spin"/>}
        </span>
            <div className="min-w-0">
                <div className="flex items-center gap-2">
                    <p className="text-sm font-semibold">{isColdStart ? "Waking backend on Render" : message}</p>
                    <LoaderCircle
                        className="h-3.5 w-3.5 animate-spin text-teal-600 dark:text-teal-300"
                    />
                </div>
                <p className="mt-1 text-xs leading-5 text-slate-600 dark:text-slate-300">
                    {isColdStart ? "First request after inactivity can take about 3 minutes. Keep this tab open while the service starts." : "Please wait while the request completes."}
                </p>
                {isLongWait &&
                    <p className="mt-2 inline-flex items-center gap-1.5 rounded-md bg-amber-50 px-2 py-1 text-xs font-medium text-amber-800 dark:bg-amber-400/10 dark:text-amber-200">
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
