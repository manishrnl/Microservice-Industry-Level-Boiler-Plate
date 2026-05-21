import {Link} from "react-router-dom";
import {ShieldAlert} from "lucide-react";

const AccessDeniedPage = () => <main
    className="grid min-h-screen place-items-center bg-slate-50 px-6">
    <section
        className="w-full max-w-md rounded-md border border-slate-200 bg-white p-8 text-center shadow-sm"
    >
        <ShieldAlert className="mx-auto h-10 w-10 text-amber-600"/>
        <h1 className="mt-4 text-xl font-semibold text-slate-950">Access denied</h1>
        <p className="mt-2 text-sm text-slate-600">Your account does not have permission to
            open this page.</p>
        <Link
            to="/"
            className="mt-5 inline-flex h-10 items-center rounded-md bg-slate-950 px-4 text-sm font-medium text-white hover:bg-slate-800"
        >
            Go to dashboard
        </Link>
    </section>
</main>;
export {
    AccessDeniedPage
};
