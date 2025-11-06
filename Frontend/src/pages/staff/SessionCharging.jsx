import React, { useEffect, useState, useRef } from "react";
import { useNavigate } from "react-router-dom";
import "./SessionCharging.css";
import paths from "../../path/paths.jsx";
import { stationAPI } from "../../api/stationApi.js";

const POLL_MS = 150000; // 5 minutes

export default function SessionCharging() {
  const navigate = useNavigate();

  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [stationId, setStationId] = useState(null);
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

  return (
    <div className="session-container">
      <div className="session-header">
        <h1>Quản lý phiên sạc</h1>
        <div>
          <button
            className="btn-primary"
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

      <div className="table-meta">
        Danh sách phiên sạc của trạm {stationId ?? "(chưa xác định)"}
      </div>

      {loading && (
        <p style={{ marginTop: 8, color: "#666" }}>
          Đang tải danh sách phiên sạc...
        </p>
      )}

      <section className="session-section">
        <h2>Đang sạc ({ongoing.length})</h2>
        {ongoing.length === 0 ? (
          <div className="no-sessions">Hiện không có phiên đang hoạt động</div>
        ) : (
          <div className="table-wrap">
            <table className="session-table">
              <thead>
                <tr>
                  <th>sessionId</th>
                  <th>cost</th>
                  <th>startTime</th>
                  <th>durationMinutes</th>
                  <th>energyKWh</th>
                  <th>initialSoc</th>
                  <th>finalSoc</th>
                  <th>status</th>
                  <th>bookingId</th>
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
                      className="row-ongoing"
                      role="button"
                      onClick={() =>
                        navigate(paths.manageSessionChargingCreate, {
                          state: { openCamera: true, bookingId },
                        })
                      }
                      style={{ cursor: "pointer" }}
                      title={`Mở camera để quét/khởi động phiên cho booking ${bookingId}`}
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
                      <td>
                        <span className={`status-badge status-${statusKey}`}>
                          {status}
                        </span>
                      </td>
                      <td>{bookingId}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section className="session-section">
        <h2>Đã hoàn thành ({completed.length})</h2>
        {completed.length === 0 ? (
          <div className="no-sessions">Chưa có phiên sạc hoàn thành</div>
        ) : (
          <div className="table-wrap">
            <table className="session-table">
              <thead>
                <tr>
                  <th>sessionId</th>
                  <th>cost</th>
                  <th>startTime</th>
                  <th>endTime</th>
                  <th>durationMinutes</th>
                  <th>energyKWh</th>
                  <th>initialSoc</th>
                  <th>finalSoc</th>
                  <th>status</th>
                  <th>bookingId</th>
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
                    <tr key={sessionId} className="row-completed">
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
                      <td>
                        <span className={`status-badge status-${statusKey}`}>
                          {status}
                        </span>
                      </td>
                      <td>{bookingId}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
