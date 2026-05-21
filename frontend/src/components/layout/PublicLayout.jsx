import {Outlet} from "react-router-dom";
import {PublicFooter} from "./PublicFooter";
import {PublicHeader} from "./PublicHeader";

const PublicLayout = () => <div className="min-h-screen bg-slate-50 text-slate-950 dark:bg-slate-950 dark:text-white">
    <PublicHeader/>
    <Outlet/>
    <PublicFooter/>
</div>;

export {
    PublicLayout
};
