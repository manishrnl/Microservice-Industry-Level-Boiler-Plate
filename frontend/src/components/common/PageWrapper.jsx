const PageWrapper = ({title, children}) => <main
    className="mx-auto w-full max-w-7xl px-6 py-6">
    <h1 className="mb-5 text-2xl font-semibold text-slate-950">{title}</h1>
    {children}
</main>;
export {
    PageWrapper
};
