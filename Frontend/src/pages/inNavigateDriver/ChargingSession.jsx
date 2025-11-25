import React, { useEffect, useState, useCallback } from "react";
import { useNavigate, useLocation, useParams } from "react-router-dom";
import paths from "../../path/paths.jsx";
import { toast } from "react-toastify";
import { stationAPI } from "../../api/stationApi.js";
import { getMySessions } from "../../api/driverApi.js";
import { isAuthenticated } from "../../utils/authUtils.js";

// Add responsive styles to document
const styleSheet = document.createElement("style");
styleSheet.textContent = `
  @media (max-width: 768px) {
    .charging-session-container {
      padding: 10px !important;
    }
    .battery-progress-circle {
      width: 180px !important;
      height: 180px !important;
    }
    .battery-progress-circle svg {
      width: 180px !important;
      height: 180px !important;
    }
    .battery-progress-circle .center-text {
      font-size: 36px !important;
    }
    .info-card-grid {
      grid-template-columns: repeat(auto-fit, minmax(130px, 1fr)) !important;
    }
    .quick-info-grid {
      grid-template-columns: 1fr !important;
    }
  }
`;
if (!document.head.querySelector("style[data-charging-session-styles]")) {
  styleSheet.setAttribute("data-charging-session-styles", "true");
  document.head.appendChild(styleSheet);
}

// Battery Progress Circle Component - Enhanced with Smooth Animation
function BatteryProgressCircle({
  initialSoc,
  energyKWh,
  capacity,
  isCharging,
  virtualSoc, // Virtual SOC from physics-based estimation
}) {
  // Use virtual SOC if available (for smooth animation), otherwise calculate from energy
  const deltaPercent = (energyKWh / capacity) * 100;
  const calculatedSoc = Math.min(initialSoc + deltaPercent, 100);
  const currentSoc = virtualSoc ?? calculatedSoc;
  const isComplete = currentSoc >= 100;

  // ✨ Smooth SOC animation (interpolation from old to new value)
  const [animatedSoc, setAnimatedSoc] = useState(currentSoc);

  useEffect(() => {
    const diff = currentSoc - animatedSoc;
    if (Math.abs(diff) < 0.1) {
      setAnimatedSoc(currentSoc);
      return;
    }

    const step = diff / 20; // 20 frames for smooth transition
    const interval = setInterval(() => {
      setAnimatedSoc((prev) => {
        const next = prev + step;
        if (
          (diff > 0 && next >= currentSoc) ||
          (diff < 0 && next <= currentSoc)
        ) {
          clearInterval(interval);
          return currentSoc;
        }
        return next;
      });
    }, 50); // Update every 50ms

    return () => clearInterval(interval);
  }, [currentSoc, animatedSoc]);

  // SVG circle parameters - using animatedSoc for smooth fill
  const size = 240;
  const strokeWidth = 16;
  const radius = (size - strokeWidth) / 2;
  const circumference = 2 * Math.PI * radius;
  const offset = circumference - (animatedSoc / 100) * circumference;

  // Colors
  const progressColor = isComplete ? "#2196f3" : "#00BFA6";
  const trackColor = "#e0e0e0";

  return (
    <div
      className="battery-progress-circle"
      style={{
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        padding: "30px 20px",
        background: "linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%)",
        borderRadius: "20px",
        boxShadow: "0 4px 20px rgba(0,0,0,0.08)",
        margin: "0 auto 30px",
        maxWidth: "400px",
      }}
    >
      {/* Battery Icon Header */}
      <div
        style={{
          fontSize: "48px",
          marginBottom: "15px",
          animation:
            isCharging && !isComplete
              ? "pulse 2s ease-in-out infinite"
              : "none",
        }}
      >
        🔋
      </div>

      {/* SVG Circle */}
      <div style={{ position: "relative", marginBottom: "20px" }}>
        <svg
          width={size}
          height={size}
          style={{
            transform: "rotate(-90deg)",
            filter: "drop-shadow(0 2px 8px rgba(0,191,166,0.3))",
          }}
        >
          {/* Background track */}
          <circle
            cx={size / 2}
            cy={size / 2}
            r={radius}
            stroke={trackColor}
            strokeWidth={strokeWidth}
            fill="none"
          />
          {/* Progress arc */}
          <circle
            cx={size / 2}
            cy={size / 2}
            r={radius}
            stroke={progressColor}
            strokeWidth={strokeWidth}
            fill="none"
            strokeDasharray={circumference}
            strokeDashoffset={offset}
            strokeLinecap="round"
            style={{
              transition:
                "stroke-dashoffset 0.8s cubic-bezier(0.4, 0, 0.2, 1), stroke 0.3s ease",
            }}
          />
        </svg>

        {/* Center text */}
        <div
          style={{
            position: "absolute",
            top: "50%",
            left: "50%",
            transform: "translate(-50%, -50%)",
            textAlign: "center",
          }}
        >
          <div
            style={{
              fontSize: "48px",
              fontWeight: "800",
              color: progressColor,
              lineHeight: "1",
              marginBottom: "5px",
            }}
          >
            {animatedSoc.toFixed(1)}%
          </div>
          <div
            style={{
              fontSize: "13px",
              color: "#666",
              fontWeight: "500",
              textTransform: "uppercase",
              letterSpacing: "0.5px",
            }}
          >
            Pin hiện tại
          </div>
        </div>
      </div>

      {/* Caption */}
      <div
        style={{
          textAlign: "center",
          fontSize: "15px",
          color: isComplete ? "#2196f3" : isCharging ? "#00BFA6" : "#666",
          fontWeight: "600",
          padding: "10px 20px",
          background: isComplete
            ? "rgba(33, 150, 243, 0.1)"
            : isCharging
            ? "rgba(0, 191, 166, 0.1)"
            : "rgba(0, 0, 0, 0.05)",
          borderRadius: "20px",
        }}
      >
        {isComplete
          ? "✅ Hoàn tất sạc"
          : isCharging
          ? "⚡ Đang sạc..."
          : "Dung lượng pin (ước tính)"}
      </div>

      <style>{`
        @keyframes pulse {
          0%, 100% { transform: scale(1); opacity: 1; }
          50% { transform: scale(1.1); opacity: 0.8; }
        }
      `}</style>
    </div>
  );
}

