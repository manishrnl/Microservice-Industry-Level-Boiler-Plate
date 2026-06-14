import {create} from "zustand";

const defaultActivity = {
    message: "Syncing with backend",
    detail: "Waiting for the API gateway to return the latest data."
};
const normalizeActivity = (activity = defaultActivity) => {
    if (typeof activity === "string") {
        return {
            ...defaultActivity,
            message: activity
        };
    }
    return {
        ...defaultActivity,
        ...activity
    };
};
const useApiActivityStore = create((set) => ({
    pendingCount: 0,
    startedAt: null,
    message: defaultActivity.message,
    detail: defaultActivity.detail,
    overlayDismissed: false,
    startActivity: (activity = defaultActivity) => {
        let active = true;
        const normalized = normalizeActivity(activity);
        set((state) => ({
            pendingCount: state.pendingCount + 1,
            startedAt: state.startedAt ?? Date.now(),
            message: normalized.message,
            detail: normalized.detail,
            overlayDismissed: state.pendingCount === 0 ? false : state.overlayDismissed
        }));
        return () => {
            if (!active) {
                return;
            }
            active = false;
            set((state) => {
                const nextCount = Math.max(0, state.pendingCount - 1);
                return {
                    pendingCount: nextCount,
                    startedAt: nextCount === 0 ? null : state.startedAt,
                    message: nextCount === 0 ? defaultActivity.message : state.message,
                    detail: nextCount === 0 ? defaultActivity.detail : state.detail,
                    overlayDismissed: nextCount === 0 ? false : state.overlayDismissed
                };
            });
        };
    },
    dismissOverlay: () => set({overlayDismissed: true})
}));
export {
    useApiActivityStore
};
