import React, { useEffect, useState, useCallback, memo, useRef } from "react";
import { useNavigate, useLocation, useParams } from "react-router-dom";
import paths from "../../path/paths.jsx";
import { toast } from "react-toastify";
import { stationAPI } from "../../api/stationApi.js";
import { getMySessions } from "../../api/driverApi.js";
import { isAuthenticated } from "../../utils/authUtils.js";

/**
 * ========== CHARGING SESSION MANAGEMENT (OPTIMIZED WITH USEREF) ==========
 *
 * Cải tiến:
 * - Sử dụng useRef để truy cập state mới nhất trong setInterval mà không cần dependency.
 * - Loại bỏ hoàn toàn việc re-create interval mỗi giây.
 * - Tách biệt luồng Simulation (Hiển thị ảo) và luồng Polling (Đồng bộ thật).
 */

// ========== CONSTANTS ==========
const CHARGING_EFFICIENCY = 0.9;
const POLLING_INTERVAL = 5000;       // 5s: Check status thay đổi
const POWER_POLLING_INTERVAL = 15000; // 15s: Check công suất
const ENERGY_POLLING_INTERVAL = 60000; // 60s: Đồng bộ năng lượng thật

// ========== UTILITY FUNCTIONS ==========
const getBatteryCapacity = () => {
  const capacity = sessionStorage.getItem("batteryCapacityKWh");
  return capacity ? parseFloat(capacity) : 60;
};

// ========== HELPER FUNCTIONS (Giữ nguyên logic của bạn) ==========
const round2 = (value) => Math.round(value * 100.0) / 100.0;

const calculateChargingMetrics = ({ startTime, initialSoc, powerKW, capacity = getBatteryCapacity(), efficiency = CHARGING_EFFICIENCY }) => {
  const now = new Date();
  const start = new Date(startTime);
  const durationMs = now - start;
  const durationMinutes = durationMs / (1000 * 60);
  const hours = durationMinutes / 60;

  const estimatedEnergyDelivered = hours * powerKW * efficiency;
  const estimatedSocIncrease = (estimatedEnergyDelivered / capacity) * 100.0;
  let rawFinalSOC = initialSoc + estimatedSocIncrease;

  if (durationMinutes > 0 && Math.floor(rawFinalSOC) === initialSoc) {
    rawFinalSOC = initialSoc + 1;
  }

  let finalSOC = Math.round(rawFinalSOC);
  finalSOC = Math.min(100, Math.max(initialSoc, finalSOC));

  const deltaSoc = finalSOC - initialSoc;
  const energyKWh = round2((deltaSoc / 100.0) * capacity);

  return { finalSOC, energyKWh, durationMinutes };
};

const extractPowerKW = (session, bookingId) => {
  if (bookingId) {
    try {
      const key = `booking_${bookingId}_maxPowerKW`;
      const storedPower = sessionStorage.getItem(key);
      if (storedPower) return JSON.parse(storedPower);
    } catch (e) { console.debug(e); }
  }
  return session?.chargingPoint?.maxPowerKW ?? session?.maxPowerKW ?? 11.0;
};

const syncSessionFromBackend = (backendData, currentState = {}) => {
  return {
    ...currentState,
    ...backendData,
    status: backendData.status ?? currentState.status,
    endTime: backendData.endTime ?? currentState.endTime,
    finalSoc: backendData.finalSoc ?? currentState.finalSoc,
    energyKWh: backendData.energyKWh ?? currentState.energyKWh,
    cost: backendData.cost ?? currentState.cost,
    durationMinutes: backendData.durationMinutes ?? currentState.durationMinutes,
    virtualSoc: backendData.finalSoc ?? currentState.virtualSoc,
    pointNumber: backendData.pointNumber ?? currentState.pointNumber,
    stationName: backendData.stationName ?? currentState.stationName,
    vehiclePlate: backendData.vehiclePlate ?? currentState.vehiclePlate,
    pricePerKWh: backendData.pricePerKWh ?? currentState.pricePerKWh,
    currency: backendData.currency ?? currentState.currency,
  };
};

