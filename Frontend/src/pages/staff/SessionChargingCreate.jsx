import React, { useEffect, useRef, useState, useCallback } from "react";
import { BrowserMultiFormatReader } from "@zxing/library";
import "./SessionCharging.css";
import { stationAPI } from "../../api/stationApi";
import { toast } from "react-toastify";

export default function SessionChargingCreate() {
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
    } catch (e) {
      console.debug("Decode error:", e);
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
  }, [handleScan]);

  async function handleStartSession() {
    setStarting(true);
    setStartError("");

    const payload = { bookingId };
    try {
      const response = await stationAPI.startChargingSession(payload);
      if (!response.success) throw new Error(response.message);
      toast.success("Phiên sạc đã được khởi động thành công!");
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

      <div style={{ marginBottom: 16 }}>
        <label
          style={{
            display: "block",
            marginBottom: 8,
            fontSize: "1rem",
            color: "#333",
          }}
        >
          Booking ID
        </label>
        <input
          value={bookingId}
          onChange={(e) => setBookingId(e.target.value)}
          style={{
            padding: 12,
            width: "100%",
            border: "1px solid #ccc",
            borderRadius: 8,
            fontSize: "1rem",
            background: "#f7f7f7",
            color: "#333",
          }}
          disabled
        />
      </div>

      <button
        onClick={handleStartSession}
        disabled={!bookingId || starting}
        style={{
          width: "100%",
          padding: 12,
          background: starting ? "#ccc" : "#137f4a",
          color: "#fff",
          border: "none",
          borderRadius: 8,
          fontSize: "1rem",
          cursor: starting ? "not-allowed" : "pointer",
        }}
      >
        {starting ? "Đang khởi động..." : "Kích hoạt phiên sạc"}
      </button>

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
