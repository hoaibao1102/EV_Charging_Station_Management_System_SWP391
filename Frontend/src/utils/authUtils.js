// Chỉ lưu 2 thông tin tối thiểu này để duy trì phiên
const TOKEN_KEY = "accessToken";
const ROLE_KEY = "role";

export const getAuthData = () => {
    return {
        accessToken: localStorage.getItem(TOKEN_KEY),
        role: localStorage.getItem(ROLE_KEY),
    };
};

export const setAuthData = ({ accessToken, role }) => {
    localStorage.setItem(TOKEN_KEY, accessToken);
    localStorage.setItem(ROLE_KEY, role);
};

export const clearAuthData = () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(ROLE_KEY);
};

export const isAuthenticated = () => {
    const token = localStorage.getItem(TOKEN_KEY);
    return !!token;
}



