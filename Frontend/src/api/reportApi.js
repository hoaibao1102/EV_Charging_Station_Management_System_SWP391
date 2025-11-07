import apiClient from "./apiUrls.js";
import handleApiCall from "./callApi.js";

// ====== Lấy tất cả các báo cáo sự cố ======
export const getAllAccidentReportsApi = () => {
  return handleApiCall(
    apiClient.get('/api/incidents'), 
    'Lấy danh sách báo cáo sự cố thất bại'
  );
};

// tao báo cáo sự cố mới
export const createAccidentReportApi = (reportData) => {
  return handleApiCall(
    apiClient.post('/api/incidents', reportData),
    'Tạo báo cáo sự cố thất bại'
  );
};
