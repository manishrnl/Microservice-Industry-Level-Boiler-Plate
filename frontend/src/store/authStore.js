import {create} from "zustand";
import {endpoints} from "../api/endpoints";
import {authUserFromToken, isTokenExpired} from "../utils/tokenUtils";
import {useApiActivityStore} from "./apiActivityStore";

const ACCESS_TOKEN_KEY = "platform.accessToken";
const storedToken = localStorage.getItem(ACCESS_TOKEN_KEY);
const initialToken = storedToken && !isTokenExpired(storedToken) ? storedToken : null;
const initialUser = authUserFromToken(initialToken);
if (storedToken && !initialToken) {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
}
const useAuthStore = create((set, get) => ({
    user: initialUser,
    accessToken: initialToken,
    isAuthenticated: Boolean(initialToken),
    isLoading: Boolean(initialToken),
    setAuth: (user, accessToken) => {
        localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
        set({user, accessToken, isAuthenticated: true, isLoading: false});
    },
    clearAuth: () => {
        localStorage.removeItem(ACCESS_TOKEN_KEY);
        set({user: null, accessToken: null, isAuthenticated: false, isLoading: false});
    },
    updateUser: (partial) => {
        const current = get().user;
        set({user: current ? {...current, ...partial} : null});
    },
    setLoading: (isLoading) => set({isLoading}),
    logout: async () => {
        try {
            await fetch(endpoints.auth.logout, {method: "POST", credentials: "include"});
        } finally {
            localStorage.removeItem(ACCESS_TOKEN_KEY);
            set({user: null, accessToken: null, isAuthenticated: false, isLoading: false});
        }
    },
    hydrate: async () => {
        const token = localStorage.getItem(ACCESS_TOKEN_KEY);
        if (!token || isTokenExpired(token)) {
            set({user: null, accessToken: null, isAuthenticated: false, isLoading: false});
            localStorage.removeItem(ACCESS_TOKEN_KEY);
            return;
        }
        const fallbackUser = authUserFromToken(token);
        set({
            user: get().user ?? fallbackUser,
            accessToken: token,
            isAuthenticated: true,
            isLoading: true
        });
        const stopActivity = useApiActivityStore.getState().startActivity("Checking session");
        try {
            const response = await fetch(endpoints.auth.me, {
                credentials: "include",
                headers: {
                    Authorization: `Bearer ${token}`
                }
            });
            if (!response.ok) {
                if (response.status === 401 || response.status === 403) {
                    localStorage.removeItem(ACCESS_TOKEN_KEY);
                    set({
                        user: null,
                        accessToken: null,
                        isAuthenticated: false,
                        isLoading: false
                    });
                    return;
                }
                if (fallbackUser) {
                    set({
                        user: fallbackUser,
                        accessToken: token,
                        isAuthenticated: true,
                        isLoading: false
                    });
                    return;
                }
                throw new Error("Unauthenticated");
            }
            const data = await response.json();
            const user = data.user ?? data.data?.user;
            const accessToken = data.accessToken ?? data.data?.accessToken ?? token;
            if (user && accessToken) {
                localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
                set({user, accessToken, isAuthenticated: true, isLoading: false});
            } else if (fallbackUser) {
                set({
                    user: fallbackUser,
                    accessToken: token,
                    isAuthenticated: true,
                    isLoading: false
                });
            } else {
                localStorage.removeItem(ACCESS_TOKEN_KEY);
                set({user: null, accessToken: null, isAuthenticated: false, isLoading: false});
            }
        } catch (error) {
            if (fallbackUser && !isTokenExpired(token)) {
                set({
                    user: fallbackUser,
                    accessToken: token,
                    isAuthenticated: true,
                    isLoading: false
                });
                return;
            }
            localStorage.removeItem(ACCESS_TOKEN_KEY);
            set({user: null, accessToken: null, isAuthenticated: false, isLoading: false});
        } finally {
            stopActivity();
        }
    }
}));
export {
    useAuthStore
};
