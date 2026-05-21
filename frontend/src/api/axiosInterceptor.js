import {apiClient} from "./axiosInstance";
import {endpoints} from "./endpoints";
import {useAuthStore} from "../store/authStore";
import {useApiActivityStore} from "../store/apiActivityStore";
import {unwrapApiData} from "../utils/responseUtils";

let refreshing = false;
let queue = [];
const flushQueue = (token) => {
    queue.forEach((resolve) => resolve(token));
    queue = [];
};
const publicAuthPaths = [
    "/api/v1/auth/signup",
    "/api/v1/auth/login",
    "/api/v1/auth/refresh",
    "/api/v1/auth/logout",
    "/api/v1/auth/verify-email",
    "/api/v1/auth/resend-verification",
    "/api/v1/auth/forgot-password",
    "/api/v1/auth/reset-password",
    "/api/v1/auth/oauth2"
];
const isPublicAuthRequest = (url) => {
    if (!url) {
        return false;
    }
    const path = url.startsWith("http") ? new URL(url).pathname : url;
    return publicAuthPaths.some((publicPath) => path.startsWith(publicPath));
};
apiClient.interceptors.request.use((config) => {
    const stopActivity = useApiActivityStore.getState().startActivity("Contacting backend");
    config._stopActivity = stopActivity;
    const token = useAuthStore.getState().accessToken;
    const hasAuthHeader = Boolean(config.headers?.Authorization || config.headers?.authorization);
    if (token && config.headers && !hasAuthHeader && !isPublicAuthRequest(config.url)) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});
apiClient.interceptors.response.use(
    (response) => {
        response.config._stopActivity?.();
        response.config._stopActivity = void 0;
        return response;
    },
    async (error) => {
        const original = error.config;
        original?._stopActivity?.();
        if (original) {
            original._stopActivity = void 0;
        }
        if (!original || error.response?.status !== 401 || original._retry || isPublicAuthRequest(original.url)) {
            return Promise.reject(error);
        }
        original._retry = true;
        if (refreshing) {
            return new Promise((resolve, reject) => {
                queue.push((token) => {
                    if (!token) {
                        reject(error);
                        return;
                    }
                    original.headers.Authorization = `Bearer ${token}`;
                    resolve(apiClient(original));
                });
            });
        }
        refreshing = true;
        try {
            const response = await apiClient.post(endpoints.auth.refresh);
            const token = unwrapApiData(response.data)?.accessToken;
            if (!token) {
                throw new Error("Refresh response did not include an access token");
            }
            const currentUser = useAuthStore.getState().user;
            if (currentUser) {
                useAuthStore.getState().setAuth(currentUser, token);
            }
            flushQueue(token);
            original.headers.Authorization = `Bearer ${token}`;
            return apiClient(original);
        } catch (refreshError) {
            flushQueue(null);
            useAuthStore.getState().clearAuth();
            window.location.assign("/login");
            return Promise.reject(refreshError);
        } finally {
            refreshing = false;
        }
    }
);
