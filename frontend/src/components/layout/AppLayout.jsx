import {Outlet} from "react-router-dom";
import {Sidebar} from "./Sidebar";
import {Topbar} from "./Topbar";

const AppLayout = () => <div className="min-h-screen">
    <Topbar/>
    <div className="flex min-h-[calc(100vh-4rem)]">
        <Sidebar/>
        <Outlet/>
    </div>
</div>;
export {
    AppLayout
};
