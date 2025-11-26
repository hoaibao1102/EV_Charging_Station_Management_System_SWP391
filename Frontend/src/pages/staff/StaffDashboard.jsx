import { useState, useEffect, useMemo } from "react";
import { toast } from "react-toastify";
// 1. Cập nhật import: Dùng AreaChart và Area thay vì LineChart/Line
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";

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

  // --- LOGIC XỬ LÝ DỮ LIỆU BIỂU ĐỒ ---
  const realTimeChartData = useMemo(() => {
    if (!allSessions || allSessions.length === 0) return [];

    const todayStart = new Date();
    todayStart.setHours(0, 0, 0, 0);
    const todayEnd = new Date();
    todayEnd.setHours(23, 59, 59, 999);

    const todaySessions = allSessions.filter((s) => {
      const startTime = new Date(s.startTime);
      return startTime >= todayStart && startTime <= todayEnd;
    });

    let timePoints = [];
    todaySessions.forEach((s) => {
      timePoints.push({ time: new Date(s.startTime).getTime(), type: "start" });
      const endTime = s.endTime ? new Date(s.endTime).getTime() : Date.now();
      if (endTime <= todayEnd.getTime()) {
        timePoints.push({ time: endTime, type: "end" });
      }
    });

    timePoints.sort((a, b) => a.time - b.time);

    let currentCount = 0;
    const data = timePoints.map((point) => {
      if (point.type === "start") currentCount += 1;
      else currentCount -= 1;

      return {
        timestamp: point.time,
        count: currentCount,
        timeLabel: new Date(point.time).toLocaleTimeString("vi-VN", {
          hour: "2-digit",
          minute: "2-digit",
        }),
      };
    });

    if (data.length > 0) {
      if (data[0].timestamp > todayStart.getTime()) {
        data.unshift({
          timestamp: todayStart.getTime(),
          count: 0,
          timeLabel: "00:00",
        });
      }
    }

    return data;
  }, [allSessions]);
  // -----------------------------------

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

  const activeCount = activeSessions.length;
  const todayBookings = confirmedBookings.length;
  const completedToday = allSessions.filter(
    (s) => s.status === "COMPLETED"
  ).length;

  return (
    <div className="management-user-container">
      <Header />

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

      <ul className="statistics-section">
        <li className="stat-card">
          ⚡ Phiên Đang Sạc <strong>{activeCount}</strong>
        </li>
        <li className="stat-card">
          📅 Booking Đã Xác Nhận <strong>{todayBookings}</strong>
        </li>
        <li className="stat-card">
          ✅ Hoàn Thành Hôm Nay <strong>{completedToday}</strong>
        </li>
        <li className="stat-card">
          📊 Tổng Phiên Sạc <strong>{allSessions.length}</strong>
        </li>
      </ul>

      <div className="table-section">
        <div className="table-scroll-container">
          <div className="dashboard-content">
            <div className="dashboard-left">
              <div className="dashboard-card">
                <h2 className="card-title">
                  Phiên Sạc Đang Hoạt Động{" "}
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
                              style={{
                                backgroundColor: "#20b2aa",
                                color: "#fff",
                                borderRadius: "20px",
                                height: "24px",
                                lineHeight: "24px",
                                padding: "0 10px",
                                fontSize: "12px",
                              }}
                            >
                              {badge.text}
                            </span>
                          </div>
                          <div className="session-info">
                            <p>🚗 {session.vehiclePlate || "N/A"}</p>
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

              {/* ----- BIỂU ĐỒ VÙNG (AREA CHART) ----- */}
              <div className="dashboard-card">
                <h2 className="card-title">Lưu Lượng Sạc (Thời Gian Thực)</h2>
                <div
                  className="chart-container"
                  style={{ width: "100%", height: "300px" }}
                >
                  {realTimeChartData.length > 0 ? (
                    <ResponsiveContainer width="100%" height="100%">
                      <AreaChart
                        data={realTimeChartData}
                        margin={{ top: 10, right: 10, left: 0, bottom: 0 }}
                      >
                        {/* Định nghĩa Gradient màu */}
                        <defs>
                          <linearGradient
                            id="colorCount"
                            x1="0"
                            y1="0"
                            x2="0"
                            y2="1"
                          >
                            <stop
                              offset="5%"
                              stopColor="#20b2aa"
                              stopOpacity={0.8}
                            />
                            <stop
                              offset="95%"
                              stopColor="#20b2aa"
                              stopOpacity={0}
                            />
                          </linearGradient>
                        </defs>

                        <CartesianGrid
                          strokeDasharray="3 3"
                          vertical={false}
                          stroke="#eee"
                        />

                        <XAxis
                          dataKey="timestamp"
                          type="number"
                          domain={["dataMin", "dataMax"]}
                          tickFormatter={(unixTime) =>
                            new Date(unixTime).toLocaleTimeString("vi-VN", {
                              hour: "2-digit",
                              minute: "2-digit",
                            })
                          }
                          scale="time"
                          tick={{ fontSize: 12, fill: "#666" }}
                        />

                        <YAxis
                          allowDecimals={false}
                          tick={{ fontSize: 12, fill: "#666" }}
                        />

                        <Tooltip
                          labelFormatter={(label) =>
                            new Date(label).toLocaleTimeString("vi-VN")
                          }
                          formatter={(value) => [value, "Xe đang sạc"]}
                          contentStyle={{
                            borderRadius: "8px",
                            border: "none",
                            boxShadow: "0 4px 12px rgba(0,0,0,0.15)",
                          }}
                        />

                        {/* Dùng Area với type monotone để đường cong mềm mại */}
                        <Area
                          type="monotone"
                          dataKey="count"
                          stroke="#20b2aa"
                          strokeWidth={3}
                          fillOpacity={1}
                          fill="url(#colorCount)"
                          dot={false}
                          activeDot={{ r: 6, strokeWidth: 0 }}
                          animationDuration={1500}
                        />
                      </AreaChart>
                    </ResponsiveContainer>
                  ) : (
                    <div className="no-data">Chưa có dữ liệu hôm nay</div>
                  )}
                </div>
              </div>
              {/* ----------------------------------- */}

              <div className="dashboard-card">
                <h2 className="card-title" style={{ marginBottom: "10px" }}>
                  Lịch Sử Phiên Sạc{" "}
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
                            style={{
                              backgroundColor: "#20b2aa",
                              borderRadius: "20px",
                              color: "#fff",
                              height: "24px",
                              lineHeight: "24px",
                              padding: "0 10px",
                              fontSize: "12px",
                            }}
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
                  Booking Đã Xác Nhận{" "}
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
                              #--{booking.connector}
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
                              🕐 {formatDateTime(booking.startTime)} --{" "}
                              {formatDateTime(booking.endTime)}
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
                <h2 className="card-title" style={{ marginBottom: "10px" }}>
                  Hoạt Động Gần Đây
                </h2>
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
                        label: "Phiên Sạc Đang Hoạt Động:",
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
