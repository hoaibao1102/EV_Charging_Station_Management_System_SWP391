import React, { useEffect, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import "./BookingDetail.css";
import { stationAPI } from "../../api/stationApi.js";
import { toast } from "react-toastify";

export default function BookingQRCode() {
  const location = useLocation();
  const navigate = useNavigate();
  const params = useParams();
  const bookingId = params?.bookingId;

  const stateBooking = location?.state?.booking;
  const qrFromState = location?.state?.qrBlobUrl;

  const [booking, setBooking] = useState(stateBooking || null);
  const [qrUrl, setQrUrl] = useState(qrFromState || null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    // If we don't have QR url but have bookingId, try to fetch booking info
    if (!booking && bookingId) {
      (async () => {
        try {
          setLoading(true);
          const res = await stationAPI.getBookingById(bookingId);
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

    // If QR blob isn't provided but server may allow re-fetch, we could call confirm again.
    // But we avoid calling confirm endpoint twice. If qrUrl absent, show instruction.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [bookingId]);

  useEffect(() => {
    return () => {
      // Revoke object URL if we created one to avoid memory leaks
      if (qrUrl && qrUrl.startsWith("blob:")) {
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
    a.download = `booking-${bookingId || "qr"}.png`;
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
            <p className="booking-field">
              <strong>Booking ID:</strong>{" "}
              {booking?.bookingId ?? booking?.id ?? bookingId ?? "-"}
            </p>

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
