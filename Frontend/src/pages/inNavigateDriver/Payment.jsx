import React, { useEffect, useState } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { toast } from "react-toastify";
import "./Payment.css";

export default function Payment() {
  const navigate = useNavigate();
  const location = useLocation();
  const sessionResult = location?.state?.sessionResult;

  const [paymentProcessing, setPaymentProcessing] = useState(false);

  useEffect(() => {
    if (!sessionResult) {
      toast.error("Không có thông tin thanh toán", { position: "top-center" });
      navigate(-1);
    }
  }, [sessionResult, navigate]);

  const handlePayment = async () => {
    try {
      setPaymentProcessing(true);

      // Simulate payment processing
      await new Promise((resolve) => setTimeout(resolve, 2000));

      toast.success("Thanh toán thành công!", { position: "top-center" });

      // Navigate to home or success page
      setTimeout(() => {
        navigate("/");
      }, 1500);
    } catch (error) {
      console.error("❌ Lỗi khi thanh toán:", error);
      toast.error("Thanh toán thất bại", { position: "top-center" });
    } finally {
      setPaymentProcessing(false);
    }
  };

  if (!sessionResult) {
    return null;
  }

  const session = sessionResult;

  return (
    <div className="payment-container">
      <button className="btn-back" onClick={() => navigate(-1)}>
        ← Quay lại
      </button>

      <h1 className="payment-header">Thanh toán phiên sạc</h1>

      <div className="payment-card">
        <div className="payment-status">
          <div className="status-icon">✅</div>
          <h2>Phiên sạc hoàn thành!</h2>
          <p className="status-text">Vui lòng thanh toán để hoàn tất</p>
        </div>

        <div className="payment-section">
          <h3 className="section-title">🚗 Thông tin xe</h3>
          <div className="info-row">
            <span className="info-label">Biển số xe:</span>
            <span className="info-value">{session.vehiclePlate ?? "-"}</span>
          </div>
        </div>

        <div className="payment-section">
          <h3 className="section-title">🏢 Thông tin trạm</h3>
          <div className="info-row">
            <span className="info-label">Trạm sạc:</span>
            <span className="info-value">{session.stationName ?? "-"}</span>
          </div>
          <div className="info-row">
            <span className="info-label">Trụ sạc:</span>
            <span className="info-value">{session.pointNumber ?? "-"}</span>
          </div>
        </div>

        <div className="payment-section">
          <h3 className="section-title">⏰ Thời gian sạc</h3>
          <div className="info-row">
            <span className="info-label">Bắt đầu:</span>
            <span className="info-value">
              {session.startTime
                ? new Date(session.startTime).toLocaleString("vi-VN")
                : "-"}
            </span>
          </div>
          <div className="info-row">
            <span className="info-label">Kết thúc:</span>
            <span className="info-value">
              {session.endTime
                ? new Date(session.endTime).toLocaleString("vi-VN")
                : "-"}
            </span>
          </div>
          <div className="info-row">
            <span className="info-label">Thời lượng:</span>
            <span className="info-value highlight">
              {session.durationMinutes ?? 0} phút
            </span>
          </div>
        </div>

        <div className="payment-section">
          <h3 className="section-title">⚡ Năng lượng & SOC</h3>
          <div className="info-row">
            <span className="info-label">Năng lượng đã sạc:</span>
            <span className="info-value highlight-green">
              {session.energyKWh ?? 0} kWh
            </span>
          </div>
          {session.initialSoc != null && (
            <div className="info-row">
              <span className="info-label">SOC ban đầu:</span>
              <span className="info-value">{session.initialSoc}%</span>
            </div>
          )}
          {session.finalSoc != null && (
            <div className="info-row">
              <span className="info-label">SOC cuối:</span>
              <span className="info-value">{session.finalSoc}%</span>
            </div>
          )}
        </div>

        <div className="payment-section payment-summary">
          <h3 className="section-title">💰 Chi tiết thanh toán</h3>
          <div className="info-row">
            <span className="info-label">Đơn giá:</span>
            <span className="info-value">
              {(session.pricePerKWh ?? 0).toLocaleString("vi-VN")}{" "}
              {session.currency ?? "VND"}/kWh
            </span>
          </div>
          <div className="info-row">
            <span className="info-label">Năng lượng:</span>
            <span className="info-value">{session.energyKWh ?? 0} kWh</span>
          </div>
          <div className="total-row">
            <span className="total-label">Tổng cộng:</span>
            <span className="total-value">
              {(session.cost ?? 0).toLocaleString("vi-VN")}{" "}
              {session.currency ?? "VND"}
            </span>
          </div>
        </div>

        <div className="payment-actions">
          <button
            className="btn-payment"
            onClick={handlePayment}
            disabled={paymentProcessing}
          >
            {paymentProcessing ? "Đang xử lý..." : "💳 Thanh toán ngay"}
          </button>

          <button
            className="btn-cancel"
            onClick={() => navigate("/")}
            disabled={paymentProcessing}
          >
            Về trang chủ
          </button>
        </div>
      </div>
    </div>
  );
}
