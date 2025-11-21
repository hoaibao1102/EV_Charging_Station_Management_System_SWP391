import apiClient from "./apiUrls.js";

// Get all notifications for current user
export const getNotifications = async () => {
  try {
    const response = await apiClient.get("/api/notifications");
    return response.data;
  } catch (error) {
    console.error("Error fetching notifications:", error);
    throw error;
  }
};

// Mark notification as read
export const markNotificationAsRead = async (notificationId) => {
  try {
    const response = await apiClient.put(
      `/api/notifications/${notificationId}/read`
    );
    return response.data;
  } catch (error) {
    console.error("Error marking notification as read:", error);
    throw error;
  }
};

// Mark all notifications as read
export const markAllNotificationsAsRead = async () => {
  try {
    const response = await apiClient.put("/api/notifications/read-all");
    return response.data;
  } catch (error) {
    console.error("Error marking all notifications as read:", error);
    throw error;
  }
};

// Delete notification
export const deleteNotification = async (notificationId) => {
  try {
    const response = await apiClient.delete(
      `/api/notifications/${notificationId}`
    );
    return response.data;
  } catch (error) {
    console.error("Error deleting notification:", error);
    throw error;
  }
};

export const notificationAPI = {
  getNotifications,
  markNotificationAsRead,
  markAllNotificationsAsRead,
  deleteNotification,
};

export default notificationAPI;
