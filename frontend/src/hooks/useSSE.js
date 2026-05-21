import {useEffect, useRef, useState} from "react";

const useSSE = (url, options = {}) => {
    const {
        enabled = true,
        eventName = "message",
        headers,
        onMessage,
        onError,
        maxRetries = 5
    } = options;
    const [connected, setConnected] = useState(false);
    const [error, setError] = useState(null);
    const [lastEvent, setLastEvent] = useState(null);
    const retryRef = useRef(0);
    const controllerRef = useRef(null);
    useEffect(() => {
        if (!enabled || !url) {
            return;
        }
        let closed = false;
        const connect = async () => {
            const controller = new AbortController();
            controllerRef.current = controller;
            try {
                const response = await fetch(url, {
                    credentials: "include",
                    headers: typeof headers === "function" ? headers() : headers,
                    signal: controller.signal
                });
                if (!response.ok || !response.body) {
                    throw new Error(`SSE connection failed with status ${response.status}`);
                }
                retryRef.current = 0;
                setConnected(true);
                await readStream(response.body, eventName, (data) => {
                    setLastEvent(data);
                    onMessage?.(data);
                });
            } catch (caught) {
                if (closed || controller.signal.aborted) {
                    return;
                }
                const nextError = caught instanceof Error ? caught : new Error("SSE connection failed");
                setConnected(false);
                setError(nextError);
                onError?.(nextError);
                if (retryRef.current < maxRetries) {
                    retryRef.current += 1;
                    window.setTimeout(connect, 500 * retryRef.current ** 2);
                }
            }
        };
        void connect();
        return () => {
            closed = true;
            controllerRef.current?.abort();
            setConnected(false);
        };
    }, [enabled, eventName, headers, maxRetries, onError, onMessage, url]);
    return {connected, error, lastEvent};
};
const readStream = async (body, eventName, onData) => {
    const reader = body.getReader();
    const decoder = new TextDecoder();
    let buffer = "";
    while (true) {
        const {value, done} = await reader.read();
        if (done) {
            break;
        }
        buffer += decoder.decode(value, {stream: true});
        const frames = buffer.split(/\r?\n\r?\n/);
        buffer = frames.pop() ?? "";
        frames.forEach((frame) => parseFrame(frame, eventName, onData));
    }
};
const parseFrame = (frame, eventName, onData) => {
    const lines = frame.split(/\r?\n/);
    const event = lines.find((line) => line.startsWith("event:"))?.slice(6).trim() ?? "message";
    if (event !== eventName) {
        return;
    }
    const data = lines.filter((line) => line.startsWith("data:")).map((line) => line.slice(5).trimStart()).join("\n");
    if (!data) {
        return;
    }
    onData(JSON.parse(data));
};
export {
    useSSE
};
