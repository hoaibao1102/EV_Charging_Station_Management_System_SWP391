import apiClient from './apiUrls.js';
import handleApiCall from './callApi.js';

export const getProfileApi = () => {
    return handleApiCall(
        () => apiClient.get('/api/driver/own-profile-driver'),
        'Lấy thông tin cá nhân thất bại'
    );
}

//update profile
export const updateProfileApi = (profileData) => {
    return handleApiCall(
        () => apiClient.put('/api/driver/updateProfile', profileData),
        'Cập nhật thông tin cá nhân thất bại'
    );
}

//lấy ra danh sách phương tiện của tài xế
export const getMyVehiclesApi = () => {
    return handleApiCall(
        () => apiClient.get('/api/driver/me/vehicles'),
        'Lấy danh sách phương tiện thất bại'
    );
}

//Xóa phưong tiện của tài xế
export const deleteVehicleApi = (vehicleId) => {
    return handleApiCall(
        () => apiClient.delete(`/api/driver/me/vehicles/${vehicleId}`),
        'Xóa phương tiện thất bại'
    );
}

//lấy ra tất cả brand xe
export const getAllVehicleBrandsApi = () => {
    return handleApiCall(
        () => apiClient.get('/api/vehicle-models/list-brands'),
        'Lấy danh sách thương hiệu xe thất bại'
    );
}

//lấy ra các mẫu xe theo brand
export const getModelsByBrandApi = (brand) => {
    return handleApiCall(
        () => apiClient.get(`/api/vehicle-models?brand=${brand}`),
        'Lấy danh sách mẫu xe theo thương hiệu thất bại'
    );
}

//add phương tiện cho driver
export const addVehicleApi = (vehicle) => {
    return handleApiCall(
        () => apiClient.post('/api/driver/me/vehicles', vehicle),
        'Thêm phương tiện thất bại'
    );
}

//lấy ra thông tin thông báo 
export const getNotificationsApi = () => {
    return handleApiCall(
        () => apiClient.get('/api/notifications/unread'),
        'Lấy danh sách thông báo thất bại'
    );
}