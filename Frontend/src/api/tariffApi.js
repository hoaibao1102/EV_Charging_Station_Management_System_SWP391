import apiClient from "./apiUrls.js";
import handleApiCall from "./callApi.js";

// ====== Lấy tất cả các biểu phí ======
export const getAllTariffs = () => {
  return handleApiCall(
    () => apiClient.get('/api/tariffs'),
    "Lấy danh sách biểu phí thất bại"
  );
};

// ====== Thêm biểu phí mới ======
export const addTariffApi = (tariffData) => {
  return handleApiCall(
    () => apiClient.post('/api/tariffs', tariffData),
    "Thêm biểu phí thất bại"
  );
}

// ====== Cập nhật biểu phí ======
export const updateTariffApi = (tariffId, tariffData) => {
  return handleApiCall(
    () => apiClient.put(`/api/tariffs/${tariffId}`, tariffData),
    "Cập nhật biểu phí thất bại"
  );
}