import {Outlet} from "react-router-dom";
import {MobileNav} from "./MobileNav";
import {PublicFooter} from "./PublicFooter";
import {Topbar} from "./Topbar";

const AppLayout = () => <div className="app-shell min-h-screen bg-slate-50 text-slate-950 dark:bg-slate-950 dark:text-slate-100">
    <Topbar/>
    <div className="flex min-h-[calc(100vh-4rem)] pb-20 md:pb-0">
        <main className="min-w-0 flex-1">
            <Outlet/>
        </main>
    </div>
    <PublicFooter/>
    <MobileNav/>
</div>;
export {
    AppLayout
};
