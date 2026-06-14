import {useMutation, useQuery, useQueryClient} from "@tanstack/react-query";
import {
    ChevronLeft,
    ChevronRight,
    CreditCard,
    Eye,
    EyeOff,
    KeyRound,
    LoaderCircle,
    LockOpen,
    RefreshCw,
    Save,
    Search,
    ShieldAlert,
    UserRound
} from "lucide-react";
import {useEffect, useState} from "react";
import toast from "react-hot-toast";
import {apiClient} from "../../api/axiosInstance";
import {endpoints} from "../../api/endpoints";
import {PageWrapper} from "../../components/common/PageWrapper";
import {asArray, unwrapApiData} from "../../utils/responseUtils";

const roleOptions = ["SUPER_ADMIN", "ADMIN", "EDITOR", "CREATOR", "VIEWER", "USER"];
const pageSize = 10;

const pagePayload = (payload) => {
    const data = unwrapApiData(payload);
    if (data && typeof data === "object" && Array.isArray(data.content)) {
        return data;
    }
    return {content: asArray(payload), page: 0, size: pageSize, totalElements: asArray(payload).length, totalPages: 1, last: true};
};

const userLabel = (user) => user?.username || user?.name || user?.email || "user";
const userStatus = (user) => user?.accountLocked ? "LOCKED" : user?.accountStatus || "ACTIVE";

