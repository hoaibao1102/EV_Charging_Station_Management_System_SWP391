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

  const clearSimState = useCallback((sessionId) => {
    if (!sessionId) return;
    try {
      const key = getSimulationKey(sessionId);
      if (key) localStorage.removeItem(key);
    } catch (err) {
      console.debug("Failed to clear simulation state:", err);
    }
  }, []);

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
        setCurrentSession(null);
        return;
      }
      const session = response.data ?? response;
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
              // Status changed from IN_PROGRESS to COMPLETED - preserve virtualSoc
              return {
                ...completedSession,
                virtualSoc: prev.virtualSoc || completedSession.finalSoc,
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

  // 🔋 Virtual SOC simulation - FAST CHARGING SIMULATION
  // Real-time: Every 2 seconds = 2 minutes of charging (60x speed)
  // Example with 11kW charger, 60kWh battery, 90% efficiency:
  // - 10 minutes real = 10 hours simulated
  // - Energy = 10h × 11kW × 0.9 = 99 kWh
  // - SOC increase = (99 / 60) × 100 = 165% → clamped to 100%
  // This allows full charge demo in ~1 minute instead of 6+ hours
  //
  // 📐 CÔNG THỨC TÍNH finalSOC (khớp với Backend):
  // 1. initialSoc = % pin ban đầu
  // 2. estEnergy = năng lượng đã sạc (kWh) = hours × ratedKW × efficiency
  // 3. capKWh = dung lượng pin xe (kWh)
  // 4. deltaSOC = (estEnergy / capKWh) × 100
  // 5. finalSOC = initialSoc + deltaSOC
  // 6. Làm tròn finalSOC thành số nguyên gần nhất
  // 7. Ràng buộc:
  //    - Nếu finalSOC < initialSoc → lấy initialSoc
  //    - Nếu finalSOC > 100 → đặt finalSOC = 100
  //    - Nếu estEnergy > 0 nhưng finalSOC = initialSoc → ép finalSOC = initialSoc + 1
  useEffect(() => {
    if (!currentSession || currentSession.status !== "IN_PROGRESS") {
      // Clean up simulation state if session is not in progress
      if (currentSession?.sessionId) {
        clearSimState(currentSession.sessionId);
      }
      return;
    }

    // Get parameters from session or use defaults (one-time retrieval)
    const capacity =
      currentSession.vehicle?.model?.batteryCapacityKWh ??
      DEFAULT_BATTERY_CAPACITY;
    const ratedKW = currentSession.chargingPoint?.maxPowerKW ?? 11.0; // Default 11 kW charger
    const efficiency = 0.9; // ✅ Match backend exactly (ChargingSessionTxHandler)
    const initialSoc = currentSession.initialSoc ?? 20;

    // Try to restore previous simulation state (for page reload)
    let accumulatedMinutes = currentSession.durationMinutes ?? 0;
    const savedState = loadSimState(currentSession.sessionId);

    if (savedState && savedState.sessionId === currentSession.sessionId) {
      // Restore from saved state
      accumulatedMinutes = savedState.durationMinutes ?? accumulatedMinutes;

      // Update current session with restored values
      setCurrentSession((prev) =>
        prev
          ? {
              ...prev,
              virtualSoc: savedState.virtualSoc ?? initialSoc,
              energyKWh: savedState.energyKWh ?? prev.energyKWh,
              durationMinutes: accumulatedMinutes,
            }
          : prev
      );
    } else if (!currentSession.virtualSoc) {
      // Initialize virtual SOC for first time
      setCurrentSession((prev) =>
        prev ? { ...prev, virtualSoc: initialSoc } : prev
      );
    }

    const virtualChargeInterval = setInterval(() => {
      accumulatedMinutes += 2 / 60; // +2 minutes per tick (2 seconds in real-time = 2 minutes simulation)
      const hours = accumulatedMinutes / 60; // Convert to hours

      // ⚡ BƯỚC 1: Tính năng lượng đã sạc (estEnergy)
      // Công thức: estEnergy = hours × ratedKW × efficiency
      const estEnergy = hours * ratedKW * efficiency;

      // ⚡ BƯỚC 2: Tính deltaSOC (phần trăm SOC tăng thêm)
      // Công thức: deltaSOC = (estEnergy / capKWh) × 100
      const deltaSOC = (estEnergy / capacity) * 100;

      // ⚡ BƯỚC 3: Tính finalSOC trước khi ràng buộc
      // Công thức: finalSOC = initialSoc + deltaSOC
      let rawFinalSOC = initialSoc + deltaSOC;

      // ⚡ BƯỚC 4: Làm tròn finalSOC thành số nguyên gần nhất
      let finalSOC = Math.round(rawFinalSOC);

      // ⚡ BƯỚC 5: Ràng buộc - finalSOC không được nhỏ hơn initialSoc
      if (finalSOC < initialSoc) {
        finalSOC = initialSoc;
      }

      // ⚡ BƯỚC 6: Ràng buộc - finalSOC không được vượt quá 100%
      if (finalSOC > 100) {
        finalSOC = 100;
      }

      // ⚡ BƯỚC 7: Trường hợp đặc biệt
      // Nếu estEnergy > 0 nhưng deltaSOC quá nhỏ khiến finalSOC = initialSoc
      // thì ép tăng finalSOC = initialSoc + 1
      if (estEnergy > 0 && finalSOC === initialSoc) {
        finalSOC = initialSoc + 1;
        // Đảm bảo không vượt quá 100%
        if (finalSOC > 100) {
          finalSOC = 100;
        }
      }

      // ⚡ BƯỚC 8: Tính energyKWh thực tế từ finalSOC
      // (năng lượng tương ứng với % pin đã tăng)
      const actualDeltaSOC = finalSOC - initialSoc;
      const energyKWh = +(capacity * (actualDeltaSOC / 100)).toFixed(2);

      // Auto-complete when reaching 100% or time limit exceeded
      const timeLimit = currentSession.booking?.endTime
        ? new Date(currentSession.booking.endTime).getTime()
        : null;
      const currentTime = new Date().getTime();
      const isTimeExpired = timeLimit && currentTime >= timeLimit;

      if (finalSOC >= 100 || isTimeExpired) {
        const completionReason =
          finalSOC >= 100
            ? "🔋 Đã sạc đầy pin (100%)"
            : "⏰ Hết thời gian sạc đã đặt";

        console.log(`✅ Auto-completing session: ${completionReason}`);

        clearInterval(virtualChargeInterval);
        clearSimState(currentSession.sessionId);

        const completedSession = {
          ...currentSession,
          virtualSoc: finalSOC >= 100 ? 100 : finalSOC,
          finalSoc: finalSOC >= 100 ? 100 : Math.round(finalSOC),
          energyKWh,
          durationMinutes: Math.round(accumulatedMinutes),
          status: "COMPLETED",
          completionReason, // Add reason for completion
        };

        setCurrentSession(completedSession);

        // Show toast notification
        toast.success(
          completionReason + ". Đang chuyển sang trang thanh toán...",
          {
            position: "top-center",
            autoClose: 2500,
          }
        );

        // Auto-redirect to payment after 3 seconds
        setTimeout(() => {
          navigate(paths.payment, {
            state: { sessionResult: completedSession },
          });
        }, 3000);

        return;
      }

      // Continuous update + persist state
      const updatedSession = {
        ...currentSession,
        virtualSoc: finalSOC, // Use calculated finalSOC with smooth decimal for animation
        energyKWh,
        durationMinutes: Math.round(accumulatedMinutes),
      };
      setCurrentSession(updatedSession);
      saveSimState(updatedSession);
    }, 2000); // Update every 2 seconds

    return () => {
      clearInterval(virtualChargeInterval);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentSession?.status, currentSession?.sessionId]);

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

        const finalSocValue = Math.round(
          currentSession.virtualSoc || currentSession.initialSoc
        );

        stationAPI
          .stopChargingSession(currentSession.sessionId, finalSocValue)
          .then(() => {
            console.log(
              `✅ Session #${currentSession.sessionId} auto-stopped at time expiry (SOC: ${finalSocValue}%)`
            );
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

      // Gửi virtualSoc (hoặc finalSoc nếu có) để backend có SOC chính xác từ frontend
      const finalSocToSend =
        currentSession.virtualSoc ?? currentSession.finalSoc;

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

      // Update UI with real data from backend
      setCurrentSession((prev) =>
        prev
          ? {
              ...prev,
              status: sessionResult.status ?? "COMPLETED",
              finalSoc:
                sessionResult.finalSoc ?? prev.virtualSoc ?? prev.initialSoc,
              energyKWh: sessionResult.energyKWh ?? prev.energyKWh,
              cost: sessionResult.cost ?? prev.cost,
              durationMinutes:
                sessionResult.durationMinutes ?? prev.durationMinutes,
              actualEndTime:
                sessionResult.actualEndTime ?? new Date().toISOString(),
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
              value={Math.round(currentSession.durationMinutes ?? 0)}
              unit=""
              color="#2196f3"
            />
            <InfoCard
              icon="⚙️"
              label="Công suất TB"
              value={
                currentSession.durationMinutes > 0
                  ? (
                      (currentSession.energyKWh ?? 0) /
                      (currentSession.durationMinutes / 60)
                    ).toFixed(1)
                  : "0"
              }
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
                  {(
                    currentSession.virtualSoc ??
                    Math.min(
                      currentSession.initialSoc +
                        ((currentSession.energyKWh ?? 0) /
                          DEFAULT_BATTERY_CAPACITY) *
                          100,
                      100
                    )
                  ).toFixed(1)}
                  %
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
