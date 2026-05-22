import {Activity, BarChart3, ExternalLink, Gauge, RadioTower, ServerCog, ShieldCheck} from "lucide-react";
import {env} from "../../config/env";
import {PageWrapper} from "../../components/common/PageWrapper";

const observabilityLinks = [
    {
        label: "Grafana",
        description: "Dashboards for service metrics and platform health.",
        href: env.grafanaUrl,
        Icon: BarChart3
    },
    {
        label: "Prometheus",
        description: "Metrics database and query UI.",
        href: env.prometheusUrl,
        Icon: Activity
    },
    {
        label: "Zipkin",
        description: "Distributed traces across gateway and services.",
        href: env.zipkinUrl,
        Icon: RadioTower
    },
    {
        label: "Eureka",
        description: "Service discovery registry and instance status.",
        href: env.discoveryUrl,
        Icon: ServerCog
    },
    {
        label: "Gateway Health",
        description: "API gateway readiness and health details.",
        href: env.gatewayHealthUrl,
        Icon: ShieldCheck
    },
    {
        label: "Gateway Metrics",
        description: "Raw Prometheus metrics from the API gateway.",
        href: env.gatewayMetricsUrl,
        Icon: Gauge
    },
    {
        label: "Config Server",
        description: "Config server home with health and example config paths.",
        href: env.configServerUrl,
        Icon: ServerCog
    },
    {
        label: "Config Health",
        description: "Configuration server readiness check.",
        href: `${env.configServerUrl}/actuator/health`,
        Icon: ShieldCheck
    },
    {
        label: "Gateway Config",
        description: "Resolved gateway configuration from Config Server.",
        href: `${env.configServerUrl}/api-gateway/default`,
        Icon: ServerCog
    }
];

const ObservabilityPage = () => <PageWrapper title="Observability">
    <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {observabilityLinks.map(({label, description, href, Icon}) => <a
            key={label}
            href={href}
            target="_blank"
            rel="noreferrer"
            className="group rounded-md border border-slate-200 bg-white p-4 transition hover:-translate-y-0.5 hover:border-slate-300 hover:shadow-lg dark:border-white/10 dark:bg-slate-950 dark:hover:border-white/20"
        >
            <span className="flex items-start justify-between gap-4">
                <span className="grid h-10 w-10 place-items-center rounded-md bg-slate-100 text-slate-700 dark:bg-white/10 dark:text-teal-300">
                    <Icon className="h-5 w-5"/>
                </span>
                <ExternalLink className="h-4 w-4 text-slate-400 transition group-hover:text-slate-700 dark:group-hover:text-white"/>
            </span>
            <span className="mt-4 block text-sm font-semibold text-slate-950 dark:text-white">{label}</span>
            <span className="mt-1 block text-sm leading-6 text-slate-600 dark:text-slate-300">{description}</span>
            <span className="mt-3 block break-all text-xs text-slate-500 dark:text-slate-400">{href}</span>
        </a>)}
    </div>
</PageWrapper>;

export {
    ObservabilityPage
};
