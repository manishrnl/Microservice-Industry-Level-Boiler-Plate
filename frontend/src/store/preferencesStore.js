import {create} from "zustand";
import {getBrowserTimeZone} from "../utils/clientContext";

const STORAGE_KEY = "platform_preferences";

const readStoredTimeZone = () => {
    try {
        const stored = JSON.parse(localStorage.getItem(STORAGE_KEY) || "{}");
        return typeof stored.timezone === "string" && stored.timezone ? stored.timezone : null;
    } catch {
        return null;
    }
};

const writeStoredTimeZone = (timezone) => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({timezone}));
};

const initialTimeZone = () => readStoredTimeZone() ?? getBrowserTimeZone() ?? "UTC";

const usePreferencesStore = create((set, get) => ({
    timezone: initialTimeZone(),
    setTimezone: (timezone) => {
        if (!timezone) {
            return;
        }
        writeStoredTimeZone(timezone);
        set({timezone});
    },
    hydrate: () => {
        const timezone = readStoredTimeZone();
        if (timezone && timezone !== get().timezone) {
            set({timezone});
        }
    }
}));

const getPreferredTimeZone = () => usePreferencesStore.getState().timezone || getBrowserTimeZone() || "UTC";

export {
    getPreferredTimeZone,
    usePreferencesStore
};
