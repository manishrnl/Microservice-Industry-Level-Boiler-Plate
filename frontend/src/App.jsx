import {useEffect} from "react";
import {Navigate, Route, Routes} from "react-router-dom";
import {ApiActivityOverlay} from "./components/common/ApiActivityOverlay";
import {AppLayout} from "./components/layout/AppLayout";
import {PublicLayout} from "./components/layout/PublicLayout";
import {ProtectedRoute} from "./components/common/ProtectedRoute";
import {Loader} from "./components/common/Loader";
import {useAuthStore} from "./store/authStore";
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
import {NotFoundPage} from "./pages/NotFoundPage";
import {AccessDeniedPage} from "./pages/AccessDeniedPage";

const PublicHome = () => {
    const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
    const isLoading = useAuthStore((state) => state.isLoading);
    if (isLoading) {
        return <Loader variant="fullscreen" message="Checking session"/>;
    }
    if (isAuthenticated) {
        return <Navigate to="/app/dashboard" replace/>;
    }
    return <PublicLayout/>;
};

const App = () => {
    const hydrate = useAuthStore((state) => state.hydrate);
    useEffect(() => {
        void hydrate();
    }, [hydrate]);
    return <>
        <Routes>
            <Route element={<PublicHome/>}>
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
            <Route path="/403" element={<AccessDeniedPage/>}/>
            <Route path="*" element={<NotFoundPage/>}/>
        </Routes>
        <ApiActivityOverlay/>
    </>;
};
export {
    App
};
