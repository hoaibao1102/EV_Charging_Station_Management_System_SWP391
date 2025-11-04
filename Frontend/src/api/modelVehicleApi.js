import apiClient from "./apiUrls.js";
import handleApiCall from "./callApi.js";

// ====== Lấy tất cả các mẫu xe ======
export const getAllVehicleModels = () => {
  return handleApiCall(
    () => apiClient.get("/api/vehicle-models/models"),
    "Lấy danh sách mẫu xe thất bại"
  );
}

// ====== Thay đổi trạng thái mẫu xe ======
export const changeStatusModelApi = (modelId, status) => {
  return handleApiCall(
    () => apiClient.patch(`/api/vehicle-models/${modelId}/status`, {'status': status }),
    "Thay đổi trạng thái mẫu xe thất bại"
  );
}
 
// ====== Thêm mẫu xe mới ======
export const addVehicleModelApi = (modelData) => {
  return handleApiCall(
    () => apiClient.post('/api/vehicle-models', modelData),
    "Thêm mẫu xe thất bại"
  );
}

// ====== Cập nhật mẫu xe ======
export const updateVehicleModelApi = (modelId, modelData) => {
  return handleApiCall(
    () => apiClient.put(`/api/vehicle-models/${modelId}`, modelData),
    "Cập nhật mẫu xe thất bại"
  );
}

