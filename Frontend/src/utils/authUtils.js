// Chỉ lưu 2 thông tin tối thiểu này để duy trì phiên
const TOKEN_KEY = "accessToken";
const ROLE_KEY = "role";
const USER_DETAILS_KEY = "userDetails";

export const getAuthData = () => {
    return {
        accessToken: localStorage.getItem(TOKEN_KEY),
        role: localStorage.getItem(ROLE_KEY),
    };
};

export const setAuthData = ({ accessToken, role, userDetails }) => {
    localStorage.setItem(TOKEN_KEY, accessToken);
    localStorage.setItem(ROLE_KEY, role);
    localStorage.setItem(USER_DETAILS_KEY, JSON.stringify(userDetails));
};

export const clearAuthData = () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(ROLE_KEY);
    localStorage.removeItem(USER_DETAILS_KEY);
};

export const isAuthenticated = () => {
    const token = localStorage.getItem(TOKEN_KEY);
    return !!token;
}



