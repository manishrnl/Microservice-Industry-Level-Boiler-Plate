const pad = (value, size = 2) => String(value).padStart(size, "0");

const getClientTimeZone = () => {
    if (typeof Intl === "undefined") {
        return "";
    }
    return Intl.DateTimeFormat().resolvedOptions().timeZone ?? "";
};

const getClientLocalTime = (date = new Date()) => {
    const year = date.getFullYear();
    const month = pad(date.getMonth() + 1);
    const day = pad(date.getDate());
    const hours = pad(date.getHours());
    const minutes = pad(date.getMinutes());
    const seconds = pad(date.getSeconds());
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
};

export {
    getClientLocalTime,
    getClientTimeZone
};
