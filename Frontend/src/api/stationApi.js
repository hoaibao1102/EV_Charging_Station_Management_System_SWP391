import apiClient from "./apiUrls.js";
import handleApiCall from "./callApi.js";

// ====== Lấy tất cả loại connector ======
export const getConnectorTypes = () => {
  return handleApiCall(
    () => apiClient.get("/api/connector-types"),
    "Lấy thông tin loại cổng sạc thất bại"
  );
};

//update connector type
export const updateConnectorTypeApi = (connectorTypeId, connectorTypeData) => {
  return handleApiCall(
    () =>
      apiClient.put(
        `/api/connector-types/${connectorTypeId}`,
        connectorTypeData
      ),
    "Cập nhật thông tin loại cổng sạc thất bại"
  );
};

//tạo mới connector type
export const addConnectorTypeApi = (connectorTypeData) => {
  return handleApiCall(
    () => apiClient.post("/api/connector-types", connectorTypeData),
    "Thêm mới loại cổng sạc thất bại"
  );
};

// ====== Lấy danh sách tất cả trạm sạc ======
export const getAllStations = () => {
  return handleApiCall(
    () => apiClient.get("/api/charging-stations"),
    "Lấy danh sách trạm sạc thất bại"
  );
};

//thay đổi status trạm sạc
export const updateStationStatus = (stationId, status) => {
  return handleApiCall(
    () =>
      apiClient.put(
        `/api/charging-stations/${stationId}/status?status=${status}`
      ),
    "Cập nhật trạng thái trạm sạc thất bại"
  );
};

//thêm mới trạm sạc
export const addStationApi = (stationData) => {
  return handleApiCall(
    () => apiClient.post("/api/charging-stations", stationData),
    "Thêm mới trạm sạc thất bại"
  );
};

//sửa thông tin trạm sạc
export const updateStationApi = (stationId, stationData) => {
  return handleApiCall(
    () => apiClient.put(`/api/charging-stations/${stationId}`, stationData),
    "Cập nhật thông tin trạm sạc thất bại"
  );
};

//lay het thong tin slot config
export const getAllSlotConfigs = () => {
  return handleApiCall(
    () => apiClient.get("/api/slot-configs"),
    "Lấy danh sách cấu hình slot thất bại"
  );
};

//them cau hinh moi cho slot
export const addSlotConfigApi = (slotConfigData) => {
  return handleApiCall(
    () => apiClient.post("/api/slot-configs", slotConfigData),
    "Thêm mới cấu hình slot thất bại"
  );
};

// ====== Lấy thông tin trạm theo ID ======
export const getStationById = (id) => {
  return handleApiCall(
    () => apiClient.get(`/api/charging-stations/${id}`),
    "Lấy thông tin trạm sạc thất bại"
  );
};

// ====== Lấy danh sách trụ sạc theo StationID ======
export const getChargingPointsByStationId = (stationId) => {
  return handleApiCall(
    () => apiClient.get(`/api/charging-points/station/${stationId}`),
    "Lấy thông tin trụ sạc thất bại"
  );
};

// ====== Lấy danh sách slot sạc theo trụ sạc ======
export const getAvaila = (pointId) => {
  return handleApiCall(
    () => apiClient.get(`/api/slot-availability/${pointId}`),
    "Lấy danh sách slot sạc thất bại"
  );
};

// ====== Lấy danh sách slot sạc theo trụ sạc ======
export const getTemplate = (templateId) => {
  return handleApiCall(
    () => apiClient.get(`/api/slot-templates/${templateId}`),
    "Lấy danh sách slot sạc thất bại"
  );
};

// ====== Lấy chi tiết booking theo ID ======
export const getBookingById = (bookingId) => {
  return handleApiCall(
    () => apiClient.get(`/api/bookings/${bookingId}`),
    "Lấy thông tin booking thất bại"
  );
};

// ====== Xác nhận booking (trả về QR image binary) ======
export const confirmBooking = async (bookingId) => {
  try {
    // The confirm endpoint returns an image (PNG). Request binary data.
    const response = await apiClient.put(
      `/api/bookings/${bookingId}/confirm`,
      null,
      { responseType: "arraybuffer" }
    );

    return {
      success: true,
      data: response.data,
      headers: response.headers,
      status: response.status,
    };
  } catch (error) {
    console.error("confirmBooking API error:", error);
    const errorData = error.response?.data;
    const errorStatus = error.response?.status;
    const message =
      (errorData && (errorData.message || JSON.stringify(errorData))) ||
      "Xác nhận booking thất bại";
    return {
      success: false,
      message,
      status: errorStatus,
      errorData,
    };
  }
};
// ====== Tạo booking mới ======
export const createBooking = (payload) => {
  return handleApiCall(
    () => apiClient.post(`/api/bookings/create`, payload),
    "Tạo booking thất bại"
  );
};

// ====== Hủy booking ======
export const cancelBooking = (bookingId) => {
  return handleApiCall(
    () => apiClient.put(`/api/bookings/${bookingId}/cancel`),
    "Hủy booking thất bại"
  );
};

// ====== Khởi động phiên sạc ======
export const startChargingSession = (payload) => {
  return handleApiCall(
    () => apiClient.post("/api/charging-sessions/start", payload),
    "Khởi động phiên sạc thất bại"
  );
};

// ====== Lấy phiên sạc hiện tại ======
export const getCurrentChargingSession = () => {
  return handleApiCall(
    () => apiClient.get("/api/charging-sessions/charging-sessions/current"),
    "Lấy thông tin phiên sạc hiện tại thất bại"
  );
};

// ====== Dừng phiên sạc (driver) ======
export const stopChargingSession = (sessionId, finalSoc = null) => {
  const payload = { sessionId };
  if (finalSoc !== null && finalSoc !== undefined) {
    payload.finalSoc = Math.round(finalSoc); // Làm tròn SOC về số nguyên
  }
  return handleApiCall(
    () => apiClient.post("/api/charging-sessions/driver-stop", payload),
    "Dừng phiên sạc thất bại"
  );
};

// ====== Lấy thông tin session theo ID (bất kể status) ======
export const getChargingSessionById = (sessionId) => {
  return handleApiCall(
    () => apiClient.get(`/api/charging-sessions/${sessionId}`),
    "Lấy thông tin phiên sạc thất bại"
  );
};

// ====== Export default object ======
export const stationAPI = {
  getConnectorTypes,
  getAllStations,
  getStationById,
  getChargingPointsByStationId,
  getAvaila,
  getTemplate,
  createBooking,
  confirmBooking,
  cancelBooking,
  startChargingSession,
  getCurrentChargingSession,
  stopChargingSession,
  getChargingSessionById,
};

// ====== Lấy thông tin station-staff hiện tại (STAFF) ======
export const getStationStaffMe = () => {
  return handleApiCall(
    () => apiClient.get(`/api/station-staff/me`),
    "Lấy thông tin station staff thất bại"
  );
};

// ====== Lấy danh sách phiên sạc theo stationId (STAFF view) ======
export const getChargingSessionsByStation = (stationId) => {
  return handleApiCall(
    () =>
      apiClient.get(
        `/api/charging-sessions/stations/${stationId}/charging-sessions`
      ),
    "Lấy danh sách phiên sạc theo trạm thất bại"
  );
};

// Mở rộng stationAPI
stationAPI.getStationStaffMe = getStationStaffMe;
stationAPI.getChargingSessionsByStation = getChargingSessionsByStation;
