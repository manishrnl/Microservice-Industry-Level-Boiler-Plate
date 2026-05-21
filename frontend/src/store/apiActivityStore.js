import {create} from "zustand";

const useApiActivityStore = create((set) => ({
    pendingCount: 0,
    startedAt: null,
    message: "Contacting backend",
    startActivity: (message = "Contacting backend") => {
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
                    message: nextCount === 0 ? "Contacting backend" : state.message
                };
            });
        };
    }
}));
export {
    useApiActivityStore
};
