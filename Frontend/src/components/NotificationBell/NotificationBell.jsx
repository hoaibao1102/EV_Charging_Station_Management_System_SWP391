import React, { useState, useEffect, useRef, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { notificationAPI } from "../../api/notificationApi.js";
import { isAuthenticated } from "../../utils/authUtils.js";
import "./NotificationBell.css";

export default function NotificationBell() {
  const navigate = useNavigate();
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [isOpen, setIsOpen] = useState(false);
  const [showAll, setShowAll] = useState(false);
  const dropdownRef = useRef(null);

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
      }
    };

    if (isOpen) {
      document.addEventListener("mousedown", handleClickOutside);
    }

    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [isOpen]);

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

    // Navigate based on notification type
    if (notification.relatedEntityType && notification.relatedEntityId) {
      switch (notification.relatedEntityType) {
        case "BOOKING":
          navigate(`/bookings/${notification.relatedEntityId}`);
          break;
        case "TRANSACTION":
          navigate(
            `/profile/transaction-history/${notification.relatedEntityId}`
          );
          break;
        case "SESSION":
          navigate(`/chargingSession`);
          break;
        default:
          break;
      }
    }

    setIsOpen(false);
    setShowAll(false);
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
    <div className="shopee-notification-bell" ref={dropdownRef}>
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
    </div>
  );
}
