import apiClient from "./apiUrls.js";
import handleApiCall from "./callApi.js";

// lấy hóa đơn theo phiên sạc
export const getInvoiceBySessionApi = (sessionId) => {
  return handleApiCall(
    () => apiClient.get(`/api/invoices/charging-sessions/${sessionId}`),
    "Lấy thông tin hóa đơn thất bại"
  );
}