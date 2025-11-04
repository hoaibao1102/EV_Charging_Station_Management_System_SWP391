import { useState, useEffect } from "react";
import { getNotificationsApi, markNotificationAsReadApi } from "../../api/driverApi.js";
import { toast } from "react-toastify";
import NotificationCard from "../../components/driver/NotifiationCard.jsx";
import "./Notification.css";

export default function Notification() {
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);


  const fetchNotifications = async () => {
    try {
      setLoading(true);
      const response = await getNotificationsApi();
      if (response.success) {
        setNotifications(response.data.content || []);
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
  }, []);

  const handleReaded = async (notification) => {
    if (notification.isRead) {
        console.log("Thông báo này đã đọc rồi.");
        return; 
    }

    setNotifications(prevNotifications =>
      prevNotifications.map(n =>
        n.notificationId === notification.notificationId
          ? { ...n, isRead: true }
          : n
      )
    );

    try {
      const response = await markNotificationAsReadApi(notification.notificationId);
      if (response.success) {
        console.log(`Đánh dấu ${notification.notificationId} thành công (server)`);
      } else {
        toast.error("Đánh dấu đã đọc thất bại, vui lòng thử lại.");
        setNotifications(prevNotifications =>
          prevNotifications.map(n =>
            n.notificationId === notification.notificationId
              ? { ...n, isRead: false } 
              : n
          )
        );
      }
    } catch (error) {
      console.error(`Lỗi khi đánh dấu đã đọc:`, error);
      toast.error("Lỗi khi đánh dấu đã đọc.");
      setNotifications(prevNotifications =>
        prevNotifications.map(n =>
          n.notificationId === notification.notificationId
            ? { ...n, isRead: false }
            : n
        )
      );
    }
  };

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
            <NotificationCard 
                key={notification.notificationId} 
                notification={notification} 
                onSelect={handleReaded} 
            />
          ))}
        </ul>
      )}
    </div>
  );
}