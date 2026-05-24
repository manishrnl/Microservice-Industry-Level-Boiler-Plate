const hasOwn = (value, key) => Boolean(value) && Object.prototype.hasOwnProperty.call(value, key);

const cleanText = (value) => typeof value === "string" ? value.trim() : "";

const emailLocalPart = (email) => {
    const normalized = cleanText(email);
    const atIndex = normalized.indexOf("@");
    return atIndex > 0 ? normalized.slice(0, atIndex) : "";
};

const displayUserName = (name, email, fallback = "User") => {
    const normalizedName = cleanText(name);
    if (normalizedName && !normalizedName.includes("@")) {
        return normalizedName;
    }
    return emailLocalPart(email) || fallback;
};

const firstDisplayName = (name, email, fallback = "User") => displayUserName(name, email, fallback)
    .split(/\s+/)
    .filter(Boolean)[0] || fallback;

const uppercaseDisplayName = (name, email, fallback = "User") => displayUserName(name, email, fallback).toUpperCase();

const mergeAccountIdentity = (user, account) => {
    const accountLoaded = Boolean(account);
    const email = cleanText(account?.email) || cleanText(user?.email);
    const accountName = cleanText(account?.name);
    const fallbackName = accountLoaded ? "" : cleanText(user?.name);
    return {
        ...(user ?? {}),
        ...(account ?? {}),
        email,
        name: displayUserName(accountName || fallbackName, email),
        username: hasOwn(account, "username") ? account.username : user?.username,
        avatarUrl: hasOwn(account, "avatarUrl") ? account.avatarUrl : user?.avatarUrl,
        roles: Array.isArray(account?.roles) ? account.roles : Array.isArray(user?.roles) ? user.roles : []
    };
};

export {
    displayUserName,
    firstDisplayName,
    mergeAccountIdentity,
    uppercaseDisplayName
};
