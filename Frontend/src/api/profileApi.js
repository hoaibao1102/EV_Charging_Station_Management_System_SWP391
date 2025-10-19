import apiClient from './apiUrls.js';
import handleApiCall from './callApi.js';

export const getProfileApi = () => {
    return handleApiCall(
        () => apiClient.get('/api/driver/own-profile-driver'),
        'Lấy thông tin cá nhân thất bại'
    );
}