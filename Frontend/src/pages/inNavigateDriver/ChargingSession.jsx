import React, { useEffect, useState } from "react";
import { useNavigate, useLocation, useParams } from "react-router-dom";
import paths from "../../path/paths.jsx";
import { toast } from "react-toastify";
import { stationAPI } from "../../api/stationApi.js";
import { isAuthenticated } from "../../utils/authUtils.js";

export default function ChargingSession() {
  const navigate = useNavigate();
  const location = useLocation();
  const params = useParams();

  const [currentSession, setCurrentSession] = useState(null);
  const [loading, setLoading] = useState(false);
  const [stopping, setStopping] = useState(false);

  // QR / booking state (merged behavior)
  const qrFromState = location?.state?.qrBlobUrl;
  const stateBooking = location?.state?.booking;
  const bookingIdFromParams = params?.bookingId;

  const [qrUrl, setQrUrl] = useState(qrFromState || null);
  const [booking, setBooking] = useState(stateBooking || null);
  const [bookingLoading, setBookingLoading] = useState(false);

  // Helper to build sessionStorage key
  const qrStorageKey = (id) => (id ? `qr_booking_${id}` : null);

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

  // Polling: check current session periodically so the UI updates automatically
  // e.g., when an external staff action marks the session IN_PROGRESS.
  useEffect(() => {
    let intervalId = null;

    // small wrapper to call API and update state (avoids dependency on fetchCurrentSession)
    const poll = async () => {
      try {
        const response = await stationAPI.getCurrentChargingSession();
        if (!response || response.success === false) {
          setCurrentSession(null);
          return;
        }
        const session = response.data ?? response;
        setCurrentSession((prev) => {
          // Only update if something changed to avoid extra renders
          if (!prev) return session;
          if (prev.sessionId !== session.sessionId || prev.status !== session.status) return session;
          return prev;
        });
      } catch (err) {
        // don't spam console on intermittent network issues
        console.debug("Polling getCurrentChargingSession error:", err);
      }
    };

    // run immediately then set interval
    poll();
    intervalId = setInterval(poll, 5000);

    return () => {
      if (intervalId) clearInterval(intervalId);
    };
  }, []);

  const handleStopSession = async () => {
    if (!currentSession?.sessionId) {
      toast.error("Không có sessionId để dừng", { position: "top-center" });
      return;
    }
    if (!window.confirm("Bạn có chắc chắn muốn dừng phiên sạc này không?"))
      return;

    try {
      setStopping(true);
      const response = await stationAPI.stopChargingSession(
        currentSession.sessionId
      );
      if (!response || response.success === false) {
        toast.error(response?.message || "Dừng phiên sạc thất bại", {
          position: "top-center",
        });
        return;
      }
      const sessionResult = response.data ?? response;
      toast.success("Dừng phiên sạc thành công!", { position: "top-center" });
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

      navigate(paths.payment, { state: { sessionResult } });
    } catch (err) {
      console.error("Lỗi khi dừng phiên sạc:", err);
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
    <div style={{ padding: "20px", maxWidth: "1200px", margin: "0 auto" }}>
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
              <p style={{ marginBottom: "10px" }}>
                <strong>Booking ID:</strong>{" "}
                {booking?.bookingId ??
                  booking?.id ??
                  bookingIdFromParams ??
                  "-"}
              </p>

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
                  background:
                    currentSession.status === "IN_PROGRESS"
                      ? "#4caf50"
                      : currentSession.status === "COMPLETED"
                      ? "#2196f3"
                      : currentSession.status === "FAILED"
                      ? "#f44336"
                      : "#ff9800",
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

          <div style={{ marginBottom: "20px" }}>
            <h3 style={{ color: "#00BFA6", marginBottom: "10px" }}>
              🚗 Thông tin xe
            </h3>
            <p style={{ marginBottom: "10px" }}>
              <strong>Biển số xe:</strong> {currentSession.vehiclePlate ?? "-"}
            </p>
          </div>

          <div style={{ marginBottom: "20px" }}>
            <h3 style={{ color: "#00BFA6", marginBottom: "10px" }}>
              🏢 Thông tin trạm
            </h3>
            <p style={{ marginBottom: "10px" }}>
              <strong>Trạm sạc:</strong> {currentSession.stationName ?? "-"}
            </p>
          </div>

          <div style={{ marginBottom: "20px" }}>
            <h3 style={{ color: "#00BFA6", marginBottom: "10px" }}>
              ⏰ Thời gian
            </h3>
            <p style={{ marginBottom: "10px" }}>
              <strong>Bắt đầu:</strong>{" "}
              {currentSession.startTime
                ? new Date(currentSession.startTime).toLocaleString("vi-VN")
                : "-"}
            </p>
            <p style={{ marginBottom: "10px" }}>
              <strong>Kết thúc:</strong>{" "}
              {currentSession.endTime
                ? new Date(currentSession.endTime).toLocaleString("vi-VN")
                : "Đang sạc..."}
            </p>
            <p style={{ marginBottom: "10px" }}>
              <strong>Thời lượng:</strong> {currentSession.durationMinutes ?? 0}{" "}
              phút
            </p>
          </div>

          <div style={{ marginBottom: "20px" }}>
            <h3 style={{ color: "#00BFA6", marginBottom: "10px" }}>
              ⚡ Năng lượng & Chi phí
            </h3>
            <p style={{ marginBottom: "10px" }}>
              <strong>Năng lượng đã sạc:</strong>{" "}
              <span
                style={{
                  color: "#4caf50",
                  fontSize: "18px",
                  fontWeight: "600",
                }}
              >
                {currentSession.energyKWh ?? 0} kWh
              </span>
            </p>
            {currentSession.initialSoc != null && (
              <p style={{ marginBottom: "10px" }}>
                <strong>SOC ban đầu:</strong> {currentSession.initialSoc}%
              </p>
            )}
            <p style={{ marginBottom: "10px" }}>
              <strong>Chi phí:</strong>{" "}
              <span
                style={{
                  color: "#ff9800",
                  fontSize: "18px",
                  fontWeight: "600",
                }}
              >
                {(currentSession.cost ?? 0).toLocaleString("vi-VN")}{" "}
                {currentSession.currency ?? "VND"}
              </span>
            </p>
          </div>

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
