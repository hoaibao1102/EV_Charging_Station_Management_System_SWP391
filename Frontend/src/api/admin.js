import apiClient from './apiUrls.js';
import handleApiCall from './callApi.js';

//lấy ra tất cả users
export const getAllUsersApi = () => {
    return handleApiCall(
        () => apiClient.get('/api/admin/all-users'),
        'Lấy danh sách người dùng thất bại'
    );
}

//thêm nhân viên mới
export const addStaffApi = (staffData) => {
    return handleApiCall(
        () => apiClient.post('/api/admin/register-staff', staffData),
        'Thêm nhân viên thất bại'
    );
}

//nhân viên nghỉ, status = BANNED, nhân viên quay lại làm việc, status = ACTIVE
export const statusStaffApi = (staffId, status) => {
    return handleApiCall(
        () => apiClient.put(`/api/admin/${staffId}/status-staffs?status=${status}`),
        status === 'BANNED' ? 'Xóa nhân viên thất bại' : 'Kích hoạt nhân viên thất bại'
    );
}

//gỡ lệnh khóa tài khoản tài xế
export const unbanDriverApi = (driverId) => {
    return handleApiCall(
        () => apiClient.put(`/api/admin/${driverId}/status-divers?status=ACTIVE`),
        'Gỡ lệnh khóa tài xế thất bại'
    );
}


