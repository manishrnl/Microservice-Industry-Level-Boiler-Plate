import {Check, Copy, Edit3, LoaderCircle, Plus, RefreshCcw, Send, X} from "lucide-react";
import {useQuery, useQueryClient} from "@tanstack/react-query";
import {useEffect, useRef, useState} from "react";
import ReactMarkdown from "react-markdown";
import {apiClient} from "../../api/axiosInstance";
import {endpoints} from "../../api/endpoints";
import {useAccountIdentity} from "../../hooks/useAccountIdentity";
import {usePreferencesStore} from "../../store/preferencesStore";
import {asArray, extractAssistantContent, unwrapApiData} from "../../utils/responseUtils";
import {displayUserName} from "../../utils/userDisplay";

const createClientId = () => {
    if (globalThis.crypto?.randomUUID) {
        return globalThis.crypto.randomUUID();
    }
    return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
};
const isIdentityQuestion = (content) => /\b(who\s+am\s+i|whoami|who\s+i\s+am)\b/i.test(content.trim());
const isWeatherQuestion = (content) => /\b(weather|temperature|forecast|rain|raining|climate)\b/i.test(content.trim());
const identityFallback = (content, user) => {
    if (!isIdentityQuestion(content) || !user) {
        return "";
    }
    const displayName = displayUserName(user.name, user.email, "the signed-in user");
    const roles = Array.isArray(user.roles) && user.roles.length ? ` Your roles are ${user.roles.join(", ")}.` : "";
    const email = user.email ? ` (${user.email})` : "";
    return `You are ${displayName}${email}.${roles}`;
};
const weatherDescriptions = {
    0: "Clear sky",
    1: "Mainly clear",
    2: "Partly cloudy",
    3: "Overcast",
    45: "Fog",
    48: "Depositing rime fog",
    51: "Light drizzle",
    53: "Moderate drizzle",
    55: "Dense drizzle",
    61: "Slight rain",
    63: "Moderate rain",
    65: "Heavy rain",
    71: "Slight snow",
    73: "Moderate snow",
    75: "Heavy snow",
    80: "Slight rain showers",
    81: "Moderate rain showers",
    82: "Violent rain showers",
    95: "Thunderstorm",
    96: "Thunderstorm with slight hail",
    99: "Thunderstorm with heavy hail"
};
const weatherFallback = async (content, location) => {
    if (!isWeatherQuestion(content)) {
        return "";
    }
    if (!location) {
        return "I need browser location permission to check weather near you. Please allow location access or ask with a city name.";
    }
    const params = new URLSearchParams({
        latitude: String(location.latitude),
        longitude: String(location.longitude),
        current: "temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m",
        timezone: "auto"
    });
    try {
        const response = await fetch(`https://api.open-meteo.com/v1/forecast?${params.toString()}`);
        if (!response.ok) {
            return "I could not reach the weather service right now. Please try again in a moment.";
        }
        const data = await response.json();
        const current = data.current;
        if (!current) {
            return "";
        }
        const description = weatherDescriptions[current.weather_code] ?? "Weather data available";
        const temperature = Math.round(current.temperature_2m);
        const feelsLike = Math.round(current.apparent_temperature);
        const humidity = Math.round(current.relative_humidity_2m);
        const wind = Math.round(current.wind_speed_10m);
        const rain = Number(current.precipitation ?? 0);
        return `Weather near you: ${description}. Temperature is ${temperature} degrees C, feels like ${feelsLike} degrees C. Humidity is ${humidity}%, wind is ${wind} km/h, and current precipitation is ${rain} mm.`;
    } catch {
        return "I could not reach the weather service right now. Please try again in a moment.";
    }
};

