import {useMutation, useQuery, useQueryClient} from "@tanstack/react-query";
import {CreditCard, KeyRound, LoaderCircle, LockOpen, RefreshCw, Save, Search, ShieldAlert, UserRound} from "lucide-react";
import {useMemo, useState} from "react";
import toast from "react-hot-toast";
import {apiClient} from "../../api/axiosInstance";
import {endpoints} from "../../api/endpoints";
import {PageWrapper} from "../../components/common/PageWrapper";
import {asArray, unwrapApiData} from "../../utils/responseUtils";

const roleOptions = ["SUPER_ADMIN", "ADMIN", "EDITOR", "CREATOR", "VIEWER", "USER"];

const SuperAdminDashboardPage = () => {
    const queryClient = useQueryClient();
    const [search, setSearch] = useState("");
    const [selectedUserId, setSelectedUserId] = useState("");
    const [passwords, setPasswords] = useState({password: "", confirmPassword: ""});
    const [draftRoles, setDraftRoles] = useState({});
    const usersQuery = useQuery({
        queryKey: ["super-admin-users"],
        queryFn: async () => asArray((await apiClient.get(endpoints.users.list)).data),
        refetchOnMount: "always"
    });
    const users = usersQuery.data ?? [];
    const selectedUser = users.find((user) => user.userId === selectedUserId) ?? users[0] ?? null;
    const activeUserId = selectedUser?.userId ?? "";
    const paymentsQuery = useQuery({
        queryKey: ["super-admin-payments", activeUserId],
        enabled: Boolean(activeUserId),
        queryFn: async () => asArray((await apiClient.get(endpoints.payments.adminByUser(activeUserId))).data)
    });
    const unlockUser = useMutation({
        mutationFn: async (userId) => unwrapApiData((await apiClient.post(endpoints.auth.adminUnlockUser(userId))).data),
        onSuccess: async () => {
            toast.success("Account lock removed");
            await queryClient.invalidateQueries({queryKey: ["super-admin-users"]});
            await queryClient.invalidateQueries({queryKey: ["users"]});
        },
        onError: (error) => toast.error(error?.response?.data?.detail || "Could not unlock account")
    });
    const changePassword = useMutation({
        mutationFn: async ({userId, payload}) => unwrapApiData((await apiClient.put(endpoints.auth.adminPassword(userId), payload)).data),
        onSuccess: () => {
            setPasswords({password: "", confirmPassword: ""});
            toast.success("Password changed and sessions revoked");
        },
        onError: (error) => toast.error(error?.response?.data?.detail || "Could not change password")
    });
    const updateRole = useMutation({
        mutationFn: async ({userId, roles}) => unwrapApiData((await apiClient.put(endpoints.users.role(userId), {roles})).data),
        onSuccess: async (saved) => {
            setDraftRoles((current) => {
                const next = {...current};
                delete next[saved.userId];
                return next;
            });
            toast.success("Account roles saved");
            await queryClient.invalidateQueries({queryKey: ["super-admin-users"]});
            await queryClient.invalidateQueries({queryKey: ["users"]});
        },
        onError: (error) => toast.error(error?.response?.data?.detail || "Could not save account roles")
    });
    const visibleUsers = useMemo(() => {
        const term = search.trim().toLowerCase();
        return users.filter((user) => {
            const haystack = [user.name, user.email, user.username, user.accountStatus, user.roles?.join(" "), user.userId]
                .filter(Boolean)
                .join(" ")
                .toLowerCase();
            return !term || haystack.includes(term);
        });
    }, [search, users]);
    const lockedUsers = users.filter((user) => user.accountLocked).length;
    const payments = paymentsQuery.data ?? [];
    const totalPaymentAmount = payments.reduce((sum, payment) => sum + Number(payment.amount ?? 0), 0);
    const selectedUserRestricted = Boolean(selectedUser?.accountLocked || selectedUser?.lockedUntil || Number(selectedUser?.failedAttempts ?? 0) > 0 || selectedUser?.accountStatus === "SUSPENDED");
    const rolesFor = (user) => draftRoles[user.userId] ?? user.roles ?? ["USER"];
    const hasRoleDraft = Boolean(activeUserId && draftRoles[activeUserId]);
    const toggleRole = (user, role) => {
        setDraftRoles((current) => {
            const existing = new Set(rolesFor(user));
            if (existing.has(role)) {
                existing.delete(role);
            } else {
                existing.add(role);
            }
            if (existing.size === 0) {
                existing.add("USER");
            }
            return {...current, [user.userId]: Array.from(existing)};
        });
    };
    const submitPassword = (event) => {
        event.preventDefault();
        if (!activeUserId) {
            return;
        }
        changePassword.mutate({userId: activeUserId, payload: passwords});
    };

    return <PageWrapper title="Super Admin">
        <div className="mb-4 grid gap-3 md:grid-cols-3">
            <Metric Icon={UserRound} label="Users" value={users.length}/>
            <Metric Icon={ShieldAlert} label="Locked accounts" value={lockedUsers}/>
            <Metric Icon={CreditCard} label="Selected payments" value={payments.length}/>
        </div>

        <div className="grid gap-4 xl:grid-cols-[minmax(280px,360px)_1fr]">
            <section className="rounded-md border border-slate-200 bg-white dark:border-white/10 dark:bg-slate-900">
                <div className="border-b border-slate-200 p-4 dark:border-white/10">
                    <label className="relative block">
                        <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400"/>
                        <input
                            value={search}
                            onChange={(event) => setSearch(event.target.value)}
                            placeholder="Search users"
                            className="h-11 w-full rounded-md border border-slate-200 bg-white px-3 pl-10 text-sm outline-none focus:border-teal-500 dark:border-white/10 dark:bg-white/[0.06] dark:text-white"
                        />
                    </label>
                </div>
                {usersQuery.isLoading ? <div className="flex items-center gap-2 p-4 text-sm text-slate-500">
                    <LoaderCircle className="h-4 w-4 animate-spin"/> Loading users...
                </div> : <div className="max-h-[640px] overflow-auto">
                    {visibleUsers.map((user) => <button
                        key={user.userId}
                        type="button"
                        onClick={() => setSelectedUserId(user.userId)}
                        className={`block w-full border-b border-slate-200 p-4 text-left text-sm last:border-0 dark:border-white/10 ${activeUserId === user.userId ? "bg-teal-50 dark:bg-teal-300/10" : "hover:bg-slate-50 dark:hover:bg-white/10"}`}
                    >
                        <span className="block font-semibold text-slate-950 dark:text-white">{user.name}</span>
                        <span className="block break-words text-xs text-slate-500">{user.email}</span>
                        <span className={`mt-2 inline-flex rounded-md px-2 py-1 text-xs font-bold ${user.accountLocked ? "bg-red-50 text-red-700 dark:bg-red-500/10 dark:text-red-200" : "bg-emerald-50 text-emerald-700 dark:bg-emerald-500/10 dark:text-emerald-200"}`}>
                            {user.accountLocked ? "LOCKED" : user.accountStatus || "ACTIVE"}
                        </span>
                    </button>)}
                </div>}
            </section>

            <section className="space-y-4">
                {selectedUser ? <>
                    <div className="rounded-md border border-slate-200 bg-white p-4 dark:border-white/10 dark:bg-slate-900">
                        <div className="flex flex-wrap items-start justify-between gap-3">
                            <div>
                                <h2 className="text-lg font-semibold text-slate-950 dark:text-white">{selectedUser.name}</h2>
                                <p className="break-words text-sm text-slate-500">{selectedUser.email}</p>
                            </div>
                            <button
                                type="button"
                                onClick={() => usersQuery.refetch()}
                                className="inline-flex h-10 items-center gap-2 rounded-md border border-slate-200 px-3 text-sm font-semibold hover:bg-slate-50 dark:border-white/10 dark:hover:bg-white/10"
                            >
                                <RefreshCw className={`h-4 w-4 ${usersQuery.isFetching ? "animate-spin" : ""}`}/> Refresh
                            </button>
                        </div>
                        <dl className="mt-4 grid gap-3 text-sm sm:grid-cols-2 lg:grid-cols-3">
                            <Info label="User ID" value={selectedUser.userId}/>
                            <Info label="Username" value={selectedUser.username || "Not set"}/>
                            <Info label="Provider" value={selectedUser.provider || "LOCAL"}/>
                            <Info label="Email verified" value={selectedUser.emailVerified ? "Yes" : "No"}/>
                            <Info label="Status" value={selectedUser.accountStatus || "ACTIVE"}/>
                            <Info label="Failed attempts" value={`${selectedUser.failedAttempts ?? 0} / 10`}/>
                            <Info label="Locked until" value={selectedUser.lockedUntil || "Not locked"}/>
                            <Info label="Roles" value={(selectedUser.roles ?? []).join(", ") || "USER"}/>
                        </dl>
                        <div className="mt-4 flex flex-wrap gap-2">
                            <button
                                type="button"
                                onClick={() => unlockUser.mutate(activeUserId)}
                                disabled={unlockUser.isPending || !activeUserId}
                                className="inline-flex h-10 items-center gap-2 rounded-md border border-emerald-500/40 bg-emerald-600 px-3 text-sm font-semibold text-white shadow-sm transition duration-200 hover:-translate-y-0.5 hover:bg-emerald-500 hover:shadow-md focus:outline-none focus:ring-2 focus:ring-emerald-500/25 disabled:cursor-wait disabled:opacity-70"
                            >
                                {unlockUser.isPending ? <LoaderCircle className="h-4 w-4 animate-spin"/> : <LockOpen className="h-4 w-4"/>}
                                {selectedUserRestricted ? "Lift account restrictions" : "Refresh restrictions"}
                            </button>
                        </div>
                        <div className="mt-4 border-t border-slate-200 pt-4 dark:border-white/10">
                            <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
                                <h3 className="text-sm font-semibold text-slate-950 dark:text-white">Account roles</h3>
                                <button
                                    type="button"
                                    onClick={() => updateRole.mutate({userId: activeUserId, roles: rolesFor(selectedUser)})}
                                    disabled={updateRole.isPending || !hasRoleDraft}
                                    className="inline-flex h-9 items-center gap-2 rounded-md border border-slate-200 px-3 text-sm font-semibold hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60 dark:border-white/10 dark:hover:bg-white/10"
                                >
                                    {updateRole.isPending ? <LoaderCircle className="h-4 w-4 animate-spin"/> : <Save className="h-4 w-4"/>}
                                    Save roles
                                </button>
                            </div>
                            <div className="flex flex-wrap gap-2">
                                {roleOptions.map((role) => {
                                    const checked = rolesFor(selectedUser).includes(role);
                                    return <label
                                        key={role}
                                        className={`inline-flex h-9 cursor-pointer items-center gap-2 rounded-md border px-2.5 text-xs font-semibold ${checked ? "border-teal-500/40 bg-teal-50 text-teal-800 dark:bg-teal-300/10 dark:text-teal-100" : "border-slate-200 text-slate-500 dark:border-white/10 dark:text-slate-300"}`}
                                    >
                                        <input
                                            type="checkbox"
                                            checked={checked}
                                            onChange={() => toggleRole(selectedUser, role)}
                                            className="h-3.5 w-3.5"
                                        />
                                        {role}
                                    </label>;
                                })}
                            </div>
                        </div>
                    </div>

                    <div className="grid gap-4 lg:grid-cols-[minmax(260px,360px)_1fr]">
                        <form onSubmit={submitPassword} className="rounded-md border border-slate-200 bg-white p-4 dark:border-white/10 dark:bg-slate-900">
                            <h3 className="mb-3 text-sm font-semibold text-slate-950 dark:text-white">Change password</h3>
                            <PasswordInput label="New password" value={passwords.password} onChange={(value) => setPasswords((current) => ({...current, password: value}))}/>
                            <PasswordInput label="Confirm password" value={passwords.confirmPassword} onChange={(value) => setPasswords((current) => ({...current, confirmPassword: value}))}/>
                            <button
                                disabled={changePassword.isPending}
                                className="mt-3 inline-flex h-10 w-full items-center justify-center gap-2 rounded-md bg-slate-950 px-3 text-sm font-semibold text-white disabled:cursor-wait disabled:opacity-70 dark:bg-teal-300 dark:text-slate-950"
                            >
                                {changePassword.isPending ? <LoaderCircle className="h-4 w-4 animate-spin"/> : <KeyRound className="h-4 w-4"/>}
                                Change password
                            </button>
                        </form>

                        <div className="rounded-md border border-slate-200 bg-white dark:border-white/10 dark:bg-slate-900">
                            <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3 dark:border-white/10">
                                <div>
                                    <h3 className="text-sm font-semibold text-slate-950 dark:text-white">Payments</h3>
                                    <p className="text-xs font-medium text-slate-500">Read-only ledger. Amounts are never editable from Super Admin.</p>
                                </div>
                                <span className="text-xs font-semibold text-slate-500">Total {totalPaymentAmount.toFixed(2)}</span>
                            </div>
                            {paymentsQuery.isLoading ? <div className="flex items-center gap-2 p-4 text-sm text-slate-500">
                                <LoaderCircle className="h-4 w-4 animate-spin"/> Loading payments...
                            </div> : payments.length === 0 ? <div className="p-4 text-sm text-slate-500">No payments for this user.</div> :
                                payments.map((payment) => <div key={payment.paymentId} className="grid gap-2 border-b border-slate-200 px-4 py-3 text-sm last:border-0 dark:border-white/10 md:grid-cols-[1fr_auto_auto]">
                                    <div className="min-w-0">
                                        <p className="truncate font-semibold text-slate-950 dark:text-white">{payment.description || payment.paymentId}</p>
                                        <p className="truncate text-xs text-slate-500">{payment.provider} - {payment.paymentId}</p>
                                    </div>
                                    <span className="font-semibold">{payment.amount} {payment.currency}</span>
                                    <span className="rounded-md bg-slate-100 px-2 py-1 text-xs font-bold text-slate-700 dark:bg-white/10 dark:text-slate-200">{payment.status}</span>
                                </div>)}
                        </div>
                    </div>
                </> : <div className="rounded-md border border-slate-200 bg-white p-6 text-sm text-slate-500 dark:border-white/10 dark:bg-slate-900">Select a user to inspect account data.</div>}
            </section>
        </div>
    </PageWrapper>;
};

