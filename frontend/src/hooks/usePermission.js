import {useAuthStore} from "../store/authStore";

const usePermission = () => {
    const roles = useAuthStore((state) => state.user?.roles ?? []);
    const hasRole = (role) => roles.includes(role);
    const hasAnyRole = (required) => required.some(hasRole);
    const hasAllRoles = (required) => required.every(hasRole);
    const isSuperAdmin = () => hasRole("SUPER_ADMIN");
    const isAdmin = () => hasAnyRole(["SUPER_ADMIN", "ADMIN"]);
    return {hasRole, hasAnyRole, hasAllRoles, isSuperAdmin, isAdmin};
};
export {
    usePermission
};