// Info Card Component
function InfoCard({ icon, label, value, color = "#00BFA6", unit = "" }) {
  return (
    <div
      style={{
        background: "white",
        padding: "20px",
        borderRadius: "12px",
        boxShadow: "0 2px 12px rgba(0,0,0,0.06)",
        textAlign: "center",
        border: `2px solid ${color}15`,
        transition: "transform 0.2s ease, box-shadow 0.2s ease",
      }}
      onMouseEnter={(e) => {
        e.currentTarget.style.transform = "translateY(-3px)";
        e.currentTarget.style.boxShadow = "0 4px 20px rgba(0,0,0,0.12)";
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.transform = "translateY(0)";
        e.currentTarget.style.boxShadow = "0 2px 12px rgba(0,0,0,0.06)";
      }}
    >
      <div style={{ fontSize: "32px", marginBottom: "8px" }}>{icon}</div>
      <div
        style={{
          fontSize: "13px",
          color: "#666",
          marginBottom: "8px",
          fontWeight: "500",
        }}
      >
        {label}
      </div>
      <div
        style={{
          fontSize: "24px",
          fontWeight: "700",
          color: color,
        }}
      >
        {value}
        {unit && (
          <span
            style={{ fontSize: "16px", fontWeight: "500", marginLeft: "4px" }}
          >
            {unit}
          </span>
        )}
      </div>
    </div>
  );
}

