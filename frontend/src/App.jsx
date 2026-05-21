import {useEffect} from "react";
import {Route, Routes} from "react-router-dom";
import {ApiActivityOverlay} from "./components/common/ApiActivityOverlay";
import {AppLayout} from "./components/layout/AppLayout";
import {ProtectedRoute} from "./components/common/ProtectedRoute";
import {useAuthStore} from "./store/authStore";
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

const App = () => {
    const hydrate = useAuthStore((state) => state.hydrate);
    useEffect(() => {
        void hydrate();
    }, [hydrate]);
    return <>
        <Routes>
            <Route path="/login" element={<LoginPage/>}/>
            <Route path="/signup" element={<SignupPage/>}/>
            <Route path="/forgot-password" element={<ForgotPasswordPage/>}/>
            <Route path="/oauth/callback" element={<OAuthCallbackPage/>}/>
            <Route element={<ProtectedRoute/>}>
                <Route element={<AppLayout/>}>
                    <Route index element={<Dashboard/>}/>
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
            <Route path="/403" element={<AccessDeniedPage/>}/>
            <Route path="*" element={<NotFoundPage/>}/>
        </Routes>
        <ApiActivityOverlay/>
    </>;
};
export {
    App
};
