import {ArrowRight, AtSign, ImagePlus, LoaderCircle, Mail, UserRound} from "lucide-react";
import {useState} from "react";
import {useForm} from "react-hook-form";
import {Link, useNavigate} from "react-router-dom";
import {apiClient} from "../../api/axiosInstance";
import {endpoints} from "../../api/endpoints";
import {Avatar} from "../../components/common/Avatar";
import {readAvatarFile} from "../../utils/imageUtils";
import {AuthShell} from "./AuthShell";
import {OAuthButtons} from "./OAuthButtons";
import {PasswordField} from "./PasswordField";

const inputClassName = "h-12 w-full rounded-md border border-slate-200 bg-slate-50/80 px-3 pl-11 text-sm text-slate-950 shadow-inner shadow-slate-950/[0.03] outline-none transition placeholder:text-slate-400 focus:border-teal-500 focus:bg-white focus:ring-4 focus:ring-teal-500/10 dark:border-white/10 dark:bg-white/[0.06] dark:text-white dark:placeholder:text-slate-500 dark:focus:bg-white/[0.09]";
const SignupPage = () => {
    const [activationSent, setActivationSent] = useState(false);
    const [signupError, setSignupError] = useState("");
    const [avatarPreview, setAvatarPreview] = useState(null);
    const [avatarError, setAvatarError] = useState("");
    const {register, handleSubmit, watch, setValue, formState} = useForm();
    const navigate = useNavigate();
    const fullName = watch("fullName");
    const email = watch("email");
    const submit = handleSubmit(async (values) => {
        setActivationSent(false);
        setSignupError("");
        const payload = {
            ...values,
            email: values.email?.trim().toLowerCase(),
            username: values.username?.trim()
        };
        if (!payload.username) {
            delete payload.username;
        }
        try {
            await apiClient.post(endpoints.auth.signup, payload);
            setActivationSent(true);
            window.setTimeout(() => navigate("/login", {
                replace: true,
                state: {
                    email: payload.email,
                    notice: "Activation OTP sent to your email.",
                    verificationRequired: true
                }
            }), 1800);
        } catch (error) {
            const status = error?.response?.status;
            const detail = error?.response?.data?.detail ?? error?.response?.data?.message ?? "";
            const lowerDetail = String(detail).toLowerCase();
            const isDuplicateEmail = status === 409 && lowerDetail.includes("email");
            const isDuplicateUsername = status === 409 && lowerDetail.includes("username");
            if (isDuplicateUsername) {
                setSignupError("Username already registered. Choose another username or leave it blank.");
                setValue("username", "", {shouldDirty: true, shouldValidate: true});
                return;
            }
            if (isDuplicateEmail) {
                setSignupError("Email already registered. Redirecting to login...");
                window.setTimeout(() => navigate("/login", {
                    replace: true,
                    state: {
                        email: payload.email,
                        notice: "Email already registered. Sign in instead."
                    }
                }), 1000);
                return;
            }
            if (status === 409) {
                setSignupError(detail || "Submitted email or username conflicts with an existing account.");
                return;
            }
            setSignupError(detail || "Could not create account. Please try again.");
        }
    });
    const handleAvatarChange = async (file) => {
        setAvatarError("");
        if (!file) {
            return;
        }
        try {
            const avatarUrl = await readAvatarFile(file);
            setAvatarPreview(avatarUrl);
            setValue("avatarUrl", avatarUrl, {shouldDirty: true});
        } catch (error) {
            setAvatarError(error instanceof Error ? error.message : "Could not use this image.");
        }
    };
    return <AuthShell
        title="Create account"
        subtitle="Set up your profile or continue with a connected provider."
    >
        <form onSubmit={submit} className="space-y-5">
            <input type="hidden" {...register("avatarUrl")} />
            <div className="flex items-center gap-4">
                <Avatar src={avatarPreview} name={fullName} email={email} size="lg"/>
                <label
                    className="inline-flex h-10 cursor-pointer items-center gap-2 rounded-md border border-slate-200 px-3 text-sm font-medium text-slate-700 transition hover:bg-slate-50 dark:border-white/10 dark:text-slate-200 dark:hover:bg-white/10">
                    <ImagePlus className="h-4 w-4"/>
                    Profile image
                    <input
                        type="file"
                        accept="image/*"
                        className="sr-only"
                        onChange={(event) => void handleAvatarChange(event.target.files?.[0])}
                    />
                </label>
            </div>
            {avatarError &&
                <p className="-mt-3 text-sm text-red-600 dark:text-red-300">{avatarError}</p>}
            <label className="mb-4 block">
                    <span
                        className="mb-1.5 block text-sm font-semibold text-slate-700 dark:text-slate-200"
                    >Full name</span>
                <span className="relative block">
            <UserRound
                className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400"
            />
            <input
                className={inputClassName}
                placeholder="Your name"
                {...register("fullName", {required: true})}
            />
          </span>
            </label>
            <label className="mb-4 block">
                    <span
                        className="mb-1.5 block text-sm font-semibold text-slate-700 dark:text-slate-200"
                    >Username</span>
                <span className="relative block">
            <AtSign
                className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400"
            />
            <input
                className={inputClassName}
                placeholder="Choose a username"
                autoComplete="off"
                {...register("username")}
            />
          </span>
            </label>
            <label className="mb-4 block">
                    <span
                        className="mb-1.5 block text-sm font-semibold text-slate-700 dark:text-slate-200"
                    >Email</span>
                <span className="relative block">
            <Mail
                className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400"
            />
            <input
                className={inputClassName}
                placeholder="you@company.com"
                type="email"
                {...register("email", {required: true})}
            />
          </span>
            </label>
            <PasswordField
                label="Password"
                placeholder="Create password"
                registration={register("password", {
                    required: true,
                    minLength: 8
                })}
            />
            <PasswordField
                label="Confirm password"
                placeholder="Repeat password"
                registration={register("confirmPassword", {
                    required: true,
                    validate: (value) => value === watch("password") || "Passwords do not match"
                })}
            />
            {formState.errors.confirmPassword &&
                <p className="-mt-1 mb-3 text-sm text-red-600 dark:text-red-300">{formState.errors.confirmPassword.message}</p>}
            <button
                disabled={formState.isSubmitting}
                className="group mt-1 flex h-12 w-full items-center justify-center gap-2 rounded-md bg-slate-950 px-4 text-sm font-semibold text-white shadow-lg shadow-slate-950/15 transition hover:-translate-y-0.5 hover:bg-slate-800 hover:shadow-xl focus:outline-none focus:ring-4 focus:ring-teal-500/20 disabled:cursor-wait disabled:opacity-80 dark:bg-teal-400 dark:text-slate-950 dark:hover:bg-teal-300"
            >
                {formState.isSubmitting ? "Creating account" : "Create account"}
                {formState.isSubmitting ? <LoaderCircle className="h-4 w-4 animate-spin"/> :
                    <ArrowRight
                        className="h-4 w-4 transition group-hover:translate-x-0.5"
                    />}
            </button>
            {activationSent &&
                <p className="mt-3 text-sm font-medium text-emerald-700 dark:text-emerald-300">Activation
                    OTP sent to your email. Redirecting to login...</p>}
            {signupError &&
                <p className="mt-3 text-sm font-medium text-red-600 dark:text-red-300">{signupError}</p>}
            <div
                className="my-5 flex items-center gap-3 text-xs font-medium text-slate-500 dark:text-slate-400"
            >
                <span className="h-px flex-1 bg-slate-200 dark:bg-white/10"/>
                <span>or continue with</span>
                <span className="h-px flex-1 bg-slate-200 dark:bg-white/10"/>
            </div>
            <OAuthButtons mode="signup"/>
            <Link
                to="/login"
                className="mt-5 block text-center text-sm font-semibold text-teal-700 transition hover:text-teal-800 dark:text-teal-300"
            >Sign
                in</Link>
        </form>
    </AuthShell>;
};
export {
    SignupPage
};
