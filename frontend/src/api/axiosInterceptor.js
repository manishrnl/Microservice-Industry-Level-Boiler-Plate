import {apiClient} from "./axiosInstance";
import {endpoints} from "./endpoints";
import {useAuthStore} from "../store/authStore";
import {useApiActivityStore} from "../store/apiActivityStore";
import {getPreferredTimeZone} from "../store/preferencesStore";
import {unwrapApiData} from "../utils/responseUtils";
import {authUserFromToken} from "../utils/tokenUtils";
import {getClientLocalTime} from "../utils/clientContext";

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
const activity = (message, detail) => ({message, detail});
const pathFromUrl = (url = "") => {
    try {
        return new URL(url, window.location.origin).pathname;
    } catch {
        return String(url).split("?")[0];
    }
};
const queryFromUrl = (url = "") => {
    try {
        return new URL(url, window.location.origin).searchParams;
    } catch {
        return new URLSearchParams(String(url).split("?")[1] ?? "");
    }
};
const apiActivityFor = (config) => {
    const method = String(config.method ?? "get").toUpperCase();
    const path = pathFromUrl(config.url);
    const query = queryFromUrl(config.url);
    if (path === "/api/v1/auth/login") {
        return activity("Signing you in", "Verifying credentials and opening a fresh account session.");
    }
    if (path === "/api/v1/auth/signup") {
        return activity("Creating account", "Saving your profile, role, and login record in the auth database.");
    }
    if (path === "/api/v1/auth/refresh") {
        return activity("Refreshing secure session", "Checking the refresh cookie and issuing a new access token.");
    }
    if (path === "/api/v1/auth/logout") {
        return activity("Signing you out", "Revoking the current session and clearing secure cookies.");
    }
    if (path === "/api/v1/auth/me" && method === "GET") {
        return activity("Checking account session", "Loading the signed-in user and profile permissions.");
    }
    if (path === "/api/v1/auth/me" && method === "PUT") {
        return activity("Saving profile", "Updating your account name and profile details.");
    }
    if (path === "/api/v1/auth/me/avatar") {
        return activity("Saving profile avatar", "Updating the avatar URL stored with your account.");
    }
    if (path === "/api/v1/auth/sessions" && method === "GET") {
        return activity("Loading active sessions", "Reading browser, device, IP, and login records from the auth database.");
    }
    if (path === "/api/v1/auth/sessions/all" && method === "DELETE") {
        return activity("Revoking all sessions", "Invalidating saved login sessions across your devices.");
    }
    if (path.startsWith("/api/v1/auth/sessions/") && method === "DELETE") {
        return activity("Revoking session", "Deleting the selected browser session from the auth database.");
    }
    if (path.includes("/verify-email")) {
        return activity("Verifying email", "Checking the verification code and activating the account.");
    }
    if (path.includes("/forgot-password")) {
        return activity("Sending reset email", "Creating a password reset request and sending instructions.");
    }
    if (path.includes("/reset-password")) {
        return activity("Resetting password", "Validating the reset token and saving the new password.");
    }
    if (path === "/api/v1/users" && method === "GET") {
        return activity("Loading users", "Reading user accounts, roles, and status records.");
    }
    if (path.startsWith("/api/v1/users/") && path.endsWith("/role")) {
        return activity("Saving role changes", "Updating permissions for the selected user account.");
    }
    if (path === "/api/v1/users/me/preferences" && method === "GET") {
        return activity("Loading preferences", "Reading timezone and display settings for this account.");
    }
    if (path === "/api/v1/users/me/preferences" && method !== "GET") {
        return activity("Saving preferences", "Updating timezone and account preference records.");
    }
    if (path === "/api/v1/notifications" && method === "GET") {
        return activity("Loading notifications", "Fetching unread and historical account messages.");
    }
    if (path === "/api/v1/notifications/read-all") {
        return activity("Marking notifications read", "Updating all unread notification records.");
    }
    if (path.startsWith("/api/v1/notifications/") && path.endsWith("/read")) {
        return activity("Marking notification read", "Saving the read state for this notification.");
    }
    if (path === "/api/v1/notifications" && method === "DELETE") {
        return activity("Clearing notifications", "Deleting saved account notification records.");
    }
    if (path.startsWith("/api/v1/notifications/") && method === "DELETE") {
        return activity("Deleting notification", "Removing the selected notification from the database.");
    }
    if (path === "/api/v1/files/my-files") {
        return activity("Loading files", "Reading file metadata and ownership records.");
    }
    if (path === "/api/v1/files/upload") {
        return activity("Uploading file", "Saving file content and metadata for your account.");
    }
    if (path.endsWith("/metadata") && path.startsWith("/api/v1/files/")) {
        return activity("Loading file metadata", "Reading filename, size, type, and storage details.");
    }
    if (path.endsWith("/download-url") && path.startsWith("/api/v1/files/")) {
        return activity("Preparing file link", "Creating a browser-safe download link for the selected file.");
    }
    if (path.endsWith("/download") && path.startsWith("/api/v1/files/")) {
        return query.get("disposition") === "inline"
            ? activity("Opening file preview", "Streaming the file with its original format for browser viewing.")
            : activity("Downloading file", "Streaming the saved bytes and filename from file storage.");
    }
    if (path.startsWith("/api/v1/files/") && method === "DELETE") {
        return activity("Deleting file", "Removing the file metadata and saved content record.");
    }
    if (path === "/api/v1/payments" && method === "GET") {
        return activity("Loading payments", "Reading payment history, amounts, providers, and statuses.");
    }
    if (path === "/api/v1/payments" && method === "POST") {
        return activity("Starting payment", "Creating a payment record and requesting checkout details.");
    }
    if (path.startsWith("/api/v1/payments/") && path.endsWith("/confirm")) {
        return activity("Confirming payment", "Checking provider status and updating the payment record.");
    }
    if (path === "/api/v1/ai/sessions" && method === "GET") {
        return activity("Loading AI chats", "Reading saved assistant conversation sessions.");
    }
    if (path === "/api/v1/ai/sessions" && method === "POST") {
        return activity("Creating AI chat", "Starting a new assistant conversation record.");
    }
    if (path.startsWith("/api/v1/ai/sessions/") && path.endsWith("/messages")) {
        return activity("Loading chat messages", "Reading saved messages for this assistant conversation.");
    }
    if (path.startsWith("/api/v1/ai/sessions/") && method === "DELETE") {
        return activity("Deleting AI chat", "Removing the selected conversation and its saved messages.");
    }
    if (path === "/api/v1/ai/chat") {
        return activity("Sending message to AI", "Saving your prompt and waiting for the assistant response.");
    }
    if (path === "/api/v1/ai/usage") {
        return activity("Loading AI usage", "Reading token and request usage totals.");
    }
    if (path === "/api/v1/audit" && method === "GET") {
        return activity("Loading audit log", "Reading admin activity, before/after state, and request metadata.");
    }
    if (path === "/api/v1/audit/export") {
        return activity("Exporting audit log", "Preparing the latest audit records for download.");
    }
    return activity("Syncing with backend", `${method} ${path || "/"} is waiting for the API gateway response.`);
};
apiClient.interceptors.request.use((config) => {
    const stopActivity = useApiActivityStore.getState().startActivity(apiActivityFor(config));
    config._stopActivity = stopActivity;
    if (config.headers) {
        const timeZone = getPreferredTimeZone();
        if (timeZone) {
            config.headers["X-Client-Time-Zone"] = timeZone;
        }
        config.headers["X-Client-Local-Time"] = getClientLocalTime(timeZone);
    }
    const token = useAuthStore.getState().accessToken;
    const user = useAuthStore.getState().user;
    const hasAuthHeader = Boolean(config.headers?.Authorization || config.headers?.authorization);
    if (token && config.headers && !hasAuthHeader && !isPublicAuthRequest(config.url)) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    if (config.headers && user && !isPublicAuthRequest(config.url)) {
        if (user.name) {
            config.headers["X-User-Name"] = user.name;
        }
        if (user.email) {
            config.headers["X-User-Email"] = user.email;
        }
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
            const refreshedUser = currentUser ?? authUserFromToken(token);
            if (refreshedUser) {
                useAuthStore.getState().setAuth(refreshedUser, token);
            }
            flushQueue(token);
            original.headers.Authorization = `Bearer ${token}`;
            return apiClient(original);
        } catch (refreshError) {
            flushQueue(null);
            useAuthStore.getState().expireSession();
            window.location.assign("/login");
            return Promise.reject(refreshError);
        } finally {
            refreshing = false;
        }
    }
);
