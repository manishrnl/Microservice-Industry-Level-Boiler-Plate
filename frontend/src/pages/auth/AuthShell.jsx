import {LockKeyhole, Network, ShieldCheck, Sparkles} from "lucide-react";
import {ThemeToggle} from "../../components/common/ThemeToggle";

const AuthShell = ({title, subtitle, children}) => <main
    className="relative min-h-screen overflow-hidden bg-[#eef3f8] text-slate-950 dark:bg-[#070a10]"
>
    <div
        className="absolute inset-0 bg-[linear-gradient(120deg,rgba(15,23,42,0.06)_0%,rgba(255,255,255,0.78)_36%,rgba(20,184,166,0.14)_100%)] dark:bg-[linear-gradient(125deg,rgba(2,6,23,0.98)_0%,rgba(15,23,42,0.94)_48%,rgba(13,42,48,0.92)_100%)]"
    />
    <div
        className="absolute inset-0 opacity-[0.18] dark:opacity-[0.12] [background-image:linear-gradient(rgba(15,23,42,0.36)_1px,transparent_1px),linear-gradient(90deg,rgba(15,23,42,0.36)_1px,transparent_1px)] [background-size:44px_44px]"
    />
    <div className="relative z-10 flex min-h-screen flex-col">
        <header
            className="mx-auto flex w-full max-w-6xl items-center justify-between px-5 py-5"
        >
            <div className="flex items-center gap-3">
          <span
              className="grid h-10 w-10 place-items-center rounded-md border border-slate-950/10 bg-white text-slate-950 shadow-sm dark:border-white/10 dark:bg-white/10 dark:text-white"
          >
            <Network className="h-5 w-5"/>
          </span>
                <div>
                    <p className="text-sm font-semibold text-slate-950 dark:text-white">Microservice
                        Platform</p>
                    <p className="text-xs text-slate-500 dark:text-slate-400">Identity
                        Console</p>
                </div>
            </div>
            <ThemeToggle/>
        </header>
        <section
            className="mx-auto grid w-full max-w-6xl flex-1 items-center gap-8 px-5 pb-8 lg:grid-cols-[minmax(0,1fr)_440px]"
        >
            <div className="hidden max-w-xl lg:block">
                <div
                    className="mb-5 inline-flex items-center gap-2 rounded-md border border-slate-950/10 bg-white/80 px-3 py-2 text-sm font-medium text-slate-700 shadow-sm backdrop-blur dark:border-white/10 dark:bg-white/10 dark:text-slate-200"
                >
                    <Sparkles className="h-4 w-4 text-amber-500"/>
                    Enterprise-ready access
                </div>
                <h1 className="max-w-lg text-5xl font-semibold leading-[1.05] text-slate-950 dark:text-white">A
                    cleaner front door for every service.</h1>
                <p className="mt-5 max-w-lg text-base leading-7 text-slate-600 dark:text-slate-300">
                    Sign in once, move across the platform with confidence, and keep every
                    route behind a polished identity layer.
                </p>
                <div className="mt-8 grid max-w-lg grid-cols-3 gap-3">
                    {[
                        {label: "OAuth2", value: "Ready", Icon: ShieldCheck},
                        {label: "JWT", value: "Secured", Icon: LockKeyhole},
                        {label: "RBAC", value: "Scoped", Icon: Network}
                    ].map(({label, value, Icon}) => <div
                        key={label}
                        className="rounded-md border border-white/70 bg-white/70 p-4 shadow-sm backdrop-blur dark:border-white/10 dark:bg-white/[0.08]"
                    >
                        <Icon
                            className="mb-4 h-5 w-5 text-teal-700 dark:text-teal-300"
                        />
                        <p className="text-xs font-medium uppercase text-slate-500 dark:text-slate-400">{label}</p>
                        <p className="mt-1 text-sm font-semibold text-slate-950 dark:text-white">{value}</p>
                    </div>)}
                </div>
            </div>
            <div
                className="mx-auto w-full max-w-[440px] rounded-lg border border-white/80 bg-white/[0.92] p-7 shadow-[0_28px_80px_rgba(15,23,42,0.16)] backdrop-blur-xl dark:border-white/10 dark:bg-[#0b1120]/[0.88] dark:shadow-black/40"
            >
                <div className="mb-6">
                    <p className="mb-2 text-xs font-semibold uppercase text-teal-700 dark:text-teal-300">Secure
                        access</p>
                    <h2 className="text-2xl font-semibold text-slate-950 dark:text-white">{title}</h2>
                    <p className="mt-2 text-sm leading-6 text-slate-600 dark:text-slate-300">{subtitle}</p>
                </div>
                {children}
            </div>
        </section>
    </div>
</main>;
export {
    AuthShell
};
