import {create} from "zustand";
import {getBrowserTimeZone} from "../utils/clientContext";
import {getSystemLanguage, languageByCode} from "../config/languages";

const STORAGE_KEY = "platform_preferences";

const readStoredPreferences = () => {
    try {
        return JSON.parse(localStorage.getItem(STORAGE_KEY) || "{}");
    } catch {
        return {};
    }
};

const writeStoredPreferences = (preferences) => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({
        ...readStoredPreferences(),
        ...preferences
    }));
};

const initialTimeZone = () => {
    const timezone = readStoredPreferences().timezone;
    return typeof timezone === "string" && timezone ? timezone : getBrowserTimeZone() ?? "UTC";
};

const initialLanguage = () => {
    const stored = readStoredPreferences();
    return stored.languageExplicit === true && languageByCode(stored.language)
        ? stored.language
        : getSystemLanguage();
};

const usePreferencesStore = create((set, get) => ({
    timezone: initialTimeZone(),
    language: initialLanguage(),
    setTimezone: (timezone) => {
        if (!timezone) {
            return;
        }
        writeStoredPreferences({timezone});
        set({timezone});
    },
    setLanguage: (language) => {
        if (!languageByCode(language)) {
            return;
        }
        writeStoredPreferences({language, languageExplicit: true});
        set({language});
    },
    hydrate: () => {
        const stored = readStoredPreferences();
        const preferences = {};
        if (stored.timezone && stored.timezone !== get().timezone) {
            preferences.timezone = stored.timezone;
        }
        if (stored.languageExplicit === true && languageByCode(stored.language) && stored.language !== get().language) {
            preferences.language = stored.language;
        }
        if (Object.keys(preferences).length) {
            set(preferences);
        }
    }
}));

const getPreferredTimeZone = () => usePreferencesStore.getState().timezone || getBrowserTimeZone() || "UTC";

export {
    getPreferredTimeZone,
    usePreferencesStore
};