// Styles (Giữ nguyên)
const styleSheet = document.createElement("style");
styleSheet.textContent = `
  @media (max-width: 768px) {
    .charging-session-container { padding: 10px !important; }
    .battery-progress-circle { width: 180px !important; height: 180px !important; }
    .battery-progress-circle svg { width: 180px !important; height: 180px !important; }
    .battery-progress-circle .center-text { font-size: 36px !important; }
    .info-card-grid { grid-template-columns: repeat(auto-fit, minmax(130px, 1fr)) !important; }
    .quick-info-grid { grid-template-columns: 1fr !important; }
  }
`;
if (!document.head.querySelector("style[data-charging-session-styles]")) {
  styleSheet.setAttribute("data-charging-session-styles", "true");
  document.head.appendChild(styleSheet);
}

// Components (Giữ nguyên logic hiển thị)
const BatteryProgressCircle = memo(function BatteryProgressCircle({ initialSoc, energyKWh, capacity, isCharging, virtualSoc }) {
  const deltaPercent = (energyKWh / capacity) * 100;
  const calculatedSoc = Math.min(initialSoc + deltaPercent, 100);
  const currentSoc = virtualSoc ?? calculatedSoc;
  const isComplete = currentSoc >= 100;
  const [animatedSoc, setAnimatedSoc] = useState(currentSoc);

  useEffect(() => {
    const diff = currentSoc - animatedSoc;
    if (Math.abs(diff) < 0.1) { setAnimatedSoc(currentSoc); return; }
    const step = diff / 20;
    const interval = setInterval(() => {
      setAnimatedSoc((prev) => {
        const next = prev + step;
        if ((diff > 0 && next >= currentSoc) || (diff < 0 && next <= currentSoc)) { clearInterval(interval); return currentSoc; }
        return next;
      });
    }, 50);
    return () => clearInterval(interval);
  }, [currentSoc, animatedSoc]);

  const size = 240; const strokeWidth = 16; const radius = (size - strokeWidth) / 2; const circumference = 2 * Math.PI * radius; const offset = circumference - (animatedSoc / 100) * circumference;
  const progressColor = isComplete ? "#2196f3" : "#00BFA6"; const trackColor = "#e0e0e0";

  return (
    <div className="battery-progress-circle" style={{ display: "flex", flexDirection: "column", alignItems: "center", padding: "30px 20px", background: "linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%)", borderRadius: "20px", boxShadow: "0 4px 20px rgba(0,0,0,0.08)", margin: "0 auto 30px", maxWidth: "400px" }}>
      <div style={{ fontSize: "48px", marginBottom: "15px", animation: isCharging && !isComplete ? "pulse 2s ease-in-out infinite" : "none" }}>🔋</div>
      <div style={{ position: "relative", marginBottom: "20px" }}>
        <svg width={size} height={size} style={{ transform: "rotate(-90deg)", filter: "drop-shadow(0 2px 8px rgba(0,191,166,0.3))" }}>
          <circle cx={size / 2} cy={size / 2} r={radius} stroke={trackColor} strokeWidth={strokeWidth} fill="none" />
          <circle cx={size / 2} cy={size / 2} r={radius} stroke={progressColor} strokeWidth={strokeWidth} fill="none" strokeDasharray={circumference} strokeDashoffset={offset} strokeLinecap="round" style={{ transition: "stroke-dashoffset 0.8s cubic-bezier(0.4, 0, 0.2, 1), stroke 0.3s ease" }} />
        </svg>
        <div style={{ position: "absolute", top: "50%", left: "50%", transform: "translate(-50%, -50%)", textAlign: "center" }}>
          <div style={{ fontSize: "48px", fontWeight: "800", color: progressColor, lineHeight: "1", marginBottom: "5px" }}>{animatedSoc.toFixed(0)}%</div>
          <div style={{ fontSize: "13px", color: "#666", fontWeight: "500", textTransform: "uppercase", letterSpacing: "0.5px" }}>Pin hiện tại</div>
        </div>
      </div>
      <div style={{ textAlign: "center", fontSize: "15px", color: isComplete ? "#2196f3" : isCharging ? "#00BFA6" : "#666", fontWeight: "600", padding: "10px 20px", background: isComplete ? "rgba(33, 150, 243, 0.1)" : isCharging ? "rgba(0, 191, 166, 0.1)" : "rgba(0, 0, 0, 0.05)", borderRadius: "20px" }}>
        {isComplete ? "✅ Hoàn tất sạc" : isCharging ? "⚡ Đang sạc..." : "Dung lượng pin (ước tính)"}
      </div>
      <style>{`@keyframes pulse { 0%, 100% { transform: scale(1); opacity: 1; } 50% { transform: scale(1.1); opacity: 0.8; } }`}</style>
    </div>
  );
});

