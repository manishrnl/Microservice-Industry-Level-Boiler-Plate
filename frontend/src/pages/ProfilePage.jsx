import {useMutation, useQuery} from "@tanstack/react-query";
import {ImagePlus, Save, Trash2} from "lucide-react";
import {useEffect, useState} from "react";
import {apiClient} from "../api/axiosInstance";
import {endpoints} from "../api/endpoints";
import {Avatar} from "../components/common/Avatar";
import {PageWrapper} from "../components/common/PageWrapper";
import {useAuthStore} from "../store/authStore";
import {readAvatarFile} from "../utils/imageUtils";
import {unwrapApiData} from "../utils/responseUtils";

const ProfilePage = () => {
    const user = useAuthStore((state) => state.user);
    const updateUser = useAuthStore((state) => state.updateUser);
    const [name, setName] = useState(displayName(user?.name, user?.email));
    const [timezone, setTimezone] = useState("UTC");
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
        mutationFn: async () => apiClient.put(endpoints.users.preferences, {timezone}),
        onSuccess: () => preferences.refetch()
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
        }
    }, [preferences.data]);
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
                    <input
                        value={timezone}
                        onChange={(event) => setTimezone(event.target.value)}
                        className="h-11 w-full rounded-md border border-slate-200 px-3 text-sm outline-none focus:border-slate-400"
                    />
                </label>
                <button
                    onClick={() => savePreferences.mutate()}
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
