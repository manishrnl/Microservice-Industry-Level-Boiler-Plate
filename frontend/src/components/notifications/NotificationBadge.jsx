const NotificationBadge = ({count}) => {
    if (count <= 0) {
        return null;
    }
    return <span
        className="absolute -right-1 -top-1 flex h-5 min-w-5 items-center justify-center rounded-full bg-red-600 px-1 text-[11px] font-semibold text-white transition-all"
    >
      {count > 99 ? "99+" : count}
    </span>;
};
export {
    NotificationBadge
};
