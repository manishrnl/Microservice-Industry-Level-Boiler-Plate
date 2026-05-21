import {create} from "zustand";
import toast from "react-hot-toast";
import {endpoints} from "../api/endpoints";
import {authUserFromToken, isTokenExpired} from "../utils/tokenUtils";
import {useApiActivityStore} from "./apiActivityStore";

const ACCESS_TOKEN_KEY = "platform.accessToken";
const AUTH_NOTICE_KEY = "platform.authNotice";
const SESSION_EXPIRED_MESSAGE = "Session expired. Log in again.";
const storedToken = localStorage.getItem(ACCESS_TOKEN_KEY);
const initialToken = storedToken && !isTokenExpired(storedToken) ? storedToken : null;
const initialUser = initialToken ? authUserFromToken(initialToken) : null;
const readAccessToken = (payload) => payload?.accessToken ?? payload?.data?.accessToken ?? null;
const readUser = (payload) => payload?.user ?? payload?.data?.user ?? null;
const refreshAccessToken = async () => {
    const response = await fetch(endpoints.auth.refresh, {
        method: "POST",
        credentials: "include"
    });
    if (!response.ok) {
        throw new Error("Refresh token is invalid or expired");
    }
    const data = await response.json();
    const accessToken = readAccessToken(data);
    if (!accessToken) {
        throw new Error("Refresh response did not include an access token");
    }
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    return accessToken;
};
const storeAuthNotice = (message) => {
    if (message) {
        sessionStorage.setItem(AUTH_NOTICE_KEY, message);
    }
};
const consumeAuthNotice = () => {
    const message = sessionStorage.getItem(AUTH_NOTICE_KEY) ?? "";
    sessionStorage.removeItem(AUTH_NOTICE_KEY);
    return message;
};
const clearStoredAuth = () => {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
};
const fetchCurrentUser = async (accessToken) => {
    const response = await fetch(endpoints.auth.me, {
        credentials: "include",
        headers: {
            Authorization: `Bearer ${accessToken}`
        }
    });
    if (!response.ok) {
        const error = new Error("Session check failed");
        error.status = response.status;
        throw error;
    }
    const data = await response.json();
    const user = readUser(data);
    const refreshedAccessToken = readAccessToken(data) ?? accessToken;
    return {user, accessToken: refreshedAccessToken};
};
const useAuthStore = create((set, get) => ({
    user: initialUser,
    accessToken: initialToken,
    isAuthenticated: Boolean(initialToken),
    isLoading: true,
    authNotice: "",
    setAuth: (user, accessToken) => {
        localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
        sessionStorage.removeItem(AUTH_NOTICE_KEY);
        set({user, accessToken, isAuthenticated: true, isLoading: false, authNotice: ""});
    },
    clearAuth: (notice = "") => {
        clearStoredAuth();
        if (notice) {
            storeAuthNotice(notice);
        }
        set({user: null, accessToken: null, isAuthenticated: false, isLoading: false, authNotice: notice});
    },
    consumeAuthNotice: () => {
        const notice = get().authNotice || consumeAuthNotice();
        if (notice) {
            set({authNotice: ""});
        }
        return notice;
    },
    expireSession: (notice = SESSION_EXPIRED_MESSAGE) => {
        get().clearAuth(notice);
        toast.error(notice);
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
            clearStoredAuth();
            set({user: null, accessToken: null, isAuthenticated: false, isLoading: false, authNotice: ""});
        }
    },
    hydrate: async () => {
        let token = localStorage.getItem(ACCESS_TOKEN_KEY);
        const fallbackUser = token && !isTokenExpired(token) ? authUserFromToken(token) : null;
        set({
            user: get().user ?? fallbackUser,
            accessToken: token && !isTokenExpired(token) ? token : null,
            isAuthenticated: Boolean(token && !isTokenExpired(token)),
            isLoading: true
        });
        const stopActivity = useApiActivityStore.getState().startActivity("Checking session");
        try {
            if (!token || isTokenExpired(token)) {
                token = await refreshAccessToken();
            }
            let session;
            try {
                session = await fetchCurrentUser(token);
            } catch (error) {
                if (error.status !== 401 && error.status !== 403) {
                    throw error;
                }
                token = await refreshAccessToken();
                session = await fetchCurrentUser(token);
            }
            const user = session.user ?? authUserFromToken(session.accessToken);
            if (!user) {
                throw new Error("Session response did not include a user");
            }
            localStorage.setItem(ACCESS_TOKEN_KEY, session.accessToken);
            sessionStorage.removeItem(AUTH_NOTICE_KEY);
            set({user, accessToken: session.accessToken, isAuthenticated: true, isLoading: false, authNotice: ""});
        } catch (error) {
            get().clearAuth(SESSION_EXPIRED_MESSAGE);
        } finally {
            stopActivity();
        }
    }
}));
export {
    useAuthStore
};
