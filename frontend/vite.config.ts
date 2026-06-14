import react from "@vitejs/plugin-react";
import {networkInterfaces} from "node:os";
import {createLogger, defineConfig, loadEnv} from "vite";

export default defineConfig(({mode}) => {
    const frontendEnv = loadEnv(mode, ".", "");
    const firstValue = (...values: Array<string | undefined>) => values.find((value) => value && value.trim().length > 0);
    const defaultApiGatewayUrl = "http://127.0.0.1:8080";
    const defaultFrontendPublicUrl = "http://localhost:5173";
    const normalizeUrl = (value: string | undefined) => value?.trim().replace(/\/+$/, "");
    const resolveApiGatewayUrl = () => {
        const configuredUrl = normalizeUrl(firstValue(process.env.VITE_API_GATEWAY_URL, frontendEnv.VITE_API_GATEWAY_URL));
        return configuredUrl ?? defaultApiGatewayUrl;
    };
    const apiGatewayUrl = resolveApiGatewayUrl();
    const proxyTarget = normalizeUrl(firstValue(process.env.VITE_DEV_PROXY_TARGET, frontendEnv.VITE_DEV_PROXY_TARGET)) ?? apiGatewayUrl;
    const frontendPublicUrl = normalizeUrl(firstValue(process.env.VITE_FRONTEND_PUBLIC_URL, frontendEnv.VITE_FRONTEND_PUBLIC_URL)) ?? defaultFrontendPublicUrl;
    const appEnv = firstValue(process.env.VITE_APP_ENV, frontendEnv.VITE_APP_ENV) ?? mode;
    const companyName = firstValue(process.env.COMPANY_NAME, process.env.COMAPNY_NAME, frontendEnv.COMPANY_NAME, frontendEnv.COMAPNY_NAME) ?? "Microservice Template";
    const companyShortDescription = firstValue(process.env.COMPANY_SHORT_DESCRIPTION, frontendEnv.COMPANY_SHORT_DESCRIPTION) ?? "Production-grade Java platform";
    const prometheusUrl = normalizeUrl(firstValue(process.env.VITE_PROMETHEUS_URL, frontendEnv.VITE_PROMETHEUS_URL));
    const grafanaUrl = normalizeUrl(firstValue(process.env.VITE_GRAFANA_URL, frontendEnv.VITE_GRAFANA_URL));
    const zipkinUrl = normalizeUrl(firstValue(process.env.VITE_ZIPKIN_URL, frontendEnv.VITE_ZIPKIN_URL));
    const lokiUrl = normalizeUrl(firstValue(process.env.VITE_LOKI_URL, frontendEnv.VITE_LOKI_URL));
    const discoveryUrl = normalizeUrl(firstValue(process.env.VITE_DISCOVERY_URL, frontendEnv.VITE_DISCOVERY_URL));
    const configServerUrl = normalizeUrl(firstValue(process.env.VITE_CONFIG_SERVER_URL, frontendEnv.VITE_CONFIG_SERVER_URL));
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
            "import.meta.env.VITE_APP_ENV": JSON.stringify(appEnv),
            "import.meta.env.VITE_COMPANY_NAME": JSON.stringify(companyName),
            "import.meta.env.VITE_COMPANY_SHORT_DESCRIPTION": JSON.stringify(companyShortDescription),
            "import.meta.env.VITE_PROMETHEUS_URL": JSON.stringify(prometheusUrl),
            "import.meta.env.VITE_GRAFANA_URL": JSON.stringify(grafanaUrl),
            "import.meta.env.VITE_ZIPKIN_URL": JSON.stringify(zipkinUrl),
            "import.meta.env.VITE_LOKI_URL": JSON.stringify(lokiUrl),
            "import.meta.env.VITE_DISCOVERY_URL": JSON.stringify(discoveryUrl),
            "import.meta.env.VITE_CONFIG_SERVER_URL": JSON.stringify(configServerUrl)
        },
        server: {
            port: 5173,
            strictPort: true,
            proxy: {
                "/api": {
                    target: proxyTarget,
                    changeOrigin: true,
                    secure: false
                },
                "/actuator": {
                    target: proxyTarget,
                    changeOrigin: true,
                    secure: false
                }
            }
        }
    };
});
