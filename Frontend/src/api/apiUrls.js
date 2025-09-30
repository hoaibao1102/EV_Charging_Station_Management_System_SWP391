import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

const apiClient = axios.create({
    baseURL: API_BASE_URL,
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json',
    },
});

apiClient.interceptors.request.use((config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
}, (error) => {
    return Promise.reject(error);
});

apiClient.interceptors.response.use(
    (response) => {
        return response;
    },
    async (error) => {
        const originalRequest = error.config;

        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;
            
            const refreshTokenValue = localStorage.getItem('refreshToken');
            if (refreshTokenValue) {
                try {
                    const refreshResponse = await apiClient.post('/auth/refresh', { 
                        refreshToken: refreshTokenValue 
                    });
                    
                    const { accessToken } = refreshResponse.data;
                    
                    if (accessToken) {
                        localStorage.setItem('accessToken', accessToken); // Changed from 'token' to 'accessToken'
                        
                        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
                        return apiClient(originalRequest);
                    }
                } catch (refreshError) {
                    console.error('Auto refresh token failed:', refreshError);
                    localStorage.removeItem('accessToken'); // Changed from 'token' to 'accessToken'
                    localStorage.removeItem('refreshToken');
                    localStorage.removeItem('userProfile');
                    window.location.href = '/login'; 
                }
            } else {
                localStorage.removeItem('accessToken'); // Changed from 'token' to 'accessToken'
                window.location.href = '/login';
            }
        }

        return Promise.reject(error);
    }
);

export default apiClient;  