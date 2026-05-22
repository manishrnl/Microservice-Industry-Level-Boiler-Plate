const pad = (value, size = 2) => String(value).padStart(size, "0");

const getBrowserTimeZone = () => {
    if (typeof Intl === "undefined") {
        return "";
    }
    return Intl.DateTimeFormat().resolvedOptions().timeZone ?? "";
};

const getPart = (parts, type) => parts.find((part) => part.type === type)?.value ?? "";

const getClientLocalTime = (timeZone = getBrowserTimeZone(), date = new Date()) => {
    if (timeZone && typeof Intl !== "undefined") {
        try {
            const parts = new Intl.DateTimeFormat("en-CA", {
                timeZone,
                year: "numeric",
                month: "2-digit",
                day: "2-digit",
                hour: "2-digit",
                minute: "2-digit",
                second: "2-digit",
                hour12: false
            }).formatToParts(date);
            const year = getPart(parts, "year");
            const month = getPart(parts, "month");
            const day = getPart(parts, "day");
            const hours = getPart(parts, "hour");
            const minutes = getPart(parts, "minute");
            const seconds = getPart(parts, "second");
            return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
        } catch {
            // Fall back to browser-local components below.
        }
    }
    const year = date.getFullYear();
    const month = pad(date.getMonth() + 1);
    const day = pad(date.getDate());
    const hours = pad(date.getHours());
    const minutes = pad(date.getMinutes());
    const seconds = pad(date.getSeconds());
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
};

export {
    getBrowserTimeZone,
    getClientLocalTime,
};
