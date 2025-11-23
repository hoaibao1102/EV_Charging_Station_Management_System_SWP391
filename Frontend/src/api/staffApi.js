import apiClient from "./apiUrls.js";
import handleApiCall from "./callApi.js";

// cập nhật mật khẩu staff
export const updateStaffPasswordApi = (passwordData) => {
  return handleApiCall(
    () => apiClient.put(`/api/staff/password`, passwordData),
    "Cập nhật mật khẩu staff thất bại"
  );
};

// ==================== STAFF DASHBOARD APIs ====================

// Lấy trạm được phân công cho staff
export const getMyStationApi = () => {
  return handleApiCall(
    () => apiClient.get("/api/station-staff/me"),
    "Lấy thông tin trạm thất bại"
  );
};

// Lấy phiên sạc đang hoạt động tại trạm
export const getActiveSessionsApi = (stationId) => {
  return handleApiCall(
    () => apiClient.get(`/api/charging-sessions/active?stationId=${stationId}`),
    "Lấy phiên sạc đang hoạt động thất bại"
  );
};

// Lấy tất cả phiên sạc của trạm
export const getAllSessionsByStationApi = (stationId) => {
  return handleApiCall(
    () =>
      apiClient.get(
        `/api/charging-sessions/stations/${stationId}/charging-sessions`
      ),
    "Lấy danh sách phiên sạc thất bại"
  );
};

// Lấy booking đã confirm của staff
export const getConfirmedBookingsApi = () => {
  return handleApiCall(
    () => apiClient.get("/api/bookings/confirmed/compact"),
    "Lấy danh sách booking thất bại"
  );
};

// Lấy thống kê tổng quan (general stats)
export const getDashboardStatsApi = () => {
  return handleApiCall(
    () => apiClient.get("/api/staff/dashboard/stats"),
    "Lấy thống kê dashboard thất bại"
  );
};

// Lấy hoạt động gần đây
export const getRecentActivitiesApi = (limit = 10) => {
  return handleApiCall(
    () =>
      apiClient.get(`/api/staff/dashboard/recent-activities?limit=${limit}`),
    "Lấy hoạt động gần đây thất bại"
  );
};

// Lấy dữ liệu biểu đồ phiên sạc theo giờ
export const getSessionsPerHourChartApi = () => {
  return handleApiCall(
    () => apiClient.get("/api/staff/dashboard/chart/sessions-per-hour"),
    "Lấy dữ liệu biểu đồ thất bại"
  );
};

// ==================== STAFF TRANSACTION MANAGEMENT APIs ====================

// Lấy danh sách giao dịch của trạm (có phân trang, filter, sort)
export const getStationTransactionsApi = (params = {}) => {
  const {
    status = null,
    page = 0,
    size = 100,
    sortBy = null, // Không gửi default để tránh lỗi Backend
    sortDir = null,
  } = params;

  let url = `/api/staff/transactions?page=${page}&size=${size}`;

  // Chỉ thêm sortBy và sortDir khi có giá trị
  if (sortBy && sortDir) {
    url += `&sortBy=${sortBy}&sortDir=${sortDir}`;
  }

  if (status) {
    url += `&status=${status}`;
  }

  return handleApiCall(
    () => apiClient.get(url),
    "Lấy danh sách giao dịch thất bại"
  );
};

// Lấy thống kê giao dịch của trạm
export const getStationTransactionStatsApi = () => {
  return handleApiCall(
    () => apiClient.get("/api/staff/transactions/stats"),
    "Lấy thống kê giao dịch thất bại"
  );
};
