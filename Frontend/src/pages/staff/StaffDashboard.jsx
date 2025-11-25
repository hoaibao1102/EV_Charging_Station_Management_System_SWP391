import { useState, useEffect } from "react";
import { toast } from "react-toastify";
import {
  getMyStationApi,
  getActiveSessionsApi,
  getAllSessionsByStationApi,
  getConfirmedBookingsApi,
  getDashboardStatsApi,
  getRecentActivitiesApi,
  getSessionsPerHourChartApi,
} from "../../api/staffApi";
import Header from "../../components/admin/Header.jsx";
import "../admin/ManagementUser.css";
import "./StaffDashboard.css";

export default function StaffDashboard() {
  const [myStation, setMyStation] = useState(null);
  const [activeSessions, setActiveSessions] = useState([]);
  const [allSessions, setAllSessions] = useState([]);
  const [confirmedBookings, setConfirmedBookings] = useState([]);
  const [recentActivities, setRecentActivities] = useState([]);
  const [chartData, setChartData] = useState([]);
  const [generalStats, setGeneralStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const fetchMyStation = async () => {
    try {
      const response = await getMyStationApi();
      if (response.success && response.data && response.data.length > 0) {
        setMyStation(response.data[0]);
        return response.data[0].stationId;
      } else {
        toast.error("Bạn chưa được phân công trạm nào");
        setLoading(false);
        return null;
      }
    } catch (error) {
      toast.error("Không thể tải thông tin trạm", error);
      setLoading(false);
      return null;
    }
  };

  const fetchDashboardData = async (stationId, isRefresh = false) => {
    if (isRefresh) setRefreshing(true);

    try {
      const [
        activeSessionsRes,
        allSessionsRes,
        bookingsRes,
        activitiesRes,
        chartRes,
        statsRes,
      ] = await Promise.all([
        getActiveSessionsApi(stationId),
        getAllSessionsByStationApi(stationId),
        getConfirmedBookingsApi(),
        getRecentActivitiesApi(15),
        getSessionsPerHourChartApi(),
        getDashboardStatsApi(),
      ]);

      if (activeSessionsRes.success)
        setActiveSessions(activeSessionsRes.data || []);
      if (allSessionsRes.success) setAllSessions(allSessionsRes.data || []);
      if (bookingsRes.success) setConfirmedBookings(bookingsRes.data || []);
      if (activitiesRes.success) setRecentActivities(activitiesRes.data || []);
      if (chartRes.success) setChartData(chartRes.data || []);
      if (statsRes.success) setGeneralStats(statsRes.data);
    } catch (error) {
      toast.error("Không thể tải dữ liệu dashboard", error);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    const init = async () => {
      const stationId = await fetchMyStation();
      if (stationId) await fetchDashboardData(stationId);
    };
    init();
  }, []);

  useEffect(() => {
    if (!myStation) return;
    const interval = setInterval(
      () => fetchDashboardData(myStation.stationId, true),
      30000
    );
    return () => clearInterval(interval);
  }, [myStation]);

  const formatCurrency = (amount) =>
    new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
    }).format(amount);
  const formatDateTime = (ts) =>
    ts
      ? new Date(ts).toLocaleString("vi-VN", {
          day: "2-digit",
          month: "2-digit",
          hour: "2-digit",
          minute: "2-digit",
        })
      : "";
  const formatTime = (ts) =>
    ts
      ? new Date(ts).toLocaleTimeString("vi-VN", {
          hour: "2-digit",
          minute: "2-digit",
        })
      : "";

  const getActivityIcon = (type) =>
    ({ SESSION_START: "⚡", BOOKING_NEW: "📅", PAYMENT_SUCCESS: "💳" }[type] ||
    "📋");
  const getSessionStatusBadge = (status) =>
    ({
      IN_PROGRESS: { text: "Đang sạc", color: "#28a745" },
      COMPLETED: { text: "Hoàn thành", color: "#6c757d" },
      CANCELLED: { text: "Đã hủy", color: "#dc3545" },
    }[status] || { text: status, color: "#6c757d" });
  const getBookingStatusBadge = (status) =>
    ({
      PENDING: { text: "Chờ xác nhận", color: "#ffc107" },
      CONFIRMED: { text: "Đã xác nhận", color: "#28a745" },
      CANCELLED: { text: "Đã hủy", color: "#dc3545" },
      COMPLETED: { text: "Hoàn thành", color: "#6c757d" },
    }[status] || { text: status, color: "#6c757d" });

  if (loading)
    return <div className="loading-overlay">Đang tải dữ liệu dashboard...</div>;
  if (!myStation)
    return (
      <div className="management-user-container">
        <Header />
        <div style={{ padding: "40px", textAlign: "center" }}>
          <div style={{ fontSize: "48px", marginBottom: "20px" }}>⚠️</div>
          <h2>Chưa có trạm được phân công</h2>
          <p>Vui lòng liên hệ quản trị viên để được phân công trạm sạc.</p>
        </div>
      </div>
    );

  const maxChartValue = Math.max(...chartData.map((d) => d.count), 1);
  const activeCount = activeSessions.length;
  const todayBookings = confirmedBookings.length;
  const completedToday = allSessions.filter(
    (s) => s.status === "COMPLETED"
  ).length;

  return (
    <div className="management-user-container">
      <Header />

      {/* Action Section */}
      <div className="action-section">
        <h2>Dashboard Trạm #{myStation.stationId}</h2>
        <button
          className="btn-add-staff"
          onClick={() => fetchDashboardData(myStation.stationId, true)}
          disabled={refreshing}
        >
          {refreshing ? "🔄 Đang làm mới..." : "🔄 Làm mới"}
        </button>
      </div>

      {/* Statistics Section */}
      <ul className="statistics-section">
        <li className="stat-card">
          ⚡ Phiên Đang Sạc
          <strong>{activeCount}</strong>
        </li>
        <li className="stat-card">
          📅 Booking Đã Xác Nhận
          <strong>{todayBookings}</strong>
        </li>
        <li className="stat-card">
          ✅ Hoàn Thành Hôm Nay
          <strong>{completedToday}</strong>
        </li>
        <li className="stat-card">
          📊 Tổng Phiên Sạc
          <strong>{allSessions.length}</strong>
        </li>
      </ul>

      {/* Table Section */}
      <div className="table-section">
        <div className="table-scroll-container">
          <div className="dashboard-content">
            <div className="dashboard-left">
              <div className="dashboard-card">
                <h2 className="card-title">
                  Phiên Sạc Đang Hoạt Động
                  <span className="badge">{activeCount}</span>
                </h2>
                <div className="sessions-list">
                  {activeSessions.length > 0 ? (
                    activeSessions.map((session) => {
                      const badge = getSessionStatusBadge(session.status);
                      return (
                        <div key={session.sessionId} className="session-item">
                          <div className="session-header">
                            <span className="session-id">
                              #{session.sessionId}
                            </span>
                            <span
                              className="session-status"
                              style={{ backgroundColor: badge.color }}
                            >
                              {badge.text}
                            </span>
                          </div>
                          <div className="session-info">
                            <p>🚗 {session.vehiclePlate || "N/A"}</p>
                            <p>🔌 Cổng #{session.chargingPointId || "N/A"}</p>
                            <p>
                              🕐 Bắt đầu: {formatDateTime(session.startTime)}
                            </p>
                            {session.estimatedEndTime && (
                              <p>
                                ⏰ Dự kiến:{" "}
                                {formatTime(session.estimatedEndTime)}
                              </p>
                            )}
                          </div>
                        </div>
                      );
                    })
                  ) : (
                    <div className="no-data">
                      Không có phiên sạc đang hoạt động
                    </div>
                  )}
                </div>
              </div>

              <div className="dashboard-card">
                <h2 className="card-title">Phiên Sạc Theo Giờ (Hôm Nay)</h2>
                <div className="chart-container">
                  {chartData.length > 0 ? (
                    <div className="bar-chart">
                      {chartData.map((item, i) => (
                        <div key={i} className="chart-bar">
                          <div
                            className="bar-fill"
                            style={{
                              height: `${(item.count / maxChartValue) * 100}%`,
                              minHeight: item.count > 0 ? "5%" : "0%",
                            }}
                            title={`${item.count} phiên`}
                          >
                            <span className="bar-value">{item.count}</span>
                          </div>
                          <span className="bar-label">{item.hour}</span>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <div className="no-data">Chưa có dữ liệu</div>
                  )}
                </div>
              </div>

              <div className="dashboard-card">
                <h2 className="card-title">
                  Lịch Sử Phiên Sạc
                  <span className="badge">{allSessions.length}</span>
                </h2>
                <div className="sessions-list sessions-history">
                  {allSessions.slice(0, 10).map((session) => {
                    const badge = getSessionStatusBadge(session.status);
                    return (
                      <div
                        key={session.sessionId}
                        className="session-item compact"
                      >
                        <div className="session-header">
                          <span className="session-id">
                            #{session.sessionId}
                          </span>
                          <span
                            className="session-status"
                            style={{ backgroundColor: badge.color }}
                          >
                            {badge.text}
                          </span>
                        </div>
                        <div className="session-info">
                          <p>🚗 {session.vehiclePlate || "N/A"}</p>
                          <p>🕐 {formatDateTime(session.startTime)}</p>
                        </div>
                      </div>
                    );
                  })}
                  {allSessions.length === 0 && (
                    <div className="no-data">Chưa có phiên sạc nào</div>
                  )}
                </div>
              </div>
            </div>

            <div className="dashboard-right">
              <div className="dashboard-card">
                <h2 className="card-title">
                  Booking Đã Xác Nhận
                  <span className="badge">{confirmedBookings.length}</span>
                </h2>
                <div className="bookings-list">
                  {confirmedBookings.length > 0 ? (
                    confirmedBookings.map((booking) => {
                      const badge = getBookingStatusBadge(booking.status);
                      return (
                        <div key={booking.bookingId} className="booking-item">
                          <div className="booking-header">
                            <span className="booking-id">
                              #{booking.bookingId}
                            </span>
                            <span
                              className="booking-status"
                              style={{ backgroundColor: badge.color }}
                            >
                              {badge.text}
                            </span>
                          </div>
                          <div className="booking-info">
                            <p>👤 {booking.driverName || "N/A"}</p>
                            <p>🚗 {booking.vehiclePlate || "N/A"}</p>
                            <p>📍 Trạm #{myStation.stationId}</p>
                            <p>
                              🕐 {formatDateTime(booking.scheduledStartTime)}
                            </p>
                          </div>
                        </div>
                      );
                    })
                  ) : (
                    <div className="no-data">Chưa có booking</div>
                  )}
                </div>
              </div>

              <div className="dashboard-card">
                <h2 className="card-title">Hoạt Động Gần Đây</h2>
                <div className="activities-list">
                  {recentActivities.length > 0 ? (
                    recentActivities.map((activity, i) => (
                      <div
                        key={`${activity.type}-${activity.id}-${i}`}
                        className="activity-item"
                      >
                        <div className="activity-icon">
                          {getActivityIcon(activity.type)}
                        </div>
                        <div className="activity-content">
                          <p className="activity-description">
                            {activity.description}
                          </p>
                          <span className="activity-time">
                            {formatDateTime(activity.timestamp)}
                          </span>
                        </div>
                      </div>
                    ))
                  ) : (
                    <div className="no-data">Chưa có hoạt động nào</div>
                  )}
                </div>
              </div>

              {generalStats && (
                <div className="dashboard-card">
                  <h2 className="card-title">Thống Kê Hệ Thống</h2>
                  <div className="system-stats">
                    {[
                      {
                        label: "Phiên Sạc Hệ Thống:",
                        value: generalStats.activeSessions,
                      },
                      {
                        label: "Booking Hôm Nay:",
                        value: generalStats.todayBookings,
                      },
                      {
                        label: "Doanh Thu:",
                        value: formatCurrency(generalStats.todayRevenue),
                      },
                    ].map((stat, i) => (
                      <div key={i} className="system-stat-item">
                        <span className="stat-label">{stat.label}</span>
                        <span className="stat-value">{stat.value}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
