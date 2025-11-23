import React, { useEffect, useState } from "react";
import { useNavigate, useLocation, useParams } from "react-router-dom";
import paths from "../../path/paths.jsx";
import "./BookingDetail.css";
import { toast } from "react-toastify";
import { stationAPI } from "../../api/stationApi.js";

export default function BookingDetail() {
  const navigate = useNavigate();
  const location = useLocation();
  const params = useParams();

  // bookingId may come from URL param or navigation state
  const bookingIdFromParam = params?.bookingId;
  const bookingState = location?.state?.booking;
  const bookingIdState = location?.state?.bookingId;

  const bookingId = bookingIdFromParam ?? bookingIdState;

  const [booking, setBooking] = useState(bookingState || null);
  const [loading, setLoading] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [cancelling, setCancelling] = useState(false);

  useEffect(() => {
    if (!booking && bookingId) {
      // fetch booking detail
      (async () => {
        try {
          setLoading(true);
          const res = await stationAPI.getBookingById(bookingId);
          if (!res || res.success === false) {
            console.error("❌ getBookingById failed:", res);
            toast.error(res?.message || "Không thể lấy thông tin booking", {
              position: "top-center",
            });
            navigate(-1);
            return;
          }
          const data = res.data ?? res;
          setBooking(data);
        } catch (err) {
          console.error("❌ Lỗi khi lấy booking:", err);
          toast.error("Không thể lấy thông tin booking", {
            position: "top-center",
          });
          navigate(-1);
        } finally {
          setLoading(false);
        }
      })();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [bookingId]);

  if (!booking && !bookingId) {
    return (
      <div style={{ padding: 20 }}>
        <p>Không có thông tin booking. Vui lòng quay lại.</p>
        <button onClick={() => navigate(-1)}>← Quay lại</button>
      </div>
    );
  }

  const handleConfirm = async () => {
    if (!bookingId) {
      toast.error("Không có bookingId để xác nhận", { position: "top-center" });
      return;
    }

    try {
      setConfirming(true);
      const res = await stationAPI.confirmBooking(bookingId);

      if (!res || res.success === false) {
        console.error("❌ confirmBooking failed:", res);
        toast.error(res?.message || "Xác nhận thất bại", {
          position: "top-center",
        });
        return;
      }

      // res.data is binary ArrayBuffer (PNG). Create blob URL and a data URL (base64)
      const arrayBuffer = res.data;
      const blob = new Blob([arrayBuffer], { type: "image/png" });
      const blobUrl = URL.createObjectURL(blob);

      // Convert blob to data URL so we can persist it across refreshes (sessionStorage)
      const toDataURL = (blob) =>
        new Promise((resolve, reject) => {
          try {
            const reader = new FileReader();
            reader.onloadend = () => resolve(reader.result);
            reader.onerror = reject;
            reader.readAsDataURL(blob);
          } catch (e) {
            reject(e);
          }
        });

      let dataUrl = null;
      try {
        dataUrl = await toDataURL(blob);
        if (dataUrl && bookingId) {
          try {
            sessionStorage.setItem(`qr_booking_${bookingId}`, dataUrl);
          } catch (e) {
            // ignore storage errors
            console.warn("Could not persist QR to sessionStorage", e);
          }
        }
      } catch (err) {
        console.warn("Could not convert QR blob to data URL:", err);
      }

      // update booking local state to mark as confirmed
      setBooking((prev) => ({ ...(prev || {}), status: "confirmed" }));

      // Navigate to charging session page and pass booking + qr url in state
      // We also persisted the data URL to sessionStorage above so ChargingSession can restore it after a refresh.
      navigate(paths.chargingSession, {
        state: { booking: booking || {}, qrBlobUrl: blobUrl },
      });
    } catch (err) {
      console.error("❌ Lỗi khi xác nhận booking:", err);
      toast.error("Xác nhận thất bại", { position: "top-center" });
    } finally {
      setConfirming(false);
    }
  };

  const handleCancel = async () => {
    if (!bookingId) {
      toast.error("Không có bookingId để hủy", { position: "top-center" });
      return;
    }

    // Confirm with user
    if (!window.confirm("Bạn có chắc chắn muốn hủy booking này không?")) {
      return;
    }

    try {
      setCancelling(true);
      const res = await stationAPI.cancelBooking(bookingId);

      if (!res || res.success === false) {
        console.error("❌ cancelBooking failed:", res);
        toast.error(res?.message || "Hủy booking thất bại", {
          position: "top-center",
        });
        return;
      }

      // update booking local state to mark as cancelled
      setBooking((prev) => ({ ...(prev || {}), status: "cancelled" }));

      toast.success("Hủy booking thành công!", { position: "top-center" });

      // Navigate back to previous page after a short delay
      setTimeout(() => {
        navigate(-1);
      }, 1500);
    } catch (err) {
      console.error("❌ Lỗi khi hủy booking:", err);
      toast.error("Hủy booking thất bại", { position: "top-center" });
    } finally {
      setCancelling(false);
    }
  };

  // Render booking fields (safe access)
  const b = booking || {};

  return (
    <div className="booking-container">
      <button className="btn-back" onClick={() => navigate(-1)}>
        ← Quay lại
      </button>

      <h1 className="booking-header">Chi tiết đặt chỗ</h1>

      {loading ? (
        <p>Đang tải...</p>
      ) : (
        <div className="booking-card">
          <p className="booking-field">
            <strong>Booking ID:</strong>{" "}
            {b.bookingId ?? b.bookingID ?? b.id ?? "-"}
          </p>
          <p className="booking-field">
            <strong>Vehicle:</strong> {b.vehicleName ?? b.vehicleName ?? "-"}
          </p>
          <p className="booking-field">
            <strong>Thời gian:</strong>{" "}
            {b.timeRange ?? b.timeRangeString ?? b.bookingDate ?? "-"}
          </p>
          <p className="booking-field">
            <strong>Slot:</strong> {b.slotName ?? b.slotName ?? "-"}
          </p>
          <p className="booking-field">
            <strong>Connector:</strong>{" "}
            {b.connectorType ?? b.connectorType ?? "-"}
          </p>
          <p className="booking-field">
            <strong>Giá:</strong> {b.price ?? "-"}
          </p>
          <p className="booking-field">
            <strong>Trạm:</strong> {b.stationName ?? "-"}
          </p>
          <p className="booking-field">
            <strong>Ngày đặt:</strong> {b.bookingDate ?? b.bookingTime ?? "-"}
          </p>
          <p className="booking-field">
            <strong>Trạng thái:</strong> {b.status ?? "-"}
          </p>

          <div className="booking-actions">
            <button
              onClick={handleConfirm}
              disabled={
                confirming ||
                b.status === "confirmed" ||
                b.status === "cancelled"
              }
              className="btn-primary"
            >
              {b.status === "confirmed"
                ? "Đã xác nhận"
                : confirming
                ? "Đang xác nhận..."
                : "Xác nhận"}
            </button>

            <button
              onClick={handleCancel}
              disabled={cancelling || b.status === "cancelled"}
              className="btn-danger"
              style={{
                background:
                  cancelling || b.status === "cancelled" ? "#ccc" : "#f44336",
                color: "white",
                border: "none",
                padding: "12px 24px",
                borderRadius: "8px",
                fontSize: "16px",
                fontWeight: "600",
                cursor:
                  cancelling || b.status === "cancelled"
                    ? "not-allowed"
                    : "pointer",
              }}
            >
              {b.status === "cancelled"
                ? "Đã hủy"
                : cancelling
                ? "Đang hủy..."
                : "Hủy booking"}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
