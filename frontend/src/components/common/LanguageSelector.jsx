import {Check, ChevronDown, Languages} from "lucide-react";
import {useEffect, useRef, useState} from "react";
import {flushSync} from "react-dom";
import {usePreferencesStore} from "../../store/preferencesStore";
import {languageByCode, languages} from "../../config/languages";

let translateScriptPromise;

const clearTranslationCookies = () => {
    const hostname = window.location.hostname;
    const domainParts = hostname.split(".");
    const domains = new Set(["", hostname, `.${hostname}`]);
    for (let index = 0; index < domainParts.length - 1; index += 1) {
        const domain = domainParts.slice(index).join(".");
        domains.add(domain);
        domains.add(`.${domain}`);
    }
    domains.forEach((domain) => {
        const domainAttribute = domain ? `;domain=${domain}` : "";
        document.cookie = `googtrans=;path=/;expires=Thu, 01 Jan 1970 00:00:00 GMT;Max-Age=0;SameSite=Lax${domainAttribute}`;
    });
};

const setTranslationCookie = (language) => {
    clearTranslationCookies();
    const value = `/en/${language}`;
    document.cookie = `googtrans=${value};path=/;SameSite=Lax`;
    if (window.location.hostname.includes(".")) {
        document.cookie = `googtrans=${value};path=/;domain=.${window.location.hostname};SameSite=Lax`;
    }
};

const loadGoogleTranslate = () => {
    if (window.google?.translate?.TranslateElement) {
        return Promise.resolve();
    }
    if (translateScriptPromise) {
        return translateScriptPromise;
    }

    translateScriptPromise = new Promise((resolve, reject) => {
        window.__platformGoogleTranslateReady = resolve;
        const script = document.createElement("script");
        script.src = "https://translate.google.com/translate_a/element.js?cb=__platformGoogleTranslateReady";
        script.async = true;
        script.onerror = reject;
        document.head.appendChild(script);
    });
    return translateScriptPromise;
};

const initializeTranslator = async () => {
    await loadGoogleTranslate();
    let host = document.getElementById("platform-google-translate");
    if (!host) {
        host = document.createElement("div");
        host.id = "platform-google-translate";
        host.hidden = true;
        document.body.appendChild(host);
    }
    if (!host.hasChildNodes()) {
        new window.google.translate.TranslateElement({
            pageLanguage: "en",
            includedLanguages: languages.map(({code}) => code).join(","),
            autoDisplay: false
        }, "platform-google-translate");
    }
};

const selectGoogleLanguage = (language) => {
    const googleSelect = document.querySelector(".goog-te-combo");
    if (!googleSelect) {
        return false;
    }
    googleSelect.value = language;
    googleSelect.dispatchEvent(new Event("change", {bubbles: true}));
    return true;
};

const refreshPageTranslation = (language) => {
    if (!language || language === "en") {
        return;
    }
    window.setTimeout(() => selectGoogleLanguage(language), 250);
};

