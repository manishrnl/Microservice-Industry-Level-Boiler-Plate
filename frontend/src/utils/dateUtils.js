import {formatDistanceToNow} from "date-fns";
import {getPreferredTimeZone} from "../store/preferencesStore";

const backendDatePattern = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?$/;

const parseBackendDate = (value) => {
    if (!value) {
        return null;
    }
    const normalized = typeof value === "string" && backendDatePattern.test(value) ? `${value}Z` : value;
    const date = new Date(normalized);
    return Number.isNaN(date.getTime()) ? null : date;
};

const relativeTime = (value) => {
    const date = parseBackendDate(value);
    return date ? `${formatDistanceToNow(date)} ago` : "Not reported";
};
const formatDateTime = (value, timezone = getPreferredTimeZone()) => {
    const date = parseBackendDate(value);
    if (!date) {
        return "Not reported";
    }
    return new Intl.DateTimeFormat("en-IN", {
        dateStyle: "medium",
        timeStyle: "medium",
        timeZone: timezone
    }).format(date);
};

const formatMonth = (value, timezone = getPreferredTimeZone()) => {
    const date = parseBackendDate(value);
    if (!date) {
        return "Unknown date";
    }
    return new Intl.DateTimeFormat("en", {month: "long", year: "numeric", timeZone: timezone}).format(date);
};

export {
    formatDateTime,
    formatMonth,
    parseBackendDate,
    relativeTime
};