const SuperAdminDashboardPage = () => {
    const queryClient = useQueryClient();
    const [search, setSearch] = useState("");
    const [page, setPage] = useState(0);
    const [selectedUserId, setSelectedUserId] = useState("");
    const [passwords, setPasswords] = useState({password: "", confirmPassword: ""});
    const [showPasswords, setShowPasswords] = useState({password: false, confirmPassword: false});
    const [draftRoles, setDraftRoles] = useState({});
    const usersQuery = useQuery({
        queryKey: ["super-admin-users", page, search],
        queryFn: async () => pagePayload((await apiClient.get(endpoints.users.list, {
            params: {page, size: pageSize, q: search.trim() || undefined}
        })).data),
        refetchOnMount: "always"
    });
    const userPage = usersQuery.data ?? {content: [], page, size: pageSize, totalElements: 0, totalPages: 1, last: true};
    const users = userPage.content ?? [];
    useEffect(() => {
        setPage(0);
    }, [search]);
    useEffect(() => {
        if (users.length === 0) {
            setSelectedUserId("");
            return;
        }
        setSelectedUserId((current) => users.some((user) => user.userId === current) ? current : users[0].userId);
    }, [users]);
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
    const lockedUsers = users.filter((user) => user.accountLocked).length;
    const payments = paymentsQuery.data ?? [];
    const totalPaymentAmount = payments.reduce((sum, payment) => sum + Number(payment.amount ?? 0), 0);
    const selectedUserRestricted = Boolean(selectedUser?.accountLocked || selectedUser?.lockedUntil || Number(selectedUser?.failedAttempts ?? 0) > 0 || selectedUser?.accountStatus === "SUSPENDED");
    const rolesFor = (user) => draftRoles[user.userId] ?? user.roles ?? ["USER"];
    const hasRoleDraft = Boolean(activeUserId && draftRoles[activeUserId]);
    const pageStart = userPage.totalElements === 0 ? 0 : page * pageSize + 1;
    const pageEnd = Math.min((page + 1) * pageSize, userPage.totalElements ?? users.length);
    const canGoBack = page > 0;
    const canGoNext = !userPage.last && page + 1 < Math.max(userPage.totalPages ?? 1, 1);
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
    const togglePassword = (field) => {
        setShowPasswords((current) => ({...current, [field]: !current[field]}));
    };

    return <PageWrapper title="Super Admin">
        <section className="mb-5 rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-950">
            <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                <div>
                    <p className="text-sm font-semibold uppercase text-slate-500">Control center</p>
                    <h2 className="mt-1 text-2xl font-semibold text-slate-950 dark:text-white">Accounts, roles, passwords, and payment ledger</h2>
                </div>
                <button
                    type="button"
                    onClick={() => usersQuery.refetch()}
                    className="inline-flex h-10 items-center justify-center gap-2 rounded-md border border-slate-200 px-3 text-sm font-semibold hover:bg-slate-50 dark:border-white/10 dark:text-white dark:hover:bg-white/10"
                >
                    <RefreshCw className={`h-4 w-4 ${usersQuery.isFetching ? "animate-spin" : ""}`}/> Refresh
                </button>
            </div>
            <div className="mt-5 grid gap-3 md:grid-cols-3">
                <Metric Icon={UserRound} label="Users in result" value={userPage.totalElements ?? users.length}/>
                <Metric Icon={ShieldAlert} label="Locked on page" value={lockedUsers}/>
                <Metric Icon={CreditCard} label="Selected payments" value={payments.length}/>
            </div>
        </section>

        <div className="grid gap-5 xl:grid-cols-[360px_1fr]">
            <section className="overflow-hidden rounded-md border border-slate-200 bg-white shadow-sm dark:border-white/10 dark:bg-slate-950">
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
                    <div className="mt-3 flex items-center justify-between text-xs font-medium text-slate-500">
                        <span>{pageStart}-{pageEnd} of {userPage.totalElements ?? users.length}</span>
                        <span>10 per page</span>
                    </div>
                </div>
                {usersQuery.isLoading ? <div className="flex items-center gap-2 p-4 text-sm text-slate-500">
                    <LoaderCircle className="h-4 w-4 animate-spin"/> Loading users...
                </div> : <div className="max-h-[632px] overflow-auto">
                    {users.map((user) => <button
                        key={user.userId}
                        type="button"
                        onClick={() => setSelectedUserId(user.userId)}
                        className={`flex h-14 w-full items-center justify-between gap-3 border-b border-slate-200 px-4 text-left text-sm last:border-0 dark:border-white/10 ${activeUserId === user.userId ? "bg-slate-100 dark:bg-white/10" : "hover:bg-slate-50 dark:hover:bg-white/10"}`}
                    >
                        <span className="min-w-0 truncate font-semibold text-slate-950 dark:text-white">{userLabel(user)}</span>
                        <span className={`shrink-0 rounded-md px-2 py-1 text-xs font-bold ${user.accountLocked ? "bg-red-50 text-red-700 dark:bg-red-500/10 dark:text-red-200" : "bg-emerald-50 text-emerald-700 dark:bg-emerald-500/10 dark:text-emerald-200"}`}>
                            {userStatus(user)}
                        </span>
                    </button>)}
                    {!usersQuery.isLoading && users.length === 0 && <div className="p-4 text-sm text-slate-500">No users matched this page.</div>}
                </div>}
                <div className="flex items-center justify-between border-t border-slate-200 p-3 dark:border-white/10">
                    <button
                        type="button"
                        onClick={() => setPage((current) => Math.max(0, current - 1))}
                        disabled={!canGoBack || usersQuery.isFetching}
                        className="inline-flex h-9 items-center gap-1 rounded-md border border-slate-200 px-3 text-sm font-semibold disabled:cursor-not-allowed disabled:opacity-50 dark:border-white/10 dark:text-white"
                    >
                        <ChevronLeft className="h-4 w-4"/> Prev
                    </button>
                    <span className="text-xs font-semibold text-slate-500">Page {page + 1} of {Math.max(userPage.totalPages ?? 1, 1)}</span>
                    <button
                        type="button"
                        onClick={() => setPage((current) => current + 1)}
                        disabled={!canGoNext || usersQuery.isFetching}
                        className="inline-flex h-9 items-center gap-1 rounded-md border border-slate-200 px-3 text-sm font-semibold disabled:cursor-not-allowed disabled:opacity-50 dark:border-white/10 dark:text-white"
                    >
                        Next <ChevronRight className="h-4 w-4"/>
                    </button>
                </div>
            </section>

            <section className="space-y-5">
                {selectedUser ? <>
                    <div className="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-950">
                        <div className="flex flex-wrap items-start justify-between gap-4">
                            <div className="min-w-0">
                                <div className="flex flex-wrap items-center gap-2">
                                    <h2 className="text-xl font-semibold text-slate-950 dark:text-white">{selectedUser.name || userLabel(selectedUser)}</h2>
                                    <span className={`rounded-md px-2 py-1 text-xs font-bold ${selectedUser.accountLocked ? "bg-red-50 text-red-700 dark:bg-red-500/10 dark:text-red-200" : "bg-emerald-50 text-emerald-700 dark:bg-emerald-500/10 dark:text-emerald-200"}`}>{userStatus(selectedUser)}</span>
                                </div>
                                <p className="mt-1 break-words text-sm text-slate-500">{selectedUser.email}</p>
                            </div>
                            <button
                                type="button"
                                onClick={() => unlockUser.mutate(activeUserId)}
                                disabled={unlockUser.isPending || !activeUserId}
                                className="inline-flex h-10 items-center gap-2 rounded-md bg-slate-950 px-3 text-sm font-semibold text-white shadow-sm transition hover:bg-slate-800 disabled:cursor-wait disabled:opacity-70 dark:bg-teal-300 dark:text-slate-950"
                            >
                                {unlockUser.isPending ? <LoaderCircle className="h-4 w-4 animate-spin"/> : <LockOpen className="h-4 w-4"/>}
                                {selectedUserRestricted ? "Lift restrictions" : "Refresh restrictions"}
                            </button>
                        </div>
                        <dl className="mt-5 grid gap-3 text-sm md:grid-cols-2 xl:grid-cols-4">
                            <Info label="Username" value={selectedUser.username || "Not set"}/>
                            <Info label="Provider" value={selectedUser.provider || "LOCAL"}/>
                            <Info label="Email verified" value={selectedUser.emailVerified ? "Yes" : "No"}/>
                            <Info label="Failed attempts" value={`${selectedUser.failedAttempts ?? 0} / 10`}/>
                            <Info label="User ID" value={selectedUser.userId}/>
                            <Info label="Locked until" value={selectedUser.lockedUntil || "Not locked"}/>
                            <Info label="Roles" value={(selectedUser.roles ?? []).join(", ") || "USER"}/>
                            <Info label="Deleted at" value={selectedUser.deletedAt || "Active account"}/>
                        </dl>
                    </div>

                    <div className="grid gap-5 xl:grid-cols-[360px_1fr]">
                        <div className="space-y-5">
                            <form onSubmit={submitPassword} className="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-950">
                                <h3 className="mb-4 text-sm font-semibold text-slate-950 dark:text-white">Change password</h3>
                                <PasswordInput
                                    label="New password"
                                    value={passwords.password}
                                    visible={showPasswords.password}
                                    onToggle={() => togglePassword("password")}
                                    onChange={(value) => setPasswords((current) => ({...current, password: value}))}
                                />
                                <PasswordInput
                                    label="Confirm password"
                                    value={passwords.confirmPassword}
                                    visible={showPasswords.confirmPassword}
                                    onToggle={() => togglePassword("confirmPassword")}
                                    onChange={(value) => setPasswords((current) => ({...current, confirmPassword: value}))}
                                />
                                <button
                                    disabled={changePassword.isPending}
                                    className="mt-2 inline-flex h-10 w-full items-center justify-center gap-2 rounded-md bg-slate-950 px-3 text-sm font-semibold text-white disabled:cursor-wait disabled:opacity-70 dark:bg-teal-300 dark:text-slate-950"
                                >
                                    {changePassword.isPending ? <LoaderCircle className="h-4 w-4 animate-spin"/> : <KeyRound className="h-4 w-4"/>}
                                    Change password
                                </button>
                            </form>

                            <div className="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-950">
                                <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
                                    <h3 className="text-sm font-semibold text-slate-950 dark:text-white">Account roles</h3>
                                    <button
                                        type="button"
                                        onClick={() => updateRole.mutate({userId: activeUserId, roles: rolesFor(selectedUser)})}
                                        disabled={updateRole.isPending || !hasRoleDraft}
                                        className="inline-flex h-9 items-center gap-2 rounded-md border border-slate-200 px-3 text-sm font-semibold hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60 dark:border-white/10 dark:hover:bg-white/10"
                                    >
                                        {updateRole.isPending ? <LoaderCircle className="h-4 w-4 animate-spin"/> : <Save className="h-4 w-4"/>}
                                        Save
                                    </button>
                                </div>
                                <div className="flex flex-wrap gap-2">
                                    {roleOptions.map((role) => {
                                        const checked = rolesFor(selectedUser).includes(role);
                                        return <label
                                            key={role}
                                            className={`inline-flex h-9 cursor-pointer items-center gap-2 rounded-md border px-2.5 text-xs font-semibold ${checked ? "border-teal-500/40 bg-teal-50 text-teal-800 dark:bg-teal-300/10 dark:text-teal-100" : "border-slate-200 text-slate-500 dark:border-white/10 dark:text-slate-300"}`}
                                        >
                                            <input type="checkbox" checked={checked} onChange={() => toggleRole(selectedUser, role)} className="h-3.5 w-3.5"/>
                                            {role}
                                        </label>;
                                    })}
                                </div>
                            </div>
                        </div>

                        <div className="overflow-hidden rounded-md border border-slate-200 bg-white shadow-sm dark:border-white/10 dark:bg-slate-950">
                            <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3 dark:border-white/10">
                                <div>
                                    <h3 className="text-sm font-semibold text-slate-950 dark:text-white">Payments</h3>
                                    <p className="text-xs font-medium text-slate-500">Read-only ledger for the selected account.</p>
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
                </> : <div className="rounded-md border border-slate-200 bg-white p-6 text-sm text-slate-500 shadow-sm dark:border-white/10 dark:bg-slate-950">Select a user to inspect account data.</div>}
            </section>
        </div>
    </PageWrapper>;
};

