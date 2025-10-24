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
    () =>
      apiClient.get(
        "https://68f35999fd14a9fcc4288a78.mockapi.io/stationcharging"
      ),
    "Lấy danh sách trạm sạc thất bại"
  );
};

// ====== Lấy thông tin trạm theo ID ======
export const getStationById = (id) => {
  return handleApiCall(
    () =>
      apiClient.get(
        `https://68f35999fd14a9fcc4288a78.mockapi.io/stationcharging?StationID=${id}`
      ),
    "Lấy thông tin trạm sạc thất bại"
  );
};

// ====== Lấy danh sách trụ sạc theo StationID ======
export const getChargingPointsByStationId = (stationId) => {
  return handleApiCall(
    () =>
      apiClient.get(
        `https://68f35999fd14a9fcc4288a78.mockapi.io/Charging_Points?StationID=${stationId}`
      ),
    "Lấy thông tin trụ sạc thất bại"
  );
};

// ====== Lấy danh sách slot sạc theo trụ sạc ======
export const getAvaila = (pointId) => {
  return handleApiCall(
    () =>
      apiClient.get(
        `https://68f6f46af7fb897c66141d83.mockapi.io/slot_available?PointID=${pointId}`
      ),
    "Lấy danh sách slot sạc thất bại"
  );
};

// ====== Export default object ======
export const stationAPI = {
  getConnectorTypes,
  getAllStations,
  getStationById,
  getChargingPointsByStationId,
};
