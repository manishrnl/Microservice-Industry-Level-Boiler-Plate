import {Bot, Check, Gauge, LoaderCircle, LockKeyhole, Sparkles, Zap} from "lucide-react";
import {useMutation} from "@tanstack/react-query";
import {apiClient} from "../api/axiosInstance";
import {endpoints} from "../api/endpoints";
import {PageWrapper} from "../components/common/PageWrapper";
import {COMPANY_NAME} from "../components/layout/BrandMark";
import {unwrapApiData} from "../utils/responseUtils";

const currency = "INR";

const plans = [
    {
        name: "Free",
        price: 0,
        tokenLimit: "10,000",
        description: "Trial access with 10% of the premium token pool.",
        features: ["AI chat access", "Saved chat history", "Basic usage tracking"],
        cta: "Current free tier"
    },
    {
        name: "Basic",
        price: 99,
        tokenLimit: "25,000",
        description: "For light AI usage and personal workflow testing.",
        features: ["2.5x free token limit", "Standard AI chat", "Payment history"],
        cta: "Choose Basic"
    },
    {
        name: "Moderate",
        price: 299,
        tokenLimit: "50,000",
        description: "For regular users who need more room for daily prompts.",
        features: ["5x free token limit", "Longer chat sessions", "Priority-ready billing record"],
        cta: "Choose Moderate"
    },
    {
        name: "Advanced",
        price: 499,
        tokenLimit: "75,000",
        description: "For heavier work with larger conversations and more frequent use.",
        features: ["7.5x free token limit", "Advanced AI workspace", "Higher monthly usage ceiling"],
        cta: "Choose Advanced"
    },
    {
        name: "Premium",
        price: 799,
        tokenLimit: "100,000",
        description: "Full premium allocation for production AI workflows.",
        features: ["Full premium token pool", "Priority processing", "Premium usage controls"],
        cta: "Choose Premium",
        highlighted: true
    }
];

const features = [
    {title: "Higher token limits", text: "Move beyond the free-trial pool for longer chats and larger working context.", Icon: Gauge},
    {title: "Advanced AI workspace", text: "Use richer prompts, saved workflows, and priority AI capacity for heavier tasks.", Icon: Bot},
    {title: "Priority processing", text: "Keep AI tasks responsive when usage grows across your team or account.", Icon: Zap},
    {title: "Premium controls", text: "Prepare billing, usage controls, and admin visibility for production plans.", Icon: LockKeyhole}
];

const PremiumPage = () => {
    const checkout = useMutation({
        mutationFn: async (plan) => {
            const description = `${COMPANY_NAME} ${plan.name} plan - ${plan.tokenLimit} AI tokens - ${currency} ${plan.price}`;
            const response = await apiClient.post(endpoints.payments.create, {
                amount: plan.price,
                currency,
                method: "STRIPE",
                description
            });
            return unwrapApiData(response.data);
        },
        onSuccess: (created) => {
            if (created.checkoutUrl) {
                window.location.assign(created.checkoutUrl);
            }
        }
    });
    const activePlanName = checkout.variables?.name;
    return <PageWrapper title="Premium">
        <section className="rounded-md border border-slate-200 bg-white p-6 shadow-sm dark:border-white/10 dark:bg-slate-950">
            <div className="max-w-3xl">
                <div className="inline-flex items-center gap-2 rounded-md border border-teal-200 bg-teal-50 px-3 py-1 text-xs font-semibold text-teal-700 dark:border-teal-300/30 dark:bg-teal-300/10 dark:text-teal-200">
                    <Sparkles className="h-4 w-4"/>
                    Advanced AI
                </div>
                <h2 className="mt-4 text-2xl font-semibold text-slate-950 dark:text-white">Pricing for every AI usage level</h2>
                <p className="mt-3 text-sm leading-6 text-slate-600 dark:text-slate-300">
                    Free trial users receive 10% of the premium token pool. Paid plans create a Stripe Checkout session with the selected plan, token allocation, currency, and amount.
                </p>
            </div>
        </section>

        <section className="mt-5 grid gap-4 md:grid-cols-2 xl:grid-cols-5">
            {plans.map((plan) => <article
                key={plan.name}
                className={`flex min-h-[360px] flex-col rounded-md border bg-white p-5 shadow-sm dark:bg-slate-950 ${plan.highlighted ? "border-teal-400 ring-2 ring-teal-100 dark:ring-teal-300/20" : "border-slate-200 dark:border-white/10"}`}
            >
                <div className="flex items-start justify-between gap-3">
                    <div>
                        <h3 className="text-base font-semibold text-slate-950 dark:text-white">{plan.name}</h3>
                        <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{plan.description}</p>
                    </div>
                    {plan.highlighted && <span className="rounded-md bg-teal-50 px-2 py-1 text-xs font-semibold text-teal-700 dark:bg-teal-300/10 dark:text-teal-200">Best</span>}
                </div>
                <div className="mt-5">
                    <p className="text-3xl font-semibold text-slate-950 dark:text-white">
                        {plan.price === 0 ? "Free" : `₹${plan.price}`}
                    </p>
                    <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{plan.tokenLimit} tokens</p>
                </div>
                <ul className="mt-5 flex-1 space-y-3">
                    {plan.features.map((feature) => <li key={feature} className="flex gap-2 text-sm text-slate-600 dark:text-slate-300">
                        <Check className="mt-0.5 h-4 w-4 shrink-0 text-teal-600 dark:text-teal-300"/>
                        <span>{feature}</span>
                    </li>)}
                </ul>
                <button
                    type="button"
                    disabled={plan.price === 0 || checkout.isPending}
                    onClick={() => checkout.mutate(plan)}
                    className={`mt-6 inline-flex h-11 items-center justify-center gap-2 rounded-md px-4 text-sm font-semibold transition disabled:cursor-not-allowed disabled:opacity-70 ${plan.highlighted ? "bg-slate-950 text-white hover:bg-slate-800 dark:bg-teal-300 dark:text-slate-950 dark:hover:bg-teal-200" : "border border-slate-200 text-slate-700 hover:bg-slate-50 dark:border-white/10 dark:text-slate-200 dark:hover:bg-white/10"}`}
                >
                    {checkout.isPending && activePlanName === plan.name ? <LoaderCircle className="h-4 w-4 animate-spin"/> : <Sparkles className="h-4 w-4"/>}
                    {plan.cta}
                </button>
            </article>)}
        </section>

        {checkout.isError && <p className="mt-4 rounded-md border border-red-200 bg-red-50 p-3 text-sm font-medium text-red-700">
            Stripe checkout could not be started. Check the backend Stripe secret key and gateway connection.
        </p>}

        <section className="mt-5 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            {features.map(({title, text, Icon}) => <article
                key={title}
                className="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-950"
            >
                <Icon className="h-5 w-5 text-slate-500 dark:text-slate-400"/>
                <h3 className="mt-4 text-base font-semibold text-slate-950 dark:text-white">{title}</h3>
                <p className="mt-2 text-sm leading-6 text-slate-600 dark:text-slate-300">{text}</p>
            </article>)}
        </section>
    </PageWrapper>;
};

export {
    PremiumPage
};
