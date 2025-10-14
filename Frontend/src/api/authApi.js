import apiClient from './apiUrls.js';

// Gọi API đăng nhập
export const loginApi = async (phone, password) => {
    try {
        const response = await apiClient.post('/api/users/login', {  
            phoneNumber: phone,
            password: password,
        });
        return {
            success: true,
            data: response.data
        };
    } catch (error) {
        console.error('Login API error:', error);
        return {
            success: false,
            message: error.response?.data?.message || 'Đăng nhập thất bại'
        };
    }
};

// Gọi API đăng ký
export const registerApi = async (formData) => {
    try {
        const response = await apiClient.post('/api/users/register', formData);  
        return {
            success: true,
            data: response.data
        };
    } catch (error) {
        console.error('Register API error:', error);
        return {
            success: false,
            message: error.response?.data?.message || 'Đăng ký thất bại'
        };
    }
};

// Thêm API logout nếu cần
export const logoutApi = async () => {
    try {
        const response = await apiClient.post('/api/users/logout');
        return {
            success: true,
            data: response.data
        };
    } catch (error) {
        console.error('Logout API error:', error);
        return {
            success: false,
            message: error.response?.data?.message || 'Đăng xuất thất bại'
        };
    }
};
