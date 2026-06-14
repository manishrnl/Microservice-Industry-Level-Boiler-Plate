const languages = [
    {code: "en", locale: "en", label: "English", shortLabel: "EN", name: "English", group: "International"},
    {code: "hi", locale: "hi-IN", label: "हिन्दी (Hindi)", shortLabel: "हि", name: "Hindi", group: "Indian"},
    {code: "sa", locale: "sa-IN", label: "संस्कृतम् (Sanskrit)", shortLabel: "सं", name: "Sanskrit", group: "Indian"},
    {code: "mai", locale: "mai-IN", label: "मैथिली (Maithili)", shortLabel: "मै", name: "Maithili", group: "Indian"},
    {code: "ta", locale: "ta-IN", label: "தமிழ் (Tamil)", shortLabel: "த", name: "Tamil", group: "Indian"},
    {code: "bn", locale: "bn-IN", label: "বাংলা (Bangla)", shortLabel: "বা", name: "Bangla", group: "Indian"},
    {code: "te", locale: "te-IN", label: "తెలుగు (Telugu)", shortLabel: "తె", name: "Telugu", group: "Indian"},
    {code: "mr", locale: "mr-IN", label: "मराठी (Marathi)", shortLabel: "म", name: "Marathi", group: "Indian"},
    {code: "gu", locale: "gu-IN", label: "ગુજરાતી (Gujarati)", shortLabel: "ગુ", name: "Gujarati", group: "Indian"},
    {code: "kn", locale: "kn-IN", label: "ಕನ್ನಡ (Kannada)", shortLabel: "ಕ", name: "Kannada", group: "Indian"},
    {code: "ml", locale: "ml-IN", label: "മലയാളം (Malayalam)", shortLabel: "മ", name: "Malayalam", group: "Indian"},
    {code: "ru", locale: "ru-RU", label: "Русский (Russian)", shortLabel: "RU", name: "Russian", group: "International"},
    {code: "fr", locale: "fr-FR", label: "Français (French)", shortLabel: "FR", name: "French", group: "International"},
    {code: "es", locale: "es-ES", label: "Español (Spanish)", shortLabel: "ES", name: "Spanish", group: "International"},
    {code: "de", locale: "de-DE", label: "Deutsch (German)", shortLabel: "DE", name: "German", group: "International"},
    {code: "ar", locale: "ar", label: "العربية (Arabic)", shortLabel: "عر", name: "Arabic", group: "International"},
    {code: "pt", locale: "pt-PT", label: "Português (Portuguese)", shortLabel: "PT", name: "Portuguese", group: "International"},
    {code: "ja", locale: "ja-JP", label: "日本語 (Japanese)", shortLabel: "日", name: "Japanese", group: "International"},
    {code: "ko", locale: "ko-KR", label: "한국어 (Korean)", shortLabel: "한", name: "Korean", group: "International"},
    {code: "zh-CN", locale: "zh-CN", label: "中文 (Chinese)", shortLabel: "中", name: "Chinese", group: "International"}
];

const languageByCode = (code) => languages.find((language) => language.code === code);

const languageCodeForLocale = (locale) => {
    const normalized = String(locale || "").trim().toLowerCase().replace("_", "-");
    if (!normalized) {
        return null;
    }
    if (normalized.startsWith("zh")) {
        return "zh-CN";
    }
    return languages.find(({code, locale: supportedLocale}) =>
        code.toLowerCase() === normalized
        || supportedLocale.toLowerCase() === normalized
        || code.toLowerCase() === normalized.split("-")[0]
    )?.code ?? null;
};

const getSystemLanguage = () => {
    if (typeof navigator === "undefined") {
        return "en";
    }
    const primaryLocale = navigator.language || Intl.DateTimeFormat().resolvedOptions().locale;
    return languageCodeForLocale(primaryLocale) ?? "en";
};

export {
    getSystemLanguage,
    languageByCode,
    languages
};
