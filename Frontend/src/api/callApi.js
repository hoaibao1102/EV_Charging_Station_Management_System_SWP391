import { clearAuthData } from '../utils/authUtils.js';

export default async function handleApiCall(apiCall, defaultMessage) {
    try {
        const response = await apiCall();
        
        // Xử lý dọn dẹp đặc biệt cho Logout 
        if (response.config.url.includes('/logout')) {
            clearAuthData();
        }

        return {
            success: true,
            data: response.data
        };
    } catch (error) {
        console.error('API error:', error);
        // Backend trả data trực tiếp là string, không phải object {message: ...}
        const errorMessage = error.response?.data?.message || error.response?.data || defaultMessage;
        return {
            success: false,
            message: errorMessage
        };
    }
};