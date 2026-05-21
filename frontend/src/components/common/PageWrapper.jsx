const PageWrapper = ({title, children}) => <main
    className="mx-auto w-full max-w-7xl overflow-x-hidden px-4 py-5 sm:px-6 sm:py-6">
    <h1 className="mb-5 text-xl font-semibold text-slate-950 dark:text-white sm:text-2xl">{title}</h1>
    {children}
</main>;
export {
    PageWrapper
};
