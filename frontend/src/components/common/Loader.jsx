import {useApiActivityStore} from "../../store/apiActivityStore";

const Loader = ({message = "Loading", variant = "inline"}) => {
    const pendingCount = useApiActivityStore((state) => state.pendingCount);
    if (variant === "skeleton") {
        return <div
            className="h-24 w-full animate-pulse rounded-md bg-slate-200"
            aria-label={message}
        />;
    }
    const spinner = <span
        className="h-5 w-5 animate-spin rounded-full border-2 border-slate-400/30 border-t-teal-600 dark:border-white/20 dark:border-t-teal-200"
    />;
    if (variant === "fullscreen") {
        if (pendingCount > 0) {
            return null;
        }
        return <div className="pointer-events-none fixed bottom-4 right-4 z-[80] max-w-[calc(100vw-2rem)]">
            <div className="flex items-center gap-3 rounded-md border border-white/45 bg-white/45 px-4 py-3 text-slate-950 shadow-[0_18px_60px_rgba(15,23,42,0.16)] backdrop-blur-2xl dark:border-white/10 dark:bg-slate-950/45 dark:text-white">
                {spinner}
                <p className="text-sm font-semibold">{message}</p>
            </div>
        </div>;
    }
    return <span className="inline-flex items-center gap-2 text-sm text-slate-600 dark:text-slate-300">
      {spinner}
        {message}
    </span>;
};
export {
    Loader
};
