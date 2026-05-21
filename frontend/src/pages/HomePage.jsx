import {
    ArrowRight,
    Bell,
    Bot,
    CheckCircle2,
    Cloud,
    CreditCard,
    Database,
    FileText,
    Gauge,
    GitBranch,
    LockKeyhole,
    Network,
    Radio,
    ShieldCheck,
    Sparkles,
    Users
} from "lucide-react";
import {Link} from "react-router-dom";
import {useAuthStore} from "../store/authStore";

const metrics = [
    {label: "Spring Boot services", value: "10+"},
    {label: "Gateway routes", value: "7"},
    {label: "Auth flows", value: "8"},
    {label: "Infra profiles", value: "Cloud + Local"}
];

const architectureNodes = [
    {label: "Frontend", detail: "React, Vite, responsive shell", Icon: Sparkles, tone: "bg-teal-50 text-teal-700"},
    {label: "API Gateway", detail: "CORS, rate limits, JWT edge checks", Icon: Network, tone: "bg-slate-100 text-slate-800"},
    {label: "Auth Service", detail: "JWT, sessions, OAuth, OTP email", Icon: LockKeyhole, tone: "bg-rose-50 text-rose-700"},
    {label: "Domain Services", detail: "Users, files, payments, AI, audit", Icon: GitBranch, tone: "bg-indigo-50 text-indigo-700"},
    {label: "Data Layer", detail: "PostgreSQL, Redis, MinIO, Flyway", Icon: Database, tone: "bg-amber-50 text-amber-700"},
    {label: "Observability", detail: "Health, Prometheus, Grafana, Zipkin", Icon: Radio, tone: "bg-emerald-50 text-emerald-700"}
];

const capabilities = [
    {title: "Identity and access", text: "Email signup, OTP verification, OAuth providers, JWT access tokens, refresh cookies, session revocation, profile updates, and role-aware admin routes.", Icon: ShieldCheck},
    {title: "Gateway-first integration", text: "A single public API surface fronts the services, handles browser CORS rules, applies rate limits, forwards auth headers, and keeps frontend code away from internal service URLs.", Icon: Cloud},
    {title: "User operations", text: "Profile, notifications, file management, payments, AI chat, audit log, admin user management, and session-control screens give the template realistic product coverage.", Icon: Users},
    {title: "Production posture", text: "Docker Compose, Render-oriented env files, config-server profiles, Flyway migrations, health endpoints, and observability components make the platform deployable, not just demonstrable.", Icon: Gauge}
];

const serviceReport = [
    {
        title: "Architecture quality",
        grade: "Strong foundation",
        points: [
            "The system has clear service boundaries: gateway, auth, user, notification, payment, file, AI, audit, discovery, and config.",
            "Config-server centralizes operational settings, which is useful when moving from local Docker Compose to hosted deployment.",
            "The gateway pattern keeps browser clients stable while service ports and internal hostnames remain private."
        ]
    },
    {
        title: "Security posture",
        grade: "Practical and extensible",
        points: [
            "JWT access tokens, refresh cookies, OAuth callbacks, email verification, and session revocation cover the common SaaS authentication surface.",
            "Role checks exist for admin areas, and gateway JWT validation creates a first line of defense before requests reach downstream services.",
            "The next hardening step is environment-specific cookie policy: secure SameSite=None for HTTPS deployment and Lax for local HTTP development."
        ]
    },
    {
        title: "Operational readiness",
        grade: "Above template level",
        points: [
            "Health endpoints, service discovery, Prometheus, Grafana, and Zipkin give the project an operations story from day one.",
            "Flyway migrations make schema changes repeatable, which is critical once multiple services own separate databases.",
            "Local LAN access now adapts to changing Wi-Fi IPs by deriving the API host from the current browser host."
        ]
    },
    {
        title: "Frontend experience",
        grade: "Improved for real devices",
        points: [
            "The public home page explains the system before asking users to sign in.",
            "The authenticated shell now has mobile navigation instead of relying only on a desktop sidebar.",
            "The next product step is deeper mobile tuning on dense admin tables, especially users, audit, sessions, and file lists."
        ]
    }
];

const workflow = [
    {label: "Request enters gateway", Icon: Network},
    {label: "JWT and rate policy checked", Icon: ShieldCheck},
    {label: "Service route selected", Icon: GitBranch},
    {label: "Data and events persisted", Icon: Database},
    {label: "User receives live feedback", Icon: Bell}
];

