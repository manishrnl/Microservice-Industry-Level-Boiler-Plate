import {Component} from "react";

class ErrorBoundary extends Component {
    state = {hasError: false};

    static getDerivedStateFromError() {
        return {hasError: true};
    }

    componentDidCatch(error, info) {
        console.error(error, info);
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
