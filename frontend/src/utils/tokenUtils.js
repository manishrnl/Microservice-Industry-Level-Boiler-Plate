const bearer = (token) => token ? `Bearer ${token}` : "";
const decodeJwtPayload = (token) => {
    if (!token) {
        return null;
    }
    const [, payload] = token.split(".");
    if (!payload) {
        return null;
    }
    try {
        const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
        const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, "=");
        return JSON.parse(atob(padded));
    } catch {
        return null;
    }
};
const isTokenExpired = (token) => {
    const payload = decodeJwtPayload(token);
    if (!payload?.exp) {
        return false;
    }
    return payload.exp * 1e3 <= Date.now();
};
const authUserFromToken = (token) => {
    const payload = decodeJwtPayload(token);
    if (!payload?.sub || !payload.email) {
        return null;
    }
    return {
        userId: payload.sub,
        name: payload.name || displayName(payload.email),
        username: payload.username || "",
        email: payload.email,
        roles: normalizeRoles(payload.roles),
        avatarUrl: null
    };
};
const normalizeRoles = (roles) => {
    if (!roles?.length) {
        return ["USER"];
    }
    return roles.filter((role) => role === "SUPER_ADMIN" || role === "ADMIN" || role === "EDITOR" || role === "CREATOR" || role === "VIEWER" || role === "USER");
};
const displayName = (email) => {
    const atIndex = email.indexOf("@");
    return atIndex > 0 ? email.slice(0, atIndex) : "User";
};
export {
    authUserFromToken,
    bearer,
    decodeJwtPayload,
    isTokenExpired
};
