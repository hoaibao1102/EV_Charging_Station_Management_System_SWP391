import React from "react";
import { useNavigate } from "react-router-dom";
import "./SessionCharging.css";
import paths from "../../path/paths.jsx";

const SAMPLE_SESSIONS = [
  // ongoing (status: charging)
  {
    sessionid: 1001,
    cost: 12.5,
    created_at: "2025-10-28T08:00:00",
    duration_minutes: 25,
    end_time: null,
    energykwh: 8.2,
    final_soc: null,
    initial_soc: 20,
    start_time: "2025-10-28T08:05:00",
    status: "charging",
    updated_at: "2025-10-28T08:30:00",
    bookingid: 501,
  },
  {
    sessionid: 1002,
    cost: 3.0,
    created_at: "2025-10-28T09:15:00",
    duration_minutes: 10,
    end_time: null,
    energykwh: 2.1,
    final_soc: null,
    initial_soc: 60,
    start_time: "2025-10-28T09:18:00",
    status: "charging",
    updated_at: "2025-10-28T09:25:00",
    bookingid: 502,
  },

  // completed
  {
    sessionid: 9001,
    cost: 25.0,
    created_at: "2025-10-27T12:00:00",
    duration_minutes: 120,
    end_time: "2025-10-27T14:00:00",
    energykwh: 45.3,
    final_soc: 95,
    initial_soc: 10,
    start_time: "2025-10-27T12:00:00",
    status: "completed",
    updated_at: "2025-10-27T14:05:00",
    bookingid: 401,
  },
  {
    sessionid: 9002,
    cost: 7.5,
    created_at: "2025-10-26T18:30:00",
    duration_minutes: 30,
    end_time: "2025-10-26T19:00:00",
    energykwh: 12.0,
    final_soc: 80,
    initial_soc: 55,
    start_time: "2025-10-26T18:30:00",
    status: "completed",
    updated_at: "2025-10-26T19:02:00",
    bookingid: 402,
  },
];

export default function SessionCharging() {
  const navigate = useNavigate();

  // Split sessions: ongoing first (status = charging), then completed
  const ongoing = SAMPLE_SESSIONS.filter((s) => s.status === "charging");
  const completed = SAMPLE_SESSIONS.filter((s) => s.status !== "charging");

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

      <section className="session-section">
        <h2>Đang sạc ({ongoing.length})</h2>
        <div className="table-wrap">
          <table className="session-table">
            <thead>
              <tr>
                <th>sessionid</th>
                <th>cost</th>
                <th>start_time</th>
                <th>duration_minutes</th>
                <th>energykwh</th>
                <th>initial_soc</th>
                <th>final_soc</th>
                <th>status</th>
                <th>bookingid</th>
              </tr>
            </thead>
            <tbody>
              {ongoing.map((s) => (
                <tr
                  key={s.sessionid}
                  className="row-ongoing"
                  role="button"
                  onClick={() =>
                    navigate(paths.manageSessionChargingCreate, {
                      state: { openCamera: true, bookingId: s.bookingid },
                    })
                  }
                  style={{ cursor: "pointer" }}
                  title={`Mở camera để quét/khởi động phiên cho booking ${s.bookingid}`}
                >
                  <td>{s.sessionid}</td>
                  <td>{s.cost}</td>
                  <td>{s.start_time}</td>
                  <td>{s.duration_minutes}</td>
                  <td>{s.energykwh}</td>
                  <td>{s.initial_soc}</td>
                  <td>{s.final_soc ?? "-"}</td>
                  <td>{s.status}</td>
                  <td>{s.bookingid}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="session-section">
        <h2>Đã hoàn thành ({completed.length})</h2>
        <div className="table-wrap">
          <table className="session-table">
            <thead>
              <tr>
                <th>sessionid</th>
                <th>cost</th>
                <th>start_time</th>
                <th>end_time</th>
                <th>duration_minutes</th>
                <th>energykwh</th>
                <th>initial_soc</th>
                <th>final_soc</th>
                <th>status</th>
                <th>bookingid</th>
              </tr>
            </thead>
            <tbody>
              {completed.map((s) => (
                <tr key={s.sessionid} className="row-completed">
                  <td>{s.sessionid}</td>
                  <td>{s.cost}</td>
                  <td>{s.start_time}</td>
                  <td>{s.end_time ?? "-"}</td>
                  <td>{s.duration_minutes}</td>
                  <td>{s.energykwh}</td>
                  <td>{s.initial_soc}</td>
                  <td>{s.final_soc}</td>
                  <td>{s.status}</td>
                  <td>{s.bookingid}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}
