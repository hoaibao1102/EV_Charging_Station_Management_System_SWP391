import { clearAuthData } from '../utils/authUtils.js';

export default async function handleApiCall(apiCall, defaultMessage) {
    try {
        const response = await apiCall();
        if (response.config.url.includes('/logout')) {
            clearAuthData();
        }

        return {
            success: true,
            data: response.data,
        };
    } catch (error) {
        console.error('API error:', error);
        const errorMessage = error.response?.data?.message || error.response?.data || defaultMessage;
        return {
            success: false,
            message: errorMessage
        };
    }
};