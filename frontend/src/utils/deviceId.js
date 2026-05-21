const DEVICE_ID_KEY = "platform.deviceId";

const fallbackId = () => `browser-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;

const createDeviceId = () => {
    if (typeof crypto !== "undefined" && crypto.randomUUID) {
        return `browser-${crypto.randomUUID()}`;
    }
    return fallbackId();
};

const getDeviceId = () => {
    const existing = localStorage.getItem(DEVICE_ID_KEY);
    if (existing) {
        return existing;
    }
    const deviceId = createDeviceId();
    localStorage.setItem(DEVICE_ID_KEY, deviceId);
    return deviceId;
};

export {
    getDeviceId
};