const LanguageSelector = ({compact = false}) => {
    const language = usePreferencesStore((state) => state.language);
    const setLanguage = usePreferencesStore((state) => state.setLanguage);
    const [open, setOpen] = useState(false);
    const [canHover, setCanHover] = useState(false);
    const containerRef = useRef(null);
    const closeTimerRef = useRef(null);

    useEffect(() => {
        document.documentElement.lang = language;
        document.documentElement.dir = language === "ar" ? "rtl" : "ltr";
        if (language === "en") {
            clearTranslationCookies();
            return;
        }
        setTranslationCookie(language);
        void initializeTranslator().then(() => {
            window.setTimeout(() => selectGoogleLanguage(language), 300);
        }).catch(() => {
            // The selector remains usable if the translation service is temporarily unavailable.
        });
    }, [language]);

    useEffect(() => {
        const media = window.matchMedia("(hover: hover) and (pointer: fine)");
        const updateInputMode = () => setCanHover(media.matches);
        updateInputMode();
        media.addEventListener("change", updateInputMode);
        return () => media.removeEventListener("change", updateInputMode);
    }, []);

    useEffect(() => {
        const closeMenu = (event) => {
            if (!containerRef.current?.contains(event.target)) {
                window.clearTimeout(closeTimerRef.current);
                setOpen(false);
            }
        };
        const closeOnEscape = (event) => {
            if (event.key === "Escape") {
                setOpen(false);
            }
        };
        document.addEventListener("pointerdown", closeMenu, true);
        document.addEventListener("touchstart", closeMenu, true);
        document.addEventListener("keydown", closeOnEscape);
        return () => {
            window.clearTimeout(closeTimerRef.current);
            document.removeEventListener("pointerdown", closeMenu, true);
            document.removeEventListener("touchstart", closeMenu, true);
            document.removeEventListener("keydown", closeOnEscape);
        };
    }, []);

    const openOnHover = () => {
        if (canHover) {
            window.clearTimeout(closeTimerRef.current);
            setOpen(true);
        }
    };

    const closeOnHoverLeave = () => {
        if (canHover) {
            closeTimerRef.current = window.setTimeout(() => setOpen(false), 120);
        }
    };

    const handleChange = async (nextLanguage) => {
        window.clearTimeout(closeTimerRef.current);
        flushSync(() => setOpen(false));
        if (nextLanguage === language) {
            return;
        }
        setLanguage(nextLanguage);
        if (nextLanguage === "en") {
            clearTranslationCookies();
            window.location.reload();
            return;
        }
        setTranslationCookie(nextLanguage);
        try {
            await initializeTranslator();
            window.setTimeout(() => {
                if (!selectGoogleLanguage(nextLanguage)) {
                    window.location.reload();
                }
            }, 100);
        } catch {
            window.location.reload();
        }
    };

    const selectedLanguage = languageByCode(language) ?? languages[0];
    const languageGroups = [
        {label: "Indian languages", items: languages.filter(({group}) => group === "Indian")},
        {label: "International", items: languages.filter(({group}) => group === "International")}
    ];

    return <div
        ref={containerRef}
        className={`language-selector notranslate relative inline-flex shrink-0 ${compact ? "" : "w-full sm:w-auto"}`}
        translate="no"
        onMouseEnter={openOnHover}
        onMouseLeave={closeOnHoverLeave}
    >
        {open && !canHover && <button
            type="button"
            className="fixed inset-0 z-[80] cursor-default bg-slate-950/10 backdrop-blur-[1px] dark:bg-black/25"
            onPointerDown={() => setOpen(false)}
            onTouchStart={() => setOpen(false)}
            aria-label="Close language menu"
        />}
        <button
            type="button"
            onClick={() => {
                window.clearTimeout(closeTimerRef.current);
                setOpen((value) => !value);
            }}
            className={`group inline-flex h-10 items-center rounded-md border border-slate-200 bg-white font-semibold text-slate-700 shadow-sm transition duration-200 hover:-translate-y-0.5 hover:border-teal-300 hover:bg-teal-50 hover:text-slate-950 hover:shadow-md focus:outline-none focus:ring-2 focus:ring-teal-500/25 dark:border-white/10 dark:bg-slate-900 dark:text-slate-100 dark:shadow-none dark:hover:border-teal-300/50 dark:hover:bg-teal-300/10 dark:hover:text-white ${compact ? "w-10 justify-center px-0 sm:w-auto sm:min-w-32 sm:justify-start sm:px-3" : "w-full justify-between px-3 sm:min-w-44"}`}
            aria-label="Change language"
            aria-haspopup="listbox"
            aria-expanded={open}
        >
            <Languages className="h-4 w-4 shrink-0 text-slate-500 transition group-hover:text-teal-700 dark:text-teal-300"/>
            <span className={compact ? "hidden min-w-0 flex-1 truncate text-left text-sm sm:block" : "min-w-0 flex-1 truncate px-2 text-left text-sm"}>
                {selectedLanguage.label}
            </span>
            {compact && <span className="ml-0.5 text-[9px] font-extrabold sm:hidden">{selectedLanguage.shortLabel}</span>}
            <ChevronDown className={`h-3.5 w-3.5 shrink-0 transition duration-200 ${compact ? "hidden sm:block" : ""} ${open ? "rotate-180 text-teal-600 dark:text-teal-300" : "text-slate-400"}`}/>
        </button>

        {open && <div
            className="fixed inset-x-3 top-20 z-[90] overflow-hidden rounded-xl border border-slate-200/80 bg-white/95 p-3 shadow-[0_24px_70px_rgba(15,23,42,0.22)] ring-1 ring-slate-950/5 backdrop-blur-xl dark:border-white/10 dark:bg-slate-950/95 dark:shadow-[0_28px_80px_rgba(0,0,0,0.55)] dark:ring-white/10 md:inset-x-auto md:right-4 md:top-[4.5rem] md:w-[30vw] md:min-w-[30rem] md:max-w-[42rem]"
            role="listbox"
            aria-label="Available languages"
        >
            <div className="mb-1 border-b border-slate-100 px-2 pb-2 pt-1 dark:border-white/10">
                <p className="text-sm font-bold text-slate-950 dark:text-white">Choose language</p>
                <p className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">Your selection is remembered on this device.</p>
            </div>
            <div className="p-1">
                {languageGroups.map((group) => <div key={group.label} className="mb-2 last:mb-0">
                    <p className="px-2 py-1.5 text-[10px] font-bold uppercase tracking-[0.14em] text-slate-400 dark:text-slate-500">
                        {group.label}
                    </p>
                    <div className="grid grid-cols-2 gap-1 md:grid-cols-3">
                        {group.items.map((item) => {
                            const active = item.code === language;
                            return <button
                                key={item.code}
                                type="button"
                                onClick={() => void handleChange(item.code)}
                                className={`group/item flex min-w-0 items-center gap-2 rounded-md px-2 py-1.5 text-left transition duration-150 ${active ? "bg-teal-50 text-teal-900 ring-1 ring-teal-200 dark:bg-teal-300/15 dark:text-teal-100 dark:ring-teal-300/25" : "text-slate-600 hover:bg-slate-100 hover:text-slate-950 dark:text-slate-300 dark:hover:bg-white/10 dark:hover:text-white"}`}
                                role="option"
                                aria-selected={active}
                            >
                                <span className={`grid h-7 w-7 shrink-0 place-items-center rounded-md text-[10px] font-extrabold ${active ? "bg-teal-600 text-white dark:bg-teal-300 dark:text-slate-950" : "bg-slate-100 text-slate-500 group-hover/item:bg-white dark:bg-white/10 dark:text-slate-300 dark:group-hover/item:bg-white/15"}`}>
                                    {item.shortLabel}
                                </span>
                                <span className="min-w-0 flex-1 truncate text-xs font-semibold">{item.label}</span>
                                {active && <Check className="h-3.5 w-3.5 shrink-0 text-teal-600 dark:text-teal-300"/>}
                            </button>;
                        })}
                    </div>
                </div>)}
            </div>
        </div>}
    </div>;
};

export {
    LanguageSelector,
    refreshPageTranslation
};
