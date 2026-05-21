import react from "@vitejs/plugin-react";
import {networkInterfaces} from "node:os";
import {createLogger, defineConfig, loadEnv} from "vite";

export default defineConfig(({mode}) => {
    const frontendEnv = loadEnv(mode, ".", "");
    const rootEnv = loadEnv(mode, "../", "");
    const firstValue = (...values: Array<string | undefined>) => values.find((value) => value && value.trim().length > 0);
    const defaultApiGatewayUrl = "http://127.0.0.1:8080";
    const defaultFrontendPublicUrl = "http://localhost:5173";
    const normalizeUrl = (value: string | undefined) => value?.trim().replace(/\/+$/, "");
    const resolveApiGatewayUrl = () => {
        const configuredUrl = normalizeUrl(firstValue(process.env.VITE_API_GATEWAY_URL, frontendEnv.VITE_API_GATEWAY_URL, rootEnv.VITE_API_GATEWAY_URL, process.env.BACKEND_PUBLIC_URL, rootEnv.BACKEND_PUBLIC_URL));
        return configuredUrl ?? defaultApiGatewayUrl;
    };
    const apiGatewayUrl = resolveApiGatewayUrl();
    const frontendPublicUrl = normalizeUrl(firstValue(process.env.VITE_FRONTEND_PUBLIC_URL, frontendEnv.VITE_FRONTEND_PUBLIC_URL, rootEnv.VITE_FRONTEND_PUBLIC_URL, process.env.FRONTEND_PUBLIC_URL, rootEnv.FRONTEND_PUBLIC_URL)) ?? defaultFrontendPublicUrl;
    const appEnv = firstValue(process.env.VITE_APP_ENV, frontendEnv.VITE_APP_ENV, rootEnv.VITE_APP_ENV) ?? mode;
    const virtualAdapterPattern = /(virtual|vethernet|wsl|docker|hyper-v|vmware|virtualbox|loopback|bluetooth)/i;
    const hiddenNetworkHosts = new Set(Object.entries(networkInterfaces())
        .filter(([name]) => virtualAdapterPattern.test(name))
        .flatMap(([, addresses = []]) => addresses
            .filter((address) => address.family === "IPv4")
            .map((address) => address.address)));
    const logger = createLogger();
    const baseInfo = logger.info;
    logger.info = (message, options) => {
        const filteredMessage = message
            .split("\n")
            .filter((line) => !Array.from(hiddenNetworkHosts).some((host) => line.includes(host)))
            .join("\n");
        if (filteredMessage.trim()) {
            baseInfo(filteredMessage, options);
        }
    };

    return {
        plugins: [react()],
        customLogger: logger,
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
