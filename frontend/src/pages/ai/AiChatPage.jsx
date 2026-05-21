import {Copy, LoaderCircle, Plus, RefreshCcw, Send} from "lucide-react";
import {useQuery} from "@tanstack/react-query";
import {useEffect, useRef, useState} from "react";
import ReactMarkdown from "react-markdown";
import {apiClient} from "../../api/axiosInstance";
import {endpoints} from "../../api/endpoints";
import {asArray, extractAssistantContent, unwrapApiData} from "../../utils/responseUtils";

const AiChatPage = () => {
    const [sessions, setSessions] = useState([{
        id: crypto.randomUUID(),
        title: "New chat"
    }]);
    const [activeSession, setActiveSession] = useState(sessions[0].id);
    const [messages, setMessages] = useState([]);
    const [input, setInput] = useState("");
    const [streaming, setStreaming] = useState(false);
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
        timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
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
        if (sessionsQuery.data?.length) {
            setSessions(sessionsQuery.data);
            setActiveSession((current) => sessionsQuery.data.some((session) => session.id === current) ? current : sessionsQuery.data[0].id);
        }
    }, [sessionsQuery.data]);
    useEffect(() => {
        if (messagesQuery.data) {
            setMessages(messagesQuery.data);
        }
    }, [messagesQuery.data]);
    const createSession = async () => {
        const response = await apiClient.post(endpoints.ai.createSession, {title: "New chat"});
        const created = unwrapApiData(response.data);
        const session = {
            id: created.id ?? crypto.randomUUID(),
            title: created.title ?? "New chat",
            createdAt: created.createdAt
        };
        setSessions((current) => [session, ...current]);
        setActiveSession(session.id);
        setMessages([]);
    };
    const sendMessage = async (content) => {
        if (!content.trim() || streaming) {
            return;
        }
        const userMessage = {id: crypto.randomUUID(), role: "user", content};
        const assistantId = crypto.randomUUID();
        setMessages((current) => [...current, userMessage, {
            id: assistantId,
            role: "assistant",
            content: ""
        }]);
        setInput("");
        setStreaming(true);
        try {
            const location = await readLocation();
            const response = await apiClient.post(endpoints.ai.chat, {
                sessionId: activeSession,
                message: userMessage.content,
                stream: false,
                location,
                context: userContext()
            });
            const text = extractAssistantContent(response.data) || "I could not read a usable response from the AI provider.";
            setMessages((current) => current.map((message) => message.id === assistantId ? {
                ...message,
                content: text
            } : message));
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
    return <main className="grid min-h-[calc(100vh-4rem)] flex-1 grid-cols-[260px_1fr]">
        <aside className="border-r border-slate-200 bg-white p-4">
            <button
                onClick={() => void createSession()}
                className="mb-4 flex w-full items-center justify-center gap-2 rounded-md bg-slate-950 px-3 py-2 text-sm font-medium text-white"
            >
                <Plus className="h-4 w-4"/> New chat
            </button>
            <div className="space-y-1">
                {sessions.map((session) => <button
                    key={session.id}
                    onClick={() => setActiveSession(session.id)}
                    className={`w-full rounded-md px-3 py-2 text-left text-sm ${session.id === activeSession ? "bg-slate-100 font-medium" : "hover:bg-slate-50"}`}
                >
                    {session.title}
                </button>)}
            </div>
        </aside>
        <section className="flex min-w-0 flex-col">
            <div className="flex-1 space-y-4 overflow-y-auto p-6">
                {messages.map((message) => <div
                    key={message.id}
                    className={`max-w-3xl rounded-md px-4 py-3 ${message.role === "user" ? "ml-auto bg-slate-950 text-white" : "bg-white text-slate-900 shadow-sm"}`}
                >
                    {message.role === "assistant" ?
                        <ReactMarkdown>{message.content || (streaming ? "Typing..." : "")}</ReactMarkdown> : message.content}
                    {message.role === "assistant" && message.content && <button
                        onClick={() => navigator.clipboard.writeText(message.content)}
                        className="mt-2 inline-flex items-center gap-1 text-xs text-slate-500"
                    >
                        <Copy className="h-3 w-3"/> Copy
                    </button>}
                </div>)}
            </div>
            <div className="border-t bg-white p-4">
                <div className="mx-auto flex max-w-4xl gap-2">
                        <textarea
                            value={input}
                            onChange={(event) => setInput(event.target.value)}
                            rows={2}
                            className="min-h-12 flex-1 resize-none rounded-md border px-3 py-2"
                            placeholder="Message AI"
                        />
                    <button
                        onClick={send}
                        disabled={streaming || !input.trim()}
                        className="grid h-12 w-12 place-items-center rounded-md bg-slate-950 text-white transition hover:bg-slate-800 disabled:cursor-wait disabled:opacity-60"
                        aria-label="Send message"
                    >
                        {streaming ? <LoaderCircle className="h-5 w-5 animate-spin"/> :
                            <Send className="h-5 w-5"/>}
                    </button>
                    <button
                        onClick={regenerate}
                        disabled={streaming || !messages.some((message) => message.role === "user")}
                        className="grid h-12 w-12 place-items-center rounded-md border bg-white transition hover:bg-slate-50 disabled:cursor-wait disabled:opacity-60"
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
