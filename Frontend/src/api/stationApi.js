import apiClient from "./apiUrls.js";
import handleApiCall from "./callApi.js";

// ====== Lấy tất cả loại connector ======
export const getConnectorTypes = () => {
  return handleApiCall(
    () => apiClient.get("/api/connector-types"),
    "Lấy thông tin loại cổng sạc thất bại"
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

// ====== Khởi động phiên sạc ======
export const startChargingSession = (payload) => {
  return handleApiCall(
    () =>
      apiClient.post("/api/charging-sessions/start", payload, {
        headers: {
          Authorization: "Bearer " + localStorage.getItem("token"),
        },
      }),
    "Khởi động phiên sạc thất bại"
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
  startChargingSession,
};
