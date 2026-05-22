import React from "react";
import ReactDOM from "react-dom/client";
import {QueryClient, QueryClientProvider} from "@tanstack/react-query";
import {BrowserRouter} from "react-router-dom";
import {Toaster} from "react-hot-toast";
import "./api/axiosInterceptor";
import {App} from "./App";
import "./index.css";

const queryClient = new QueryClient();
ReactDOM.createRoot(document.getElementById("root")).render(
    <React.StrictMode>
        <QueryClientProvider client={queryClient}>
            <BrowserRouter future={{v7_relativeSplatPath: true, v7_startTransition: true}}>
                <App/>
                <Toaster
                    position="bottom-right"
                    gutter={8}
                    containerStyle={{
                        bottom: 16,
                        right: 16
                    }}
                    toastOptions={{
                        className: "app-glass-toast",
                        duration: 4200,
                        success: {
                            className: "app-glass-toast app-glass-toast-success"
                        },
                        error: {
                            className: "app-glass-toast app-glass-toast-error",
                            duration: 5600
                        }
                    }}
                />
            </BrowserRouter>
        </QueryClientProvider>
    </React.StrictMode>
);
