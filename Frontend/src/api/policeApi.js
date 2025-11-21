import apiClient from "./apiUrls.js";
import handleApiCall from "./callApi.js";

// Gọi API lấy danh sách điều khoản
export const getPoliceListApi = () => {
  return handleApiCall(
    () => apiClient.get("/api/policies"),
    "Lấy danh sách điều khoản thất bại"
  );
}
// Gọi API tạo điều khoản mới
export const createPoliceApi = (policy) => {
  return handleApiCall(
    () => apiClient.post("/api/policies", policy),
    "Tạo điều khoản mới thất bại"
  );
}

// Gọi API cập nhật điều khoản
export const updatePoliceApi = (policyId, policy) => {
  return handleApiCall(
    () => apiClient.put(`/api/policies/${policyId}`, policy),
    "Cập nhật điều khoản thất bại"
  );
}

// Gọi API xóa điều khoản
export const deletePoliceApi = (policyId) => {
  return handleApiCall(
    () => apiClient.delete(`/api/policies/${policyId}`),
    "Xóa điều khoản thất bại"
  );
}

// Gọi API lấy chi tiết điều khoản theo ID
export const getPoliceByIdApi = (policyId) => {
  return handleApiCall(
    () => apiClient.get(`/api/policies/${policyId}`),
    "Lấy chi tiết điều khoản thất bại"
  );
}