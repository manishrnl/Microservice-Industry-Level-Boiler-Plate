const env = {
    appEnv: import.meta.env.VITE_APP_ENV || import.meta.env.MODE,
    isProduction: import.meta.env.PROD,
    apiGatewayUrl: import.meta.env.VITE_API_GATEWAY_URL || (import.meta.env.PROD ? "https://microservice-industry-level-boiler-plate.onrender.com" : "http://localhost:8080"),
    frontendPublicUrl: import.meta.env.VITE_FRONTEND_PUBLIC_URL || (import.meta.env.PROD ? "https://manishrnl-microservice-template.netlify.app" : "https://manishrnl-microservice-template.netlify.app"),
    oauthProviders: (import.meta.env.VITE_OAUTH_PROVIDERS ?? "google,github,linkedin").split(",").map((provider) => provider.trim()).filter(Boolean)
};
export {
    env
};
