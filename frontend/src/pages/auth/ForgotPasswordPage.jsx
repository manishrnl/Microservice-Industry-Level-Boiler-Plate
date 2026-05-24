import {ArrowRight, LoaderCircle, Mail} from "lucide-react";
import {useState} from "react";
import {useForm} from "react-hook-form";
import {Link, useNavigate} from "react-router-dom";
import {apiClient} from "../../api/axiosInstance";
import {endpoints} from "../../api/endpoints";
import {AuthShell} from "./AuthShell";
import {PasswordField} from "./PasswordField";

const inputClassName = "h-12 w-full rounded-md border border-slate-200 bg-slate-50/80 px-3 pl-11 text-sm text-slate-950 shadow-inner shadow-slate-950/[0.03] outline-none transition placeholder:text-slate-400 focus:border-teal-500 focus:bg-white focus:ring-4 focus:ring-teal-500/10 dark:border-white/10 dark:bg-white/[0.06] dark:text-white dark:placeholder:text-slate-500 dark:focus:bg-white/[0.09]";
const ForgotPasswordPage = () => {
    const [otpSent, setOtpSent] = useState(false);
    const [resetCompleted, setResetCompleted] = useState(false);
    const [error, setError] = useState("");
    const [notice, setNotice] = useState("");
    const navigate = useNavigate();
    const {register, handleSubmit, watch, formState} = useForm();
    const submit = handleSubmit(async (values) => {
        setError("");
        setNotice("");
        try {
            const email = values.email?.trim().toLowerCase();
            if (!otpSent) {
                await apiClient.post(endpoints.auth.forgotPassword, {email});
                setOtpSent(true);
                setNotice("Reset OTP sent. Check MailHog or your configured inbox.");
                return;
            }
            await apiClient.post(endpoints.auth.resetPassword, {...values, email});
            setResetCompleted(true);
            window.setTimeout(() => navigate("/login", {replace: true}), 1600);
        } catch (requestError) {
            const detail = requestError?.response?.data?.detail ?? requestError?.response?.data?.message;
            if (requestError?.response?.status === 404) {
                setError("No account exists for this email. Check the address or create a new account.");
                return;
            }
            setError(detail || "Could not reset password. Please try again.");
        }
    });
    return <AuthShell
        title="Reset password"
        subtitle={otpSent ? "Enter the OTP from your email and choose a new password." : "Enter your account email and we will send a reset OTP."}
    >
        <form onSubmit={submit}>
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
            {otpSent && <>
                <label className="mb-4 block">
                            <span
                                className="mb-1.5 block text-sm font-semibold text-slate-700 dark:text-slate-200"
                            >OTP</span>
                    <input
                        className="h-12 w-full rounded-md border border-slate-200 bg-slate-50/80 px-3 text-sm tracking-[0.25em] text-slate-950 shadow-inner shadow-slate-950/[0.03] outline-none transition placeholder:tracking-normal placeholder:text-slate-400 focus:border-teal-500 focus:bg-white focus:ring-4 focus:ring-teal-500/10 dark:border-white/10 dark:bg-white/[0.06] dark:text-white dark:placeholder:text-slate-500 dark:focus:bg-white/[0.09]"
                        placeholder="000000"
                        inputMode="numeric"
                        maxLength={6}
                        {...register("otp", {
                            required: otpSent,
                            minLength: 6
                        })}
                    />
                </label>
                <PasswordField
                    label="New password"
                    placeholder="Create new password"
                    registration={register("password", {
                        required: otpSent,
                        minLength: 8
                    })}
                />
                <PasswordField
                    label="Confirm password"
                    placeholder="Repeat new password"
                    registration={register("confirmPassword", {
                        required: otpSent,
                        validate: (value) => value === watch("password") || "Passwords do not match"
                    })}
                />
                {formState.errors.confirmPassword &&
                    <p className="-mt-1 mb-3 text-sm text-red-600 dark:text-red-300">{formState.errors.confirmPassword.message}</p>}
            </>}
            {error &&
                <p className="-mt-1 mb-3 text-sm font-medium text-red-600 dark:text-red-300">{error}</p>}
            <button
                disabled={formState.isSubmitting}
                className="group flex h-12 w-full items-center justify-center gap-2 rounded-md bg-slate-950 px-4 text-sm font-semibold text-white shadow-lg shadow-slate-950/15 transition hover:-translate-y-0.5 hover:bg-slate-800 hover:shadow-xl focus:outline-none focus:ring-4 focus:ring-teal-500/20 disabled:cursor-wait disabled:opacity-80 dark:bg-teal-400 dark:text-slate-950 dark:hover:bg-teal-300"
            >
                {formState.isSubmitting ? otpSent ? "Changing password" : "Sending OTP" : resetCompleted ? "Redirecting" : otpSent ? "Change password" : "Send reset OTP"}
                {formState.isSubmitting ? <LoaderCircle className="h-4 w-4 animate-spin"/> :
                    <ArrowRight
                        className="h-4 w-4 transition group-hover:translate-x-0.5"
                    />}
            </button>
            {otpSent && !resetCompleted &&
                <p className="mt-3 text-sm font-medium text-emerald-700 dark:text-emerald-300">{notice || "Reset OTP sent."}</p>}
            {resetCompleted &&
                <p className="mt-3 text-sm font-medium text-emerald-700 dark:text-emerald-300">Password
                    changed successfully. Redirecting to login...</p>}
            <Link
                to="/login"
                className="mt-5 block text-center text-sm font-semibold text-teal-700 transition hover:text-teal-800 dark:text-teal-300"
            >Back
                to sign in</Link>
        </form>
    </AuthShell>;
};
export {
    ForgotPasswordPage
};
