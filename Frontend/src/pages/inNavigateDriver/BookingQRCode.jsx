import React, { useEffect, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import "./BookingDetail.css";
import { stationAPI } from "../../api/stationApi.js";
import { toast } from "react-toastify";

export default function BookingQRCode() {
  const location = useLocation();
  const navigate = useNavigate();
  const params = useParams();
  const bookingIdParam = params?.bookingId;

  const stateBooking = location?.state?.booking;
  const qrFromState = location?.state?.qrBlobUrl;

  const [booking, setBooking] = useState(stateBooking || null);
  const [qrUrl, setQrUrl] = useState(qrFromState || null);
  const [loading, setLoading] = useState(false);

  // Try to restore QR from sessionStorage when needed
  useEffect(() => {
    if (qrUrl) return;

    // try bookingId from params or booking object
    const attemptRestore = async () => {
      try {
        const id = booking?.bookingId ?? booking?.id ?? bookingIdParam;
        if (id) {
          const key = `qr_booking_${id}`;
          const stored = sessionStorage.getItem(key);
          if (stored) {
            setQrUrl(stored);
            return;
          }
        }

        // fallback: if exactly one qr_booking_ key exists, use it
        const keys = Object.keys(sessionStorage).filter(
          (k) => k && k.startsWith("qr_booking_")
        );
        if (keys.length === 1) {
          const s = sessionStorage.getItem(keys[0]);
          if (s) setQrUrl(s);
        }
      } catch (e) {
        console.warn("Could not restore QR from sessionStorage", e);
      }
    };

    attemptRestore();
  }, [qrUrl, booking, bookingIdParam]);

  // If we have a bookingId param but no booking data, fetch it
  useEffect(() => {
    if (!booking && bookingIdParam) {
      (async () => {
        try {
          setLoading(true);
          const res = await stationAPI.getBookingById(bookingIdParam);
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
          setLoading(false);
        }
      })();
    }
  }, [booking, bookingIdParam]);

  useEffect(() => {
    return () => {
      // Revoke object URL if we created one to avoid memory leaks
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
    a.download = `booking-${booking?.bookingId ?? bookingIdParam ?? "qr"}.png`;
    document.body.appendChild(a);
    a.click();
    a.remove();
  };

  return (
    <div className="booking-container">
      <button className="btn-back" onClick={() => navigate(-1)}>
        ← Quay lại
      </button>

      <h1 className="booking-header">Mã QR đặt chỗ</h1>

      <div className="booking-card">
        {loading ? (
          <p>Đang tải...</p>
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
                  <button className="btn-primary" onClick={handleDownload}>
                    Tải mã QR
                  </button>
                </div>
              </div>
            ) : (
              <p className="booking-field">
                QR chưa có. Vui lòng xác nhận booking trước.
              </p>
            )}
          </>
        )}
      </div>
    </div>
  );
}
