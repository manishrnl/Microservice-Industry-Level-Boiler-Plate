import {useEffect} from "react";
import {Navigate, Route, Routes, useLocation} from "react-router-dom";
import {ApiActivityOverlay} from "./components/common/ApiActivityOverlay";
import {AppLayout} from "./components/layout/AppLayout";
import {PublicLayout} from "./components/layout/PublicLayout";
import {COMPANY_NAME} from "./components/layout/BrandMark";
import {ProtectedRoute} from "./components/common/ProtectedRoute";
import {useAuthStore} from "./store/authStore";
import {usePreferencesStore} from "./store/preferencesStore";
import {HomePage} from "./pages/HomePage";
import {LoginPage} from "./pages/auth/LoginPage";
import {SignupPage} from "./pages/auth/SignupPage";
import {OAuthCallbackPage} from "./pages/auth/OAuthCallbackPage";
import {ForgotPasswordPage} from "./pages/auth/ForgotPasswordPage";
import {Dashboard} from "./pages/Dashboard";
import {ProfilePage} from "./pages/ProfilePage";
import {NotificationsPage} from "./pages/NotificationsPage";
import {SessionsPage} from "./pages/SessionsPage";
import {FilesPage} from "./pages/FilesPage";
import {PaymentsPage} from "./pages/PaymentsPage";
import {AiChatPage} from "./pages/ai/AiChatPage";
import {UserManagementPage} from "./pages/admin/UserManagementPage";
import {AuditLogPage} from "./pages/admin/AuditLogPage";
import {ObservabilityPage} from "./pages/admin/ObservabilityPage";
import {NotFoundPage} from "./pages/NotFoundPage";
import {AccessDeniedPage} from "./pages/AccessDeniedPage";

const pageTitles = [
    {path: "/", title: "Home", exact: true},
    {path: "/login", title: "Login"},
    {path: "/signup", title: "Signup"},
    {path: "/forgot-password", title: "Forgot Password"},
    {path: "/oauth/callback", title: "OAuth Callback"},
    {path: "/app/dashboard", title: "Dashboard"},
    {path: "/app/profile", title: "Profile"},
    {path: "/app/notifications", title: "Notifications"},
    {path: "/app/sessions", title: "Sessions"},
    {path: "/app/files", title: "Files"},
    {path: "/app/payments", title: "Payments"},
    {path: "/app/ai", title: "AI Chat"},
    {path: "/app/admin/users", title: "Admin Users"},
    {path: "/app/admin/audit", title: "Admin Audit"},
    {path: "/app/admin/observability", title: "Observability"},
    {path: "/403", title: "Access Denied"}
];

const titleForPath = (pathname) => pageTitles.find((item) => item.exact ? pathname === item.path : pathname.startsWith(item.path))?.title ?? "Not Found";

const App = () => {
    const hydrate = useAuthStore((state) => state.hydrate);
    const hydratePreferences = usePreferencesStore((state) => state.hydrate);
    const {pathname} = useLocation();
    useEffect(() => {
        hydratePreferences();
        void hydrate();
    }, [hydrate, hydratePreferences]);
    useEffect(() => {
        if (window.__PLATFORM_CONSOLE_BANNER__) {
            return;
        }
        window.__PLATFORM_CONSOLE_BANNER__ = true;
        console.log(
            "%c Microservice Platform %c Auth, profiles, payments, AI and audit are online ",
            "background:#0f172a;color:#67e8f9;font-weight:800;padding:6px 10px;border-radius:6px 0 0 6px;",
            "background:#14b8a6;color:#042f2e;font-weight:700;padding:6px 10px;border-radius:0 6px 6px 0;"
        );
    }, []);
    useEffect(() => {
        document.title = `${titleForPath(pathname)} | ${COMPANY_NAME}`;
    }, [pathname]);
    return <>
        <Routes>
            <Route element={<PublicLayout/>}>
                <Route index element={<HomePage/>}/>
            </Route>
            <Route path="/login" element={<LoginPage/>}/>
            <Route path="/signup" element={<SignupPage/>}/>
            <Route path="/forgot-password" element={<ForgotPasswordPage/>}/>
            <Route path="/oauth/callback" element={<OAuthCallbackPage/>}/>
            <Route element={<ProtectedRoute/>}>
                <Route path="/app" element={<AppLayout/>}>
                    <Route index element={<Navigate to="/app/dashboard" replace/>}/>
                    <Route path="dashboard" element={<Dashboard/>}/>
                    <Route path="profile" element={<ProfilePage/>}/>
                    <Route path="notifications" element={<NotificationsPage/>}/>
                    <Route path="sessions" element={<SessionsPage/>}/>
                    <Route path="files" element={<FilesPage/>}/>
                    <Route path="payments" element={<PaymentsPage/>}/>
                    <Route path="ai" element={<AiChatPage/>}/>
                    <Route element={<ProtectedRoute
                        requiredAnyRole={["ADMIN", "SUPER_ADMIN"]}
                    />}>
                        <Route path="admin/users" element={<UserManagementPage/>}/>
                        <Route path="admin/audit" element={<AuditLogPage/>}/>
                        <Route path="admin/observability" element={<ObservabilityPage/>}/>
                    </Route>
                </Route>
            </Route>
            <Route path="/dashboard" element={<Navigate to="/app/dashboard" replace/>}/>
            <Route path="/profile" element={<Navigate to="/app/profile" replace/>}/>
            <Route path="/notifications" element={<Navigate to="/app/notifications" replace/>}/>
            <Route path="/sessions" element={<Navigate to="/app/sessions" replace/>}/>
            <Route path="/files" element={<Navigate to="/app/files" replace/>}/>
            <Route path="/payments" element={<Navigate to="/app/payments" replace/>}/>
            <Route path="/ai" element={<Navigate to="/app/ai" replace/>}/>
            <Route path="/admin/users" element={<Navigate to="/app/admin/users" replace/>}/>
            <Route path="/admin/audit" element={<Navigate to="/app/admin/audit" replace/>}/>
            <Route path="/admin/observability" element={<Navigate to="/app/admin/observability" replace/>}/>
            <Route path="/403" element={<AccessDeniedPage/>}/>
            <Route path="*" element={<NotFoundPage/>}/>
        </Routes>
        <ApiActivityOverlay/>
    </>;
};
export {
    App
};
