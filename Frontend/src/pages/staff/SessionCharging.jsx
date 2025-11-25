import React, { useEffect, useState, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import Nav from "react-bootstrap/Nav";
import Table from "react-bootstrap/Table";
import "../admin/ManagementUser.css";
import Header from "../../components/admin/Header.jsx";
import paths from "../../path/paths.jsx";
import { stationAPI } from "../../api/stationApi.js";
import { staffStopSessionApi } from "../../api/staffApi.js";

const POLL_MS = 50000; // 5 minutes

export default function SessionCharging() {
  const navigate = useNavigate();

  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [stationId, setStationId] = useState(null);
  const [activeTab, setActiveTab] = useState("ongoing");
  const pollingRef = useRef(null);
  const isFetchingRef = useRef(false);

  const loadSessionsForStation = async (sid) => {
    if (!sid) return;
    try {
      if (isFetchingRef.current) return; // avoid overlapping
      isFetchingRef.current = true;
      setLoading(true);
      const res = await stationAPI.getChargingSessionsByStation(sid);
      if (!res || res.success === false) {
        // only update if different
        if (sessions.length !== 0) setSessions([]);
        return;
      }
      const data = res.data ?? res;
      const arr = Array.isArray(data) ? data : [data];
      // shallow compare lengths and JSON as fallback to avoid unnecessary re-renders
      const prev = sessions;
      const sameLength = prev.length === arr.length;
      const sameContent =
        sameLength && JSON.stringify(prev) === JSON.stringify(arr);
      if (!sameContent) setSessions(arr);
    } catch (err) {
      console.error("Error loading sessions for station:", err);
      if (sessions.length !== 0) setSessions([]);
    } finally {
      isFetchingRef.current = false;
      setLoading(false);
    }
  };

  // init: find stationId (localStorage or API) and load sessions once
  useEffect(() => {
    let mounted = true;
    const init = async () => {
      try {
        const stored = localStorage.getItem("stationId");
        if (stored) {
          setStationId(stored);
          try {
            setLoading(true);
            const r = await stationAPI.getChargingSessionsByStation(stored);
            const d = r?.data ?? r;
            const arr = Array.isArray(d) ? d : [d];
            setSessions(arr);
          } catch {
            /* ignore init fetch errors */
          } finally {
            setLoading(false);
          }
          return;
        }

        const res = await stationAPI.getStationStaffMe();
        const staff = res?.data?.data ?? res?.data ?? res;
        const staffObj = Array.isArray(staff) ? staff[0] : staff;
        const resolved =
          staffObj?.stationId ??
          staffObj?.station?.id ??
          staffObj?.station_id ??
          staffObj?.id ??
          undefined;
        if (resolved && mounted) {
          localStorage.setItem("stationId", String(resolved));
          setStationId(String(resolved));
          try {
            setLoading(true);
            const r2 = await stationAPI.getChargingSessionsByStation(
              String(resolved)
            );
            const d2 = r2?.data ?? r2;
            const arr2 = Array.isArray(d2) ? d2 : [d2];
            setSessions(arr2);
          } catch {
            /* ignore init fetch errors */
          } finally {
            setLoading(false);
          }
        }
      } catch {
        console.warn("Could not init stationStaff in SessionCharging");
      }
    };

    init();

    return () => {
      mounted = false;
      if (pollingRef.current) {
        clearInterval(pollingRef.current);
        pollingRef.current = null;
      }
    };
  }, []);

  const isOngoingStatus = (s) => {
    const st = String(s?.status ?? "").toUpperCase();
    return (
      st === "IN_PROGRESS" ||
      st === "CHARGING" ||
      st === "ACTIVE" ||
      st === "ONGOING"
    );
  };

  const ongoing = sessions.filter((s) => isOngoingStatus(s));
  const completed = sessions.filter((s) => !isOngoingStatus(s));

  // polling: when there is any ongoing session, poll every POLL_MS to refresh the list
  // depend only on stationId and number of ongoing sessions to avoid effect churn
  useEffect(() => {
    const sid = stationId ?? localStorage.getItem("stationId");
    if (!sid) return;

    const ongoingCount = ongoing.length;

    if (ongoingCount > 0) {
      if (!pollingRef.current) {
        // immediate refresh if not currently fetching
        if (!isFetchingRef.current) loadSessionsForStation(sid);
        pollingRef.current = setInterval(() => {
          if (!isFetchingRef.current) loadSessionsForStation(sid);
        }, POLL_MS);
      }
    } else {
      if (pollingRef.current) {
        clearInterval(pollingRef.current);
        pollingRef.current = null;
      }
    }

    return () => {
      if (pollingRef.current) {
        clearInterval(pollingRef.current);
        pollingRef.current = null;
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [stationId, ongoing.length]);

  const formatDate = (raw) => {
    if (!raw) return "-";
    try {
      return new Date(raw).toLocaleString();
    } catch {
      return String(raw);
    }
  };

  const handleStopSession = async (sessionId, e) => {
    e.stopPropagation(); // Prevent row click navigation

    if (
      !window.confirm(`Bạn có chắc chắn muốn dừng phiên sạc #${sessionId}?`)
    ) {
      return;
    }

    try {
      // ✅ Đọc virtualSoc từ sessionStorage (nếu Driver đang chạy phiên sạc)
      let finalSocFromDriver = null;
      try {
        const liveDataKey = `session_${sessionId}_live_soc`;
        const liveDataStr = sessionStorage.getItem(liveDataKey);
        if (liveDataStr) {
          const liveData = JSON.parse(liveDataStr);
          // Kiểm tra dữ liệu không quá cũ (trong vòng 30 giây)
          const age = Date.now() - (liveData.timestamp || 0);
          if (age < 30000 && liveData.virtualSoc != null) {
            finalSocFromDriver = liveData.virtualSoc;
            console.log(
              `✅ Found live SOC from Driver: ${finalSocFromDriver}% (age: ${Math.round(
                age / 1000
              )}s)`
            );
          } else {
            console.log(
              `⚠️ Live SOC data is too old (${Math.round(
                age / 1000
              )}s), ignoring`
            );
          }
        }
      } catch (err) {
        console.debug("Could not read live SOC from sessionStorage:", err);
      }

      // ✅ Gửi finalSoc kèm trong API call (nếu có)
      const response = await staffStopSessionApi(sessionId, finalSocFromDriver);

      if (response.success) {
        toast.success("Đã dừng phiên sạc thành công!");

        // ✅ Xóa live SOC data sau khi dừng thành công
        try {
          const liveDataKey = `session_${sessionId}_live_soc`;
          sessionStorage.removeItem(liveDataKey);
        } catch (err) {
          console.debug("Failed to remove live SOC:", err);
        }

        // Reload sessions after stopping
        const sid = stationId ?? localStorage.getItem("stationId");
        if (sid) {
          await loadSessionsForStation(sid);
        }
      } else {
        toast.error(response.message || "Dừng phiên sạc thất bại!");
      }
    } catch (error) {
      console.error("Error stopping session:", error);
      toast.error("Có lỗi xảy ra khi dừng phiên sạc!");
    }
  };

  return (
    <div className="management-user-container">
      <Header />

      <div className="action-section">
        <h2>Quản lý phiên sạc</h2>
        <div>
          <button
            style={{ marginRight: "10px" }}
            className="btn-add-staff"
            onClick={() => navigate(paths.instantCharging)}
          >
            ➕ Tạo phiên sạc
          </button>
          <button
            className="btn-add-staff"
            onClick={() =>
              navigate(paths.manageSessionChargingCreate, {
                state: { openCamera: true },
              })
            }
          >
            ➕ Khởi động phiên sạc
          </button>
        </div>
      </div>

      <ul className="statistics-section">
        <li className="stat-card">
          Đang sạc
          <strong>{ongoing.length}</strong>
        </li>
        <li className="stat-card">
          Đã hoàn thành
          <strong>{completed.length}</strong>
        </li>
        <li className="stat-card">
          Tổng phiên sạc
          <strong>{sessions.length}</strong>
        </li>
      </ul>

      <div className="table-section">
        <div className="table-scroll-container">
          <div className="filter-section">
            <Nav
              justify
              variant="tabs"
              activeKey={activeTab}
              onSelect={(k) => setActiveTab(k)}
            >
              <Nav.Item>
                <Nav.Link eventKey="ongoing">Đang sạc</Nav.Link>
              </Nav.Item>
              <Nav.Item>
                <Nav.Link eventKey="completed">Đã hoàn thành</Nav.Link>
              </Nav.Item>
            </Nav>
          </div>

          {loading && (
            <div style={{ textAlign: "center", padding: "30px" }}>
              Đang tải danh sách phiên sạc...
            </div>
          )}

          {!loading &&
            activeTab === "ongoing" &&
            (ongoing.length === 0 ? (
              <div style={{ textAlign: "center", padding: "30px" }}>
                Hiện không có phiên đang hoạt động
              </div>
            ) : (
              <Table className="custom-table">
                <thead>
                  <tr>
                    <th>MÃ PHIÊN</th>
                    <th>CHI PHÍ</th>
                    <th>THỜI GIAN BẮT ĐẦU</th>
                    <th>THỜI LƯỢNG (PHÚT)</th>
                    <th>NĂNG LƯỢNG (KWH)</th>
                    <th>SOC ĐẦU</th>
                    <th>SOC CUỐI</th>
                    <th>TRẠNG THÁI</th>
                    <th>MÃ BOOKING</th>
                    <th>HÀNH ĐỘNG</th>
                  </tr>
                </thead>
                <tbody>
                  {ongoing.map((s, idx) => {
                    const sessionId =
                      s?.sessionId ??
                      s?.sessionid ??
                      s?.session_id ??
                      s?.id ??
                      `${idx}`;
                    const bookingId =
                      s?.bookingId ?? s?.bookingid ?? s?.booking_id ?? null;
                    const cost = s?.cost ?? 0;
                    const startTime = formatDate(s?.startTime ?? s?.start_time);
                    const durationMinutes =
                      s?.durationMinutes ??
                      s?.duration_minutes ??
                      s?.duration ??
                      0;
                    const energyKWh =
                      s?.energyKWh ?? s?.energykwh ?? s?.energy ?? 0;
                    const initialSoc =
                      s?.initialSoc ?? s?.initial_soc ?? s?.initial ?? "-";
                    const finalSoc =
                      s?.finalSoc ?? s?.final_soc ?? s?.final ?? "-";
                    const status = s?.status ?? "";
                    const statusKey = String(status)
                      .toLowerCase()
                      .replace(/\s+/g, "_");
                    return (
                      <tr
                        key={sessionId}
                        onClick={() =>
                          navigate(paths.manageSessionChargingCreate, {
                            state: { openCamera: true, bookingId },
                          })
                        }
                        style={{ cursor: "pointer" }}
                      >
                        <td>{sessionId}</td>
                        <td>
                          {Number(cost).toLocaleString()} {s?.currency ?? ""}
                        </td>
                        <td>{startTime}</td>
                        <td>{durationMinutes}</td>
                        <td>{energyKWh}</td>
                        <td>{initialSoc}</td>
                        <td>{finalSoc ?? "-"}</td>
                        <td>{statusKey}</td>
                        <td>{bookingId}</td>
                        <td>
                          <button
                            className="btn btn-danger btn-sm"
                            onClick={(e) => handleStopSession(sessionId, e)}
                            style={{
                              padding: "4px 12px",
                              fontSize: "13px",
                              fontWeight: "500",
                            }}
                          >
                            ⏹ Stop
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </Table>
            ))}

          {!loading &&
            activeTab === "completed" &&
            (completed.length === 0 ? (
              <div style={{ textAlign: "center", padding: "30px" }}>
                Chưa có phiên sạc hoàn thành
              </div>
            ) : (
              <Table className="custom-table">
                <thead>
                  <tr>
                    <th>MÃ PHIÊN</th>
                    <th>CHI PHÍ</th>
                    <th>THỜI GIAN BẮT ĐẦU</th>
                    <th>THỜI GIAN KẾT THÚC</th>
                    <th>THỜI LƯỢNG (PHÚT)</th>
                    <th>NĂNG LƯỢNG (KWH)</th>
                    <th>SOC ĐẦU</th>
                    <th>SOC CUỐI</th>
                    <th>TRẠNG THÁI</th>
                    <th>MÃ BOOKING</th>
                  </tr>
                </thead>
                <tbody>
                  {completed.map((s, idx) => {
                    const sessionId =
                      s?.sessionId ??
                      s?.sessionid ??
                      s?.session_id ??
                      s?.id ??
                      `${idx}`;
                    const bookingId =
                      s?.bookingId ?? s?.bookingid ?? s?.booking_id ?? null;
                    const cost = s?.cost ?? 0;
                    const startTime = formatDate(s?.startTime ?? s?.start_time);
                    const endTime = formatDate(s?.endTime ?? s?.end_time);
                    const durationMinutes =
                      s?.durationMinutes ??
                      s?.duration_minutes ??
                      s?.duration ??
                      0;
                    const energyKWh =
                      s?.energyKWh ?? s?.energykwh ?? s?.energy ?? 0;
                    const initialSoc =
                      s?.initialSoc ?? s?.initial_soc ?? s?.initial ?? "-";
                    const finalSoc =
                      s?.finalSoc ?? s?.final_soc ?? s?.final ?? "-";
                    const status = s?.status ?? "";
                    const statusKey = String(status)
                      .toLowerCase()
                      .replace(/\s+/g, "_");
                    return (
                      <tr key={sessionId}>
                        <td>{sessionId}</td>
                        <td>
                          {Number(cost).toLocaleString()} {s?.currency ?? ""}
                        </td>
                        <td>{startTime}</td>
                        <td>{endTime ?? "-"}</td>
                        <td>{durationMinutes}</td>
                        <td>{energyKWh}</td>
                        <td>{initialSoc}</td>
                        <td>{finalSoc}</td>
                        <td>{statusKey}</td>
                        <td>{bookingId}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </Table>
            ))}
        </div>
      </div>
    </div>
  );
}
