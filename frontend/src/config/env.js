const API_GATEWAY_STORAGE_KEY = "platform.apiGatewayUrl";
const LOCAL_API_PORT = "8080";
const LOCAL_API_HOSTS = new Set(["localhost", "127.0.0.1", "0.0.0.0", "::1"]);
const trimTrailingSlash = (value) => value?.trim().replace(/\/+$/, "");
const isHttpUrl = (value) => /^https?:\/\//i.test(value);

const runtimeApiGatewayUrl = () => {
    if (typeof window === "undefined") {
        return undefined;
    }
    const params = new URLSearchParams(window.location.search);
    const override = trimTrailingSlash(params.get("api") || params.get("apiUrl") || params.get("backend"));
    if (override?.toLowerCase() === "reset") {
        window.localStorage.removeItem(API_GATEWAY_STORAGE_KEY);
        return undefined;
    }
    if (override && isHttpUrl(override)) {
        window.localStorage.setItem(API_GATEWAY_STORAGE_KEY, override);
        return override;
    }
    const stored = trimTrailingSlash(window.localStorage.getItem(API_GATEWAY_STORAGE_KEY));
    return stored && isHttpUrl(stored) ? stored : undefined;
};

const isLocalApiUrl = (value) => {
    try {
        return LOCAL_API_HOSTS.has(new URL(value).hostname);
    } catch {
        return false;
    }
};
const apiGatewayUrlFromBrowserHost = () => {
    if (typeof window === "undefined") {
        return undefined;
    }
    return `${window.location.protocol}//${window.location.hostname}:${LOCAL_API_PORT}`;
};
const configuredApiGatewayUrl = trimTrailingSlash(import.meta.env.VITE_API_GATEWAY_URL || "http://127.0.0.1:8080");
const buildApiGatewayUrl = isLocalApiUrl(configuredApiGatewayUrl)
    ? apiGatewayUrlFromBrowserHost()
    : configuredApiGatewayUrl;
const resolvedApiGatewayUrl = runtimeApiGatewayUrl() || buildApiGatewayUrl;

const env = {
    appEnv: import.meta.env.VITE_APP_ENV || import.meta.env.MODE,
    isProduction: import.meta.env.PROD,
    apiGatewayUrl: resolvedApiGatewayUrl,
    frontendPublicUrl: trimTrailingSlash(import.meta.env.VITE_FRONTEND_PUBLIC_URL || "http://localhost:5173"),
    oauthProviders: (import.meta.env.VITE_OAUTH_PROVIDERS ?? "google,github,linkedin").split(",").map((provider) => provider.trim()).filter(Boolean)
};
export {
    API_GATEWAY_STORAGE_KEY,
    env
};
