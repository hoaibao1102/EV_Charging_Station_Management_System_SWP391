import axios from "axios";
import { clearAuthData } from "../utils/authUtils.js";

const API_BASE_URL = "http://localhost:8080"; // Thay đổi theo cấu hình của bạn

// Khởi tạo Axios Instance
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
  },
});

apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("accessToken");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

apiClient.interceptors.response.use(
  (response) => {
    return response;
  },
  async (error) => {
    const status = error.response?.status;
    const requestUrl = error.config?.url || "";

    // Danh sách các public endpoints không cần redirect khi 401
    const publicEndpoints = [
      "/api/charging-stations",
      "/api/connector-types",
      "/api/charging-points/station",
      "/api/policies", // Thêm API điều khoản vào public
    ];

    const isPublicEndpoint = publicEndpoints.some((endpoint) =>
      requestUrl.includes(endpoint)
    );

    if (status === 401 && !isPublicEndpoint) {
      console.log("Token hết hạn, tự động đăng xuất...");

      // Sử dụng hàm chung để xóa thông tin đăng nhập
      clearAuthData();

      // Chuyển về trang login
      window.location.href = "/login";
    }

    return Promise.reject(error);
  }
);

export default apiClient;
