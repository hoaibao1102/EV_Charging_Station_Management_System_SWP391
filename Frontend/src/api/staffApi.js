import apiClient from './apiUrls.js';
import handleApiCall from './callApi.js';

// cập nhật mật khẩu staff
export const updateStaffPasswordApi = (passwordData) => {
    return handleApiCall(
        () => apiClient.put(`/api/staff/password`, passwordData),
        'Cập nhật mật khẩu staff thất bại'
    );
}