const InfoCard = memo(function InfoCard({ icon, label, value, color = "#00BFA6", unit = "" }) {
  return (
    <div style={{ background: "white", padding: "20px", borderRadius: "12px", boxShadow: "0 2px 12px rgba(0,0,0,0.06)", textAlign: "center", border: `2px solid ${color}15`, transition: "transform 0.2s ease, box-shadow 0.2s ease" }}
      onMouseEnter={(e) => { e.currentTarget.style.transform = "translateY(-3px)"; e.currentTarget.style.boxShadow = "0 4px 20px rgba(0,0,0,0.12)"; }}
      onMouseLeave={(e) => { e.currentTarget.style.transform = "translateY(0)"; e.currentTarget.style.boxShadow = "0 2px 12px rgba(0,0,0,0.06)"; }}
    >
      <div style={{ fontSize: "32px", marginBottom: "8px" }}>{icon}</div>
      <div style={{ fontSize: "13px", color: "#666", marginBottom: "8px", fontWeight: "500" }}>{label}</div>
      <div style={{ fontSize: "24px", fontWeight: "700", color: color }}>{value}<span style={{ fontSize: "16px", fontWeight: "500", marginLeft: "4px" }}>{unit}</span></div>
    </div>
  );
});

export default function ChargingSession() {
  const navigate = useNavigate();
  const location = useLocation();
  const params = useParams();

  // State Management
  const [currentSession, setCurrentSession] = useState(null);
  const [loading, setLoading] = useState(false);
  const [stopping, setStopping] = useState(false);
  const [autoRedirected, setAutoRedirected] = useState(false);
  const [currentPower, setCurrentPower] = useState(0);
  const [batteryCapacity, setBatteryCapacity] = useState(getBatteryCapacity());
  const [lastEnergySync, setLastEnergySync] = useState(null);

  // Booking & QR State
  const qrFromState = location?.state?.qrBlobUrl;
  const stateBooking = location?.state?.booking;
  const bookingIdFromParams = params?.bookingId;
  const [qrUrl, setQrUrl] = useState(qrFromState || null);
  const [booking, setBooking] = useState(stateBooking || null);
  const [bookingLoading, setBookingLoading] = useState(false);

  // =================================================================
  // 🚀 REFACTOR: REFS (Để truy cập state mới nhất trong setInterval)
  // =================================================================
  const sessionRef = useRef(currentSession);
  const powerRef = useRef(currentPower);
  const capacityRef = useRef(batteryCapacity);

  // Sync Refs với State
  useEffect(() => {
    sessionRef.current = currentSession;
    powerRef.current = currentPower;
    capacityRef.current = batteryCapacity;
  }, [currentSession, currentPower, batteryCapacity]);

  const statusColors = { IN_PROGRESS: "#4caf50", COMPLETED: "#2196f3", FAILED: "#f44336", PENDING: "#ff9800" };
  const qrStorageKey = (id) => (id ? `qr_booking_${id}` : null);
  const getSimulationKey = (sessionId) => sessionId ? `chargingSession_simulation_${sessionId}` : null;

  // Persistence logic (Giữ nguyên)
  const saveSimState = useCallback((session) => {
    if (!session?.sessionId) return;
    try {
      const key = getSimulationKey(session.sessionId);
      if (!key) return;
      localStorage.setItem(key, JSON.stringify({
        sessionId: session.sessionId,
        virtualSoc: session.virtualSoc,
        energyKWh: session.energyKWh,
        durationMinutes: session.durationMinutes,
        lastUpdated: Date.now(),
        status: session.status,
      }));
    } catch (err) { console.debug("Failed to save simulation state:", err); }
  }, []);

  const clearSimState = useCallback((sessionId) => {
    if (!sessionId) return;
    try {
      const key = getSimulationKey(sessionId);
      if (key) localStorage.removeItem(key);
      if (sessionRef.current?.bookingId) {
        sessionStorage.removeItem(`booking_${sessionRef.current.bookingId}_maxPowerKW`);
      }
    } catch (err) { console.debug("Failed to clear simulation state:", err); }
  }, []);

  // Restore QR Logic
  useEffect(() => {
    if (qrUrl) return;
    const attemptRestore = () => {
      const idCandidates = [bookingIdFromParams, booking?.bookingId ?? booking?.id, currentSession?.bookingId];
      for (const id of idCandidates) {
        if (!id) continue;
        const key = qrStorageKey(id);
        const stored = key ? sessionStorage.getItem(key) : null;
        if (stored) { setQrUrl(stored); return; }
      }
    };
    attemptRestore();
  }, [booking, bookingIdFromParams, qrUrl, currentSession]);

  // Auth Check & Initial Load
  useEffect(() => {
    if (!isAuthenticated()) {
      toast.warning("Bạn chưa đăng nhập. Vui lòng đăng nhập!", { position: "top-center", autoClose: 3000 });
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
        setCurrentPower(0);
        return;
      }
      const session = response.data ?? response;
      const power = extractPowerKW(session, session.bookingId);
      const pointNumber = session.pointNumber ?? session.chargingPoint?.pointNumber ?? null;
      setCurrentPower(power);
      setCurrentSession({ ...session, pointNumber });
    } catch (error) {
      console.error("Lỗi khi lấy phiên sạc:", error);
      toast.error("Không thể lấy thông tin phiên sạc");
    } finally {
      setLoading(false);
    }
  };

  // =================================================================
  // ⚡ CORE LOGIC: POLLING & SIMULATION (Optimized with useRef)
  // =================================================================

  // 1. Unified Polling Effect (Status, Power, Energy)
  // Chỉ chạy 1 lần khi mount, không bao giờ reset interval do state change
  useEffect(() => {
    const statusInterval = setInterval(async () => {
      try {
        const response = await getMySessions();
        if (!response?.success) return;
        const sessions = response.data ?? response;
        if (!Array.isArray(sessions)) return;

        // Logic check status
        const currentId = sessionRef.current?.sessionId;
        const inProgress = sessions.find(s => s.status === 'IN_PROGRESS');

        if (inProgress) {
            // Nếu có session mới hoặc session hiện tại thay đổi trạng thái
            if (!currentId || currentId !== inProgress.sessionId) {
                 setCurrentSession(inProgress);
            }
        } else if (currentId) {
            // Không còn IN_PROGRESS, kiểm tra xem session hiện tại đã xong chưa
            const justCompleted = sessions.find(s => s.sessionId === currentId);
            if (justCompleted && (justCompleted.status === 'COMPLETED' || justCompleted.status === 'FINISHED')) {
                 console.log("✅ Polling detected completion:", justCompleted);
                 const pricePerKWh = justCompleted.energyKWh > 0 ? Math.round((justCompleted.cost / justCompleted.energyKWh) * 100) / 100 : 0;
                 setCurrentSession(prev => ({
                     ...justCompleted,
                     virtualSoc: justCompleted.finalSoc,
                     pricePerKWh,
                     pointNumber: prev?.pointNumber
                 }));
            }
        }
      } catch (e) { console.debug("Status polling error", e); }
    }, POLLING_INTERVAL);

    const powerInterval = setInterval(async () => {
      if (sessionRef.current?.status !== "IN_PROGRESS") return;
      try {
        const response = await stationAPI.getCurrentChargingSession();
        const updated = response.data ?? response;
        const newPower = updated.chargingPoint?.maxPowerKW;
        if (newPower && newPower !== powerRef.current) {
          console.log(`⚡ Power updated: ${newPower} kW`);
          setCurrentPower(newPower);
        }
      } catch (e) { console.debug("Power polling error", e); }
    }, POWER_POLLING_INTERVAL);

    const energyInterval = setInterval(async () => {
      if (sessionRef.current?.status !== "IN_PROGRESS") return;
      try {
        console.log("🔄 Syncing energy from Backend...");
        const response = await stationAPI.getCurrentChargingSession();
        const backendSession = response.data ?? response;
        if (backendSession?.status === "IN_PROGRESS") {
           const startTime = new Date(backendSession.startTime);
           const now = new Date();
           const durationMinutes = (now - startTime) / (1000 * 60);
           
           // Tính toán metrics dựa trên dữ liệu thật từ Backend + Power hiện tại
           const metrics = calculateChargingMetrics({
              startTime: backendSession.startTime,
              initialSoc: backendSession.initialSoc ?? 20,
              powerKW: powerRef.current || 11.0,
              capacity: capacityRef.current,
              efficiency: CHARGING_EFFICIENCY
           });
           
           console.log(`📊 Synced Energy: ${metrics.energyKWh} kWh`);
           setCurrentSession(prev => {
              if (!prev) return prev;
              // Chỉ update nếu session đang chạy
              return { ...prev, energyKWh: metrics.energyKWh, virtualSoc: metrics.finalSOC, durationMinutes };
           });
           setLastEnergySync(new Date());
        }
      } catch (e) { console.debug("Energy polling error", e); }
    }, ENERGY_POLLING_INTERVAL);

    return () => {
        clearInterval(statusInterval);
        clearInterval(powerInterval);
        clearInterval(energyInterval);
    };
  }, []); // ✅ Empty deps array: Intervals never restart!

  // 2. Simulation Effect (Visual Smoothness)
  // Tách biệt hoàn toàn, chỉ tính toán số liệu ảo để hiển thị
  useEffect(() => {
    if (!currentSession || currentSession.status !== 'IN_PROGRESS') return;

    const simInterval = setInterval(() => {
        setCurrentSession(prev => {
            if (!prev || prev.status !== 'IN_PROGRESS') return prev;

            const now = new Date();
            const start = new Date(prev.startTime);
            const durationMinutes = (now - start) / (1000 * 60);

            // Giả lập công suất sạc (Charging Curve)
            let maxStationPower = powerRef.current || 11;
            let currentSoc = prev.virtualSoc || prev.initialSoc || 0;
            let simulatedPower = maxStationPower;

            // Giảm công suất khi pin > 80%
            if (currentSoc > 80) {
                const dropFactor = Math.max(0.1, (100 - currentSoc) / 20);
                simulatedPower = maxStationPower * dropFactor;
            }
            // Thêm nhiễu nhẹ để số nhảy thật hơn
            const noise = (Math.random() - 0.5) * 0.2;
            simulatedPower = Math.max(0, simulatedPower + noise);
            
            // Cập nhật power hiển thị (chỉ visual)
            setCurrentPower(simulatedPower);

            const { finalSOC } = calculateChargingMetrics({
                startTime: prev.startTime,
                initialSoc: prev.initialSoc ?? 20,
                powerKW: simulatedPower,
                capacity: capacityRef.current,
                efficiency: CHARGING_EFFICIENCY
            });

            // Auto-stop logic (Gọi 1 lần)
            if (finalSOC >= 100 && (prev.virtualSoc < 100 || !prev.virtualSoc)) {
                 handleAutoStop(prev.sessionId);
                 return { ...prev, virtualSoc: 100, status: 'FINISHING' }; // Chặn gọi lại
            }

            saveSimState({ ...prev, virtualSoc: finalSOC, durationMinutes });

            return { ...prev, virtualSoc: finalSOC, durationMinutes };
        });
    }, 1000);

    return () => clearInterval(simInterval);
  }, [currentSession?.status]); // Chỉ chạy lại khi status thay đổi (Start/Stop)

  // Auto Stop Helper
  const handleAutoStop = (sessionId) => {
      console.log("🔋 Battery 100% - Auto stopping...");
      stationAPI.stopChargingSession(sessionId, 100).then(() => {
          toast.success("Đã tự động ngắt sạc do pin đầy 100%");
      }).catch(err => console.error("Auto stop failed", err));
  };

  // Auto Redirect Logic
  useEffect(() => {
    if (!currentSession) return;
    const normStatus = String(currentSession.status || "").toUpperCase();
    const isEnded = ["COMPLETED", "STOPPED", "FINISHED"].includes(normStatus);

    if (isEnded && !autoRedirected) {
      console.log(`✅ Session ended (${normStatus}) - Redirecting...`);
      setAutoRedirected(true);
      toast.info(normStatus === "STOPPED" ? "Phiên sạc đã dừng" : "Phiên sạc hoàn tất", { autoClose: 2000 });
      clearSimState(currentSession.sessionId);

      setTimeout(() => {
        navigate(paths.payment, { state: { sessionResult: currentSession } });
      }, 2000);
    }
  }, [currentSession, autoRedirected, navigate, clearSimState]);

  // Handle Manual Stop
  const handleStopSession = async () => {
    if (!currentSession?.sessionId) return;
    if (!window.confirm("Bạn có chắc chắn muốn dừng phiên sạc này không?")) return;

    try {
      setStopping(true);
      setCurrentSession(prev => ({ ...prev, status: "STOPPING" })); // Optimistic update
      const finalSocToSend = Math.round(currentSession.virtualSoc || currentSession.initialSoc || 20);
      
      const response = await stationAPI.stopChargingSession(currentSession.sessionId, finalSocToSend);
      if (!response || response.success === false) {
        throw new Error(response?.message);
      }
      
      const result = response.data ?? response;
      setCurrentSession(prev => syncSessionFromBackend(result, prev));
      toast.success("Đã dừng phiên sạc");
    } catch (err) {
      setCurrentSession(prev => ({ ...prev, status: "IN_PROGRESS" })); // Revert
      toast.error("Lỗi khi dừng phiên sạc");
    } finally {
      setStopping(false);
    }
  };

  // Load Booking & QR helpers (Giữ nguyên)
  useEffect(() => {
    if (!booking && bookingIdFromParams) {
      setBookingLoading(true);
      stationAPI.getBookingById(bookingIdFromParams)
        .then(res => { if(res.success !== false) setBooking(res.data ?? res); })
        .catch(console.error)
        .finally(() => setBookingLoading(false));
    }
  }, [booking, bookingIdFromParams]);

  const handleDownload = () => {
    if (!qrUrl) return;
    const a = document.createElement("a");
    a.href = qrUrl; a.download = `booking-${booking?.bookingId ?? "qr"}.png`;
    document.body.appendChild(a); a.click(); a.remove();
  };


  // =================================================================
  // 🎨 UI RENDER (Giữ nguyên 100% như file cũ)
  // =================================================================
  return (
    <div className="charging-session-container" style={{ padding: "20px", maxWidth: "1200px", margin: "0 auto" }}>
      <h1 style={{ color: "#00BFA6", marginBottom: "30px" }}>Phiên sạc hiện tại</h1>

      {loading ? (
        <p>Đang tải thông tin phiên sạc...</p>
      ) : qrUrl && (!currentSession || currentSession.status !== "IN_PROGRESS") ? (
        <div style={{ background: "white", padding: "20px", borderRadius: "12px", boxShadow: "0 2px 8px rgba(0,0,0,0.1)", textAlign: "center" }}>
          <h2 style={{ color: "#333", marginBottom: "15px" }}>Mã QR đặt chỗ</h2>
          {bookingLoading ? <p>Đang tải...</p> : (
            <>
              <img src={qrUrl} alt="QR Code" style={{ maxWidth: "320px", width: "100%", height: "auto" }} />
              <div style={{ marginTop: 12 }}>
                <button onClick={handleDownload} style={{ padding: "10px 18px", borderRadius: 8, background: "#00BFA6", color: "white", border: "none" }}>Tải mã QR</button>
              </div>
            </>
          )}
        </div>
      ) : currentSession ? (
        <div style={{ background: "white", padding: "20px", borderRadius: "12px", boxShadow: "0 2px 8px rgba(0,0,0,0.1)" }}>
          <h2 style={{ color: "#333", marginBottom: "15px" }}>⚡ Thông tin phiên sạc</h2>
          <div style={{ marginBottom: "20px" }}>
            <p><strong>Booking ID:</strong> {currentSession.bookingId ?? "-"}</p>
            <p><strong>Trạng thái:</strong> <span style={{ padding: "4px 12px", borderRadius: "20px", background: statusColors[currentSession.status] || "#9e9e9e", color: "white", fontSize: "14px", fontWeight: "600" }}>{currentSession.status === "IN_PROGRESS" ? "Đang sạc" : currentSession.status}</span></p>
          </div>

          <div className="quick-info-grid" style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(250px, 1fr))", gap: "15px", marginBottom: "25px" }}>
            <div style={{ background: "#f8f9fa", padding: "15px", borderRadius: "10px", border: "1px solid #e0e0e0" }}>
              <div style={{ fontSize: "14px", color: "#666", marginBottom: "8px" }}>🚗 Thông tin xe</div>
              <div style={{ fontSize: "18px", fontWeight: "600", color: "#333" }}>{currentSession.vehiclePlate ?? "-"}</div>
            </div>
            <div style={{ background: "#f8f9fa", padding: "15px", borderRadius: "10px", border: "1px solid #e0e0e0" }}>
              <div style={{ fontSize: "14px", color: "#666", marginBottom: "8px" }}>🏢 Thông tin trạm</div>
              <div style={{ fontSize: "18px", fontWeight: "600", color: "#333" }}>{currentSession.stationName ?? "-"}</div>
            </div>
            <div style={{ background: "#f8f9fa", padding: "15px", borderRadius: "10px", border: "1px solid #e0e0e0" }}>
               <div style={{ fontSize: "14px", color: "#666", marginBottom: "8px" }}>⏰ Bắt đầu</div>
               <div style={{ fontSize: "16px", fontWeight: "600", color: "#333" }}>{currentSession.startTime ? new Date(currentSession.startTime).toLocaleString("vi-VN") : "-"}</div>
            </div>
          </div>

          {currentSession.initialSoc != null && (
            <BatteryProgressCircle
              initialSoc={currentSession.initialSoc}
              energyKWh={currentSession.energyKWh ?? 0}
              capacity={batteryCapacity}
              isCharging={currentSession.status === "IN_PROGRESS"}
              virtualSoc={currentSession.virtualSoc}
            />
          )}

          <div className="info-card-grid" style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(150px, 1fr))", gap: "15px", marginBottom: "30px" }}>
            <InfoCard icon="⚡" label="Năng lượng" value={(currentSession.energyKWh ?? 0).toFixed(2)} unit="kWh" color="#4caf50" />
            <InfoCard icon="⏱️" label="Thời lượng" value={(currentSession.durationMinutes ?? 0).toFixed(0)} unit="phút" color="#2196f3" />
            <InfoCard icon="⚡" label="Công suất" value={currentPower.toFixed(1)} unit="kW" color="#9c27b0" />
          </div>

          {currentSession.initialSoc != null && (
            <div style={{ background: "#f8f9fa", padding: "20px", borderRadius: "12px", marginBottom: "30px", display: "flex", justifyContent: "space-around", flexWrap: "wrap", gap: "20px" }}>
                <div style={{ textAlign: "center" }}><div style={{fontSize: "14px", color: "#666"}}>SOC Ban đầu</div><div style={{fontSize: "28px", fontWeight: "700", color: "#666"}}>{currentSession.initialSoc}%</div></div>
                <div style={{ width: "2px", background: "#ddd", margin: "0 10px" }} />
                <div style={{ textAlign: "center" }}><div style={{fontSize: "14px", color: "#666"}}>SOC Hiện tại</div><div style={{fontSize: "28px", fontWeight: "700", color: "#00BFA6"}}>{currentSession.virtualSoc?.toFixed(1)}%</div></div>
            </div>
          )}

          <div style={{ marginTop: "30px", display: "flex", gap: "15px" }}>
            <button onClick={fetchCurrentSession} style={{ padding: "12px 24px", background: "#00BFA6", color: "white", border: "none", borderRadius: "8px", fontWeight: "600", cursor: "pointer" }}>🔄 Làm mới</button>
            {currentSession.status === "IN_PROGRESS" && (
              <button onClick={handleStopSession} disabled={stopping} style={{ padding: "12px 24px", background: stopping ? "#ccc" : "#f44336", color: "white", border: "none", borderRadius: "8px", fontWeight: "600", cursor: stopping ? "not-allowed" : "pointer" }}>{stopping ? "Đang dừng..." : "🛑 Dừng phiên sạc"}</button>
            )}
          </div>
        </div>
      ) : (
        <div style={{ background: "#f5f5f5", padding: "20px", borderRadius: "12px", textAlign: "center" }}>
          <p style={{ color: "#666" }}>Không có phiên sạc nào đang hoạt động</p>
        </div>
      )}
    </div>
  );
}