export default function ChargingSession() {
  const navigate = useNavigate();
  const location = useLocation();
  const params = useParams();

  const [currentSession, setCurrentSession] = useState(null);
  const [loading, setLoading] = useState(false);
  const [stopping, setStopping] = useState(false);
  const [autoRedirected, setAutoRedirected] = useState(false);
  const [currentPower, setCurrentPower] = useState(0); // ✅ Track maxPowerKW separately

  // QR / booking state (merged behavior)
  const qrFromState = location?.state?.qrBlobUrl;
  const stateBooking = location?.state?.booking;
  const bookingIdFromParams = params?.bookingId;

  const [qrUrl, setQrUrl] = useState(qrFromState || null);
  const [booking, setBooking] = useState(stateBooking || null);
  const [bookingLoading, setBookingLoading] = useState(false);

  // Battery capacity constant (used by BatteryProgressCircle)
  const DEFAULT_BATTERY_CAPACITY = 60; // kWh

  // 🎨 Status color mapping for cleaner code
  const statusColors = {
    IN_PROGRESS: "#4caf50",
    COMPLETED: "#2196f3",
    FAILED: "#f44336",
    PENDING: "#ff9800",
  };

  // Helper to build sessionStorage key
  const qrStorageKey = (id) => (id ? `qr_booking_${id}` : null);

  // 🔋 Simulation state persistence helpers
  const getSimulationKey = (sessionId) =>
    sessionId ? `chargingSession_simulation_${sessionId}` : null;

  const saveSimState = (session) => {
    if (!session || !session.sessionId) return;
    try {
      const key = getSimulationKey(session.sessionId);
      if (!key) return;
      localStorage.setItem(
        key,
        JSON.stringify({
          sessionId: session.sessionId,
          virtualSoc: session.virtualSoc,
          energyKWh: session.energyKWh,
          durationMinutes: session.durationMinutes,
          lastUpdated: Date.now(),
          status: session.status,
        })
      );
    } catch (err) {
      console.debug("Failed to save simulation state:", err);
    }
  };

  const loadSimState = useCallback((sessionId) => {
    if (!sessionId) return null;
    try {
      const key = getSimulationKey(sessionId);
      if (!key) return null;
      const data = localStorage.getItem(key);
      return data ? JSON.parse(data) : null;
    } catch (err) {
      console.debug("Failed to load simulation state:", err);
      return null;
    }
  }, []);

  const clearSimState = useCallback(
    (sessionId) => {
      if (!sessionId) return;
      try {
        const key = getSimulationKey(sessionId);
        if (key) localStorage.removeItem(key);

        // ✅ Also clear maxPowerKW from sessionStorage
        if (currentSession?.bookingId) {
          const powerKey = `booking_${currentSession.bookingId}_maxPowerKW`;
          sessionStorage.removeItem(powerKey);
          console.log(
            `🗑️ Cleared maxPowerKW for booking #${currentSession.bookingId}`
          );
        }
      } catch (err) {
        console.debug("Failed to clear simulation state:", err);
      }
    },
    [currentSession]
  );

  // If navigation state didn't include qrBlobUrl, try to restore from sessionStorage (data URL)
  useEffect(() => {
    if (qrUrl) return; // already have one

    const attemptRestore = () => {
      // try bookingIdFromParams, booking object, then currentSession bookingId
      const idCandidates = [
        bookingIdFromParams,
        booking?.bookingId ?? booking?.id,
        currentSession?.bookingId,
      ];

      for (const id of idCandidates) {
        if (!id) continue;
        try {
          const key = qrStorageKey(id);
          const stored = key ? sessionStorage.getItem(key) : null;
          if (stored) {
            // stored is a data URL (base64) created at confirm time
            // use it as the qrUrl so <img src=qrUrl /> can render it
            setQrUrl(stored);
            return;
          }
        } catch {
          // ignore storage errors
        }
      }

      // fallback: if there's exactly one qr_booking_ key in sessionStorage, use it
      try {
        const keys = Object.keys(sessionStorage).filter(
          (k) => k && k.startsWith("qr_booking_")
        );
        if (keys.length === 1) {
          const s = sessionStorage.getItem(keys[0]);
          if (s) setQrUrl(s);
        }
      } catch {
        // ignore
      }
    };

    attemptRestore();
  }, [booking, bookingIdFromParams, qrUrl, currentSession]);

  useEffect(() => {
    if (!isAuthenticated()) {
      toast.warning(
        "Bạn chưa đăng nhập. Vui lòng đăng nhập để xem phiên sạc!",
        {
          position: "top-center",
          autoClose: 3000,
        }
      );
      navigate(paths.login);
      return;
    }

    fetchCurrentSession();
  }, [navigate]);

  const fetchCurrentSession = async () => {
    try {
      setLoading(true);
      const response = await stationAPI.getCurrentChargingSession();
      if (!response || response.success === false) {
        console.log("❌ No current session");
        setCurrentSession(null);
        setCurrentPower(0);
        return;
      }
      const session = response.data ?? response;
      console.log("✅ Current session data:", session);

      // ✅ Try to get maxPowerKW from sessionStorage first (saved during booking)
      let power = 0;
      const bookingId = session.bookingId;

      console.log("🔍 DEBUG - Looking for power with bookingId:", bookingId);
      console.log(
        "🔍 DEBUG - SessionStorage keys:",
        Object.keys(sessionStorage)
      );

      if (bookingId) {
        try {
          const key = `booking_${bookingId}_maxPowerKW`;
          console.log("🔍 DEBUG - Looking for key:", key);
          const storedPower = sessionStorage.getItem(key);
          console.log("🔍 DEBUG - Found value:", storedPower);

          if (storedPower) {
            power = JSON.parse(storedPower);
            console.log(
              `✅ Retrieved maxPowerKW=${power} from sessionStorage for booking #${bookingId}`
            );
          } else {
            console.warn(
              `❌ No maxPowerKW found in sessionStorage for booking #${bookingId}`
            );
          }
        } catch (e) {
          console.warn("Failed to retrieve maxPowerKW from sessionStorage:", e);
        }
      } else {
        console.warn("❌ No bookingId in session object");
      }

      // ✅ Fallback to response data if not in sessionStorage
      if (!power) {
        power =
          session.chargingPoint?.maxPowerKW ??
          session.maxPowerKW ??
          session.ratedKW ??
          session.powerKW ??
          response.maxPowerKW ??
          11.0; // Default fallback
        console.log("🔍 Final extracted maxPowerKW from API:", power);
      }

      setCurrentPower(power);
      setCurrentSession(session);
    } catch (error) {
      console.error("Lỗi khi lấy phiên sạc hiện tại:", error);
      toast.error("Không thể lấy thông tin phiên sạc", {
        position: "top-center",
      });
    } finally {
      setLoading(false);
    }
  };

  // ⚡ Polling maxPowerKW: Update charging power realtime when database changes
  useEffect(() => {
    if (!currentSession || currentSession.status !== "IN_PROGRESS") return;

    const pollPowerInterval = setInterval(async () => {
      try {
        const response = await stationAPI.getCurrentChargingSession();
        const updatedSession = response.data ?? response;

        // ✅ Sử dụng callback để đọc giá trị mới nhất (tránh stale closure)
        setCurrentSession((prev) => {
          if (!prev) return prev;

          const oldPower = prev.chargingPoint?.maxPowerKW;
          const newPower = updatedSession.chargingPoint?.maxPowerKW;

          if (newPower && newPower !== oldPower) {
            console.log(`⚡ Power updated: ${oldPower} kW → ${newPower} kW`);
            setCurrentPower(newPower); // ✅ Update currentPower state

            return {
              ...prev,
              chargingPoint: {
                ...prev.chargingPoint,
                maxPowerKW: newPower,
              },
            };
          }

          return prev;
        });
      } catch (err) {
        console.debug("Polling power error:", err);
      }
    }, 10000); // Mỗi 10 giây

    return () => clearInterval(pollPowerInterval);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentSession?.sessionId, currentSession?.status]); // ✅ Chỉ restart khi sessionId hoặc status thay đổi

  // ⚡ Polling: check current session periodically (mainly for status changes)
  // During IN_PROGRESS, frontend handles all calculations via virtualSoc
  // Poll every 2s to quickly detect when Staff stops the session
  useEffect(() => {
    let intervalId = null;

    // small wrapper to call API and update state
    const poll = async () => {
      try {
        // ✅ Use /api/driver/sessions instead of /current
        // This API returns ALL sessions (including COMPLETED), no error when stopped
        const response = await getMySessions();
        if (!response || response.success === false) {
          setCurrentSession(null);
          return;
        }

        const sessions = response.data ?? response;
        if (!Array.isArray(sessions) || sessions.length === 0) {
          setCurrentSession(null);
          return;
        }

        // First, try to find IN_PROGRESS session
        const inProgressSession = sessions.find((s) => {
          const status = String(s.status || "").toUpperCase();
          return status === "IN_PROGRESS";
        });

        // If found IN_PROGRESS, use it
        if (inProgressSession) {
          console.log(
            `📊 Found IN_PROGRESS session #${inProgressSession.sessionId}`
          );

          // ✅ Update currentPower from sessionStorage when session starts
          const bookingId = inProgressSession.bookingId;
          if (bookingId) {
            try {
              const key = `booking_${bookingId}_maxPowerKW`;
              const storedPower = sessionStorage.getItem(key);
              if (storedPower) {
                const power = JSON.parse(storedPower);
                setCurrentPower(power);
                console.log(
                  `⚡ Auto-loaded maxPowerKW=${power} kW from sessionStorage for booking #${bookingId}`
                );
              }
            } catch (e) {
              console.debug("Failed to auto-load maxPowerKW:", e);
            }
          }

          setCurrentSession((prev) => {
            if (!prev) return inProgressSession;

            // If session changed, update
            if (prev.sessionId !== inProgressSession.sessionId)
              return inProgressSession;

            // If status changed (should not happen for IN_PROGRESS to IN_PROGRESS)
            if (prev.status !== inProgressSession.status) {
              console.log(
                `🔄 Session status changed: ${prev.status} → ${inProgressSession.status}`
              );
              return {
                ...inProgressSession,
                virtualSoc: prev.virtualSoc,
              };
            }

            // Keep frontend simulation for IN_PROGRESS
            return prev;
          });
          return;
        }

        // No IN_PROGRESS session found - check if we HAD one that just completed
        setCurrentSession((prev) => {
          if (!prev || !prev.sessionId) {
            // No previous session, and no IN_PROGRESS → nothing to show
            return null;
          }

          // We had a session - check if it's now COMPLETED
          const prevSessionId = prev.sessionId;
          const completedSession = sessions.find(
            (s) => s.sessionId === prevSessionId
          );

          if (completedSession) {
            const status = String(completedSession.status || "").toUpperCase();
            console.log(
              `📊 Previous session #${prevSessionId} is now: ${status}`
            );

            if (status === "COMPLETED" || status === "FINISHED") {
              // ✅ Session completed - đồng bộ đầy đủ dữ liệu từ Backend
              console.log(
                `📊 Session #${prevSessionId} COMPLETED - syncing from backend:`,
                {
                  finalSoc: completedSession.finalSoc,
                  energyKWh: completedSession.energyKWh,
                  durationMinutes: completedSession.durationMinutes,
                  endTime: completedSession.endTime,
                  cost: completedSession.cost,
                }
              );

              return {
                ...completedSession,
                // ✅ Đồng bộ virtualSoc với finalSoc từ Backend để UI hiển thị đúng
                virtualSoc: completedSession.finalSoc,
              };
            }
          }

          // Session not found or status unclear - keep previous
          return prev;
        });
      } catch (err) {
        console.error("Polling getMySessions error:", err);
        // Don't clear session on error, keep previous state
      }
    };

    // run immediately then set interval
    poll();
    // ⚡ Poll every 2 seconds for faster detection when Staff stops session
    intervalId = setInterval(poll, 2000);

    return () => {
      if (intervalId) clearInterval(intervalId);
    };
  }, []);

  // 🔋 Virtual SOC simulation - REALTIME CHARGING
  // Tính toán dựa trên thời gian thực từ startTime
  // Công thức: duration = now - startTime → energy = duration × power × efficiency → SOC = energy / capacity
  useEffect(() => {
    if (!currentSession || currentSession.status !== "IN_PROGRESS") {
      // Clean up simulation state if session is not in progress
      if (currentSession?.sessionId) {
        clearSimState(currentSession.sessionId);
      }
      return;
    }

    // Get parameters from session or use defaults
    const capacity =
      currentSession.vehicle?.model?.batteryCapacityKWh ??
      DEFAULT_BATTERY_CAPACITY;
    const efficiency = 0.9; // ✅ Match backend exactly (ChargingSessionTxHandler)
    const initialSoc = currentSession.initialSoc ?? 20;

    // ✅ Initialize virtualSoc if not set
    if (!currentSession.virtualSoc) {
      setCurrentSession((prev) =>
        prev ? { ...prev, virtualSoc: initialSoc } : prev
      );
    }

    const virtualChargeInterval = setInterval(() => {
      setCurrentSession((prev) => {
        if (!prev || prev.status !== "IN_PROGRESS") return prev;

        // ✅ ĐỌC maxPowerKW từ currentPower state (đã lấy từ sessionStorage)
        const ratedKW = currentPower || 11.0;

        console.log(
          `⚡ Using power: ${ratedKW} kW (currentPower=${currentPower})`
        );

        // ✅ Tính thời lượng thực tế từ startTime (CHÍNH XÁC TUYỆT ĐỐI)
        const startTime = new Date(prev.startTime);
        const now = new Date();
        const durationMs = now - startTime;
        const newDurationMinutes = durationMs / (1000 * 60); // Convert ms to minutes
        const hours = newDurationMinutes / 60; // Convert to hours

        // ⚡ CÔNG THỨC GIỐNG BACKEND (ChargingSessionTxHandler.java line 376-389)

        // 6️⃣ Ước lượng điện năng nạp được (kWh) = giờ * kW * hiệu suất
        const estEnergy = hours * ratedKW * efficiency;

        // 7️⃣ Chuyển đổi từ kWh sang % pin: (estEnergy / capKWh) * 100
        let rawFinalSOC = initialSoc + (estEnergy / capacity) * 100.0;

        // Làm tròn thành số nguyên (Backend dùng Math.round)
        let finalSOC = Math.round(rawFinalSOC);

        // 8️⃣ Nếu có thời gian sạc > 0 mà % không đổi -> tăng tối thiểu 1% cho hợp lý
        if (newDurationMinutes > 0 && finalSOC === initialSoc) {
          finalSOC = initialSoc + 1;
        }

        // ⚡ Clamp kết quả trong [initialSoc .. 100]
        finalSOC = Math.min(100, Math.max(initialSoc, finalSOC));

        // ⚡ Tính energyKWh thực tế từ finalSOC (để đồng bộ với % hiển thị)
        const actualDeltaSOC = finalSOC - initialSoc;
        const energyKWh = +(capacity * (actualDeltaSOC / 100)).toFixed(2);

        // Auto-complete when reaching 100%
        if (finalSOC >= 100) {
          console.log("🔋 Battery reached 100% - auto-stopping session");

          // ✅ Gửi finalSoc = 100 (chuẩn Backend: số nguyên)
          stationAPI
            .stopChargingSession(prev.sessionId, 100)
            .then(() => {
              console.log(
                `✅ Session #${prev.sessionId} auto-stopped at 100% SOC`
              );
              // Backend sẽ trả về dữ liệu đầy đủ: endTime, finalSoc, energyKWh, durationMinutes, cost
              // Polling sẽ detect COMPLETED và update UI với dữ liệu chính xác từ Backend
            })
            .catch((err) => {
              console.error("❌ Failed to stop session at 100%:", err);
            });

          clearInterval(virtualChargeInterval);
          clearSimState(prev.sessionId);
          // Polling sẽ detect COMPLETED status từ backend và auto-redirect
          return prev;
        }

        // ✅ Log để debug
        if (
          Math.floor(newDurationMinutes) % 5 === 0 &&
          newDurationMinutes > 0
        ) {
          console.log(
            `📊 Charging stats: duration=${newDurationMinutes.toFixed(
              1
            )}min, power=${ratedKW}kW, energy=${energyKWh}kWh, SOC=${finalSOC}%`
          );
        }

        // Continuous update + persist state
        const updatedSession = {
          ...prev,
          virtualSoc: finalSOC,
          energyKWh,
          durationMinutes: newDurationMinutes,
        };

        saveSimState(updatedSession);

        // ✅ Lưu virtualSoc vào sessionStorage để Staff có thể đọc khi dừng phiên sạc
        // Key format: session_${sessionId}_live_soc
        try {
          const liveDataKey = `session_${prev.sessionId}_live_soc`;
          const liveData = {
            sessionId: prev.sessionId,
            virtualSoc: Math.round(finalSOC), // Lưu số nguyên
            energyKWh,
            durationMinutes: newDurationMinutes,
            timestamp: Date.now(),
          };
          sessionStorage.setItem(liveDataKey, JSON.stringify(liveData));
        } catch (err) {
          console.debug("Failed to save live SOC to sessionStorage:", err);
        }

        return updatedSession;
      });
    }, 1000); // Update every 1 second (realtime mode)

    return () => {
      clearInterval(virtualChargeInterval);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentSession?.sessionId, currentSession?.status, currentPower]); // ✅ Thêm currentPower để interval đọc giá trị mới nhất

  // 🧭 Auto redirect to payment page when charging completes
  // Triggers when status changes to COMPLETED or STOPPED (by Staff)
  useEffect(() => {
    if (!currentSession) return;

    console.log(
      `🔍 Auto-redirect check: status="${currentSession.status}", autoRedirected=${autoRedirected}`
    );

    // Normalize status to uppercase for comparison (backend may return different cases)
    const normalizedStatus = String(currentSession.status || "").toUpperCase();

    // Khi trạng thái chuyển sang COMPLETED hoặc STOPPED (Staff dừng) và chưa redirect
    const isSessionEnded =
      normalizedStatus === "COMPLETED" ||
      normalizedStatus === "STOPPED" ||
      normalizedStatus === "FINISHED";

    if (isSessionEnded && !autoRedirected) {
      console.log(
        `✅ Session ended with status: ${currentSession.status} - Redirecting to payment...`
      );
      setAutoRedirected(true);

      const message =
        normalizedStatus === "STOPPED"
          ? "⏹ Phiên sạc đã bị dừng. Đang chuyển sang trang thanh toán..."
          : "⚡ Phiên sạc đã hoàn tất. Đang chuyển sang trang thanh toán...";

      toast.info(message, {
        position: "top-center",
        autoClose: 2000,
      });

      // Clear simulation state
      clearSimState(currentSession.sessionId);

      // ✅ Xóa live SOC data khỏi sessionStorage khi phiên sạc kết thúc
      try {
        const liveDataKey = `session_${currentSession.sessionId}_live_soc`;
        sessionStorage.removeItem(liveDataKey);
      } catch (err) {
        console.debug("Failed to remove live SOC from sessionStorage:", err);
      }

      // Chuyển sang trang thanh toán sau 2s
      setTimeout(() => {
        navigate(paths.payment, { state: { sessionResult: currentSession } });
      }, 2000);
    }
  }, [currentSession, autoRedirected, navigate, clearSimState]);

  // ⏰ Auto-stop session when booking time expires
  useEffect(() => {
    if (!currentSession || currentSession.status !== "IN_PROGRESS") return;
    if (!currentSession.windowEnd) return;

    const checkExpiry = setInterval(() => {
      const now = new Date();
      const endTime = new Date(currentSession.windowEnd);

      if (now >= endTime) {
        console.log("⏰ Booking time expired - auto-stopping session");

        // ✅ Gửi finalSoc là số nguyên đã làm tròn (chuẩn Backend)
        const finalSocValue = Math.round(
          currentSession.virtualSoc || currentSession.initialSoc
        );

        stationAPI
          .stopChargingSession(currentSession.sessionId, finalSocValue)
          .then((response) => {
            console.log(
              `✅ Session #${currentSession.sessionId} auto-stopped at time expiry`
            );
            console.log(
              `   Backend calculated: finalSoc=${
                response.data?.finalSoc || response.finalSoc
              }%, ` +
                `energyKWh=${
                  response.data?.energyKWh || response.energyKWh
                }, ` +
                `duration=${
                  response.data?.durationMinutes || response.durationMinutes
                }min`
            );
            // Polling sẽ detect COMPLETED và cập nhật UI với dữ liệu chính xác từ Backend
          })
          .catch((err) => {
            console.error("❌ Failed to stop session on time expiry:", err);
          });

        clearInterval(checkExpiry);
      }
    }, 5000); // Check every 5 seconds

    return () => clearInterval(checkExpiry);
  }, [currentSession]);

  const handleStopSession = async () => {
    if (!currentSession?.sessionId) {
      toast.error("Không có sessionId để dừng", { position: "top-center" });
      return;
    }
    if (!window.confirm("Bạn có chắc chắn muốn dừng phiên sạc này không?"))
      return;

    try {
      setStopping(true);

      // Stop virtual animation immediately by changing status
      // This triggers useEffect cleanup and clears the interval
      setCurrentSession((prev) =>
        prev ? { ...prev, status: "STOPPING" } : prev
      );

      // ✅ Gửi finalSoc là số nguyên đã làm tròn (chuẩn Backend - nhất quán với auto-stop)
      const finalSocToSend = Math.round(
        currentSession.virtualSoc ??
          currentSession.finalSoc ??
          currentSession.initialSoc
      );

      console.log(
        `🛑 Driver stopping session #${currentSession.sessionId} with finalSoc=${finalSocToSend}%`
      );

      const response = await stationAPI.stopChargingSession(
        currentSession.sessionId,
        finalSocToSend
      );
      if (!response || response.success === false) {
        // Revert status if stop failed
        setCurrentSession((prev) =>
          prev ? { ...prev, status: "IN_PROGRESS" } : prev
        );
        toast.error(response?.message || "Dừng phiên sạc thất bại", {
          position: "top-center",
        });
        return;
      }

      const sessionResult = response.data ?? response;

      // ✅ Cập nhật UI với dữ liệu chính xác từ Backend (đảm bảo nhất quán)
      // Backend đã tính toán: endTime, finalSoc, energyKWh, durationMinutes, cost
      console.log(
        `✅ Backend response: finalSoc=${sessionResult.finalSoc}%, ` +
          `energyKWh=${sessionResult.energyKWh}, ` +
          `duration=${sessionResult.durationMinutes}min, ` +
          `cost=${sessionResult.cost}`
      );

      setCurrentSession((prev) =>
        prev
          ? {
              ...prev,
              status: sessionResult.status ?? "COMPLETED",
              endTime: sessionResult.endTime,
              finalSoc: sessionResult.finalSoc, // ✅ Dùng finalSoc từ Backend (số nguyên)
              energyKWh: sessionResult.energyKWh, // ✅ Dùng energyKWh từ Backend
              cost: sessionResult.cost,
              durationMinutes: sessionResult.durationMinutes, // ✅ Dùng duration từ Backend
              virtualSoc: sessionResult.finalSoc, // ✅ Sync virtualSoc = finalSoc từ Backend
            }
          : prev
      );

      toast.success("Dừng phiên sạc thành công!", { position: "top-center" });

      // Clear simulation state from localStorage
      clearSimState(currentSession.sessionId);

      // cleanup persisted QR for this booking (if any)
      try {
        const key = qrStorageKey(
          booking?.bookingId ??
            bookingIdFromParams ??
            sessionResult?.bookingId ??
            currentSession?.bookingId
        );
        if (key) sessionStorage.removeItem(key);
      } catch {
        // ignore
      }

      // Navigate to payment after a short delay to show final state
      setTimeout(() => {
        navigate(paths.payment, { state: { sessionResult } });
      }, 1500);
    } catch (err) {
      console.error("Lỗi khi dừng phiên sạc:", err);
      // Revert status if error occurred
      setCurrentSession((prev) =>
        prev ? { ...prev, status: "IN_PROGRESS" } : prev
      );
      toast.error("Dừng phiên sạc thất bại", { position: "top-center" });
    } finally {
      setStopping(false);
    }
  };

  // Load booking by param if needed
  useEffect(() => {
    if (!booking && bookingIdFromParams) {
      (async () => {
        try {
          setBookingLoading(true);
          const res = await stationAPI.getBookingById(bookingIdFromParams);
          if (!res || res.success === false) {
            toast.error(res?.message || "Không thể lấy booking", {
              position: "top-center",
            });
            return;
          }
          setBooking(res.data ?? res);
        } catch (err) {
          console.error("Error fetching booking:", err);
        } finally {
          setBookingLoading(false);
        }
      })();
    }
  }, [booking, bookingIdFromParams]);

  // If a current session becomes IN_PROGRESS, remove any persisted QR for that booking
  useEffect(() => {
    if (!currentSession) return;
    if (currentSession.status === "IN_PROGRESS") {
      try {
        const id =
          booking?.bookingId ?? bookingIdFromParams ?? currentSession.bookingId;
        const key = qrStorageKey(id);
        if (key) sessionStorage.removeItem(key);
        // hide qrUrl if it was showing
        setQrUrl(null);
      } catch {
        // ignore
      }
    }
  }, [currentSession, booking, bookingIdFromParams]);

  // Cleanup blob URL on unmount
  useEffect(() => {
    return () => {
      if (qrUrl && typeof qrUrl === "string" && qrUrl.startsWith("blob:")) {
        try {
          URL.revokeObjectURL(qrUrl);
        } catch {
          // ignore
        }
      }
    };
  }, [qrUrl]);

  const handleDownload = () => {
    if (!qrUrl) return;
    const a = document.createElement("a");
    a.href = qrUrl;
    a.download = `booking-${
      booking?.bookingId ?? bookingIdFromParams ?? "qr"
    }.png`;
    document.body.appendChild(a);
    a.click();
    a.remove();
  };

  // Manual restore helper (visible when automatic restore fails)
  const restoreAnyQr = () => {
    try {
      const keys = Object.keys(sessionStorage).filter(
        (k) => k && k.startsWith("qr_booking_")
      );
      if (!keys || keys.length === 0) {
        toast.info("Không tìm thấy QR lưu trữ nào trong sessionStorage", {
          position: "top-center",
        });
        return;
      }
      // prefer match by bookingId if available
      let keyToUse = null;
      const idCandidates = [
        bookingIdFromParams,
        booking?.bookingId ?? booking?.id,
        currentSession?.bookingId,
      ];
      for (const id of idCandidates) {
        if (!id) continue;
        const candidateKey = `qr_booking_${id}`;
        if (keys.includes(candidateKey)) {
          keyToUse = candidateKey;
          break;
        }
      }
      if (!keyToUse) {
        // fallback to first key
        keyToUse = keys[0];
      }
      const val = sessionStorage.getItem(keyToUse);
      if (val) {
        setQrUrl(val);
        toast.success("Khôi phục QR thành công", { position: "top-center" });
      } else {
        toast.error("Không thể đọc QR từ sessionStorage", {
          position: "top-center",
        });
      }
    } catch (e) {
      console.warn("restoreAnyQr error", e);
      toast.error("Lỗi khi khôi phục QR", { position: "top-center" });
    }
  };

  return (
    <div
      className="charging-session-container"
      style={{ padding: "20px", maxWidth: "1200px", margin: "0 auto" }}
    >
      <button
        onClick={() => navigate(-1)}
        style={{
          marginBottom: "20px",
          padding: "10px 20px",
          background: "#00BFA6",
          color: "white",
          border: "none",
          borderRadius: "8px",
          cursor: "pointer",
        }}
      >
        ← Quay lại
      </button>

      <h1 style={{ color: "#00BFA6", marginBottom: "30px" }}>
        Phiên sạc hiện tại
      </h1>

      {loading ? (
        <p>Đang tải thông tin phiên sạc...</p>
      ) : qrUrl &&
        (!currentSession || currentSession.status !== "IN_PROGRESS") ? (
        <div
          style={{
            background: "white",
            padding: "20px",
            borderRadius: "12px",
            boxShadow: "0 2px 8px rgba(0,0,0,0.1)",
            textAlign: "center",
          }}
        >
          <h2 style={{ color: "#333", marginBottom: "15px" }}>Mã QR đặt chỗ</h2>

          {bookingLoading ? (
            <p>Đang tải thông tin booking...</p>
          ) : (
            <>
              {qrUrl ? (
                <div style={{ textAlign: "center", marginTop: 12 }}>
                  <img
                    src={qrUrl}
                    alt="QR Code"
                    style={{ maxWidth: "320px", width: "100%", height: "auto" }}
                  />
                  <div style={{ marginTop: 12 }}>
                    <button
                      onClick={handleDownload}
                      style={{
                        padding: "10px 18px",
                        borderRadius: 8,
                        background: "#00BFA6",
                        color: "white",
                        border: "none",
                      }}
                    >
                      Tải mã QR
                    </button>
                  </div>
                </div>
              ) : (
                <div style={{ marginTop: 12 }}>
                  <p>QR chưa có. Vui lòng xác nhận booking trước.</p>
                  <div style={{ marginTop: 8 }}>
                    <button
                      onClick={restoreAnyQr}
                      style={{
                        padding: "8px 12px",
                        borderRadius: 8,
                        background: "#1976d2",
                        color: "white",
                        border: "none",
                      }}
                    >
                      Khôi phục QR
                    </button>
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      ) : currentSession ? (
        <div
          style={{
            background: "white",
            padding: "20px",
            borderRadius: "12px",
            boxShadow: "0 2px 8px rgba(0,0,0,0.1)",
          }}
        >
          <h2 style={{ color: "#333", marginBottom: "15px" }}>
            ⚡ Thông tin phiên sạc
          </h2>

          <div style={{ marginBottom: "20px" }}>
            <p style={{ marginBottom: "10px" }}>
              <strong>Booking ID:</strong> {currentSession.bookingId ?? "-"}
            </p>
            <p style={{ marginBottom: "10px" }}>
              <strong>Trạng thái:</strong>{" "}
              <span
                style={{
                  padding: "4px 12px",
                  borderRadius: "20px",
                  background: statusColors[currentSession.status] || "#9e9e9e",
                  color: "white",
                  fontSize: "14px",
                  fontWeight: "600",
                }}
              >
                {currentSession.status === "IN_PROGRESS"
                  ? "Đang sạc"
                  : currentSession.status === "COMPLETED"
                  ? "Hoàn thành"
                  : currentSession.status === "FAILED"
                  ? "Thất bại"
                  : currentSession.status ?? "-"}
              </span>
            </p>
          </div>

          {/* Quick Info Cards */}
          <div
            className="quick-info-grid"
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(250px, 1fr))",
              gap: "15px",
              marginBottom: "25px",
            }}
          >
            <div
              style={{
                background: "#f8f9fa",
                padding: "15px",
                borderRadius: "10px",
                border: "1px solid #e0e0e0",
              }}
            >
              <div
                style={{
                  fontSize: "14px",
                  color: "#666",
                  marginBottom: "8px",
                  display: "flex",
                  alignItems: "center",
                  gap: "6px",
                }}
              >
                🚗 Thông tin xe
              </div>
              <div
                style={{ fontSize: "18px", fontWeight: "600", color: "#333" }}
              >
                {currentSession.vehiclePlate ?? "-"}
              </div>
            </div>

            <div
              style={{
                background: "#f8f9fa",
                padding: "15px",
                borderRadius: "10px",
                border: "1px solid #e0e0e0",
              }}
            >
              <div
                style={{
                  fontSize: "14px",
                  color: "#666",
                  marginBottom: "8px",
                  display: "flex",
                  alignItems: "center",
                  gap: "6px",
                }}
              >
                🏢 Thông tin trạm
              </div>
              <div
                style={{ fontSize: "18px", fontWeight: "600", color: "#333" }}
              >
                {currentSession.stationName ?? "-"}
              </div>
            </div>

            <div
              style={{
                background: "#f8f9fa",
                padding: "15px",
                borderRadius: "10px",
                border: "1px solid #e0e0e0",
              }}
            >
              <div
                style={{
                  fontSize: "14px",
                  color: "#666",
                  marginBottom: "8px",
                  display: "flex",
                  alignItems: "center",
                  gap: "6px",
                }}
              >
                ⏰ Bắt đầu
              </div>
              <div
                style={{ fontSize: "16px", fontWeight: "600", color: "#333" }}
              >
                {currentSession.startTime
                  ? new Date(currentSession.startTime).toLocaleString("vi-VN", {
                      hour: "2-digit",
                      minute: "2-digit",
                      day: "2-digit",
                      month: "2-digit",
                    })
                  : "-"}
              </div>
            </div>
          </div>

          {/* Battery Progress Circle */}
          {currentSession.initialSoc != null && (
            <BatteryProgressCircle
              initialSoc={currentSession.initialSoc}
              energyKWh={currentSession.energyKWh ?? 0}
              capacity={DEFAULT_BATTERY_CAPACITY}
              isCharging={currentSession.status === "IN_PROGRESS"}
              virtualSoc={currentSession.virtualSoc}
            />
          )}

          {/* Key Metrics Grid */}
          <div
            className="info-card-grid"
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(150px, 1fr))",
              gap: "15px",
              marginBottom: "30px",
            }}
          >
            <InfoCard
              icon="⚡"
              label="Năng lượng đã sạc"
              value={(currentSession.energyKWh ?? 0).toFixed(2)}
              unit="kWh"
              color="#4caf50"
            />
            <InfoCard
              icon="⏱️"
              label="Thời lượng"
              value={(currentSession.durationMinutes ?? 0).toFixed(0)}
              unit="phút"
              color="#2196f3"
            />
            <InfoCard
              icon="⚡"
              label="Công suất sạc"
              value={currentPower.toFixed(1)}
              unit="kW"
              color="#9c27b0"
            />
          </div>

          {/* SOC Info */}
          {currentSession.initialSoc != null && (
            <div
              style={{
                background: "#f8f9fa",
                padding: "20px",
                borderRadius: "12px",
                marginBottom: "30px",
                display: "flex",
                justifyContent: "space-around",
                flexWrap: "wrap",
                gap: "20px",
              }}
            >
              <div style={{ textAlign: "center" }}>
                <div
                  style={{
                    fontSize: "14px",
                    color: "#666",
                    marginBottom: "5px",
                  }}
                >
                  SOC Ban đầu
                </div>
                <div
                  style={{ fontSize: "28px", fontWeight: "700", color: "#666" }}
                >
                  {currentSession.initialSoc}%
                </div>
              </div>
              <div
                style={{
                  width: "2px",
                  background: "#ddd",
                  margin: "0 10px",
                }}
              />
              <div style={{ textAlign: "center" }}>
                <div
                  style={{
                    fontSize: "14px",
                    color: "#666",
                    marginBottom: "5px",
                  }}
                >
                  SOC Hiện tại
                </div>
                <div
                  style={{
                    fontSize: "28px",
                    fontWeight: "700",
                    color: "#00BFA6",
                  }}
                >
                  {/* ✅ Ưu tiên finalSoc từ Backend (số nguyên) khi session completed */}
                  {currentSession.status === "COMPLETED" &&
                  currentSession.finalSoc != null
                    ? `${currentSession.finalSoc}%`
                    : `${(
                        currentSession.virtualSoc ??
                        Math.min(
                          currentSession.initialSoc +
                            ((currentSession.energyKWh ?? 0) /
                              DEFAULT_BATTERY_CAPACITY) *
                              100,
                          100
                        )
                      ).toFixed(1)}%`}
                </div>
              </div>
              {currentSession.finalSoc != null && (
                <>
                  <div
                    style={{
                      width: "2px",
                      background: "#ddd",
                      margin: "0 10px",
                    }}
                  />
                  <div style={{ textAlign: "center" }}>
                    <div
                      style={{
                        fontSize: "14px",
                        color: "#666",
                        marginBottom: "5px",
                      }}
                    >
                      SOC Cuối
                    </div>
                    <div
                      style={{
                        fontSize: "28px",
                        fontWeight: "700",
                        color: "#2196f3",
                      }}
                    >
                      {currentSession.finalSoc}%
                    </div>
                  </div>
                </>
              )}
            </div>
          )}

          <div style={{ marginTop: "30px", display: "flex", gap: "15px" }}>
            <button
              onClick={fetchCurrentSession}
              style={{
                padding: "12px 24px",
                background: "#00BFA6",
                color: "white",
                border: "none",
                borderRadius: "8px",
                fontSize: "16px",
                fontWeight: "600",
                cursor: "pointer",
              }}
            >
              🔄 Làm mới
            </button>

            {currentSession.status === "IN_PROGRESS" && (
              <button
                onClick={handleStopSession}
                disabled={stopping}
                style={{
                  padding: "12px 24px",
                  background: stopping ? "#ccc" : "#f44336",
                  color: "white",
                  border: "none",
                  borderRadius: "8px",
                  fontSize: "16px",
                  fontWeight: "600",
                  cursor: stopping ? "not-allowed" : "pointer",
                }}
              >
                {stopping ? "Đang dừng..." : "🛑 Dừng phiên sạc"}
              </button>
            )}
          </div>
        </div>
      ) : (
        <div
          style={{
            background: "#f5f5f5",
            padding: "20px",
            borderRadius: "12px",
            textAlign: "center",
          }}
        >
          <p style={{ color: "#666" }}>Không có phiên sạc nào đang hoạt động</p>
        </div>
      )}
    </div>
  );
}
