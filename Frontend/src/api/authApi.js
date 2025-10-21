import apiClient from './apiUrls.js';
import handleApiCall from './callApi.js';

export const loginApi = (phone, password) => {
    return handleApiCall(
        () => apiClient.post('/api/users/login', { phoneNumber: phone, password: password }),
        'Đăng nhập thất bại'
    );
};


export const verifyOtp = (otp, profile) => {
    return handleApiCall(
        () => apiClient.post('/api/users/register/verify?otp=' + otp, profile),
        'Xác thực OTP thất bại'
    );
};


// Gọi API đăng ký
export const registerApi = (formData) => {
    // Truyền hàm gọi API (callback) và thông báo lỗi mặc định
    return handleApiCall(
        () => apiClient.post('/api/users/register', formData),
        'Đăng ký thất bại'
    );
};

// Gọi API đăng xuất
export const logoutApi = () => {
    return handleApiCall(
        () => apiClient.post('/api/users/logout'),
        'Đăng xuất thất bại'
    );
};


