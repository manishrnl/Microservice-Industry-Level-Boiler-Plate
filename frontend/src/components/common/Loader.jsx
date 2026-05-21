const Loader = ({message = "Loading", variant = "inline"}) => {
    if (variant === "skeleton") {
        return <div
            className="h-24 w-full animate-pulse rounded-md bg-slate-200"
            aria-label={message}
        />;
    }
    const spinner = <span
        className="h-5 w-5 animate-spin rounded-full border-2 border-slate-300 border-t-slate-900"
    />;
    if (variant === "fullscreen") {
        return <div className="flex min-h-screen flex-col items-center justify-center gap-3">
            {spinner}
            <p className="text-sm text-slate-600">{message}</p>
        </div>;
    }
    return <span className="inline-flex items-center gap-2 text-sm text-slate-600">
      {spinner}
        {message}
    </span>;
};
export {
    Loader
};
