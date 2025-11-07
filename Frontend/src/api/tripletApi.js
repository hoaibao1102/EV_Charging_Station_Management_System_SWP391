import apiClient from "./apiUrls.js";
import handleApiCall from "./callApi.js";

// ====== Lấy tất cả các ban account ======
export const getAllTriplets = async () => {
  return await handleApiCall(
    () => apiClient.get('/api/triplets/all'),
    'Lấy danh sách triplet thất bại'
  );
};

// ====== Cập nhật trạng thái ban ======
export const updateTripletStatus = async (tripletId) => {
  return await handleApiCall(
    () => apiClient.put(`/api/triplets/${tripletId}/pay`),
    'Cập nhật trạng thái triplet thất bại'
  );
};