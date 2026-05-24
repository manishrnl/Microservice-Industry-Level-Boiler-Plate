import {
    AtSign,
    BadgeCheck,
    CalendarDays,
    ChevronDown,
    CreditCard,
    Globe2,
    IdCard,
    ImagePlus,
    KeyRound,
    LockKeyhole,
    Mail,
    MapPin,
    Phone,
    Save,
    ShieldAlert,
    ShieldCheck,
    Trash2,
    UserRound
} from "lucide-react";
import {useMutation, useQuery} from "@tanstack/react-query";
import {useEffect, useMemo, useState} from "react";
import toast from "react-hot-toast";
import {useNavigate} from "react-router-dom";
import {apiClient} from "../api/axiosInstance";
import {endpoints} from "../api/endpoints";
import {Avatar} from "../components/common/Avatar";
import {PageWrapper} from "../components/common/PageWrapper";
import {useAuthStore} from "../store/authStore";
import {usePreferencesStore} from "../store/preferencesStore";
import {getBrowserTimeZone} from "../utils/clientContext";
import {readAvatarFile} from "../utils/imageUtils";
import {unwrapApiData} from "../utils/responseUtils";

const fallbackTimeZones = ["UTC", "Asia/Kolkata", "Asia/Calcutta", "America/New_York", "America/Chicago", "America/Denver", "America/Los_Angeles", "Europe/London", "Europe/Berlin", "Europe/Paris", "Asia/Dubai", "Asia/Singapore", "Asia/Tokyo", "Australia/Sydney"];
const featuredTimeZones = ["Asia/Kolkata", "Asia/Calcutta", "UTC", "America/New_York", "America/Chicago", "America/Denver", "America/Los_Angeles", "Europe/London", "Europe/Berlin", "Asia/Dubai", "Asia/Singapore", "Asia/Tokyo", "Australia/Sydney"];
const supportedTimeZones = typeof Intl.supportedValuesOf === "function" ? Intl.supportedValuesOf("timeZone") : fallbackTimeZones;
const emptySettings = {
    name: "",
    username: "",
    aadhaarNumber: "",
    panNumber: "",
    phoneNumber: "",
    dateOfBirth: "",
    addressLine: "",
    city: "",
    state: "",
    country: "India",
    postalCode: ""
};