const HomePage = () => {
    const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
    return <main>
        <section className="relative overflow-hidden border-b border-slate-200 bg-white dark:border-white/10 dark:bg-slate-950">
            <div className="absolute inset-0 opacity-[0.22] [background-image:linear-gradient(rgba(15,23,42,0.18)_1px,transparent_1px),linear-gradient(90deg,rgba(15,23,42,0.18)_1px,transparent_1px)] [background-size:34px_34px] dark:opacity-[0.12]"/>
            <div className="relative mx-auto grid w-full max-w-7xl gap-10 px-4 py-12 sm:px-6 sm:py-16 lg:grid-cols-[minmax(0,1fr)_520px] lg:px-8 lg:py-20">
                <div className="flex flex-col justify-center">
                    <div className="inline-flex w-fit items-center gap-2 rounded-md border border-teal-200 bg-teal-50 px-3 py-2 text-xs font-bold uppercase tracking-[0.14em] text-teal-800 dark:border-teal-300/20 dark:bg-teal-300/10 dark:text-teal-200">
                        <Sparkles className="h-4 w-4"/>
                        Microservice industry template
                    </div>
                    <h1 className="mt-5 max-w-3xl text-4xl font-bold leading-tight text-slate-950 sm:text-5xl lg:text-6xl dark:text-white">
                        A complete microservice platform with auth, gateway, AI, files, payments, and observability.
                    </h1>
                    <p className="mt-5 max-w-2xl text-base leading-7 text-slate-600 sm:text-lg dark:text-slate-300">
                        This template is built like a real SaaS foundation: independently owned services, centralized configuration, secure identity, operational monitoring, and a frontend that now works on desktop, tablet, and phones.
                    </p>
                    <div className="mt-7 flex flex-col gap-3 sm:flex-row">
                        <Link
                            to={isAuthenticated ? "/app/dashboard" : "/login"}
                            className="inline-flex h-12 items-center justify-center gap-2 rounded-md bg-slate-950 px-5 text-sm font-bold text-white shadow-lg shadow-slate-950/15 transition hover:-translate-y-0.5 hover:bg-slate-800 dark:bg-teal-300 dark:text-slate-950 dark:hover:bg-teal-200"
                        >
                            {isAuthenticated ? "Open dashboard" : "Explore the console"}
                            <ArrowRight className="h-4 w-4"/>
                        </Link>
                        <a
                            href="#analysis"
                            className="inline-flex h-12 items-center justify-center rounded-md border border-slate-300 bg-white px-5 text-sm font-bold text-slate-800 transition hover:-translate-y-0.5 hover:border-slate-400 hover:bg-slate-50 dark:border-white/15 dark:bg-white/5 dark:text-white dark:hover:bg-white/10"
                        >
                            Read analysis
                        </a>
                    </div>
                </div>

                <div className="relative min-h-[360px] overflow-hidden rounded-lg border border-slate-200 bg-slate-950 p-4 shadow-2xl shadow-slate-950/20 sm:p-5">
                    <div className="absolute inset-0 opacity-30 [background-image:radial-gradient(circle_at_20%_20%,rgba(45,212,191,0.45),transparent_28%),radial-gradient(circle_at_80%_10%,rgba(251,191,36,0.28),transparent_26%),linear-gradient(135deg,rgba(255,255,255,0.08)_0%,transparent_45%)]"/>
                    <div className="relative grid h-full min-h-[330px] grid-cols-2 gap-3 sm:grid-cols-3">
                        {architectureNodes.map(({label, detail, Icon, tone}) => <article
                            key={label}
                            className="flex min-h-[120px] flex-col justify-between rounded-md border border-white/10 bg-white/[0.08] p-3 backdrop-blur"
                        >
                            <span className={`grid h-9 w-9 place-items-center rounded-md ${tone}`}>
                                <Icon className="h-5 w-5"/>
                            </span>
                            <div>
                                <h2 className="text-sm font-bold text-white">{label}</h2>
                                <p className="mt-1 text-xs leading-5 text-slate-300">{detail}</p>
                            </div>
                        </article>)}
                    </div>
                </div>
            </div>
        </section>

        <section className="bg-slate-50 py-8 dark:bg-slate-900">
            <div className="mx-auto grid w-full max-w-7xl grid-cols-2 gap-3 px-4 sm:px-6 md:grid-cols-4 lg:px-8">
                {metrics.map((item) => <article key={item.label} className="rounded-md border border-slate-200 bg-white p-4 shadow-sm dark:border-white/10 dark:bg-white/[0.04]">
                    <p className="text-2xl font-bold text-slate-950 dark:text-white">{item.value}</p>
                    <p className="mt-1 text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">{item.label}</p>
                </article>)}
            </div>
        </section>

        <section id="architecture" className="bg-white py-14 dark:bg-slate-950">
            <div className="mx-auto w-full max-w-7xl px-4 sm:px-6 lg:px-8">
                <div className="max-w-3xl">
                    <p className="text-xs font-bold uppercase tracking-[0.16em] text-teal-700 dark:text-teal-300">Architecture</p>
                    <h2 className="mt-3 text-3xl font-bold text-slate-950 sm:text-4xl dark:text-white">How the platform is organized</h2>
                    <p className="mt-4 text-base leading-7 text-slate-600 dark:text-slate-300">
                        The template separates traffic management, identity, domain workflows, storage, and operational tooling. That makes it easier to deploy incrementally, test individual services, and replace infrastructure without rewriting the product shell.
                    </p>
                </div>
                <div className="mt-8 grid gap-3 md:grid-cols-5">
                    {workflow.map(({label, Icon}, index) => <article
                        key={label}
                        className="relative rounded-md border border-slate-200 bg-slate-50 p-4 dark:border-white/10 dark:bg-white/[0.04]"
                    >
                        <div className="flex items-center gap-3">
                            <span className="grid h-10 w-10 place-items-center rounded-md bg-white text-slate-950 shadow-sm dark:bg-slate-900 dark:text-teal-200">
                                <Icon className="h-5 w-5"/>
                            </span>
                            <span className="text-xs font-bold text-slate-400">0{index + 1}</span>
                        </div>
                        <h3 className="mt-5 text-sm font-bold text-slate-950 dark:text-white">{label}</h3>
                    </article>)}
                </div>
            </div>
        </section>

        <section id="capabilities" className="bg-slate-100 py-14 dark:bg-slate-900">
            <div className="mx-auto w-full max-w-7xl px-4 sm:px-6 lg:px-8">
                <div className="max-w-3xl">
                    <p className="text-xs font-bold uppercase tracking-[0.16em] text-indigo-700 dark:text-indigo-300">Capabilities</p>
                    <h2 className="mt-3 text-3xl font-bold text-slate-950 sm:text-4xl dark:text-white">A template with real product surface area</h2>
                </div>
                <div className="mt-8 grid gap-4 md:grid-cols-2">
                    {capabilities.map(({title, text, Icon}) => <article key={title} className="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-white/[0.04]">
                        <Icon className="h-6 w-6 text-teal-700 dark:text-teal-300"/>
                        <h3 className="mt-5 text-lg font-bold text-slate-950 dark:text-white">{title}</h3>
                        <p className="mt-3 text-sm leading-6 text-slate-600 dark:text-slate-300">{text}</p>
                    </article>)}
                </div>
            </div>
        </section>

        <section id="analysis" className="bg-white py-14 dark:bg-slate-950">
            <div className="mx-auto w-full max-w-7xl px-4 sm:px-6 lg:px-8">
                <div className="mx-auto max-w-3xl text-center">
                    <p className="text-xs font-bold uppercase tracking-[0.16em] text-amber-700 dark:text-amber-300">Detailed report</p>
                    <h2 className="mt-3 text-3xl font-bold text-slate-950 sm:text-4xl dark:text-white">Analysis of the microservice template</h2>
                    <p className="mt-4 text-base leading-7 text-slate-600 dark:text-slate-300">
                        The project is strongest as a learning-to-production bridge. It demonstrates the service boundaries and supporting infrastructure expected in a modern platform, while staying understandable enough for iteration.
                    </p>
                </div>
                <div className="mt-8 grid gap-4 lg:grid-cols-2">
                    {serviceReport.map((section) => <article key={section.title} className="rounded-md border border-slate-200 bg-slate-50 p-5 text-center dark:border-white/10 dark:bg-white/[0.04]">
                        <div className="flex flex-col items-center justify-center gap-3 sm:flex-row">
                            <h3 className="inline-flex items-center justify-center gap-2 text-lg font-bold text-slate-950 dark:text-white">
                                <CheckCircle2 className="h-5 w-5 shrink-0 text-teal-700 dark:text-teal-300"/>
                                <span>{section.title}</span>
                            </h3>
                            <span className="w-fit rounded-md border border-emerald-200 bg-emerald-50 px-3 py-1 text-xs font-bold text-emerald-800 dark:border-emerald-300/20 dark:bg-emerald-300/10 dark:text-emerald-200">{section.grade}</span>
                        </div>
                        <ul className="mx-auto mt-5 max-w-2xl space-y-3">
                            {section.points.map((point) => <li key={point} className="flex items-start justify-center gap-3 text-sm leading-6 text-slate-600 dark:text-slate-300">
                                <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-teal-700 dark:text-teal-300"/>
                                <span className="text-left sm:text-center">{point}</span>
                            </li>)}
                        </ul>
                    </article>)}
                </div>
            </div>
        </section>

        <section className="bg-slate-950 py-14 text-white">
            <div className="mx-auto grid w-full max-w-7xl gap-6 px-4 sm:px-6 lg:grid-cols-[1fr_420px] lg:px-8">
                <div>
                    <p className="text-xs font-bold uppercase tracking-[0.16em] text-teal-300">Recommended next steps</p>
                    <h2 className="mt-3 text-3xl font-bold sm:text-4xl">Where this template can grow next</h2>
                    <p className="mt-4 max-w-2xl text-sm leading-6 text-slate-300">
                        The core platform is ready for demos and serious extension. The next best investments are automated integration tests, environment-specific security policies, mobile tuning for data-heavy pages, and CI workflows that deploy services independently.
                    </p>
                </div>
                <div className="grid grid-cols-2 gap-3">
                    {[
                        {label: "AI chat", Icon: Bot},
                        {label: "Files", Icon: FileText},
                        {label: "Payments", Icon: CreditCard},
                        {label: "Alerts", Icon: Bell}
                    ].map(({label, Icon}) => <div key={label} className="rounded-md border border-white/10 bg-white/[0.06] p-4">
                        <Icon className="h-5 w-5 text-amber-200"/>
                        <p className="mt-4 text-sm font-bold">{label}</p>
                    </div>)}
                </div>
            </div>
        </section>
    </main>;
};

export {
    HomePage
};
