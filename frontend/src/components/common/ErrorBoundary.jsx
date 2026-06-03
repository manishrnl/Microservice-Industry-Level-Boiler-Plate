import {Component} from "react";

class ErrorBoundary extends Component {
    state = {hasError: false};

    static getDerivedStateFromError() {
        return {hasError: true};
    }

    componentDidCatch(error, info) {
        console.error(
            "%c ERROR %c React render failed ",
            "background:#dc2626;color:white;font-weight:800;padding:4px 8px;border-radius:5px 0 0 5px;",
            "background:#fee2e2;color:#7f1d1d;font-weight:700;padding:4px 8px;border-radius:0 5px 5px 0;",
            error,
            info
        );
    }

    render() {
        if (this.state.hasError) {
            return <div className="p-8 text-sm text-red-700">Something went wrong.</div>;
        }
        return this.props.children;
    }
}

export {
    ErrorBoundary
};
