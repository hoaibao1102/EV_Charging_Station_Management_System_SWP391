import { useState, useEffect } from "react";
import {
  getNotificationsApi,
  markNotificationAsReadApi,
} from "../../api/driverApi.js";
import { toast } from "react-toastify";
import NotificationCard from "../../components/driver/NotifiationCard.jsx";
import "./Notification.css";

export default function Notification() {
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedNotification, setSelectedNotification] = useState(null);

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

    setNotifications((prevNotifications) =>
      prevNotifications.map((n) =>
        n.notificationId === notification.notificationId
          ? { ...n, isRead: true }
          : n
      )
    );

    try {
      const response = await markNotificationAsReadApi(
        notification.notificationId
      );
      if (response.success) {
        console.log(
          `Đánh dấu ${notification.notificationId} thành công (server)`
        );
      } else {
        toast.error("Đánh dấu đã đọc thất bại, vui lòng thử lại.");
        setNotifications((prevNotifications) =>
          prevNotifications.map((n) =>
            n.notificationId === notification.notificationId
              ? { ...n, isRead: false }
              : n
          )
        );
      }
    } catch (error) {
      console.error(`Lỗi khi đánh dấu đã đọc:`, error);
      toast.error("Lỗi khi đánh dấu đã đọc.");
      setNotifications((prevNotifications) =>
        prevNotifications.map((n) =>
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

  const handleCardClick = (notification) => {
    alert("CLICKED: " + notification.title);
    console.log("Modal opening for:", notification);
    setSelectedNotification(notification);
    if (notification.status === "UNREAD") {
      handleReaded(notification);
    }
  };

  const closeModal = () => {
    setSelectedNotification(null);
  };

  const parseChargingContent = (content) => {
    const info = {};
    const patterns = {
      point: /Điểm sạc:\s*([^|]+)/,
      duration: /Thời lượng:\s*([^|]+)/,
      soc: /Tăng SOC:\s*([^|]+)/,
      energy: /Năng lượng:\s*([^|]+)/,
      timeFee: /Phí thời gian:\s*([^|]+)/,
      energyFee: /Phí điện năng:\s*([^|]+)/,
      total: /Tổng:\s*(.+)/,
    };

    Object.entries(patterns).forEach(([key, regex]) => {
      const match = content.match(regex);
      if (match) info[key] = match[1].trim();
    });

    return Object.keys(info).length > 0 ? info : null;
  };

  return (
    <div className="notification-container">
      <div
        style={{
          background: "red",
          color: "white",
          padding: "30px",
          fontSize: "24px",
          fontWeight: "bold",
          textAlign: "center",
          margin: "20px",
        }}
      >
        ⚠️ TEST: Code đã được load! Notifications: {notifications.length}
      </div>

      <h1>Những thông báo của bạn</h1>
      {loading ? (
        <p className="notification-loading">Đang tải thông báo...</p>
      ) : notifications.length === 0 ? (
        <p className="notification-empty">Không có thông báo nào.</p>
      ) : (
        <ul className="notification-list">
          {sortedNotifications.map((notification) => (
            <li
              key={notification.notificationId}
              onClick={() => handleCardClick(notification)}
              style={{
                listStyle: "none",
                cursor: "pointer",
                position: "relative",
                zIndex: 1,
              }}
            >
              <div
                style={{
                  background: "lightyellow",
                  border: "2px solid red",
                  padding: "10px",
                  margin: "5px",
                }}
              >
                DEBUG: Click me - {notification.title}
              </div>
              <NotificationCard
                notification={notification}
                onSelect={handleReaded}
              />
            </li>
          ))}
        </ul>
      )}

      {/* Modal */}
      {selectedNotification && (
        <div
          style={{
            position: "fixed",
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            background: "rgba(0,0,0,0.7)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 9999,
            padding: "20px",
          }}
          onClick={closeModal}
        >
          <div
            style={{
              background: "white",
              borderRadius: "16px",
              maxWidth: "700px",
              width: "100%",
              maxHeight: "90vh",
              overflow: "auto",
              boxShadow: "0 10px 40px rgba(0,0,0,0.3)",
            }}
            onClick={(e) => e.stopPropagation()}
          >
            {/* Header */}
            <div
              style={{
                padding: "24px",
                background: "linear-gradient(135deg, #20b2aa 0%, #17a397 100%)",
                color: "white",
                borderRadius: "16px 16px 0 0",
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
              }}
            >
              <h3 style={{ margin: 0 }}>📰 {selectedNotification.title}</h3>
              <button
                onClick={closeModal}
                style={{
                  background: "rgba(255,255,255,0.2)",
                  border: "none",
                  width: "36px",
                  height: "36px",
                  borderRadius: "50%",
                  color: "white",
                  fontSize: "24px",
                  cursor: "pointer",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                }}
              >
                ✕
              </button>
            </div>

            {/* Body */}
            <div style={{ padding: "24px" }}>
              {(() => {
                if (selectedNotification.type === "CHARGING_COMPLETED") {
                  const chargingInfo = parseChargingContent(
                    selectedNotification.content
                  );
                  if (chargingInfo) {
                    return (
                      <div
                        style={{
                          display: "grid",
                          gridTemplateColumns:
                            "repeat(auto-fit, minmax(200px, 1fr))",
                          gap: "16px",
                        }}
                      >
                        {chargingInfo.point && (
                          <div
                            style={{
                              background: "#f8f9fa",
                              padding: "16px",
                              borderRadius: "12px",
                              borderLeft: "4px solid #2196f3",
                            }}
                          >
                            <div
                              style={{
                                fontSize: "12px",
                                color: "#666",
                                marginBottom: "6px",
                              }}
                            >
                              🔌 ĐIỂM SẠC
                            </div>
                            <div
                              style={{ fontSize: "18px", fontWeight: "bold" }}
                            >
                              {chargingInfo.point}
                            </div>
                          </div>
                        )}
                        {chargingInfo.duration && (
                          <div
                            style={{
                              background: "#f8f9fa",
                              padding: "16px",
                              borderRadius: "12px",
                              borderLeft: "4px solid #9c27b0",
                            }}
                          >
                            <div
                              style={{
                                fontSize: "12px",
                                color: "#666",
                                marginBottom: "6px",
                              }}
                            >
                              ⏱️ THỜI LƯỢNG
                            </div>
                            <div
                              style={{ fontSize: "18px", fontWeight: "bold" }}
                            >
                              {chargingInfo.duration}
                            </div>
                          </div>
                        )}
                        {chargingInfo.soc && (
                          <div
                            style={{
                              background: "#f8f9fa",
                              padding: "16px",
                              borderRadius: "12px",
                              borderLeft: "4px solid #4caf50",
                            }}
                          >
                            <div
                              style={{
                                fontSize: "12px",
                                color: "#666",
                                marginBottom: "6px",
                              }}
                            >
                              🔋 TĂNG SOC
                            </div>
                            <div
                              style={{ fontSize: "18px", fontWeight: "bold" }}
                            >
                              {chargingInfo.soc}
                            </div>
                          </div>
                        )}
                        {chargingInfo.energy && (
                          <div
                            style={{
                              background: "#f8f9fa",
                              padding: "16px",
                              borderRadius: "12px",
                              borderLeft: "4px solid #ff9800",
                            }}
                          >
                            <div
                              style={{
                                fontSize: "12px",
                                color: "#666",
                                marginBottom: "6px",
                              }}
                            >
                              ⚡ NĂNG LƯỢNG
                            </div>
                            <div
                              style={{ fontSize: "18px", fontWeight: "bold" }}
                            >
                              {chargingInfo.energy}
                            </div>
                          </div>
                        )}
                        {chargingInfo.timeFee && (
                          <div
                            style={{
                              background: "#f8f9fa",
                              padding: "16px",
                              borderRadius: "12px",
                              borderLeft: "4px solid #00bcd4",
                            }}
                          >
                            <div
                              style={{
                                fontSize: "12px",
                                color: "#666",
                                marginBottom: "6px",
                              }}
                            >
                              💵 PHÍ THỜI GIAN
                            </div>
                            <div
                              style={{ fontSize: "18px", fontWeight: "bold" }}
                            >
                              {chargingInfo.timeFee}
                            </div>
                          </div>
                        )}
                        {chargingInfo.energyFee && (
                          <div
                            style={{
                              background: "#f8f9fa",
                              padding: "16px",
                              borderRadius: "12px",
                              borderLeft: "4px solid #00bcd4",
                            }}
                          >
                            <div
                              style={{
                                fontSize: "12px",
                                color: "#666",
                                marginBottom: "6px",
                              }}
                            >
                              💰 PHÍ ĐIỆN NĂNG
                            </div>
                            <div
                              style={{ fontSize: "18px", fontWeight: "bold" }}
                            >
                              {chargingInfo.energyFee}
                            </div>
                          </div>
                        )}
                        {chargingInfo.total && (
                          <div
                            style={{
                              gridColumn: "1 / -1",
                              background:
                                "linear-gradient(135deg, #e8f5e9 0%, #fff 100%)",
                              padding: "16px",
                              borderRadius: "12px",
                              borderLeft: "4px solid #4caf50",
                            }}
                          >
                            <div
                              style={{
                                fontSize: "12px",
                                color: "#666",
                                marginBottom: "6px",
                              }}
                            >
                              💳 TỔNG THANH TOÁN
                            </div>
                            <div
                              style={{
                                fontSize: "24px",
                                fontWeight: "bold",
                                color: "#4caf50",
                              }}
                            >
                              {chargingInfo.total}
                            </div>
                          </div>
                        )}
                      </div>
                    );
                  }
                }
                return (
                  <div
                    style={{
                      padding: "20px",
                      background: "#f8f9fa",
                      borderRadius: "12px",
                      borderLeft: "4px solid #20b2aa",
                      fontSize: "15px",
                      lineHeight: "1.8",
                    }}
                  >
                    {selectedNotification.content}
                  </div>
                );
              })()}
            </div>

            {/* Footer */}
            <div
              style={{
                padding: "20px 24px",
                borderTop: "2px solid #f0f0f0",
                color: "#999",
                fontSize: "13px",
              }}
            >
              🕒{" "}
              {new Date(selectedNotification.createdAt).toLocaleString("vi-VN")}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
