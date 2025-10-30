import apiClient from "./apiUrls.js";
import handleApiCall from "./callApi.js";

// lấy ra hết các trụ sạc trong hệ thống
export const getAllChargingPoints = () => {
  return handleApiCall(
    () => apiClient.get("/api/charging-points"),
    "Lấy danh sách trụ sạc thất bại"
  );
}

//thay đổi trạng thái trụ sạc
export const updateChargingPointStatus = (chargingPointId, status) => {
  return handleApiCall(
    () => apiClient.put(`/api/charging-points/stop`, { newStatus: status, pointId: chargingPointId }),
    "Cập nhật trạng thái trụ sạc thất bại"
  );
}


//thêm mới trụ sạc
export const addChargingPointApi = (chargingPointData) => {
  return handleApiCall(
    () => apiClient.post("/api/charging-points/create", chargingPointData),
    "Thêm mới trụ sạc thất bại"
  );
}
