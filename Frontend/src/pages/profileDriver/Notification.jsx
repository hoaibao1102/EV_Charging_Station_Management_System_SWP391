import { useState, useEffect } from "react";
import { getNotificationsApi } from "../../api/driverApi.js";
import { toast } from "react-toastify";
import NotificationCard from "../../components/driver/NotifiationCard.jsx";
import { markNotificationAsReadApi } from '../../api/driverApi.js';
import "./Notification.css";

export default function Notification() {
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [readedNotifications, setReadedNotifications] = useState();

  const fetchNotifications = async () => {
    try {
      setLoading(true);
      const response = await getNotificationsApi();
      if (response.success) {
        setNotifications(response.data);
        console.log("My notifications:", response.data);
      }
    } catch (error) {
      console.error("Failed to fetch my notifications:", error);
      toast.error("Không thể tải danh sách thông báo!");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchNotifications();
  }, [readedNotifications]);

  const handleReaded = async (notification) => {
    try {
      const response = await markNotificationAsReadApi(notification.notificationId);
      if (response.success) {
        console.log(`Đánh dấu thông báo ${notification.notificationId} là đã đọc thành công`);
        setReadedNotifications(notification.notificationId);
      }
    } catch (error) {
      console.error(`Đánh dấu thông báo ${notification.notificationId} là đã đọc thất bại`, error);
    }
    console.log(`Đánh dấu thông báo ${notification.notificationId} là đã đọc`);

  }

  const sortedNotifications = [...notifications].sort((a, b) => {
    return new Date(b.createdAt) - new Date(a.createdAt);
  });

  return (
    <div className="notification-container">
      <h1>Những thông báo của bạn</h1>
      {loading ? (
        <p className="notification-loading">Đang tải thông báo...</p>
      ) : notifications.length === 0 ? (
        <p className="notification-empty">Không có thông báo nào.</p>
      ) : (
        <ul className="notification-list">
          {sortedNotifications.map(notification => (
            <NotificationCard key={notification.notificationId} notification={notification} onSelect={handleReaded} />
          ))}
        </ul>
      )}
    </div>
  );
}