const Metric = ({Icon, label, value}) => <div className="rounded-md border border-slate-200 bg-white p-4 dark:border-white/10 dark:bg-slate-900">
    <div className="flex items-center gap-3">
        <span className="grid h-10 w-10 place-items-center rounded-md bg-slate-100 text-slate-700 dark:bg-white/10 dark:text-slate-200"><Icon className="h-5 w-5"/></span>
        <div>
            <p className="text-xs font-semibold uppercase text-slate-500">{label}</p>
            <p className="text-xl font-bold text-slate-950 dark:text-white">{value}</p>
        </div>
    </div>
</div>;

const Info = ({label, value}) => <div className="min-w-0 rounded-md bg-slate-50 p-3 dark:bg-white/[0.06]">
    <dt className="text-xs font-semibold uppercase text-slate-500">{label}</dt>
    <dd className="mt-1 break-words font-medium text-slate-800 dark:text-slate-100">{value}</dd>
</div>;

const PasswordInput = ({label, value, onChange}) => <label className="mb-3 block">
    <span className="mb-1 block text-sm font-medium text-slate-600 dark:text-slate-300">{label}</span>
    <input
        type="password"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        minLength={8}
        required
        className="h-10 w-full rounded-md border border-slate-200 bg-white px-3 text-sm outline-none focus:border-teal-500 dark:border-white/10 dark:bg-white/[0.06] dark:text-white"
    />
</label>;

export {
    SuperAdminDashboardPage
};
