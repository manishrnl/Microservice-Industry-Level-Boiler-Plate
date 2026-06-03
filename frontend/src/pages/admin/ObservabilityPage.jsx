import {Activity, BarChart3, ExternalLink, Gauge, RadioTower, ServerCog, ShieldCheck, Terminal} from "lucide-react";
import {env} from "../../config/env";
import {PageWrapper} from "../../components/common/PageWrapper";

const grafanaExploreUrl = `${env.grafanaUrl}/explore?left=%7B%22datasource%22:%22Loki%22,%22queries%22:%5B%7B%22expr%22:%22%7Bcompose_project%3D%5C%22microservice-industry%5C%22%7D%22%7D%5D%7D`;
const grafanaSamplesUrl = `${env.grafanaUrl}/d/platform-observability-samples/platform-observability-samples?orgId=1&from=now-30m&to=now`;

const observabilityLinks = [
    {
        label: "Grafana",
        href: env.grafanaUrl,
        Icon: BarChart3,
        tone: "bg-orange-100 text-orange-700 ring-orange-200 dark:bg-orange-500/15 dark:text-orange-300 dark:ring-orange-500/25"
    },
    {
        label: "Grafana Explore",
        href: grafanaExploreUrl,
        Icon: BarChart3,
        tone: "bg-amber-100 text-amber-700 ring-amber-200 dark:bg-amber-500/15 dark:text-amber-300 dark:ring-amber-500/25"
    },
    {
        label: "Sample Dashboard",
        href: grafanaSamplesUrl,
        Icon: Gauge,
        tone: "bg-lime-100 text-lime-700 ring-lime-200 dark:bg-lime-500/15 dark:text-lime-300 dark:ring-lime-500/25"
    },
    {
        label: "Loki API",
        href: env.lokiStatusUrl,
        Icon: Terminal,
        tone: "bg-emerald-100 text-emerald-700 ring-emerald-200 dark:bg-emerald-500/15 dark:text-emerald-300 dark:ring-emerald-500/25"
    },
    {
        label: "Prometheus",
        href: env.prometheusUrl,
        Icon: Activity,
        tone: "bg-red-100 text-red-700 ring-red-200 dark:bg-red-500/15 dark:text-red-300 dark:ring-red-500/25"
    },
    {
        label: "Zipkin",
        href: env.zipkinUrl,
        Icon: RadioTower,
        tone: "bg-violet-100 text-violet-700 ring-violet-200 dark:bg-violet-500/15 dark:text-violet-300 dark:ring-violet-500/25"
    },
    {
        label: "Eureka",
        href: env.discoveryUrl,
        Icon: ServerCog,
        tone: "bg-sky-100 text-sky-700 ring-sky-200 dark:bg-sky-500/15 dark:text-sky-300 dark:ring-sky-500/25"
    },
    {
        label: "Gateway Health",
        href: env.gatewayHealthUrl,
        Icon: ShieldCheck,
        tone: "bg-teal-100 text-teal-700 ring-teal-200 dark:bg-teal-500/15 dark:text-teal-300 dark:ring-teal-500/25"
    },
    {
        label: "Gateway Metrics",
        href: env.gatewayMetricsUrl,
        Icon: Gauge,
        tone: "bg-cyan-100 text-cyan-700 ring-cyan-200 dark:bg-cyan-500/15 dark:text-cyan-300 dark:ring-cyan-500/25"
    },
    {
        label: "Config Server",
        href: env.configServerUrl,
        Icon: ServerCog,
        tone: "bg-indigo-100 text-indigo-700 ring-indigo-200 dark:bg-indigo-500/15 dark:text-indigo-300 dark:ring-indigo-500/25"
    }
];

const ObservabilityPage = () => {
    return <PageWrapper title="Observability">
        <div className="space-y-5">
            <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
                {observabilityLinks.map((link) => <ObservabilityLink key={link.label} {...link}/>)}
            </section>
        </div>
    </PageWrapper>;
};

const ObservabilityLink = ({label, href, Icon, tone}) => {
    const content = <>
        <span className="flex items-start justify-between gap-4">
            <span className={`grid h-10 w-10 place-items-center rounded-md ring-1 ${tone}`}>
                <Icon className="h-5 w-5"/>
            </span>
            <ExternalLink className="h-4 w-4 text-slate-400 transition group-hover:text-slate-700 dark:group-hover:text-white"/>
        </span>
        <span className="mt-4 block text-sm font-semibold text-slate-950 dark:text-white">{label}</span>
        <span className="mt-3 block break-all text-xs text-slate-500 dark:text-slate-400">{href}</span>
    </>;
    const className = "group rounded-md border border-slate-200 bg-white p-4 transition hover:-translate-y-0.5 hover:border-slate-300 hover:shadow-lg dark:border-white/10 dark:bg-slate-950 dark:hover:border-white/20";
    return <a href={href} target="_blank" rel="noreferrer" className={className}>{content}</a>;
};

export {
    ObservabilityPage
};