const Metric = ({Icon, label, value}) => <div className="rounded-md border border-slate-200 bg-slate-50 p-4 dark:border-white/10 dark:bg-white/[0.04]">
    <div className="flex items-center gap-3">
        <span className="grid h-10 w-10 place-items-center rounded-md bg-white text-slate-700 shadow-sm dark:bg-white/10 dark:text-slate-200"><Icon className="h-5 w-5"/></span>
        <div>
            <p className="text-xs font-semibold uppercase text-slate-500">{label}</p>
            <p className="text-xl font-bold text-slate-950 dark:text-white">{value}</p>
        </div>
    </div>
</div>;

const Info = ({label, value}) => <div className="min-w-0 rounded-md border border-slate-200 bg-slate-50 p-3 dark:border-white/10 dark:bg-white/[0.04]">
    <dt className="text-xs font-semibold uppercase text-slate-500">{label}</dt>
    <dd className="mt-1 break-words font-medium text-slate-800 dark:text-slate-100">{value}</dd>
</div>;

const PasswordInput = ({label, value, visible, onToggle, onChange}) => <label className="mb-3 block">
    <span className="mb-1 block text-sm font-medium text-slate-600 dark:text-slate-300">{label}</span>
    <span className="flex h-10 items-center rounded-md border border-slate-200 bg-white focus-within:border-teal-500 dark:border-white/10 dark:bg-white/[0.06]">
        <input
            type={visible ? "text" : "password"}
            value={value}
            onChange={(event) => onChange(event.target.value)}
            minLength={8}
            required
            className="h-full min-w-0 flex-1 bg-transparent px-3 text-sm outline-none dark:text-white"
        />
        <button
            type="button"
            onClick={onToggle}
            className="grid h-10 w-10 place-items-center text-slate-500 hover:text-slate-900 dark:hover:text-white"
            aria-label={visible ? "Hide password" : "Show password"}
        >
            {visible ? <EyeOff className="h-4 w-4"/> : <Eye className="h-4 w-4"/>}
        </button>
    </span>
</label>;

export {
    SuperAdminDashboardPage
};
