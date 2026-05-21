import react from "@vitejs/plugin-react";
import {defineConfig, loadEnv} from "vite";

export default defineConfig(({mode}) => {
    const frontendEnv = loadEnv(mode, ".", "");
    const rootEnv = loadEnv(mode, "../", "");
    const firstValue = (...values: Array<string | undefined>) => values.find((value) => value && value.trim().length > 0);
    const defaultApiGatewayUrl = mode === "production" ? "https://microservice-industry-level-boiler-plate.onrender.com" : "http://localhost:8080";
    const defaultFrontendPublicUrl = mode === "production" ? "https://manishrnl-microservice-template.netlify.app" : "http://localhost:5173";
    const apiGatewayUrl = firstValue(process.env.VITE_API_GATEWAY_URL, frontendEnv.VITE_API_GATEWAY_URL, rootEnv.VITE_API_GATEWAY_URL, process.env.BACKEND_PUBLIC_URL, rootEnv.BACKEND_PUBLIC_URL) ?? defaultApiGatewayUrl;
    const frontendPublicUrl = firstValue(process.env.VITE_FRONTEND_PUBLIC_URL, frontendEnv.VITE_FRONTEND_PUBLIC_URL, rootEnv.VITE_FRONTEND_PUBLIC_URL, process.env.FRONTEND_PUBLIC_URL, rootEnv.FRONTEND_PUBLIC_URL) ?? defaultFrontendPublicUrl;
    const appEnv = firstValue(process.env.VITE_APP_ENV, frontendEnv.VITE_APP_ENV, rootEnv.VITE_APP_ENV) ?? mode;

    return {
        plugins: [react()],
        envDir: ".",
        define: {
            "import.meta.env.VITE_API_GATEWAY_URL": JSON.stringify(apiGatewayUrl),
            "import.meta.env.VITE_FRONTEND_PUBLIC_URL": JSON.stringify(frontendPublicUrl),
            "import.meta.env.VITE_APP_ENV": JSON.stringify(appEnv)
        },
        server: {
            port: 5173
        }
    };
});
