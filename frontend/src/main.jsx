import React from "react";
import ReactDOM from "react-dom/client";
import {QueryClient, QueryClientProvider} from "@tanstack/react-query";
import {BrowserRouter} from "react-router-dom";
import {toast, Toaster, ToastBar} from "react-hot-toast";
import {X} from "lucide-react";
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
                >
                    {(currentToast) => <ToastBar toast={currentToast}>
                        {({icon, message}) => <div className="app-toast-content">
                            <div className="app-toast-message">
                                {icon}
                                {message}
                            </div>
                            <button
                                type="button"
                                aria-label="Close notification"
                                className="app-toast-close"
                                onClick={() => toast.dismiss(currentToast.id)}
                            >
                                <X aria-hidden="true" className="app-toast-close-mark"/>
                            </button>
                        </div>}
                    </ToastBar>}
                </Toaster>
            </BrowserRouter>
        </QueryClientProvider>
    </React.StrictMode>
);
