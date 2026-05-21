import {CreditCard, ExternalLink, LoaderCircle} from "lucide-react";
import {useEffect, useMemo, useRef, useState} from "react";
import {useMutation, useQuery, useQueryClient} from "@tanstack/react-query";
import {apiClient} from "../api/axiosInstance";
import {endpoints} from "../api/endpoints";
import {PageWrapper} from "../components/common/PageWrapper";
import {asArray, unwrapApiData} from "../utils/responseUtils";

const PaymentsPage = () => {
    const queryClient = useQueryClient();
    const [amount, setAmount] = useState("49");
    const [currency, setCurrency] = useState("USD");
    const [payment, setPayment] = useState(null);
    const handledReturnRef = useRef(null);
    const paymentsQuery = useQuery({
        queryKey: ["payments"],
        queryFn: async () => asArray((await apiClient.get(endpoints.payments.list)).data)
    });
    const createPayment = useMutation({
        mutationFn: async () => {
            const response = await apiClient.post(endpoints.payments.create, {
                amount: Number(amount),
                currency,
                method: "STRIPE",
                description: "Microservice platform checkout"
            });
            return unwrapApiData(response.data);
        },
        onSuccess: (created) => {
            setPayment(created);
            queryClient.invalidateQueries({queryKey: ["payments"]});
            if (created.provider === "STRIPE" && created.checkoutUrl) {
                window.location.assign(created.checkoutUrl);
            }
        }
    });
    const confirmPayment = useMutation({
        mutationFn: async ({paymentId, sessionId, status}) => {
            const response = await apiClient.post(endpoints.payments.confirm(paymentId), {
                sessionId,
                status
            });
            return unwrapApiData(response.data);
        },
        onSuccess: (confirmed) => {
            setPayment(confirmed);
            queryClient.invalidateQueries({queryKey: ["payments"]});
        }
    });
    useEffect(() => {
        const params = new URLSearchParams(window.location.search);
        const paymentId = params.get("paymentId");
        const status = params.get("status");
        const sessionId = params.get("session_id") ?? void 0;
        if (!paymentId || !status || handledReturnRef.current === paymentId) {
            return;
        }
        if (status === "success" || status === "cancelled") {
            handledReturnRef.current = paymentId;
            confirmPayment.mutate({paymentId, sessionId, status});
        }
    }, [confirmPayment]);
    const payments = paymentsQuery.data ?? [];
    const groupedPayments = useMemo(() => {
        return payments.reduce((groups, item) => {
            const label = monthLabel(item.createdAt);
            const existing = groups.find((group) => group.label === label);
            if (existing) {
                existing.items.push(item);
            } else {
                groups.push({label, items: [item]});
            }
            return groups;
        }, []);
    }, [payments]);
    const latestPayment = payment ?? payments[0] ?? null;
    return <PageWrapper title="Payments">
        <div className="grid gap-5 lg:grid-cols-[420px_1fr]">
            <form onSubmit={(event) => {
                event.preventDefault();
                createPayment.mutate();
            }} className="rounded-md border border-slate-200 bg-white p-5">
                <label className="mb-4 block">
                        <span
                            className="mb-1 block text-sm font-medium text-slate-700"
                        >Amount</span>
                    <input
                        value={amount}
                        onChange={(event) => setAmount(event.target.value)}
                        type="number"
                        min="1"
                        className="h-11 w-full rounded-md border border-slate-200 px-3 text-sm outline-none focus:border-slate-400"
                    />
                </label>
                <label className="mb-4 block">
                        <span
                            className="mb-1 block text-sm font-medium text-slate-700"
                        >Currency</span>
                    <select
                        value={currency}
                        onChange={(event) => setCurrency(event.target.value)}
                        className="h-11 w-full rounded-md border border-slate-200 px-3 text-sm outline-none focus:border-slate-400"
                    >
                        <option>USD</option>
                        <option>INR</option>
                        <option>EUR</option>
                    </select>
                </label>
                <button
                    disabled={createPayment.isPending}
                    className="inline-flex h-11 items-center gap-2 rounded-md bg-slate-950 px-4 text-sm font-medium text-white hover:bg-slate-800 disabled:cursor-wait disabled:opacity-70"
                >
                    {createPayment.isPending ?
                        <LoaderCircle className="h-4 w-4 animate-spin"/> :
                        <CreditCard className="h-4 w-4"/>}
                    Pay with Stripe
                </button>
                {createPayment.isError &&
                    <p className="mt-3 text-sm font-medium text-red-600">Payment could not
                        be started. Check the gateway and Stripe configuration.</p>}
            </form>
            <section className="rounded-md border border-slate-200 bg-white p-5">
                <h2 className="text-sm font-semibold text-slate-950">Latest payment</h2>
                {confirmPayment.isPending &&
                    <p className="mt-3 text-sm font-medium text-slate-600">Confirming Stripe
                        payment...</p>}
                {latestPayment ? <dl className="mt-4 grid gap-3 text-sm">
                    <div className="flex justify-between border-b pb-2">
                        <dt className="text-slate-500">Payment ID</dt>
                        <dd className="font-medium">{latestPayment.paymentId}</dd>
                    </div>
                    <div className="flex justify-between border-b pb-2">
                        <dt className="text-slate-500">Provider</dt>
                        <dd className="font-medium">{latestPayment.provider ?? "STRIPE"}</dd>
                    </div>
                    <div className="flex justify-between border-b pb-2">
                        <dt className="text-slate-500">Status</dt>
                        <dd className="font-medium">{latestPayment.status}</dd>
                    </div>
                    <div className="flex justify-between">
                        <dt className="text-slate-500">Amount</dt>
                        <dd className="font-medium">{latestPayment.amount} {latestPayment.currency}</dd>
                    </div>
                    {latestPayment.message &&
                        <p className="rounded-md bg-amber-50 p-3 text-amber-800">{latestPayment.message}</p>}
                    {latestPayment.checkoutUrl && <a
                        href={latestPayment.checkoutUrl}
                        className="inline-flex h-10 items-center gap-2 rounded-md border border-slate-200 px-4 font-medium hover:bg-slate-50"
                    >
                        <ExternalLink className="h-4 w-4"/>
                        Open checkout
                    </a>}
                </dl> : <p className="mt-4 text-sm text-slate-500">No payments found for this
                    account.</p>}
            </section>
        </div>
        <section className="mt-5 overflow-hidden rounded-md border border-slate-200 bg-white">
            <div className="border-b px-4 py-3">
                <h2 className="text-sm font-semibold text-slate-950">Payment history</h2>
            </div>
            {paymentsQuery.isLoading ? <div className="p-6 text-sm text-slate-500">Loading
                payments...</div> : groupedPayments.length === 0 ?
                <div className="p-6 text-sm text-slate-500">No completed or started payments
                    yet.</div> : groupedPayments.map((group) => <div key={group.label}
                                                                     className="border-b last:border-b-0">
                    <div
                        className="bg-slate-50 px-4 py-2 text-xs font-semibold uppercase tracking-wide text-slate-500">{group.label}</div>
                    {group.items.map((item) => <div
                        key={item.paymentId}
                        className="grid gap-3 border-t px-4 py-4 text-sm sm:grid-cols-[1fr_auto_auto] sm:items-center"
                    >
                        <div>
                            <p className="font-medium text-slate-950">{item.description || "Platform payment"}</p>
                            <p className="text-xs text-slate-500">{item.paymentId}</p>
                        </div>
                        <p className="font-medium text-slate-950">{item.amount} {item.currency}</p>
                        <p className="rounded-md bg-slate-100 px-2 py-1 text-xs font-semibold text-slate-700">{item.status}</p>
                    </div>)}
                </div>)}
        </section>
    </PageWrapper>;
};
const monthLabel = (value) => {
    if (!value) {
        return "Unknown date";
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return "Unknown date";
    }
    return new Intl.DateTimeFormat("en", {month: "long", year: "numeric"}).format(date);
};
export {
    PaymentsPage
};
