import {ArrowRight, LoaderCircle, Mail} from "lucide-react";
import {useEffect, useState} from "react";
import {useForm} from "react-hook-form";
import {Link, useLocation, useNavigate} from "react-router-dom";
import {apiClient} from "../../api/axiosInstance";
import {endpoints} from "../../api/endpoints";
import {Loader} from "../../components/common/Loader";
import {useAuthStore} from "../../store/authStore";
import {getDeviceId} from "../../utils/deviceId";
import {unwrapApiData} from "../../utils/responseUtils";
import {AuthShell} from "./AuthShell";
import {OAuthButtons} from "./OAuthButtons";
import {PasswordField} from "./PasswordField";

const inputClassName = "h-12 w-full rounded-md border border-slate-200 bg-slate-50/80 px-3 pl-11 text-sm text-slate-950 shadow-inner shadow-slate-950/[0.03] outline-none transition placeholder:text-slate-400 focus:border-teal-500 focus:bg-white focus:ring-4 focus:ring-teal-500/10 dark:border-white/10 dark:bg-white/[0.06] dark:text-white dark:placeholder:text-slate-500 dark:focus:bg-white/[0.09]";
const RESEND_WAIT_SECONDS = 10 * 60;
const resendTargetTime = () => Date.now() + RESEND_WAIT_SECONDS * 1000;
const secondsUntil = (timestamp) => Math.max(0, Math.ceil((timestamp - Date.now()) / 1000));
const formatResendTime = (seconds) => {
    const minutes = Math.floor(seconds / 60).toString().padStart(2, "0");
    const remainder = (seconds % 60).toString().padStart(2, "0");
    return `${minutes}:${remainder}`;
};
const LoginPage = () => {
    const [loginError, setLoginError] = useState("");
    const location = useLocation();
    const initialVerificationRequired = Boolean(location.state?.verificationRequired);
    const [loginNotice, setLoginNotice] = useState(location.state?.notice ?? "");
    const [verificationRequired, setVerificationRequired] = useState(initialVerificationRequired);
    const [verificationEmail, setVerificationEmail] = useState(location.state?.email ?? "");
    const [isResending, setIsResending] = useState(false);
    const [resendAvailableAt, setResendAvailableAt] = useState(() => initialVerificationRequired ? resendTargetTime() : 0);
    const [resendRemainingSeconds, setResendRemainingSeconds] = useState(() => secondsUntil(resendAvailableAt));
    const {register, handleSubmit, getValues, formState} = useForm({
        defaultValues: {
            email: location.state?.email ?? "",
            otp: ""
        }
    });
    const setAuth = useAuthStore((state) => state.setAuth);
    const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
    const isLoading = useAuthStore((state) => state.isLoading);
    const navigate = useNavigate();
    useEffect(() => {
        if (isAuthenticated && !isLoading) {
            navigate("/", {replace: true});
        }
    }, [isAuthenticated, isLoading, navigate]);
    useEffect(() => {
        if (!verificationRequired || !resendAvailableAt) {
            setResendRemainingSeconds(0);
            return undefined;
        }
        const updateRemaining = () => setResendRemainingSeconds(secondsUntil(resendAvailableAt));
        updateRemaining();
        const timer = window.setInterval(updateRemaining, 1000);
        return () => window.clearInterval(timer);
    }, [resendAvailableAt, verificationRequired]);
    const startResendWindow = () => {
        const nextAvailableAt = resendTargetTime();
        setResendAvailableAt(nextAvailableAt);
        setResendRemainingSeconds(secondsUntil(nextAvailableAt));
    };
    const submit = handleSubmit(async (values) => {
        setLoginError("");
        setLoginNotice("");
        try {
            if (verificationRequired) {
                await apiClient.post(endpoints.auth.verifyEmail, {
                    email: values.email,
                    otp: values.otp
                });
                setVerificationRequired(false);
                setVerificationEmail("");
                setResendAvailableAt(0);
                setLoginNotice("Email verified. Sign in with your password.");
                return;
            }
            const response = await apiClient.post(endpoints.auth.login, {
                ...values,
                deviceId: getDeviceId()
            });
            const token = unwrapApiData(response.data).accessToken;
            const me = await apiClient.get(endpoints.auth.me, {headers: {Authorization: `Bearer ${token}`}});
            const payload = unwrapApiData(me.data);
            setAuth(payload.user, payload.accessToken ?? token);
            navigate("/");
        } catch (error) {
            const status = error?.response?.status;
            const detail = error?.response?.data?.detail ?? error?.response?.data?.message;
            const needsVerification = status === 403 && String(detail ?? "").toLowerCase().includes("verified");
            if (needsVerification) {
                setVerificationRequired(true);
                setVerificationEmail(values.email);
                startResendWindow();
                setLoginNotice("Enter the OTP sent to your email. You can request a new one after 10 minutes.");
                return;
            }
            setLoginError(status === 401 ? "Invalid email or password." : detail || "Could not sign in. Please try again.");
        }
    });
    if (isAuthenticated) {
        return <Loader variant="fullscreen" message="Opening dashboard"/>;
    }
    return <AuthShell
        title="Welcome back"
        subtitle="Use your account or continue with a connected provider."
    >
        <form onSubmit={submit} className="space-y-5">
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
            {verificationRequired ? <label className="mb-4 block">
                    <span
                        className="mb-1.5 block text-sm font-semibold text-slate-700 dark:text-slate-200"
                    >OTP</span>
                <input
                    className="h-12 w-full rounded-md border border-slate-200 bg-slate-50/80 px-3 text-sm tracking-[0.25em] text-slate-950 shadow-inner shadow-slate-950/[0.03] outline-none transition placeholder:tracking-normal placeholder:text-slate-400 focus:border-teal-500 focus:bg-white focus:ring-4 focus:ring-teal-500/10 dark:border-white/10 dark:bg-white/[0.06] dark:text-white dark:placeholder:text-slate-500 dark:focus:bg-white/[0.09]"
                    placeholder="000000"
                    inputMode="numeric"
                    maxLength={6}
                    {...register("otp", {
                        required: verificationRequired,
                        minLength: 6
                    })}
                />
            </label> : <PasswordField
                label="Password"
                placeholder="Enter password"
                registration={register("password", {required: !verificationRequired})}
            />}
            {loginNotice &&
                <p className="-mt-1 mb-3 text-sm font-medium text-emerald-700 dark:text-emerald-300">{loginNotice}</p>}
            {loginError &&
                <p className="-mt-1 mb-3 text-sm font-medium text-red-600 dark:text-red-300">{loginError}</p>}
            <button
                disabled={formState.isSubmitting}
                className="group mt-1 flex h-12 w-full items-center justify-center gap-2 rounded-md bg-slate-950 px-4 text-sm font-semibold text-white shadow-lg shadow-slate-950/15 transition hover:-translate-y-0.5 hover:bg-slate-800 hover:shadow-xl focus:outline-none focus:ring-4 focus:ring-teal-500/20 disabled:cursor-wait disabled:opacity-80 dark:bg-teal-400 dark:text-slate-950 dark:hover:bg-teal-300"
            >
                {formState.isSubmitting ? verificationRequired ? "Verifying" : "Signing in" : verificationRequired ? "Verify email" : "Sign in"}
                {formState.isSubmitting ? <LoaderCircle className="h-4 w-4 animate-spin"/> :
                    <ArrowRight
                        className="h-4 w-4 transition group-hover:translate-x-0.5"
                    />}
            </button>
            {verificationRequired && <button
                type="button"
                disabled={formState.isSubmitting || isResending || resendRemainingSeconds > 0}
                onClick={async () => {
                    setLoginError("");
                    setLoginNotice("");
                    if (resendRemainingSeconds > 0) {
                        setLoginNotice(`Resend available in ${formatResendTime(resendRemainingSeconds)}.`);
                        return;
                    }
                    const email = verificationEmail || getValues("email");
                    if (!email) {
                        setLoginError("Enter your email first.");
                        return;
                    }
                    try {
                        setIsResending(true);
                        await apiClient.post(endpoints.auth.resendVerification, {email});
                        setVerificationEmail(email);
                        startResendWindow();
                        setLoginNotice("A new OTP was sent to your email.");
                    } catch (error) {
                        const detail = error?.response?.data?.detail ?? error?.response?.data?.message;
                        setLoginError(detail || "Could not resend OTP. Please try again.");
                    } finally {
                        setIsResending(false);
                    }
                }}
                className="mt-3 w-full text-center text-sm font-semibold text-teal-700 transition hover:text-teal-800 disabled:cursor-wait disabled:opacity-70 dark:text-teal-300"
            >{isResending ? "Sending OTP" : resendRemainingSeconds > 0 ? `Resend OTP in ${formatResendTime(resendRemainingSeconds)}` : "Resend OTP"}</button>}
            <div
                className="my-5 flex items-center gap-3 text-xs font-medium text-slate-500 dark:text-slate-400"
            >
                <span className="h-px flex-1 bg-slate-200 dark:bg-white/10"/>
                <span>or continue with</span>
                <span className="h-px flex-1 bg-slate-200 dark:bg-white/10"/>
            </div>
            <OAuthButtons mode="login"/>
            <div className="mt-5 flex items-center justify-between text-sm">
                <Link
                    to="/signup"
                    className="font-semibold text-teal-700 transition hover:text-teal-800 dark:text-teal-300"
                >Create
                    account</Link>
                <Link
                    to="/forgot-password"
                    className="font-medium text-slate-500 transition hover:text-slate-950 dark:text-slate-300 dark:hover:text-white"
                >Forgot
                    password?</Link>
            </div>
        </form>
    </AuthShell>;
};
export {
    LoginPage
};
