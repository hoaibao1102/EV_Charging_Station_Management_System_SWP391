import apiClient from './apiUrls.js';
import handleApiCall from './callApi.js';

export const getProfileApi = () => {
    return handleApiCall(
        () => apiClient.get('/api/driver/profile'),
        'Lấy thông tin cá nhân thất bại'
    );
}

//update profile
export const updateProfileApi = (profileData) => {
    return handleApiCall(
        () => apiClient.put('/api/driver/profile', profileData),
        'Cập nhật thông tin cá nhân thất bại'
    );
}

//lấy ra danh sách phương tiện của tài xế
export const getMyVehiclesApi = () => {
    return handleApiCall(
        () => apiClient.get('/api/driver/vehicles'),
        'Lấy danh sách phương tiện thất bại'
    );
}

//Xóa phưong tiện của tài xế
export const deleteVehicleApi = (vehicleId) => {
    return handleApiCall(
        () => apiClient.delete(`/api/driver/vehicles/${vehicleId}`),
        'Xóa phương tiện thất bại'
    );
}

//lấy ra tất cả brand xe
export const getAllVehicleBrandsApi = () => {
    return handleApiCall(
        () => apiClient.get('/api/vehicle-models/brands'),
        'Lấy danh sách thương hiệu xe thất bại'
    );
}

//lấy ra các mẫu xe theo brand
export const getModelsByBrandApi = (brand) => {
    return handleApiCall(
        () => apiClient.get(`/api/vehicle-models/brand/models?brand=${brand}`),
        'Lấy danh sách mẫu xe theo thương hiệu thất bại'
    );
}

//add phương tiện cho driver
export const addVehicleApi = (vehicle) => {
    return handleApiCall(
        () => apiClient.post('/api/driver/vehicles', vehicle),
        'Thêm phương tiện thất bại'
    );
}

//lấy ra thông tin thông báo 
export const getNotificationsApi = () => {
    return handleApiCall(
        () => apiClient.get('/api/notifications'),
        'Lấy danh sách thông báo thất bại'
    );
}

//đánh dấu thông báo đã đọc
export const markNotificationAsReadApi = (notificationId) => {
    return handleApiCall(
        () => apiClient.put(`/api/notifications/${notificationId}/read`),
        'Đánh dấu thông báo đã đọc thất bại'
    );
}