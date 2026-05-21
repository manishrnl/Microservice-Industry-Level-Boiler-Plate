import {Link} from "react-router-dom";

const NotFoundPage = () => <main className="grid min-h-screen place-items-center">
    <div className="text-center">
        <h1 className="text-3xl font-semibold">Not found</h1>
        <Link to="/" className="mt-4 inline-block text-blue-700">Go home</Link>
    </div>
</main>;
export {
    NotFoundPage
};