const AiChatPage = () => {
    const queryClient = useQueryClient();
    const {identity: currentUser} = useAccountIdentity();
    const timezone = usePreferencesStore((state) => state.timezone);
    const [sessions, setSessions] = useState([]);
    const [activeSession, setActiveSession] = useState("");
    const [messages, setMessages] = useState([]);
    const [input, setInput] = useState("");
    const [streaming, setStreaming] = useState(false);
    const [editingSessionId, setEditingSessionId] = useState("");
    const [draftTitle, setDraftTitle] = useState("");
    const locationRef = useRef({status: "unknown", value: null});
    const readLocation = () => new Promise((resolve) => {
        if (locationRef.current.status !== "unknown") {
            resolve(locationRef.current.value);
            return;
        }
        if (!navigator.geolocation) {
            locationRef.current = {status: "unavailable", value: null};
            resolve(null);
            return;
        }
        navigator.geolocation.getCurrentPosition(
            ({coords}) => {
                const value = {
                    latitude: coords.latitude,
                    longitude: coords.longitude,
                    accuracy: coords.accuracy
                };
                locationRef.current = {status: "available", value};
                resolve(value);
            },
            () => {
                locationRef.current = {status: "denied", value: null};
                resolve(null);
            },
            {enableHighAccuracy: false, maximumAge: 300000, timeout: 5000}
        );
    });
    const userContext = () => ({
        locale: navigator.language,
        timezone,
        localTime: new Date().toString()
    });
    const sessionsQuery = useQuery({
        queryKey: ["ai-sessions"],
        queryFn: async () => asArray((await apiClient.get(endpoints.ai.sessions)).data)
    });
    const messagesQuery = useQuery({
        queryKey: ["ai-messages", activeSession],
        queryFn: async () => asArray((await apiClient.get(endpoints.ai.messages(activeSession))).data),
        enabled: Boolean(activeSession)
    });
    useEffect(() => {
        if (sessionsQuery.data) {
            setSessions(sessionsQuery.data);
            setActiveSession((current) => sessionsQuery.data.some((session) => session.id === current) ? current : sessionsQuery.data[0]?.id ?? "");
        }
    }, [sessionsQuery.data]);
    useEffect(() => {
        if (messagesQuery.data) {
            setMessages(messagesQuery.data);
        } else if (!activeSession) {
            setMessages([]);
        }
    }, [activeSession, messagesQuery.data]);
    const createSession = async () => {
        const response = await apiClient.post(endpoints.ai.createSession, {title: "New chat"});
        const created = unwrapApiData(response.data);
        const session = {
            id: created.id ?? createClientId(),
            title: created.title ?? "New chat",
            createdAt: created.createdAt
        };
        setSessions((current) => [session, ...current]);
        setActiveSession(session.id);
        setMessages([]);
        queryClient.setQueryData(["ai-sessions"], (current = []) => [session, ...asArray(current).filter((item) => item.id !== session.id)]);
        return session.id;
    };
    const saveClientMessages = async (sessionId, savedMessages) => {
        await apiClient.post(endpoints.ai.saveMessages(sessionId), savedMessages.map(({role, content}) => ({role, content})));
        await queryClient.invalidateQueries({queryKey: ["ai-sessions"]});
        await queryClient.invalidateQueries({queryKey: ["ai-messages", sessionId]});
    };
    const renameSession = async (sessionId, title) => {
        const cleanTitle = title.trim() || "New chat";
        const response = await apiClient.patch(endpoints.ai.renameSession(sessionId), {title: cleanTitle});
        const saved = unwrapApiData(response.data);
        setSessions((current) => current.map((session) => session.id === sessionId ? {...session, ...saved, title: saved.title ?? cleanTitle} : session));
        setEditingSessionId("");
        setDraftTitle("");
        await queryClient.invalidateQueries({queryKey: ["ai-sessions"]});
    };
    const startRename = (event, session) => {
        event.stopPropagation();
        setEditingSessionId(session.id);
        setDraftTitle(session.title || "New chat");
    };
    const cancelRename = (event) => {
        event.stopPropagation();
        setEditingSessionId("");
        setDraftTitle("");
    };
    const sendMessage = async (content) => {
        if (!content.trim() || streaming) {
            return;
        }
        let sessionId = activeSession;
        if (!sessionId) {
            sessionId = await createSession();
        }
        const userMessage = {id: createClientId(), role: "user", content};
        const assistantId = createClientId();
        setMessages((current) => [...current, userMessage, {
            id: assistantId,
            role: "assistant",
            content: ""
        }]);
        setInput("");
        setStreaming(true);
        try {
            const location = await readLocation();
            const localWeather = await weatherFallback(userMessage.content, location);
            if (localWeather) {
                setMessages((current) => current.map((message) => message.id === assistantId ? {
                    ...message,
                    content: localWeather
                } : message));
                await saveClientMessages(sessionId, [userMessage, {role: "assistant", content: localWeather}]);
                return;
            }
            const response = await apiClient.post(endpoints.ai.chat, {
                sessionId,
                message: userMessage.content,
                stream: false,
                location,
                context: userContext()
            });
            const text = extractAssistantContent(response.data)
                || identityFallback(userMessage.content, currentUser)
                || "I could not read a usable response from the AI provider.";
            setMessages((current) => current.map((message) => message.id === assistantId ? {
                ...message,
                content: text
            } : message));
            await queryClient.invalidateQueries({queryKey: ["ai-sessions"]});
            await queryClient.invalidateQueries({queryKey: ["ai-messages", sessionId]});
        } catch (error) {
            const fallback = identityFallback(userMessage.content, currentUser)
                || await weatherFallback(userMessage.content, locationRef.current.value);
            const providerMessage = extractAssistantContent(error?.response?.data)
                || error?.response?.data?.detail
                || error?.response?.data?.message;
            const fallbackContent = fallback || providerMessage || "AI service could not answer right now. Please check the local AI service/provider configuration.";
            setMessages((current) => current.map((message) => message.id === assistantId ? {
                ...message,
                content: fallbackContent
            } : message));
            try {
                await saveClientMessages(sessionId, [userMessage, {role: "assistant", content: fallbackContent}]);
            } catch {
                // Keep the visible fallback even if the database write fails.
            }
        } finally {
            setStreaming(false);
        }
    };
    const send = async () => {
        await sendMessage(input);
    };
    const regenerate = () => {
        const lastUser = [...messages].reverse().find((message) => message.role === "user");
        if (lastUser) {
            void sendMessage(lastUser.content);
        }
    };
    return <main className="grid min-h-[calc(100vh-4rem)] flex-1 grid-cols-1 bg-slate-50 dark:bg-slate-950 md:grid-cols-[260px_1fr]">
        <aside className="border-b border-slate-200 bg-white p-3 dark:border-white/10 dark:bg-slate-900 md:border-b-0 md:border-r md:p-4">
            <button
                onClick={() => void createSession()}
                className="mb-3 flex w-full items-center justify-center gap-2 rounded-md bg-slate-950 px-3 py-2 text-sm font-medium text-white transition hover:bg-slate-800 dark:bg-teal-300 dark:text-slate-950 dark:hover:bg-teal-200 md:mb-4"
            >
                <Plus className="h-4 w-4"/> New chat
            </button>
            <div className="flex gap-2 overflow-x-auto pb-1 md:block md:space-y-1 md:overflow-visible md:pb-0">
                {sessions.map((session) => <div
                    key={session.id}
                    onClick={() => setActiveSession(session.id)}
                    className={`group flex min-w-[220px] cursor-pointer items-center gap-2 rounded-md px-3 py-2 text-left text-sm transition md:w-full md:min-w-0 ${session.id === activeSession ? "bg-slate-100 font-medium text-slate-950 dark:bg-white/10 dark:text-white" : "text-slate-600 hover:bg-slate-50 dark:text-slate-300 dark:hover:bg-white/10"}`}
                >
                    {editingSessionId === session.id ? <>
                        <input
                            value={draftTitle}
                            onClick={(event) => event.stopPropagation()}
                            onChange={(event) => setDraftTitle(event.target.value)}
                            onKeyDown={(event) => {
                                if (event.key === "Enter") {
                                    event.preventDefault();
                                    void renameSession(session.id, draftTitle);
                                }
                                if (event.key === "Escape") {
                                    cancelRename(event);
                                }
                            }}
                            autoFocus
                            className="h-8 min-w-0 flex-1 rounded-md border border-slate-200 bg-white px-2 text-sm outline-none focus:border-teal-500 dark:border-white/10 dark:bg-slate-950 dark:text-white"
                        />
                        <button
                            type="button"
                            onClick={(event) => {
                                event.stopPropagation();
                                void renameSession(session.id, draftTitle);
                            }}
                            className="grid h-8 w-8 shrink-0 place-items-center rounded-md text-emerald-700 hover:bg-emerald-50 dark:text-emerald-300 dark:hover:bg-emerald-300/10"
                            aria-label="Save chat title"
                        >
                            <Check className="h-4 w-4"/>
                        </button>
                        <button
                            type="button"
                            onClick={cancelRename}
                            className="grid h-8 w-8 shrink-0 place-items-center rounded-md text-slate-500 hover:bg-slate-100 dark:hover:bg-white/10"
                            aria-label="Cancel rename"
                        >
                            <X className="h-4 w-4"/>
                        </button>
                    </> : <>
                        <span className="block min-w-0 flex-1 truncate">{session.title}</span>
                        <button
                            type="button"
                            onClick={(event) => startRename(event, session)}
                            className="grid h-8 w-8 shrink-0 place-items-center rounded-md text-slate-400 opacity-100 hover:bg-slate-100 hover:text-slate-700 dark:hover:bg-white/10 dark:hover:text-white md:opacity-0 md:group-hover:opacity-100"
                            aria-label="Rename chat"
                        >
                            <Edit3 className="h-4 w-4"/>
                        </button>
                    </>}
                </div>)}
                {!sessionsQuery.isLoading && sessions.length === 0 && <div className="rounded-md border border-dashed border-slate-200 p-3 text-sm text-slate-500 dark:border-white/10 dark:text-slate-400">
                    Start a new chat to save it in the database.
                </div>}
            </div>
        </aside>
        <section className="flex min-w-0 flex-col">
            <div className="flex-1 space-y-4 overflow-y-auto p-3 sm:p-5 lg:p-6">
                {messagesQuery.isFetching && activeSession ? <div className="text-sm text-slate-500 dark:text-slate-400">Loading saved chat messages...</div> : null}
                {messages.map((message) => <div
                    key={message.id}
                    className={`max-w-full break-words rounded-md px-4 py-3 text-sm sm:max-w-3xl sm:text-base ${message.role === "user" ? "ml-auto bg-slate-950 text-white dark:bg-teal-300 dark:text-slate-950" : "bg-white text-slate-900 shadow-sm dark:bg-slate-900 dark:text-slate-100 dark:shadow-none dark:ring-1 dark:ring-white/10"}`}
                >
                    {message.role === "assistant" ?
                        <ReactMarkdown>{message.content || (streaming ? "Typing..." : "")}</ReactMarkdown> : message.content}
                    {message.role === "assistant" && message.content && <button
                        onClick={() => navigator.clipboard.writeText(message.content)}
                        className="mt-2 inline-flex items-center gap-1 text-xs text-slate-500 dark:text-slate-400"
                    >
                        <Copy className="h-3 w-3"/> Copy
                    </button>}
                </div>)}
            </div>
            <div className="border-t border-slate-200 bg-white p-3 dark:border-white/10 dark:bg-slate-900 sm:p-4">
                <div className="mx-auto flex max-w-4xl gap-2">
                        <textarea
                            value={input}
                            onChange={(event) => setInput(event.target.value)}
                            rows={2}
                            className="min-h-12 min-w-0 flex-1 resize-none rounded-md border border-slate-200 bg-white px-3 py-2 text-sm text-slate-950 outline-none transition focus:border-teal-500 focus:ring-2 focus:ring-teal-500/20 dark:border-white/10 dark:bg-slate-950 dark:text-slate-100 dark:placeholder:text-slate-500 sm:text-base"
                            placeholder="Message AI"
                        />
                    <button
                        onClick={send}
                        disabled={streaming || !input.trim()}
                        className="grid h-12 w-12 shrink-0 place-items-center rounded-md bg-slate-950 text-white transition hover:bg-slate-800 disabled:cursor-wait disabled:opacity-60 dark:bg-teal-300 dark:text-slate-950 dark:hover:bg-teal-200"
                        aria-label="Send message"
                    >
                        {streaming ? <LoaderCircle className="h-5 w-5 animate-spin"/> :
                            <Send className="h-5 w-5"/>}
                    </button>
                    <button
                        onClick={regenerate}
                        disabled={streaming || !messages.some((message) => message.role === "user")}
                        className="grid h-12 w-12 shrink-0 place-items-center rounded-md border border-slate-200 bg-white text-slate-700 transition hover:bg-slate-50 disabled:cursor-wait disabled:opacity-60 dark:border-white/10 dark:bg-white/10 dark:text-slate-100 dark:hover:bg-white/15"
                        aria-label="Regenerate response"
                    >
                        {streaming ? <LoaderCircle className="h-5 w-5 animate-spin"/> :
                            <RefreshCcw className="h-5 w-5"/>}
                    </button>
                </div>
            </div>
        </section>
    </main>;
};
export {
    AiChatPage
};