const ProfilePage = () => {
    const navigate = useNavigate();
    const user = useAuthStore((state) => state.user);
    const updateUser = useAuthStore((state) => state.updateUser);
    const clearAuth = useAuthStore((state) => state.clearAuth);
    const preferredTimezone = usePreferencesStore((state) => state.timezone);
    const setPreferredTimezone = usePreferencesStore((state) => state.setTimezone);
    const [settings, setSettings] = useState({...emptySettings, name: displayName(user?.name, user?.email)});
    const [passwords, setPasswords] = useState({currentPassword: "", newPassword: "", confirmPassword: ""});
    const [suspendForm, setSuspendForm] = useState({confirmation: "", days: 7});
    const [deleteConfirmation, setDeleteConfirmation] = useState("");
    const [avatarError, setAvatarError] = useState("");
    const [timezone, setTimezone] = useState(preferredTimezone || getBrowserTimeZone() || "UTC");
    const [openPanel, setOpenPanel] = useState(null);
    const browserTimezone = getBrowserTimeZone();

    const account = useQuery({
        queryKey: ["account-settings"],
        queryFn: async () => unwrapApiData((await apiClient.get(endpoints.users.settings)).data)
    });
    const preferences = useQuery({
        queryKey: ["preferences"],
        queryFn: async () => unwrapApiData((await apiClient.get(endpoints.users.preferences)).data)
    });
    const saveSettings = useMutation({
        mutationFn: async () => unwrapApiData((await apiClient.put(endpoints.users.settings, normalizeSettings(settings))).data),
        onSuccess: (saved) => {
            setSettings(settingsFromAccount(saved));
            updateUser({name: saved.name, username: saved.username, avatarUrl: saved.avatarUrl});
            toast.success("Account settings saved");
        }
    });
    const saveAvatar = useMutation({
        mutationFn: async (avatarUrl) => unwrapApiData((await apiClient.put(endpoints.users.avatar, {avatarUrl})).data),
        onSuccess: (saved) => {
            updateUser(saved);
            account.refetch();
        }
    });
    const savePreferences = useMutation({
        mutationFn: async (nextTimezone = timezone) => apiClient.put(endpoints.users.preferences, {timezone: nextTimezone}),
        onSuccess: (_, savedTimezone) => {
            setPreferredTimezone(savedTimezone || timezone);
            preferences.refetch();
            toast.success("Timezone saved");
        }
    });
    const changePassword = useMutation({
        mutationFn: async () => unwrapApiData((await apiClient.put(endpoints.auth.password, passwords)).data),
        onSuccess: () => {
            setPasswords({currentPassword: "", newPassword: "", confirmPassword: ""});
            toast.success("Password changed");
        }
    });
    const suspendAccount = useMutation({
        mutationFn: async () => unwrapApiData((await apiClient.post(endpoints.auth.suspend, suspendForm)).data),
        onSuccess: () => {
            clearAuth("Account suspended. Sign in again after the suspension expires.");
            navigate("/login", {replace: true});
        }
    });
    const deleteAccount = useMutation({
        mutationFn: async () => unwrapApiData((await apiClient.delete(endpoints.auth.deleteAccount, {data: {confirmation: deleteConfirmation}})).data),
        onSuccess: () => {
            clearAuth("Account deleted.");
            navigate("/login", {replace: true});
        }
    });

    useEffect(() => {
        if (account.data) {
            setSettings(settingsFromAccount(account.data));
            updateUser({name: account.data.name, username: account.data.username, avatarUrl: account.data.avatarUrl});
        }
    }, [account.data, updateUser]);
    useEffect(() => {
        if (typeof preferences.data?.timezone === "string") {
            setTimezone(preferences.data.timezone);
            setPreferredTimezone(preferences.data.timezone);
        }
    }, [preferences.data, setPreferredTimezone]);

    const timeZoneOptions = useMemo(() => Array.from(new Set([
        browserTimezone,
        preferredTimezone,
        timezone,
        ...featuredTimeZones,
        ...supportedTimeZones
    ].filter(Boolean))).sort((left, right) => left.localeCompare(right)), [browserTimezone, preferredTimezone, timezone]);
    const activeProfile = account.data ?? user ?? {};
    const roles = Array.isArray(activeProfile.roles) ? activeProfile.roles : user?.roles ?? [];
    const status = String(activeProfile.accountStatus ?? "ACTIVE");

    const updateSetting = (field) => (event) => setSettings((value) => ({...value, [field]: event.target.value}));
    const updatePassword = (field) => (event) => setPasswords((value) => ({...value, [field]: event.target.value}));
    const applyTimezone = (nextTimezone) => {
        setTimezone(nextTimezone);
        setPreferredTimezone(nextTimezone);
        savePreferences.mutate(nextTimezone);
    };
    const togglePanel = (panel) => setOpenPanel((current) => current === panel ? null : panel);
    const handleAvatarChange = async (file) => {
        setAvatarError("");
        if (!file) {
            return;
        }
        try {
            saveAvatar.mutate(await readAvatarFile(file));
        } catch (error) {
            setAvatarError(error instanceof Error ? error.message : "Could not use this image.");
        }
    };

    return <PageWrapper title="Profile">
        <div className="grid gap-5 xl:grid-cols-[360px_1fr]">
            <aside className="space-y-5">
                <section className="rounded-md border border-slate-200 bg-white p-5 dark:border-white/10 dark:bg-slate-900">
                    <div className="flex items-center gap-4">
                        <Avatar src={activeProfile?.avatarUrl} name={settings.name} email={activeProfile?.email} size="xl"/>
                        <div className="min-w-0">
                            <p className="truncate text-base font-semibold text-slate-950 dark:text-white">{settings.username || settings.name || "Account holder"}</p>
                            <p className="mt-1 flex items-center gap-1.5 truncate text-sm text-slate-600 dark:text-slate-300">
                                <Mail className="h-3.5 w-3.5 shrink-0"/>
                                {activeProfile?.email}
                            </p>
                        </div>
                    </div>
                    <div className="mt-4 grid grid-cols-2 gap-2 text-xs">
                        <StatusPill label={status} Icon={status === "ACTIVE" ? ShieldCheck : ShieldAlert}/>
                        <StatusPill label={activeProfile.emailVerified ? "Verified" : "Email pending"} Icon={BadgeCheck}/>
                        <StatusPill label={activeProfile.provider || "LOCAL"} Icon={LockKeyhole}/>
                        <StatusPill label={roles.join(", ") || "USER"} Icon={UserRound}/>
                    </div>
                    <div className="mt-4 flex flex-wrap gap-2">
                        <label className="inline-flex h-10 cursor-pointer items-center gap-2 rounded-md border border-slate-200 px-3 text-sm font-medium hover:bg-slate-50 dark:border-white/10 dark:text-slate-100 dark:hover:bg-white/10">
                            <ImagePlus className="h-4 w-4"/>
                            Upload
                            <input type="file" accept="image/*" className="sr-only" onChange={(event) => void handleAvatarChange(event.target.files?.[0])}/>
                        </label>
                        <button
                            type="button"
                            onClick={() => saveAvatar.mutate(null)}
                            disabled={saveAvatar.isPending || !activeProfile?.avatarUrl}
                            className="inline-flex h-10 items-center gap-2 rounded-md border border-slate-200 px-3 text-sm font-medium hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50 dark:border-white/10 dark:text-slate-100 dark:hover:bg-white/10"
                        >
                            <Trash2 className="h-4 w-4"/>
                            Remove
                        </button>
                    </div>
                    {avatarError && <p className="mt-2 text-sm text-red-600">{avatarError}</p>}
                </section>
                <section className="rounded-md border border-slate-200 bg-white p-5 dark:border-white/10 dark:bg-slate-900">
                    <h2 className="text-sm font-semibold text-slate-950 dark:text-white">Preferences</h2>
                    <label className="mt-4 block">
                        <span className="mb-1 block text-sm text-slate-600 dark:text-slate-300">Timezone</span>
                        <div className="relative">
                            <Globe2 className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400"/>
                            <select
                                value={timezone}
                                onChange={(event) => applyTimezone(event.target.value)}
                                className="h-11 w-full rounded-md border border-slate-200 bg-white py-0 pl-9 pr-3 text-sm text-slate-900 outline-none focus:border-slate-400 dark:border-white/10 dark:bg-slate-950 dark:text-white"
                            >
                                {timeZoneOptions.map((option) => <option key={option} value={option}>{option}{option === browserTimezone ? " - browser" : ""}</option>)}
                            </select>
                        </div>
                    </label>
                    {savePreferences.isPending && <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">Saving timezone...</p>}
                </section>
            </aside>

            <main className="space-y-5">
                <SettingsPanel
                    open={openPanel === "identity"}
                    onToggle={() => togglePanel("identity")}
                    Icon={IdCard}
                    title="Identity and KYC"
                    summary={`${settings.phoneNumber || "Phone not set"} · ${settings.panNumber || "PAN not set"} · ${settings.aadhaarNumber ? "Aadhaar saved" : "Aadhaar not set"}`}
                    action={openPanel === "identity" ? (
                        <button
                            type="button"
                            onClick={(event) => {
                                event.stopPropagation();
                                saveSettings.mutate();
                            }}
                            disabled={saveSettings.isPending}
                            className="inline-flex h-10 items-center gap-2 rounded-md bg-slate-950 px-4 text-sm font-medium text-white hover:bg-slate-800 disabled:cursor-wait disabled:opacity-70"
                        >
                            <Save className="h-4 w-4"/>
                            Save account
                        </button>
                    ) : null}
                >
                    <div className="mt-5 grid gap-4 md:grid-cols-2">
                        <Field label="Full name" Icon={UserRound} value={settings.name} onChange={updateSetting("name")}/>
                        <Field label="Username" Icon={AtSign} value={settings.username} onChange={updateSetting("username")} placeholder="your_username"/>
                        <Field label="Phone number" Icon={Phone} value={settings.phoneNumber} onChange={updateSetting("phoneNumber")} placeholder="+91 98765 43210"/>
                        <Field label="Aadhaar number" Icon={IdCard} value={settings.aadhaarNumber} onChange={updateSetting("aadhaarNumber")} inputMode="numeric" maxLength={12} placeholder="12 digits"/>
                        <Field label="PAN number" Icon={CreditCard} value={settings.panNumber} onChange={(event) => setSettings((value) => ({...value, panNumber: event.target.value.toUpperCase()}))} maxLength={10} placeholder="ABCDE1234F"/>
                        <Field label="Date of birth" Icon={CalendarDays} type="date" value={settings.dateOfBirth} onChange={updateSetting("dateOfBirth")}/>
                        <Field label="Postal code" Icon={MapPin} value={settings.postalCode} onChange={updateSetting("postalCode")} inputMode="numeric"/>
                        <Field label="Address line" Icon={MapPin} value={settings.addressLine} onChange={updateSetting("addressLine")} className="md:col-span-2"/>
                        <Field label="City" Icon={MapPin} value={settings.city} onChange={updateSetting("city")}/>
                        <Field label="State" Icon={MapPin} value={settings.state} onChange={updateSetting("state")}/>
                        <Field label="Country" Icon={Globe2} value={settings.country} onChange={updateSetting("country")}/>
                        <ReadOnly label="Account ID" value={activeProfile.userId || activeProfile.id || "Not available"}/>
                    </div>
                </SettingsPanel>

                <SettingsPanel
                    open={openPanel === "password"}
                    onToggle={() => togglePanel("password")}
                    Icon={KeyRound}
                    title="Change password"
                    summary="Update your local login password."
                >
                    <div className="mt-4 space-y-3">
                        <Field label="Current password" Icon={KeyRound} type="password" value={passwords.currentPassword} onChange={updatePassword("currentPassword")}/>
                        <Field label="New password" Icon={LockKeyhole} type="password" value={passwords.newPassword} onChange={updatePassword("newPassword")}/>
                        <Field label="Confirm new password" Icon={LockKeyhole} type="password" value={passwords.confirmPassword} onChange={updatePassword("confirmPassword")}/>
                    </div>
                    <button
                        onClick={() => changePassword.mutate()}
                        disabled={changePassword.isPending}
                        className="mt-4 inline-flex h-10 items-center gap-2 rounded-md bg-slate-950 px-4 text-sm font-medium text-white hover:bg-slate-800 disabled:cursor-wait disabled:opacity-70"
                    >
                        <KeyRound className="h-4 w-4"/>
                        Update password
                    </button>
                    {changePassword.isError && <p className="mt-2 text-sm text-red-600">Password could not be changed. Check the current password and confirmation.</p>}
                </SettingsPanel>

                <SettingsPanel
                    open={openPanel === "danger"}
                    onToggle={() => togglePanel("danger")}
                    Icon={ShieldAlert}
                    title="Suspend or delete account"
                    summary="Separate controls for temporary suspension and permanent deletion."
                    danger
                >
                    <div className="mt-4 grid gap-5 lg:grid-cols-2">
                        <div className="rounded-md border border-amber-200 bg-amber-50/50 p-4 dark:border-amber-300/20 dark:bg-amber-300/5">
                            <h3 className="text-sm font-semibold text-amber-900 dark:text-amber-100">Suspend account</h3>
                            <p className="mt-1 text-sm text-amber-800/80 dark:text-amber-100/70">Temporarily locks login access for the selected number of days.</p>
                            <div className="mt-4 grid gap-3 sm:grid-cols-[1fr_110px]">
                                <Field label="Type SUSPEND" Icon={ShieldAlert} value={suspendForm.confirmation} onChange={(event) => setSuspendForm((value) => ({...value, confirmation: event.target.value.toUpperCase()}))}/>
                                <Field label="Days" Icon={CalendarDays} type="number" min="1" max="90" value={suspendForm.days} onChange={(event) => setSuspendForm((value) => ({...value, days: Number(event.target.value)}))}/>
                            </div>
                            <button
                                onClick={() => suspendAccount.mutate()}
                                disabled={suspendAccount.isPending || suspendForm.confirmation !== "SUSPEND"}
                                className="mt-3 inline-flex h-10 items-center gap-2 rounded-md border border-amber-300 bg-white px-4 text-sm font-medium text-amber-800 hover:bg-amber-100 disabled:cursor-not-allowed disabled:opacity-50 dark:bg-transparent dark:text-amber-100 dark:hover:bg-amber-300/10"
                            >
                                <ShieldAlert className="h-4 w-4"/>
                                Suspend account
                            </button>
                        </div>

                        <div className="rounded-md border border-red-200 bg-red-50/50 p-4 dark:border-red-300/20 dark:bg-red-300/5">
                            <h3 className="text-sm font-semibold text-red-800 dark:text-red-100">Delete account</h3>
                            <p className="mt-1 text-sm text-red-700/80 dark:text-red-100/70">Permanently locks the account and signs you out immediately.</p>
                            <div className="mt-4">
                            <Field label="Type DELETE" Icon={Trash2} value={deleteConfirmation} onChange={(event) => setDeleteConfirmation(event.target.value.toUpperCase())}/>
                            </div>
                            <button
                                onClick={() => deleteAccount.mutate()}
                                disabled={deleteAccount.isPending || deleteConfirmation !== "DELETE"}
                                className="mt-3 inline-flex h-10 items-center gap-2 rounded-md bg-red-600 px-4 text-sm font-medium text-white hover:bg-red-700 disabled:cursor-not-allowed disabled:opacity-50"
                            >
                                <Trash2 className="h-4 w-4"/>
                                Delete account
                            </button>
                        </div>
                    </div>
                </SettingsPanel>
            </main>
        </div>
    </PageWrapper>;
};

const SettingsPanel = ({open, onToggle, Icon, title, summary, action, danger = false, children}) => <section className={`rounded-md border bg-white dark:bg-slate-900 ${danger ? "border-red-200 dark:border-red-300/20" : "border-slate-200 dark:border-white/10"}`}>
    <button
        type="button"
        onClick={onToggle}
        className="flex w-full items-center justify-between gap-4 p-5 text-left"
    >
        <span className="flex min-w-0 items-center gap-3">
            <span className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-md ${danger ? "bg-red-50 text-red-700 dark:bg-red-300/10 dark:text-red-200" : "bg-slate-100 text-slate-700 dark:bg-white/10 dark:text-slate-200"}`}>
                <Icon className="h-5 w-5"/>
            </span>
            <span className="min-w-0">
                <span className={`block text-sm font-semibold ${danger ? "text-red-700 dark:text-red-200" : "text-slate-950 dark:text-white"}`}>{title}</span>
                <span className="mt-1 block truncate text-sm text-slate-500 dark:text-slate-400">{summary}</span>
            </span>
        </span>
        <span className="flex shrink-0 items-center gap-3">
            {action}
            <ChevronDown className={`h-5 w-5 text-slate-400 transition-transform ${open ? "rotate-180" : ""}`}/>
        </span>
    </button>
    {open && <div className="border-t border-slate-200 px-5 pb-5 dark:border-white/10">{children}</div>}
</section>;

const Field = ({label, Icon, className = "", ...props}) => <label className={`block ${className}`}>
    <span className="mb-1 block text-sm text-slate-600 dark:text-slate-300">{label}</span>
    <span className="relative block">
        <Icon className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400"/>
        <input
            {...props}
            className="h-11 w-full rounded-md border border-slate-200 bg-white pl-9 pr-3 text-sm text-slate-950 outline-none focus:border-slate-400 dark:border-white/10 dark:bg-slate-950 dark:text-white"
        />
    </span>
</label>;
const ReadOnly = ({label, value}) => <div>
    <p className="mb-1 text-sm text-slate-600 dark:text-slate-300">{label}</p>
    <p className="min-h-11 rounded-md border border-slate-200 bg-slate-50 px-3 py-3 text-sm text-slate-700 dark:border-white/10 dark:bg-slate-950 dark:text-slate-300">{value}</p>
</div>;
const StatusPill = ({label, Icon}) => <span className="inline-flex min-h-9 items-center gap-2 rounded-md bg-slate-100 px-2.5 py-1 font-medium text-slate-700 dark:bg-white/10 dark:text-slate-200">
    <Icon className="h-3.5 w-3.5 shrink-0"/>
    <span className="truncate">{label}</span>
</span>;
const settingsFromAccount = (account = {}) => ({
    ...emptySettings,
    name: account.name ?? "",
    username: account.username ?? "",
    aadhaarNumber: account.aadhaarNumber ?? "",
    panNumber: account.panNumber ?? "",
    phoneNumber: account.phoneNumber ?? "",
    dateOfBirth: account.dateOfBirth ?? "",
    addressLine: account.addressLine ?? "",
    city: account.city ?? "",
    state: account.state ?? "",
    country: account.country ?? "India",
    postalCode: account.postalCode ?? ""
});
const normalizeSettings = (settings) => ({
    ...settings,
    name: settings.name.trim(),
    username: settings.username.trim().toLowerCase(),
    aadhaarNumber: settings.aadhaarNumber.replace(/\D/g, ""),
    panNumber: settings.panNumber.trim().toUpperCase(),
    phoneNumber: settings.phoneNumber.trim(),
    postalCode: settings.postalCode.trim(),
    dateOfBirth: settings.dateOfBirth || null
});
const displayName = (name, email) => {
    if (name && !name.includes("@")) {
        return name;
    }
    return email?.split("@")[0] ?? "";
};
export {
    ProfilePage
};
