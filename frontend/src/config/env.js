const trimTrailingSlash = (value) => value?.replace(/\/+$/, "");

const env = {
    appEnv: import.meta.env.VITE_APP_ENV || import.meta.env.MODE,
    isProduction: import.meta.env.PROD,
    apiGatewayUrl: trimTrailingSlash(import.meta.env.VITE_API_GATEWAY_URL || "http://localhost:8080"),
    frontendPublicUrl: trimTrailingSlash(import.meta.env.VITE_FRONTEND_PUBLIC_URL || "https://manishrnl-microservice-template.netlify.app"),
    oauthProviders: (import.meta.env.VITE_OAUTH_PROVIDERS ?? "google,github,linkedin").split(",").map((provider) => provider.trim()).filter(Boolean)
};
export {
    env
};
