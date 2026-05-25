import {useEffect, useMemo, useState} from "react";
import {ExternalLink, Image as ImageIcon, LayoutGrid, Maximize2, Search, X} from "lucide-react";

const galleryItems = [
    {
        title: "Dashboard",
        category: "Core",
        src: "/images/Dashboard.png",
        description: "Primary application console with service overview and user workflow entry points."
    },
    {
        title: "Files",
        category: "Product",
        src: "/images/Files.png",
        description: "File management experience for uploading, viewing, and organizing user assets."
    },
    {
        title: "Observability",
        category: "Operations",
        src: "/images/Observability.png",
        description: "Operational health view for monitoring service readiness and platform telemetry."
    },
    {
        title: "Login",
        category: "Auth",
        src: "/images/Login.png",
        description: "Secure sign-in screen with a focused identity flow."
    },
    {
        title: "Signup",
        category: "Auth",
        src: "/images/Signup.png",
        description: "Account creation screen designed for clear form completion."
    },
    {
        title: "OTP Verification",
        category: "Auth",
        src: "/images/Otp.png",
        description: "One-time password verification for account and authentication flows."
    },
    {
        title: "User Profile",
        category: "Product",
        src: "/images/User.png",
        description: "User identity and profile surface used across the app shell."
    },
    {
        title: "AI Chat",
        category: "Product",
        src: "/images/AI-Chat.png",
        description: "AI assistant screen with conversation history and contextual responses."
    },
    {
        title: "Audit",
        category: "Admin",
        src: "/images/Audit.png",
        description: "Audit trail dashboard for reviewing security and activity records."
    },
    {
        title: "Home",
        category: "Core",
        src: "/images/Home.png",
        description: "Public landing screen for the microservice platform."
    },
    {
        title: "Loki Logs",
        category: "Operations",
        src: "/images/Loki-logs.png",
        description: "Log exploration interface for Loki-backed operational debugging."
    },
    {
        title: "Notifications",
        category: "Product",
        src: "/images/Notifications.png",
        description: "Notification center for system and account events."
    },
    {
        title: "Profile",
        category: "Product",
        src: "/images/Profile.png",
        description: "Account profile and preference management screen."
    },
    {
        title: "Sessions",
        category: "Security",
        src: "/images/Sessions.png",
        description: "Session management screen for reviewing and revoking active devices."
    },
    {
        title: "Stripe Payments",
        category: "Product",
        src: "/images/Stripe.png",
        description: "Payment experience integrated with Stripe-backed transaction handling."
    },
    {
        title: "Test API",
        category: "Admin",
        src: "/images/Test-API.png",
        description: "Admin test surface for checking application speed and API readiness."
    },
    {
        title: "Footer",
        category: "Core",
        src: "/images/Footer.png",
        description: "Public footer section with platform navigation and project context."
    }
];

const categories = ["All", ...Array.from(new Set(galleryItems.map((item) => item.category))).sort()];

