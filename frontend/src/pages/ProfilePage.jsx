import {useMutation, useQuery} from "@tanstack/react-query";
import {Globe2, ImagePlus, Save, Trash2} from "lucide-react";
import {useEffect, useMemo, useState} from "react";
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
const featuredTimeZones = [
    "Asia/Kolkata",
    "Asia/Calcutta",
    "UTC",
    "America/New_York",
    "America/Chicago",
    "America/Denver",
    "America/Los_Angeles",
    "Europe/London",
    "Europe/Berlin",
    "Asia/Dubai",
    "Asia/Singapore",
    "Asia/Tokyo",
    "Australia/Sydney"
];
const supportedTimeZones = typeof Intl.supportedValuesOf === "function"
    ? Intl.supportedValuesOf("timeZone")
    : fallbackTimeZones;

const ProfilePage = () => {
    const user = useAuthStore((state) => state.user);
    const updateUser = useAuthStore((state) => state.updateUser);
    const preferredTimezone = usePreferencesStore((state) => state.timezone);
    const setPreferredTimezone = usePreferencesStore((state) => state.setTimezone);
    const [name, setName] = useState(displayName(user?.name, user?.email));
    const [timezone, setTimezone] = useState(preferredTimezone || getBrowserTimeZone() || "UTC");
    const browserTimezone = getBrowserTimeZone();
    const timeZoneOptions = useMemo(() => Array.from(new Set([
        browserTimezone,
        preferredTimezone,
        timezone,
        ...featuredTimeZones,
        ...supportedTimeZones
    ].filter(Boolean))).sort((left, right) => left.localeCompare(right)), [browserTimezone, preferredTimezone, timezone]);
    const [avatarError, setAvatarError] = useState("");
    const profile = useQuery({
        queryKey: ["profile"],
        queryFn: async () => unwrapApiData((await apiClient.get(endpoints.auth.me)).data).user
    });
    const preferences = useQuery({
        queryKey: ["preferences"],
        queryFn: async () => unwrapApiData((await apiClient.get(endpoints.users.preferences)).data)
    });
    const saveProfile = useMutation({
        mutationFn: async () => unwrapApiData((await apiClient.put(endpoints.auth.me, {name})).data),
        onSuccess: (saved) => updateUser(saved)
    });
    const saveAvatar = useMutation({
        mutationFn: async (avatarUrl) => unwrapApiData((await apiClient.put(endpoints.auth.avatar, {avatarUrl})).data),
        onSuccess: (saved) => {
            updateUser(saved);
            profile.refetch();
        }
    });
    const savePreferences = useMutation({
        mutationFn: async (nextTimezone = timezone) => apiClient.put(endpoints.users.preferences, {timezone: nextTimezone}),
        onSuccess: (_, savedTimezone) => {
            setPreferredTimezone(savedTimezone || timezone);
            preferences.refetch();
        }
    });
    const activeProfile = profile.data ?? user;
    const handleAvatarChange = async (file) => {
        setAvatarError("");
        if (!file) {
            return;
        }
        try {
            const avatarUrl = await readAvatarFile(file);
            saveAvatar.mutate(avatarUrl);
        } catch (error) {
            setAvatarError(error instanceof Error ? error.message : "Could not use this image.");
        }
    };
    useEffect(() => {
        setName(displayName(profile.data?.name ?? user?.name, profile.data?.email ?? user?.email));
    }, [profile.data?.email, profile.data?.name, user?.email, user?.name]);
    useEffect(() => {
        if (profile.data) {
            updateUser(profile.data);
        }
    }, [profile.data, updateUser]);
    useEffect(() => {
        if (typeof preferences.data?.timezone === "string") {
            setTimezone(preferences.data.timezone);
            setPreferredTimezone(preferences.data.timezone);
        }
    }, [preferences.data, setPreferredTimezone]);
    const applyTimezone = (nextTimezone) => {
        setTimezone(nextTimezone);
        setPreferredTimezone(nextTimezone);
        savePreferences.mutate(nextTimezone);
    };
    return <PageWrapper title="Profile">
        <div className="grid gap-5 lg:grid-cols-2">
            <section className="rounded-md border border-slate-200 bg-white p-5">
                <h2 className="text-sm font-semibold text-slate-950">Account</h2>
                <div className="mt-4 flex items-center gap-4">
                    <Avatar src={activeProfile?.avatarUrl} name={name}
                            email={activeProfile?.email} size="xl"/>
                    <div className="flex flex-wrap gap-2">
                        <label
                            className="inline-flex h-10 cursor-pointer items-center gap-2 rounded-md border border-slate-200 px-3 text-sm font-medium hover:bg-slate-50">
                            <ImagePlus className="h-4 w-4"/>
                            Upload image
                            <input
                                type="file"
                                accept="image/*"
                                className="sr-only"
                                onChange={(event) => void handleAvatarChange(event.target.files?.[0])}
                            />
                        </label>
                        <button
                            type="button"
                            onClick={() => saveAvatar.mutate(null)}
                            disabled={saveAvatar.isPending || !activeProfile?.avatarUrl}
                            className="inline-flex h-10 items-center gap-2 rounded-md border border-slate-200 px-3 text-sm font-medium hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
                        >
                            <Trash2 className="h-4 w-4"/>
                            Remove
                        </button>
                    </div>
                </div>
                {avatarError && <p className="mt-2 text-sm text-red-600">{avatarError}</p>}
                <label className="mt-4 block">
                    <span className="mb-1 block text-sm text-slate-600">Name</span>
                    <input
                        value={name}
                        onChange={(event) => setName(event.target.value)}
                        className="h-11 w-full rounded-md border border-slate-200 px-3 text-sm outline-none focus:border-slate-400"
                    />
                </label>
                <p className="mt-3 text-sm text-slate-600">{activeProfile?.email}</p>
                <p className="mt-1 text-xs text-slate-500">{activeProfile?.roles.join(", ")}</p>
                <button
                    onClick={() => saveProfile.mutate()}
                    disabled={saveProfile.isPending}
                    className="mt-4 inline-flex h-10 items-center gap-2 rounded-md bg-slate-950 px-4 text-sm font-medium text-white hover:bg-slate-800 disabled:cursor-wait disabled:opacity-70"
                >
                    <Save className="h-4 w-4"/>
                    Save profile
                </button>
            </section>
            <section className="rounded-md border border-slate-200 bg-white p-5">
                <h2 className="text-sm font-semibold text-slate-950">Preferences</h2>
                <label className="mt-4 block">
                    <span className="mb-1 block text-sm text-slate-600">Timezone</span>
                    <div className="relative">
                        <Globe2 className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400"/>
                        <select
                            value={timezone}
                            onChange={(event) => applyTimezone(event.target.value)}
                            className="h-11 w-full rounded-md border border-slate-200 bg-white py-0 pl-9 pr-3 text-sm text-slate-900 outline-none focus:border-slate-400 dark:border-white/10 dark:bg-slate-950 dark:text-white"
                        >
                            {timeZoneOptions.map((option) => <option key={option} value={option}>
                                {option}{option === browserTimezone ? " - browser" : ""}
                            </option>)}
                        </select>
                    </div>
                    <p className="mt-2 text-xs text-slate-500">
                        Browser timezone: {browserTimezone || "Not detected"}
                    </p>
                    {savePreferences.isPending && <p className="mt-2 text-xs text-blue-600">Saving timezone...</p>}
                    {savePreferences.isSuccess && !savePreferences.isPending && <p className="mt-2 text-xs text-emerald-600">Timezone is active across the app.</p>}
                </label>
                <button
                    onClick={() => savePreferences.mutate(timezone)}
                    disabled={savePreferences.isPending}
                    className="mt-4 inline-flex h-10 items-center gap-2 rounded-md border border-slate-200 px-4 text-sm font-medium hover:bg-slate-50 disabled:cursor-wait disabled:opacity-70"
                >
                    <Save className="h-4 w-4"/>
                    Save preferences
                </button>
            </section>
        </div>
    </PageWrapper>;
};
const displayName = (name, email) => {
    if (name && !name.includes("@")) {
        return name;
    }
    return email?.split("@")[0] ?? "";
};
export {
    ProfilePage
};
