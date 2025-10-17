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
        return {
            success: false,
            message: error.response?.data?.message || defaultMessage
        };
    }
};