import apiClient from './apiUrls.js';  // Import apiClient đã cấu hình sẵn

// Gọi API đăng nhập
export const loginApi = async (phone, password) => {
    const response = await apiClient.post('/users/login', {
      phoneNumber: phone,
      password: password,
    });
    return response.data;
};

// Gọi API đăng ký
export const registerApi = async (formData) => {
    const response = await apiClient.post('/users/register', formData);
    return response.data;
};
