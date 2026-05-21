import {create} from "zustand";

const useApiActivityStore = create((set) => ({
    pendingCount: 0,
    startedAt: null,
    message: "Contacting local API",
    startActivity: (message = "Contacting local API") => {
        let active = true;
        set((state) => ({
            pendingCount: state.pendingCount + 1,
            startedAt: state.startedAt ?? Date.now(),
            message
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
                    message: nextCount === 0 ? "Contacting local API" : state.message
                };
            });
        };
    }
}));
export {
    useApiActivityStore
};
