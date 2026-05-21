import axios from "axios";
import {env} from "../config/env";

const apiClient = axios.create({
    baseURL: env.apiGatewayUrl,
    withCredentials: true,
    headers: {
        "Content-Type": "application/json"
    }
});
export {
    apiClient
};
