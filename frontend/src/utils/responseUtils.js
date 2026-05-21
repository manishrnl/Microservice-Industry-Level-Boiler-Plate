const unwrapApiData = (payload) => {
    if (payload && typeof payload === "object" && "data" in payload) {
        const data = payload.data;
        if (data && typeof data === "object" && "data" in data) {
            return data.data;
        }
        return data;
    }
    return payload;
};
const asArray = (payload) => {
    const data = unwrapApiData(payload);
    if (Array.isArray(data)) {
        return data;
    }
    if (data && typeof data === "object" && "content" in data && Array.isArray(data.content)) {
        return data.content;
    }
    return [];
};
const extractAssistantContent = (payload) => {
    const data = unwrapApiData(payload);
    const direct = extractFromObject(data);
    if (direct) {
        return direct;
    }
    if (typeof data === "string") {
        return extractFromString(data);
    }
    return "";
};
const extractFromObject = (value) => {
    if (!value || typeof value !== "object") {
        return "";
    }
    const record = value;
    if (typeof record.response === "string") {
        return extractFromString(record.response);
    }
    if (record.response) {
        return extractAssistantContent(record.response);
    }
    const choices = record.choices;
    if (Array.isArray(choices)) {
        const message = choices[0]?.message;
        if (typeof message?.content === "string") {
            return message.content.trim();
        }
    }
    const candidates = record.candidates;
    if (Array.isArray(candidates)) {
        const parts = candidates[0]?.content?.parts;
        const text = parts?.find((part) => typeof part.text === "string")?.text;
        if (typeof text === "string") {
            return text.trim();
        }
    }
    return "";
};
const extractFromString = (value) => {
    const trimmed = value.trim();
    if (!trimmed) {
        return "";
    }
    try {
        return extractAssistantContent(JSON.parse(trimmed));
    } catch {
        const contentMatch = trimmed.match(/message=\{role=assistant,\s*content=([\s\S]*?)(?:},\s*logprobs=|},\s*finish_reason=|}],\s*usage=)/);
        if (contentMatch?.[1]) {
            return contentMatch[1].trim();
        }
        const geminiMatch = trimmed.match(/parts=\[\{text=([\s\S]*?)}]/);
        if (geminiMatch?.[1]) {
            return geminiMatch[1].trim();
        }
        return trimmed;
    }
};
export {
    asArray,
    extractAssistantContent,
    unwrapApiData
};
