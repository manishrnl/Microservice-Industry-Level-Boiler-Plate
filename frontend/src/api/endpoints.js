import {env} from "../config/env";

const api = env.apiGatewayUrl;
const endpoints = {
    auth: {
        base: `${api}/api/v1/auth`,
        signup: `${api}/api/v1/auth/signup`,
        login: `${api}/api/v1/auth/login`,
        logout: `${api}/api/v1/auth/logout`,
        refresh: `${api}/api/v1/auth/refresh`,
        me: `${api}/api/v1/auth/me`,
        password: `${api}/api/v1/auth/me/password`,
        suspend: `${api}/api/v1/auth/me/suspend`,
        deleteAccount: `${api}/api/v1/auth/me`,
        verifyEmail: `${api}/api/v1/auth/verify-email`,
        resendVerification: `${api}/api/v1/auth/resend-verification`,
        forgotPassword: `${api}/api/v1/auth/forgot-password`,
        resetPassword: `${api}/api/v1/auth/reset-password`,
        adminUnlockUser: (id) => `${api}/api/v1/auth/admin/users/${id}/unlock`,
        adminPassword: (id) => `${api}/api/v1/auth/admin/users/${id}/password`,
        sessions: `${api}/api/v1/auth/sessions`,
        revokeSession: (sessionId) => `${api}/api/v1/auth/sessions/${sessionId}`,
        revokeAllSessions: `${api}/api/v1/auth/sessions/all`,
        jwks: `${api}/api/v1/auth/.well-known/jwks.json`,
        oauthAuthorize: (provider) => `${api}/api/v1/auth/oauth2/authorize/${provider}`,
        oauthCallback: (provider) => `${api}/api/v1/auth/oauth2/callback/${provider}`
    },
    users: {
        me: `${api}/api/v1/users/me`,
        byId: (id) => `${api}/api/v1/users/${id}`,
        list: `${api}/api/v1/users`,
        role: (id) => `${api}/api/v1/users/${id}/role`,
        settings: `${api}/api/v1/users/me/settings`,
        preferences: `${api}/api/v1/users/me/preferences`,
        avatar: `${api}/api/v1/users/me/avatar`
    },
    notifications: {
        list: `${api}/api/v1/notifications`,
        stream: `${api}/api/v1/notifications/stream`,
        markRead: (id) => `${api}/api/v1/notifications/${id}/read`,
        markAllRead: `${api}/api/v1/notifications/read-all`,
        deleteAll: `${api}/api/v1/notifications`,
        delete: (id) => `${api}/api/v1/notifications/${id}`
    },
    ai: {
        chat: `${api}/api/v1/ai/chat`,
        stream: (sessionId) => `${api}/api/v1/ai/chat/stream/${sessionId}`,
        sessions: `${api}/api/v1/ai/sessions`,
        messages: (sessionId) => `${api}/api/v1/ai/sessions/${sessionId}/messages`,
        saveMessages: (sessionId) => `${api}/api/v1/ai/sessions/${sessionId}/messages`,
        createSession: `${api}/api/v1/ai/sessions`,
        renameSession: (sessionId) => `${api}/api/v1/ai/sessions/${sessionId}`,
        deleteSession: (sessionId) => `${api}/api/v1/ai/sessions/${sessionId}`,
        usage: `${api}/api/v1/ai/usage`,
        systemPrompt: `${api}/api/v1/ai/admin/system-prompt`
    },
    files: {
        upload: `${api}/api/v1/files/upload`,
        metadata: (id) => `${api}/api/v1/files/${id}/metadata`,
        downloadUrl: (id) => `${api}/api/v1/files/${id}/download-url`,
        download: (id) => `${api}/api/v1/files/${id}/download`,
        view: (id) => `${api}/api/v1/files/${id}/download?disposition=inline`,
        mine: `${api}/api/v1/files/my-files`,
        delete: (id) => `${api}/api/v1/files/${id}`
    },
    payments: {
        list: `${api}/api/v1/payments`,
        adminByUser: (userId) => `${api}/api/v1/payments/admin/users/${userId}`,
        create: `${api}/api/v1/payments`,
        confirm: (paymentId) => `${api}/api/v1/payments/${paymentId}/confirm`
    },
    audit: {
        list: `${api}/api/v1/audit`,
        export: `${api}/api/v1/audit/export`
    },
    observability: {
        logs: `${api}/api/v1/observability/logs`,
        lokiLabels: `${api}/api/v1/observability/loki/labels`,
        lokiServices: `${api}/api/v1/observability/loki/services`,
        lokiQueryRange: `${api}/api/v1/observability/loki/query-range`
    }
};
export {
    endpoints
};
