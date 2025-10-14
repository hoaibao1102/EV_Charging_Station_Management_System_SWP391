import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080';  

const apiClient = axios.create({
    baseURL: API_BASE_URL,
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Thêm debug log
apiClient.interceptors.request.use((config) => {
    console.log('Making request to:', config.baseURL + config.url);  // Debug log
    console.log('Request data:', config.data);  // Debug log
    
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
        console.log('Response received:', response.status, response.data);  // Debug log
        return response;
    },
    async (error) => {
        console.error('API Error:', error.response?.status, error.response?.data);  // Debug log
        
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
                        localStorage.setItem('accessToken', accessToken);
                        
                        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
                        return apiClient(originalRequest);
                    }
                } catch (refreshError) {
                    console.error('Auto refresh token failed:', refreshError);
                    localStorage.removeItem('accessToken');
                    localStorage.removeItem('refreshToken');
                    localStorage.removeItem('userProfile');
                    window.location.href = '/login'; 
                }
            } else {
                localStorage.removeItem('accessToken');
                window.location.href = '/login';
            }
        }

        return Promise.reject(error);
    }
);

export default apiClient;