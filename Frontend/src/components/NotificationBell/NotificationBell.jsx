import React, { useState, useEffect, useRef, useCallback } from "react";
import { notificationAPI } from "../../api/notificationApi.js";
import { isAuthenticated } from "../../utils/authUtils.js";
import "./NotificationBell.css";

export default function NotificationBell() {
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [isOpen, setIsOpen] = useState(false);
  const [showAll, setShowAll] = useState(false);
  const [selectedNotification, setSelectedNotification] = useState(null);
  const dropdownRef = useRef(null);
  const detailsRef = useRef(null);

  // Fetch notifications from API
  const fetchNotifications = useCallback(async () => {
    if (!isAuthenticated()) return;

    try {
      const response = await notificationAPI.getNotifications();
      const data = response.data || response || [];

      // Sort notifications by date (newest first)
      const sortedData = data.sort((a, b) => {
        const dateA = new Date(a.createdAt || a.timestamp || 0);
        const dateB = new Date(b.createdAt || b.timestamp || 0);
        return dateB - dateA;
      });

      setNotifications(sortedData);

      // Count unread notifications
      const unread = sortedData.filter((n) => n.status === "UNREAD").length;
      setUnreadCount(unread);
    } catch (error) {
      console.error("Error fetching notifications:", error);
    }
  }, []);

  // Polling: fetch notifications every 5 seconds
  useEffect(() => {
    if (!isAuthenticated()) return;

    fetchNotifications();
    const interval = setInterval(fetchNotifications, 5000);

    return () => clearInterval(interval);
  }, [fetchNotifications]);

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsOpen(false);
        setShowAll(false);
        setSelectedNotification(null); // Close details popup too
      }
      if (
        detailsRef.current &&
        !detailsRef.current.contains(event.target) &&
        dropdownRef.current &&
        !dropdownRef.current.contains(event.target)
      ) {
        setSelectedNotification(null);
      }
    };

    if (isOpen || selectedNotification) {
      document.addEventListener("mousedown", handleClickOutside);
    }

    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [isOpen, selectedNotification]);

  const handleToggleDropdown = () => {
    setIsOpen(!isOpen);
    if (!isOpen) {
      setShowAll(false);
    }
  };

  const handleNotificationClick = async (notification) => {
    // Mark as read
    if (notification.status === "UNREAD") {
      try {
        await notificationAPI.markNotificationAsRead(
          notification.notificationId
        );

        setNotifications((prev) =>
          prev.map((n) =>
            n.notificationId === notification.notificationId
              ? { ...n, status: "READ" }
              : n
          )
        );

        setUnreadCount((prev) => Math.max(0, prev - 1));
      } catch (error) {
        console.error("Error marking notification as read:", error);
      }
    }

    // Show modal instead of navigating
    setSelectedNotification(notification);
  };

  const parseChargingContent = (content) => {
    if (!content) return null;

    const patterns = {
      point: /Điểm sạc:\s*([^|]+)/,
      duration: /Thời lượng:\s*([^|]+)/,
      soc: /Tăng SOC:\s*([^|]+)/,
      energy: /Năng lượng:\s*([^|]+)/,
      timeFee: /Phí thời gian:\s*([^|]+)/,
      energyFee: /Phí điện năng:\s*([^|]+)/,
      total: /Tổng:\s*(.+)/,
    };

    const parsed = {};
    for (const [key, pattern] of Object.entries(patterns)) {
      const match = content.match(pattern);
      if (match) {
        parsed[key] = match[1].trim();
      }
    }

    return parsed;
  };

  const closeModal = () => {
    setSelectedNotification(null);
  };

  const formatTime = (timestamp) => {
    if (!timestamp) return "";

    const now = new Date();
    const time = new Date(timestamp);
    const diffMs = now - time;
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return "Vừa xong";
    if (diffMins < 60) return `${diffMins} phút trước`;
    if (diffHours < 24) return `${diffHours} giờ trước`;
    if (diffDays < 7) return `${diffDays} ngày trước`;

    return time.toLocaleDateString("vi-VN", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    });
  };

  const getNotificationIcon = (type) => {
    const icons = {
      BOOKING_CONFIRMED: "✅",
      BOOKING_CANCELLED: "❌",
      CHARGING_STARTED: "⚡",
      CHARGING_COMPLETED: "🔋",
      PAYMENT_SUCCESS: "💳",
      PAYMENT_FAILED: "⚠️",
    };
    return icons[type] || "📢";
  };

  if (!isAuthenticated()) return null;

  const displayedNotifications = showAll
    ? notifications
    : notifications.slice(0, 5);

  return (
    <div
      className="shopee-notification-bell"
      ref={dropdownRef}
      style={{ position: "relative" }}
    >
      <button
        className="shopee-bell-button"
        onClick={handleToggleDropdown}
        aria-label="Thông báo"
      >
        <svg className="bell-icon" viewBox="0 0 24 24" fill="none">
          <path
            d="M12 2C11.4477 2 11 2.44772 11 3V3.17071C8.83481 3.58254 7.14286 5.49015 7.14286 7.8V13L5.14286 15V16H18.8571V15L16.8571 13V7.8C16.8571 5.49015 15.1652 3.58254 13 3.17071V3C13 2.44772 12.5523 2 12 2Z"
            fill="currentColor"
          />
          <path
            d="M10 19C10 20.1046 10.8954 21 12 21C13.1046 21 14 20.1046 14 19H10Z"
            fill="currentColor"
          />
        </svg>
        {unreadCount > 0 && (
          <span className="shopee-badge">
            {unreadCount > 99 ? "99+" : unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div
          className={`shopee-notification-dropdown ${
            showAll ? "expanded" : ""
          }`}
        >
          {/* Header */}
          <div className="shopee-notification-header">
            <h3>Thông Báo Mới Nhận</h3>
          </div>

          {/* Notification List */}
          <div className="shopee-notification-list">
            {notifications.length === 0 ? (
              <div className="shopee-empty-state">
                <svg className="empty-icon" viewBox="0 0 64 64" fill="none">
                  <circle cx="32" cy="32" r="30" fill="#f5f5f5" />
                  <path
                    d="M32 20C26.4772 20 22 24.4772 22 30V38L18 42V44H46V42L42 38V30C42 24.4772 37.5228 20 32 20Z"
                    fill="#d0d0d0"
                  />
                </svg>
                <p>Không có thông báo nào</p>
              </div>
            ) : (
              displayedNotifications.map((notification) => (
                <div
                  key={notification.notificationId}
                  className={`shopee-notification-item ${
                    notification.status === "UNREAD" ? "unread" : ""
                  }`}
                  onClick={() => handleNotificationClick(notification)}
                >
                  <div className="notification-icon-wrapper">
                    <span className="notification-emoji">
                      {getNotificationIcon(notification.type)}
                    </span>
                  </div>

                  <div className="notification-content">
                    <h4 className="notification-title">
                      {notification.title || "Thông báo"}
                    </h4>
                    <p className="notification-message">
                      {notification.message || notification.description || ""}
                    </p>
                    <span className="notification-time">
                      {formatTime(
                        notification.createdAt || notification.timestamp
                      )}
                    </span>
                  </div>

                  {notification.status === "UNREAD" && (
                    <div className="notification-unread-indicator"></div>
                  )}
                </div>
              ))
            )}
          </div>

          {/* Footer */}
          {notifications.length > 0 && (
            <div className="shopee-notification-footer">
              {!showAll && notifications.length > 5 && (
                <button
                  className="shopee-view-all-btn"
                  onClick={() => setShowAll(true)}
                >
                  Xem Tất Cả
                </button>
              )}
            </div>
          )}
        </div>
      )}

      {/* Modal hiển thị chi tiết thông báo */}
      {selectedNotification && (
        <div
          ref={detailsRef}
          style={{
            position: "absolute",
            top: "0",
            right: "1000%",
            marginRight: "8px",
            width: "420px",
            maxWidth: "calc(100vw - 450px)",
            maxHeight: "85vh",
            backgroundColor: "white",
            borderRadius: "16px",
            boxShadow: "0 10px 40px rgba(0, 0, 0, 0.25)",
            overflow: "hidden",
            display: "flex",
            flexDirection: "column",
            animation: "slideInRight 0.25s ease-out",
            zIndex: 10001,
          }}
        >
          {/* Modal Header */}
          <div
            style={{
              padding: "18px 20px",
              borderBottom: "1px solid #f0f0f0",
              display: "flex",
              justifyContent: "space-between",
              alignItems: "flex-start",
              gap: "12px",
              background: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)",
              color: "white",
            }}
          >
            <div style={{ flex: 1 }}>
              <div
                style={{
                  fontSize: "12px",
                  opacity: 0.9,
                  marginBottom: "4px",
                  fontWeight: "500",
                }}
              >
                {selectedNotification.type === "CHARGING_COMPLETED"
                  ? "⚡ Hoàn thành sạc"
                  : selectedNotification.type === "BOOKING_CONFIRMED"
                  ? "✓ Đặt chỗ"
                  : selectedNotification.type === "PAYMENT_SUCCESS"
                  ? "💳 Thanh toán"
                  : "📢 Thông báo"}
              </div>
              <h2
                style={{
                  margin: 0,
                  fontSize: "16px",
                  fontWeight: "600",
                  lineHeight: "1.4",
                }}
              >
                {selectedNotification.title || "Chi tiết thông báo"}
              </h2>
            </div>
            <button
              onClick={closeModal}
              style={{
                background: "rgba(255, 255, 255, 0.2)",
                border: "none",
                fontSize: "20px",
                cursor: "pointer",
                color: "white",
                padding: "4px",
                width: "28px",
                height: "28px",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                borderRadius: "6px",
                transition: "all 0.2s",
                flexShrink: 0,
              }}
              onMouseEnter={(e) =>
                (e.target.style.background = "rgba(255, 255, 255, 0.3)")
              }
              onMouseLeave={(e) =>
                (e.target.style.background = "rgba(255, 255, 255, 0.2)")
              }
            >
              ×
            </button>
          </div>

          {/* Modal Body */}
          <div
            style={{
              padding: "20px",
              overflowY: "auto",
              flex: 1,
            }}
          >
            {selectedNotification.type === "CHARGING_COMPLETED" ? (
              (() => {
                const parsed = parseChargingContent(
                  selectedNotification.content
                );
                return parsed ? (
                  <div
                    style={{
                      display: "flex",
                      flexDirection: "column",
                      gap: "14px",
                    }}
                  >
                    {/* Charging Point Card */}
                    {parsed.point && (
                      <div
                        style={{
                          padding: "14px",
                          background:
                            "linear-gradient(135deg, #667eea15 0%, #764ba215 100%)",
                          borderRadius: "10px",
                          border: "1px solid #667eea30",
                        }}
                      >
                        <div
                          style={{
                            fontSize: "12px",
                            color: "#666",
                            marginBottom: "4px",
                          }}
                        >
                          Điểm sạc
                        </div>
                        <div
                          style={{
                            fontSize: "16px",
                            fontWeight: "600",
                            color: "#667eea",
                          }}
                        >
                          {parsed.point}
                        </div>
                      </div>
                    )}

                    {/* Info Grid */}
                    <div
                      style={{
                        display: "grid",
                        gridTemplateColumns: "repeat(2, 1fr)",
                        gap: "10px",
                      }}
                    >
                      {parsed.duration && (
                        <div
                          style={{
                            padding: "12px",
                            background: "#f8f9fa",
                            borderRadius: "8px",
                            border: "1px solid #e9ecef",
                          }}
                        >
                          <div
                            style={{
                              fontSize: "11px",
                              color: "#6c757d",
                              marginBottom: "5px",
                            }}
                          >
                            ⏱️ Thời lượng
                          </div>
                          <div
                            style={{
                              fontSize: "14px",
                              fontWeight: "600",
                              color: "#212529",
                            }}
                          >
                            {parsed.duration}
                          </div>
                        </div>
                      )}

                      {parsed.soc && (
                        <div
                          style={{
                            padding: "12px",
                            background: "#f8f9fa",
                            borderRadius: "8px",
                            border: "1px solid #e9ecef",
                          }}
                        >
                          <div
                            style={{
                              fontSize: "11px",
                              color: "#6c757d",
                              marginBottom: "5px",
                            }}
                          >
                            🔋 Tăng SOC
                          </div>
                          <div
                            style={{
                              fontSize: "14px",
                              fontWeight: "600",
                              color: "#28a745",
                            }}
                          >
                            {parsed.soc}
                          </div>
                        </div>
                      )}

                      {parsed.energy && (
                        <div
                          style={{
                            padding: "12px",
                            background: "#f8f9fa",
                            borderRadius: "8px",
                            border: "1px solid #e9ecef",
                            gridColumn:
                              parsed.duration && parsed.soc ? "span 2" : "auto",
                          }}
                        >
                          <div
                            style={{
                              fontSize: "11px",
                              color: "#6c757d",
                              marginBottom: "5px",
                            }}
                          >
                            ⚡ Năng lượng
                          </div>
                          <div
                            style={{
                              fontSize: "14px",
                              fontWeight: "600",
                              color: "#212529",
                            }}
                          >
                            {parsed.energy}
                          </div>
                        </div>
                      )}
                    </div>

                    {/* Fees Section */}
                    <div
                      style={{
                        padding: "14px",
                        background: "white",
                        borderRadius: "10px",
                        border: "1px solid #e9ecef",
                      }}
                    >
                      <div
                        style={{
                          fontSize: "13px",
                          fontWeight: "600",
                          color: "#495057",
                          marginBottom: "10px",
                        }}
                      >
                        Chi tiết phí
                      </div>
                      <div
                        style={{
                          display: "flex",
                          flexDirection: "column",
                          gap: "8px",
                        }}
                      >
                        {parsed.timeFee && (
                          <div
                            style={{
                              display: "flex",
                              justifyContent: "space-between",
                              alignItems: "center",
                            }}
                          >
                            <span
                              style={{ color: "#6c757d", fontSize: "13px" }}
                            >
                              Phí thời gian
                            </span>
                            <span
                              style={{
                                fontWeight: "500",
                                color: "#212529",
                                fontSize: "13px",
                              }}
                            >
                              {parsed.timeFee}
                            </span>
                          </div>
                        )}
                        {parsed.energyFee && (
                          <div
                            style={{
                              display: "flex",
                              justifyContent: "space-between",
                              alignItems: "center",
                            }}
                          >
                            <span
                              style={{ color: "#6c757d", fontSize: "13px" }}
                            >
                              Phí điện năng
                            </span>
                            <span
                              style={{
                                fontWeight: "500",
                                color: "#212529",
                                fontSize: "13px",
                              }}
                            >
                              {parsed.energyFee}
                            </span>
                          </div>
                        )}
                        {parsed.total && (
                          <>
                            <div
                              style={{
                                height: "1px",
                                background: "#dee2e6",
                                margin: "4px 0",
                              }}
                            ></div>
                            <div
                              style={{
                                display: "flex",
                                justifyContent: "space-between",
                                alignItems: "center",
                              }}
                            >
                              <span
                                style={{
                                  fontWeight: "600",
                                  color: "#212529",
                                  fontSize: "14px",
                                }}
                              >
                                Tổng cộng
                              </span>
                              <span
                                style={{
                                  fontWeight: "700",
                                  color: "#dc3545",
                                  fontSize: "16px",
                                }}
                              >
                                {parsed.total}
                              </span>
                            </div>
                          </>
                        )}
                      </div>
                    </div>
                  </div>
                ) : (
                  <p
                    style={{
                      margin: 0,
                      color: "#495057",
                      lineHeight: "1.6",
                      fontSize: "14px",
                    }}
                  >
                    {selectedNotification.content ||
                      selectedNotification.message ||
                      "Không có nội dung"}
                  </p>
                );
              })()
            ) : (
              <div
                style={{
                  padding: "16px",
                  background: "#f8f9fa",
                  borderRadius: "10px",
                  border: "1px solid #e9ecef",
                }}
              >
                <p
                  style={{
                    margin: 0,
                    color: "#495057",
                    lineHeight: "1.7",
                    fontSize: "14px",
                  }}
                >
                  {selectedNotification.content ||
                    selectedNotification.message ||
                    "Không có nội dung"}
                </p>
              </div>
            )}
          </div>

          {/* Modal Footer */}
          <div
            style={{
              padding: "12px 20px",
              borderTop: "1px solid #f0f0f0",
              backgroundColor: "#fafafa",
              display: "flex",
              alignItems: "center",
              gap: "8px",
            }}
          >
            <svg width="14" height="14" viewBox="0 0 16 16" fill="none">
              <path
                d="M8 14A6 6 0 108 2a6 6 0 000 12z"
                stroke="#999"
                strokeWidth="1.5"
              />
              <path
                d="M8 5v3l2 2"
                stroke="#999"
                strokeWidth="1.5"
                strokeLinecap="round"
              />
            </svg>
            <small style={{ color: "#6c757d", fontSize: "12px" }}>
              {formatTime(
                selectedNotification.createdAt || selectedNotification.timestamp
              )}
            </small>
          </div>
        </div>
      )}

      <style>{`
        @keyframes slideInRight {
          from { 
            transform: translateX(-10px);
            opacity: 0;
          }
          to { 
            transform: translateX(0);
            opacity: 1;
          }
        }
        
        @media (max-width: 768px) {
          .shopee-notification-bell [ref="detailsRef"] {
            left: 0 !important;
            right: 0 !important;
            margin: 8px !important;
            width: auto !important;
            max-width: none !important;
          }
        }
      `}</style>
    </div>
  );
}
