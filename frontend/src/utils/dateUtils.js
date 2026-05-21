import {formatDistanceToNow} from "date-fns";

const relativeTime = (value) => `${formatDistanceToNow(new Date(value))} ago`;
export {
    relativeTime
};
