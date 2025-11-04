import apiClient from "./apiUrls.js";
import handleApiCall from "./callApi.js";

// lấy vé (signature) từ BE
export const getUploadSignature = () => {
  return handleApiCall(
    () => apiClient.get('/api/upload/signature'),
    'Không lấy đc vé upload'
  );
};
