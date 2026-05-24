const API_GATEWAY_STORAGE_KEY = "platform.apiGatewayUrl";
const LOCAL_API_PORT = "8080";
const LOCAL_API_HOSTS = new Set(["localhost", "127.0.0.1", "0.0.0.0", "::1"]);
const trimTrailingSlash = (value) => value?.trim().replace(/\/+$/, "");
const isHttpUrl = (value) => /^https?:\/\//i.test(value);
const localBrowserUrl = (port) => {
    if (typeof window === "undefined" || !port) {
        return undefined;
    }
    return `${window.location.protocol}//${window.location.hostname}:${port}`;
};

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
const resolvePublicUrl = (configuredUrl, fallbackPort) => {
    const value = trimTrailingSlash(configuredUrl);
    if (!value || !isHttpUrl(value)) {
        return localBrowserUrl(fallbackPort);
    }
    try {
        const url = new URL(value);
        if (!LOCAL_API_HOSTS.has(url.hostname) && !url.hostname.includes(".")) {
            return localBrowserUrl(url.port || fallbackPort);
        }
        return value;
    } catch {
        return localBrowserUrl(fallbackPort);
    }
};
const configuredApiGatewayUrl = trimTrailingSlash(import.meta.env.VITE_API_GATEWAY_URL || "http://127.0.0.1:8080");
const buildApiGatewayUrl = isLocalApiUrl(configuredApiGatewayUrl)
    ? ""
    : configuredApiGatewayUrl;
const resolvedApiGatewayUrl = runtimeApiGatewayUrl() || buildApiGatewayUrl;
const prometheusUrl = resolvePublicUrl(import.meta.env.VITE_PROMETHEUS_URL, "9090");
const grafanaUrl = resolvePublicUrl(import.meta.env.VITE_GRAFANA_URL, "3000");
const zipkinUrl = resolvePublicUrl(import.meta.env.VITE_ZIPKIN_URL, "9411");
const discoveryUrl = resolvePublicUrl(import.meta.env.VITE_DISCOVERY_URL, "8761");
const configServerUrl = resolvePublicUrl(import.meta.env.VITE_CONFIG_SERVER_URL, "8888");

const env = {
    appEnv: import.meta.env.VITE_APP_ENV || import.meta.env.MODE,
    isProduction: import.meta.env.PROD,
    apiGatewayUrl: resolvedApiGatewayUrl,
    prometheusUrl,
    grafanaUrl,
    zipkinUrl,
    discoveryUrl,
    configServerUrl,
    gatewayMetricsUrl: `${resolvedApiGatewayUrl}/actuator/prometheus`,
    gatewayHealthUrl: `${resolvedApiGatewayUrl}/actuator/health`,
    frontendPublicUrl: trimTrailingSlash(import.meta.env.VITE_FRONTEND_PUBLIC_URL || "http://localhost:5173"),
    oauthProviders: (import.meta.env.VITE_OAUTH_PROVIDERS ?? "google,github,linkedin").split(",").map((provider) => provider.trim()).filter(Boolean)
};
export {
    API_GATEWAY_STORAGE_KEY,
    env
};
