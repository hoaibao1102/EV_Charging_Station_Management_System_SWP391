import apiClient from './apiUrls.js';
import handleApiCall from './callApi.js';

//lấy ra tất cả users
export const getAllUsersApi = () => {
    return handleApiCall(
        () => apiClient.get('/api/admin/all-users'),
        'Lấy danh sách người dùng thất bại'
    );
}
