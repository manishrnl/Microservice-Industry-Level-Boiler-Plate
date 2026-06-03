import {useMutation, useQuery, useQueryClient} from "@tanstack/react-query";
import {LoaderCircle, RefreshCw, Save, Search} from "lucide-react";
import {useMemo, useState} from "react";
import {apiClient} from "../../api/axiosInstance";
import {endpoints} from "../../api/endpoints";
import {PageWrapper} from "../../components/common/PageWrapper";
import {asArray, unwrapApiData} from "../../utils/responseUtils";

const roleOptions = ["SUPER_ADMIN", "ADMIN", "EDITOR", "CREATOR", "VIEWER", "USER"];

const UserManagementPage = () => {
    const queryClient = useQueryClient();
    const [search, setSearch] = useState("");
    const [roleFilter, setRoleFilter] = useState("");
    const [sortBy, setSortBy] = useState("name");
    const [draftRoles, setDraftRoles] = useState({});
    const users = useQuery({
        queryKey: ["users"],
        queryFn: async () => asArray((await apiClient.get(endpoints.users.list)).data),
        refetchOnMount: "always",
        refetchOnWindowFocus: true
    });
    const updateRole = useMutation({
        mutationFn: async ({userId, roles}) => unwrapApiData((await apiClient.put(endpoints.users.role(userId), {roles})).data),
        onSuccess: (saved) => {
            const nextUsers = (queryClient.getQueryData(["users"]) ?? []).map((user) => user.userId === saved.userId ? saved : user);
            queryClient.setQueryData(["users"], nextUsers);
            setDraftRoles((current) => {
                const next = {...current};
                delete next[saved.userId];
                return next;
            });
        }
    });
    const visibleUsers = useMemo(() => {
        const term = search.trim().toLowerCase();
        return [...(users.data ?? [])]
            .filter((user) => {
                const roles = user.roles ?? [];
                const matchesRole = !roleFilter || roles.includes(roleFilter);
                const haystack = [
                    user.name,
                    user.email,
                    user.userId,
                    roles.join(" ")
                ].filter(Boolean).join(" ").toLowerCase();
                return matchesRole && (!term || haystack.includes(term));
            })
            .sort((left, right) => sortValue(left, sortBy).localeCompare(sortValue(right, sortBy)));
    }, [roleFilter, search, sortBy, users.data]);
    const selectedRoleCount = useMemo(() => Object.keys(draftRoles).length, [draftRoles]);
    const rolesFor = (user) => draftRoles[user.userId] ?? user.roles ?? ["USER"];
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
    return <PageWrapper title="Users">
        <div className="mb-4 grid gap-3 rounded-md border border-slate-200 bg-white/70 p-4 backdrop-blur dark:border-white/10 dark:bg-white/[0.06] md:grid-cols-[1fr_180px_180px_auto]">
            <label className="relative block">
                <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400"/>
                <input
                    value={search}
                    onChange={(event) => setSearch(event.target.value)}
                    placeholder="Search local users by name, email, role, ID"
                    className="h-11 w-full rounded-md border border-slate-200 bg-white/80 px-3 pl-10 text-sm outline-none focus:border-teal-500 dark:border-white/10 dark:bg-white/[0.06] dark:text-white"
                />
            </label>
            <select
                value={roleFilter}
                onChange={(event) => setRoleFilter(event.target.value)}
                className="h-11 rounded-md border border-slate-200 bg-white/80 px-3 text-sm outline-none focus:border-teal-500 dark:border-white/10 dark:bg-white/[0.06] dark:text-white"
            >
                <option value="">All roles</option>
                {roleOptions.map((role) => <option key={role} value={role}>{role}</option>)}
            </select>
            <select
                value={sortBy}
                onChange={(event) => setSortBy(event.target.value)}
                className="h-11 rounded-md border border-slate-200 bg-white/80 px-3 text-sm outline-none focus:border-teal-500 dark:border-white/10 dark:bg-white/[0.06] dark:text-white"
            >
                <option value="name">Sort by name</option>
                <option value="email">Sort by email</option>
                <option value="role">Sort by top role</option>
            </select>
            <button
                type="button"
                onClick={() => users.refetch()}
                disabled={users.isFetching}
                className="inline-flex h-11 items-center justify-center gap-2 rounded-md border border-slate-200 px-3 text-sm font-medium hover:bg-slate-50 disabled:cursor-wait disabled:opacity-60 dark:border-white/10 dark:text-white dark:hover:bg-white/10"
            >
                <RefreshCw className={`h-4 w-4 ${users.isFetching ? "animate-spin" : ""}`}/>
                Refresh
            </button>
        </div>
        <div className="overflow-hidden rounded-md border border-slate-200 bg-white dark:border-white/10 dark:bg-slate-900">
            <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3 dark:border-white/10">
                <p className="text-sm text-slate-600 dark:text-slate-300">{visibleUsers.length} of {(users.data ?? []).length} users, searched locally</p>
                {selectedRoleCount > 0 && <p className="text-xs font-medium text-teal-700 dark:text-teal-200">{selectedRoleCount} unsaved role edits</p>}
            </div>
            {users.isLoading ? <div className="flex items-center gap-2 p-6 text-sm text-slate-500">
                <LoaderCircle className="h-4 w-4 animate-spin"/> Loading users...
            </div> : visibleUsers.length === 0 ? <div className="p-6 text-sm text-slate-500">No users matched your search.</div> :
                visibleUsers.map((user) => <div
                key={user.userId}
                className="grid gap-4 border-b border-slate-200 px-4 py-4 text-sm last:border-0 dark:border-white/10 lg:grid-cols-[1fr_1.3fr_2fr_auto]"
            >
                <div>
                    <p className="font-medium text-slate-950 dark:text-white">{user.name}</p>
                    <p className="text-xs text-slate-500">{user.userId}</p>
                </div>
                <span className="min-w-0 break-words text-slate-600 dark:text-slate-300 lg:truncate">{user.email}</span>
                <div className="flex flex-wrap gap-2">
                    {roleOptions.map((role) => {
                        const checked = rolesFor(user).includes(role);
                        return <label
                            key={role}
                            className={`inline-flex h-8 cursor-pointer items-center gap-2 rounded-md border px-2 text-xs font-semibold ${checked ? "border-teal-500/40 bg-teal-50 text-teal-800 dark:bg-teal-300/10 dark:text-teal-100" : "border-slate-200 text-slate-500 dark:border-white/10 dark:text-slate-300"}`}
                        >
                            <input
                                type="checkbox"
                                checked={checked}
                                onChange={() => toggleRole(user, role)}
                                className="h-3.5 w-3.5"
                            />
                            {role}
                        </label>;
                    })}
                </div>
                <button
                    onClick={() => updateRole.mutate({userId: user.userId, roles: rolesFor(user)})}
                    disabled={updateRole.isPending || users.isFetching || !draftRoles[user.userId]}
                    className="inline-flex h-10 items-center justify-center gap-2 rounded-md border border-slate-200 px-3 font-medium hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60 dark:border-white/10 dark:text-white dark:hover:bg-white/10"
                >
                    {updateRole.isPending ? <LoaderCircle className="h-4 w-4 animate-spin"/> : <Save className="h-4 w-4"/>}
                    Save
                </button>
            </div>)}
        </div>
    </PageWrapper>;
};

const sortValue = (user, sortBy) => {
    if (sortBy === "email") {
        return user.email ?? "";
    }
    if (sortBy === "role") {
        return (user.roles ?? []).join(" ");
    }
    return user.name ?? user.email ?? "";
};
export {
    UserManagementPage
};
