import { clearAuthData } from "../utils/authUtils.js";

export default async function handleApiCall(apiCall, defaultMessage) {
  try {
    const response = await apiCall();
    if (response.config.url.includes("/logout")) {
      clearAuthData();
    }

    return {
      success: true,
      data: response.data,
    };
  } catch (error) {
    console.error("API error:", error);
    // Capture more details from the axios error response so callers can debug
    const errorData = error.response?.data;
    const errorStatus = error.response?.status;
    const errorMessage =
      (errorData && (errorData.message || JSON.stringify(errorData))) ||
      defaultMessage;

    return {
      success: false,
      message: errorMessage,
      status: errorStatus,
      errorData: errorData,
    };
  }
}
