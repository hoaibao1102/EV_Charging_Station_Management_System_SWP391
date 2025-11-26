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

// Lấy danh sách giao dịch của trạm (có phân trang, filter)
export const getStationTransactionsApi = (params = {}) => {
  const {
    status = null,
    page = 0,
    size = 100,
  } = params;

  let url = `/api/staff/transactions?page=${page}&size=${size}`;

  if (status) {
    url += `&status=${status}`;
  }

  console.log("🔧 [staffApi.getStationTransactionsApi] Calling URL:", url);
  console.log("🔧 [staffApi.getStationTransactionsApi] Params:", { status, page, size });

  return handleApiCall(
    () => apiClient.get(url),
    "Lấy danh sách giao dịch thất bại"
  ).then(result => {
    console.log("🔧 [staffApi.getStationTransactionsApi] handleApiCall result:", result);
    console.log("🔧 [staffApi.getStationTransactionsApi] result.success:", result.success);
    console.log("🔧 [staffApi.getStationTransactionsApi] result.data:", result.data);
    console.log("🔧 [staffApi.getStationTransactionsApi] result.data type:", typeof result.data);
    if (result.data) {
      console.log("🔧 [staffApi.getStationTransactionsApi] result.data.content:", result.data.content);
      console.log("🔧 [staffApi.getStationTransactionsApi] result.data.totalElements:", result.data.totalElements);
      console.log("🔧 [staffApi.getStationTransactionsApi] result.data.totalPages:", result.data.totalPages);
    }
    return result;
  });
};

// Lấy thống kê giao dịch của trạm
export const getStationTransactionStatsApi = () => {
  return handleApiCall(
    () => apiClient.get("/api/staff/transactions/stats"),
    "Lấy thống kê giao dịch thất bại"
  );
};

// Dừng phiên sạc
export const staffStopSessionApi = (sessionId, finalSoc = null) => {
  const body = { sessionId };
  if (finalSoc != null) {
    body.finalSoc = finalSoc;
  }
  return handleApiCall(
    () => apiClient.post(`/api/staff/staff-stop-session`, body),
    "Dừng phiên sạc thất bại"
  );
};

// Lấy danh sách hóa đơn của trạm
export const getStationInvoicesApi = (stationId) => {
  return handleApiCall(
    () => apiClient.get(`/api/invoice/station/${stationId}/details`),
    "Lấy danh sách hóa đơn thất bại"
  );
};

// Lấy chi tiết hóa đơn
export const getInvoiceDetailApi = (invoiceId) => {
  return handleApiCall(
    () => apiClient.get(`/api/invoice/${invoiceId}`),
    "Lấy chi tiết hóa đơn thất bại"
  );
};

// Thanh toán hóa đơn
export const payInvoiceApi = (invoiceId) => {
  return handleApiCall(
    () => apiClient.post(`/api/invoice/pay/${invoiceId}`),
    "Thanh toán hóa đơn thất bại"
  );
};


