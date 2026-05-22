import {Navigate, Outlet} from "react-router-dom";
import {useAuthStore} from "../../store/authStore";
import {usePermission} from "../../hooks/usePermission";
import {Loader} from "./Loader";

const ProtectedRoute = ({requiredRole, requiredAnyRole}) => {
    const {isAuthenticated, isLoading} = useAuthStore();
    const {hasRole, hasAnyRole} = usePermission();
    if (isLoading) {
        if (isAuthenticated) {
            return <div className="pointer-events-none select-none opacity-90 blur-[1px]">
                <Outlet/>
            </div>;
        }
        return <Loader variant="fullscreen" message="Checking session"/>;
    }
    if (!isAuthenticated) {
        return <Navigate to="/login" replace/>;
    }
    if (requiredRole && !hasRole(requiredRole)) {
        return <Navigate to="/403" replace/>;
    }
    if (requiredAnyRole && !hasAnyRole(requiredAnyRole)) {
        return <Navigate to="/403" replace/>;
    }
    return <Outlet/>;
};
export {
    ProtectedRoute
};
