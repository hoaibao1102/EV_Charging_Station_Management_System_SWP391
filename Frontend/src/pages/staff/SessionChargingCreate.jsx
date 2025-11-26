import React, { useEffect, useRef, useState, useCallback } from "react";
import { BrowserMultiFormatReader } from "@zxing/library";
import "./SessionCharging.css";
import { stationAPI } from "../../api/stationApi";
import { toast } from "react-toastify";
import { useNavigate, useLocation } from "react-router-dom";
import paths from "../../path/paths.jsx";

export default function SessionChargingCreate() {
  const navigate = useNavigate();
  const location = useLocation();
  const videoRef = useRef(null);
  const previewStreamRef = useRef(null);
  const codeReaderRef = useRef(null);

  const [status, setStatus] = useState("loading"); // loading | ready | error
  const [errorMessage, setErrorMessage] = useState("");
  const [bookingId, setBookingId] = useState("");
  const [starting, setStarting] = useState(false);
  const [startError, setStartError] = useState("");

  const decodeQRData = (text) => {
    try {
      const decoded = JSON.parse(atob(text));
      return decoded;
    } catch {
      console.debug("Decode error:");
      return null;
    }
  };

  const handleScan = useCallback((text) => {
    if (!text) return;
    const decoded = decodeQRData(text);
    if (decoded?.bookingId) {
      setBookingId(String(decoded.bookingId));
    } else {
      setErrorMessage("Mã QR không hợp lệ");
    }
  }, []);

  useEffect(() => {
    // If navigated here with a bookingId (e.g. from session list row), use it
    try {
      const initialBooking = location?.state?.bookingId;
      if (initialBooking) setBookingId(String(initialBooking));
    } catch {
      // ignore
    }

    let mounted = true;
    const codeReader = new BrowserMultiFormatReader();
    codeReaderRef.current = codeReader;

    async function start() {
      try {
        if (previewStreamRef.current) {
          previewStreamRef.current.getTracks().forEach((t) => t.stop());
          previewStreamRef.current = null;
        }

        const stream = await navigator.mediaDevices.getUserMedia({
          video: { facingMode: { ideal: "environment" } },
          audio: false,
        });
        if (!mounted) {
          stream.getTracks().forEach((t) => t.stop());
          return;
        }

        setStatus("ready");
        setErrorMessage("");

        if (videoRef.current) {
          videoRef.current.srcObject = stream;
          try {
            await videoRef.current.play();
          } catch (e) {
            console.debug(e);
          }
          previewStreamRef.current = stream;
        }

        codeReader.decodeFromVideoDevice(
          undefined,
          videoRef.current,
          (result, error) => {
            if (result) {
              handleScan(result.getText());
            }
            if (error) console.debug(error);
          }
        );
      } catch (err) {
        console.error("start camera error", err);
        if (!mounted) return;
        setStatus("error");
        setErrorMessage("Không thể truy cập camera");
      }
    }

    start();

    return () => {
      mounted = false;
      try {
        if (previewStreamRef.current) {
          previewStreamRef.current.getTracks().forEach((t) => t.stop());
          previewStreamRef.current = null;
        }
      } catch (e) {
        console.debug(e);
      }
      try {
        codeReader.reset();
      } catch (e) {
        console.debug(e);
      }
    };
  }, [handleScan, location?.state?.bookingId]);

  async function handleStartSession() {
    setStarting(true);
    setStartError("");

    const payload = { bookingId };
    try {
      const response = await stationAPI.startChargingSession(payload);
      if (!response.success) throw new Error(response.message);

      // ✅ Lưu pointNumber từ response vào sessionStorage
      try {
        const sessionData = response.data || response;
        const sessionId = sessionData?.sessionId;
        const pointNumber = sessionData?.pointNumber;

        if (sessionId && pointNumber) {
          sessionStorage.setItem(
            `session_${sessionId}_pointNumber`,
            pointNumber
          );
          console.log(
            `✅ Saved pointNumber=${pointNumber} for session #${sessionId}`
          );

          // Lưu cả full session data nếu cần
          sessionStorage.setItem(
            `session_${sessionId}_data`,
            JSON.stringify(sessionData)
          );
        }
      } catch (err) {
        console.debug("Failed to cache session data:", err);
      }

      toast.success("Phiên sạc đã được khởi động thành công!");
      // navigate to session list / status page for staff
      try {
        navigate(paths.manageSessionCharging);
      } catch {
        /* ignore navigation failures */
      }
    } catch (err) {
      console.error("Start session error", err);
      setStartError(err.message || "Không thể khởi động phiên sạc");
    } finally {
      setStarting(false);
    }
  }

  return (
    <div style={{ padding: 12, maxWidth: 480, margin: "0 auto" }}>
      <h1 style={{ fontSize: "1.5rem", textAlign: "center" }}>
        Khởi động phiên sạc
      </h1>

      <div
        style={{
          width: "100%",
          aspectRatio: "4/3",
          background: "#111",
          borderRadius: 8,
          overflow: "hidden",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          marginBottom: 16,
        }}
      >
        {status !== "error" ? (
          <video
            ref={videoRef}
            playsInline
            muted
            style={{ width: "100%", height: "100%", objectFit: "cover" }}
          />
        ) : (
          <div
            style={{
              color: "#fff",
              padding: 12,
              textAlign: "center",
            }}
          >
            {errorMessage || "Không thể truy cập camera"}
          </div>
        )}
      </div>

      {(() => {
        const isDisabled = !bookingId || starting;
        const bg = isDisabled ? "#bdbdbd" : "#137f4a";
        const opacity = isDisabled ? 0.65 : 1;
        const cursor = isDisabled ? "not-allowed" : "pointer";
        return (
          <button
            onClick={handleStartSession}
            disabled={isDisabled}
            aria-disabled={isDisabled}
            style={{
              width: "100%",
              padding: 12,
              background: bg,
              color: "#fff",
              border: "none",
              borderRadius: 8,
              fontSize: "1rem",
              cursor,
              opacity,
              transition: "opacity 160ms ease, background 160ms ease",
            }}
          >
            {starting ? "Đang khởi động..." : "Kích hoạt phiên sạc"}
          </button>
        );
      })()}

      {startError && (
        <div
          style={{
            marginTop: 16,
            color: "#b00020",
            textAlign: "center",
            fontSize: "0.875rem",
          }}
        >
          <strong>Lỗi:</strong> {startError}
        </div>
      )}
    </div>
  );
}
