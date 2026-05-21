import {useEffect, useRef} from "react";
import {useNavigate} from "react-router-dom";
import {apiClient} from "../../api/axiosInstance";
import {endpoints} from "../../api/endpoints";
import {Loader} from "../../components/common/Loader";
import {useAuthStore} from "../../store/authStore";
import {unwrapApiData} from "../../utils/responseUtils";
import {authUserFromToken} from "../../utils/tokenUtils";

const OAuthCallbackPage = () => {
    const setAuth = useAuthStore((state) => state.setAuth);
    const navigate = useNavigate();
    const handled = useRef(false);
    useEffect(() => {
        if (handled.current) {
            return;
        }
        handled.current = true;
        const token = new URLSearchParams(window.location.hash.replace("#", "")).get("access_token");
        if (token) {
            const fallbackUser = authUserFromToken(token);
            if (fallbackUser) {
                setAuth(fallbackUser, token);
            }
            apiClient.get(endpoints.auth.me, {headers: {Authorization: `Bearer ${token}`}}).then((response) => {
                const payload = unwrapApiData(response.data);
                setAuth(payload.user, payload.accessToken ?? token);
                navigate("/app/dashboard", {replace: true});
            }).catch(() => {
                if (fallbackUser) {
                    setAuth(fallbackUser, token);
                    navigate("/app/dashboard", {replace: true});
                    return;
                }
                navigate("/login", {replace: true});
            });
        } else {
            navigate("/login", {replace: true});
        }
    }, [navigate, setAuth]);
    return <Loader variant="fullscreen" message="Finishing sign in"/>;
};
export {
    OAuthCallbackPage
};
