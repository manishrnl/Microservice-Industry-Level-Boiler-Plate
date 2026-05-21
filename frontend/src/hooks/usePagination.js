import {useMemo, useState} from "react";

const usePagination = (initialPage = 0, initialSize = 20) => {
    const [page, setPage] = useState(initialPage);
    const [size, setSize] = useState(initialSize);
    return useMemo(() => ({
        page,
        size,
        setPage,
        setSize,
        next: () => setPage((value) => value + 1),
        previous: () => setPage((value) => Math.max(0, value - 1))
    }), [page, size]);
};
export {
    usePagination
};