const ImageGalleryPage = () => {
    const [query, setQuery] = useState("");
    const [category, setCategory] = useState("All");
    const [selected, setSelected] = useState(null);

    const filteredItems = useMemo(() => {
        const normalizedQuery = query.trim().toLowerCase();
        return galleryItems.filter((item) => {
            const categoryMatches = category === "All" || item.category === category;
            const queryMatches = !normalizedQuery
                || item.title.toLowerCase().includes(normalizedQuery)
                || item.category.toLowerCase().includes(normalizedQuery)
                || item.description.toLowerCase().includes(normalizedQuery);
            return categoryMatches && queryMatches;
        });
    }, [category, query]);

    useEffect(() => {
        if (!selected) {
            return undefined;
        }
        const closeOnEscape = (event) => {
            if (event.key === "Escape") {
                setSelected(null);
            }
        };
        window.addEventListener("keydown", closeOnEscape);
        return () => window.removeEventListener("keydown", closeOnEscape);
    }, [selected]);

    return <main className="bg-slate-50 text-slate-950 dark:bg-slate-950 dark:text-white">
        <section className="relative overflow-hidden border-b border-slate-200 bg-slate-950 dark:border-white/10">
            <img
                src="/images/Home.png"
                alt=""
                className="absolute inset-0 h-full w-full object-cover opacity-35"
            />
            <div className="absolute inset-0 bg-slate-950/70"/>
            <div className="relative mx-auto grid min-h-[430px] w-full max-w-7xl gap-8 px-4 py-12 sm:px-6 lg:grid-cols-[minmax(0,0.95fr)_minmax(360px,0.75fr)] lg:px-8">
                <div className="flex flex-col justify-center">
                    <div className="inline-flex w-fit items-center gap-2 rounded-md border border-teal-300/25 bg-teal-300/10 px-3 py-2 text-xs font-bold uppercase tracking-[0.14em] text-teal-100">
                        <ImageIcon className="h-4 w-4"/>
                        Image Gallery
                    </div>
                    <h1 className="mt-5 max-w-3xl text-4xl font-bold leading-tight text-white sm:text-5xl">
                        Microservice platform screens, collected in one polished gallery.
                    </h1>
                    <p className="mt-5 max-w-2xl text-base leading-7 text-slate-200">
                        Browse the screens from `Image_Gallery.md` plus the complete screenshot set from the frontend image build output.
                    </p>
                    <div className="mt-7 grid max-w-xl grid-cols-3 gap-3">
                        <Metric label="Screens" value={galleryItems.length}/>
                        <Metric label="Groups" value={categories.length - 1}/>
                        <Metric label="Source" value="MD + PNG"/>
                    </div>
                </div>
                <div className="hidden min-h-[300px] grid-cols-2 gap-3 self-center md:grid">
                    {galleryItems.slice(0, 4).map((item) => <div key={item.title} className="overflow-hidden rounded-md border border-white/10 bg-white/10 shadow-2xl shadow-slate-950/30">
                        <img src={item.src} alt={item.title} className="h-full min-h-[140px] w-full object-cover"/>
                    </div>)}
                </div>
            </div>
        </section>

        <section className="border-b border-slate-200 bg-white dark:border-white/10 dark:bg-slate-950">
            <div className="mx-auto flex w-full max-w-7xl flex-col gap-4 px-4 py-5 sm:px-6 lg:flex-row lg:items-center lg:justify-between lg:px-8">
                <div className="relative min-w-0 flex-1">
                    <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400"/>
                    <input
                        value={query}
                        onChange={(event) => setQuery(event.target.value)}
                        placeholder="Search screens, groups, or descriptions"
                        className="h-11 w-full rounded-md border border-slate-200 bg-slate-50 pl-10 pr-3 text-sm font-semibold text-slate-950 outline-none transition focus:border-teal-500 focus:ring-2 focus:ring-teal-500/20 dark:border-white/10 dark:bg-white/[0.04] dark:text-white"
                    />
                </div>
                <div className="flex gap-2 overflow-x-auto pb-1 lg:pb-0">
                    {categories.map((item) => <button
                        key={item}
                        type="button"
                        onClick={() => setCategory(item)}
                        className={`h-10 shrink-0 rounded-md px-3 text-sm font-bold transition ${category === item ? "bg-slate-950 text-white dark:bg-teal-300 dark:text-slate-950" : "border border-slate-200 bg-white text-slate-700 hover:bg-slate-50 dark:border-white/10 dark:bg-white/[0.04] dark:text-slate-200 dark:hover:bg-white/10"}`}
                    >
                        {item}
                    </button>)}
                </div>
            </div>
        </section>

        <section className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
            <div className="mb-5 flex items-center justify-between gap-4">
                <div>
                    <p className="text-xs font-bold uppercase tracking-[0.14em] text-slate-500 dark:text-slate-400">Gallery</p>
                    <h2 className="mt-1 text-2xl font-semibold text-slate-950 dark:text-white">{filteredItems.length} screen captures</h2>
                </div>
                <div className="hidden items-center gap-2 rounded-md border border-slate-200 bg-white px-3 py-2 text-sm font-bold text-slate-600 dark:border-white/10 dark:bg-white/[0.04] dark:text-slate-300 sm:inline-flex">
                    <LayoutGrid className="h-4 w-4"/>
                    Responsive grid
                </div>
            </div>

            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
                {filteredItems.map((item) => <article key={item.title} className="group overflow-hidden rounded-md border border-slate-200 bg-white shadow-sm transition hover:-translate-y-1 hover:shadow-xl hover:shadow-slate-950/10 dark:border-white/10 dark:bg-white/[0.04]">
                    <button
                        type="button"
                        onClick={() => setSelected(item)}
                        className="block w-full text-left"
                    >
                        <div className="relative aspect-[16/10] overflow-hidden bg-slate-900">
                            <img
                                src={item.src}
                                alt={`${item.title} screen`}
                                loading="lazy"
                                className="h-full w-full object-cover transition duration-300 group-hover:scale-[1.03]"
                            />
                            <div className="absolute inset-x-0 bottom-0 flex items-center justify-between bg-gradient-to-t from-slate-950/85 to-transparent p-3 text-white opacity-0 transition group-hover:opacity-100">
                                <span className="text-xs font-bold uppercase tracking-[0.12em]">{item.category}</span>
                                <Maximize2 className="h-4 w-4"/>
                            </div>
                        </div>
                        <div className="p-4">
                            <div className="flex items-start justify-between gap-3">
                                <div className="min-w-0">
                                    <h3 className="truncate text-base font-bold text-slate-950 dark:text-white">{item.title}</h3>
                                    <p className="mt-2 line-clamp-2 text-sm leading-6 text-slate-600 dark:text-slate-300">{item.description}</p>
                                </div>
                                <span className="shrink-0 rounded-md bg-slate-100 px-2 py-1 text-xs font-bold text-slate-600 dark:bg-white/10 dark:text-slate-300">{item.category}</span>
                            </div>
                        </div>
                    </button>
                </article>)}
            </div>

            {filteredItems.length === 0 && <div className="rounded-md border border-dashed border-slate-300 bg-white p-10 text-center dark:border-white/15 dark:bg-white/[0.04]">
                <ImageIcon className="mx-auto h-8 w-8 text-slate-400"/>
                <h3 className="mt-4 text-lg font-bold text-slate-950 dark:text-white">No matching screenshots</h3>
                <p className="mt-2 text-sm text-slate-600 dark:text-slate-300">Try a different search term or category.</p>
            </div>}
        </section>

        {selected && <div className="fixed inset-0 z-50 bg-slate-950/90 p-3 backdrop-blur-sm sm:p-6" role="dialog" aria-modal="true">
            <div className="mx-auto flex h-full max-w-7xl flex-col overflow-hidden rounded-md border border-white/10 bg-slate-950 shadow-2xl">
                <div className="flex items-center justify-between gap-3 border-b border-white/10 px-4 py-3">
                    <div className="min-w-0">
                        <p className="text-xs font-bold uppercase tracking-[0.14em] text-teal-200">{selected.category}</p>
                        <h2 className="truncate text-lg font-bold text-white">{selected.title}</h2>
                    </div>
                    <div className="flex shrink-0 items-center gap-2">
                        <a
                            href={selected.src}
                            target="_blank"
                            rel="noreferrer"
                            className="grid h-10 w-10 place-items-center rounded-md border border-white/10 bg-white/10 text-white hover:bg-white/15"
                            aria-label="Open image in a new tab"
                            title="Open image"
                        >
                            <ExternalLink className="h-4 w-4"/>
                        </a>
                        <button
                            type="button"
                            onClick={() => setSelected(null)}
                            className="grid h-10 w-10 place-items-center rounded-md border border-white/10 bg-white/10 text-white hover:bg-white/15"
                            aria-label="Close preview"
                            title="Close"
                        >
                            <X className="h-5 w-5"/>
                        </button>
                    </div>
                </div>
                <div className="min-h-0 flex-1 bg-slate-900 p-3 sm:p-5">
                    <img
                        src={selected.src}
                        alt={`${selected.title} screen`}
                        className="mx-auto h-full max-h-full w-full object-contain"
                    />
                </div>
                <div className="border-t border-white/10 px-4 py-3 text-sm leading-6 text-slate-200">
                    {selected.description}
                </div>
            </div>
        </div>}
    </main>;
};

const Metric = ({label, value}) => <div className="rounded-md border border-white/10 bg-white/10 p-3 backdrop-blur">
    <div className="text-2xl font-bold text-white">{value}</div>
    <div className="mt-1 text-xs font-bold uppercase tracking-[0.12em] text-slate-300">{label}</div>
</div>;

export {
    ImageGalleryPage
};
