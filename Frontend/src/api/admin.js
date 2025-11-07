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


//lấy ra bảng nhân viên - user
export const getStaffs_UserApi = () => {
    return handleApiCall(
        () => apiClient.get('/api/admin/all-staffs'),
        'Lấy danh sách nhân viên thất bại'
    );
}

//lấy ra bảng nhân viên - trạm
export const getStaffs_StationApi = () => {
    return handleApiCall(
        () => apiClient.get('/api/station-staff'),
        'Lấy danh sách nhân viên trạm thất bại'
    );
}

//chuyển công tác nhân viên
export const transferStaffApi = (staffId, newStationId) => {
    return handleApiCall(
        () => apiClient.put(`/api/station-staff/${staffId}/station?stationId=${newStationId}`),
        'Chuyển công tác nhân viên thất bại'
    );
}

//gỡ lệnh khóa tài khoản tài xế
export const unbanDriverApi = (driverId) => {
    return handleApiCall(
        () => apiClient.put(`/api/admin/${driverId}/status-divers?status=ACTIVE`),
        'Gỡ lệnh khóa tài xế thất bại'
    );
}

//lay ra danh sách tai nạn
export const getAllAccidentsApi = () => {
    return handleApiCall(
        () => apiClient.get('/api/incidents'),
        'Lấy danh sách tai nạn thất bại'
    );
}

//đánh dấu tai nạn đã xử lý
export const markAccidentAsResolvedApi = (incidentId) => {
    return handleApiCall(
        () => apiClient.post(`/api/incidents/${incidentId}/status?status=RESOLVED`),
        'Đánh dấu tai nạn đã xử lý thất bại'
    );
}


//lấy dữ liệu thống kê tổng quan
export const getAdminStatisticsApi = () => {
    return handleApiCall(
        () => apiClient.get('/api/statics/dashboard'),
        'Lấy dữ liệu thống kê tổng quan thất bại'
    );
}

