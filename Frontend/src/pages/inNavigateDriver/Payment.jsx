import React, { useEffect, useState } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { toast } from "react-toastify";
import apiClient from "../../api/apiUrls.js";
import "./Payment.css";

export default function Payment() {
  const navigate = useNavigate();
  const location = useLocation();
  const sessionResult = location?.state?.sessionResult;

  const [paymentProcessing, setPaymentProcessing] = useState(false);
  const [paymentMethods, setPaymentMethods] = useState([]);
  const [selectedMethod, setSelectedMethod] = useState(null);
  const [loadingMethods, setLoadingMethods] = useState(true);
  const [paymentCompleted, setPaymentCompleted] = useState(false);

  useEffect(() => {
    if (!sessionResult) {
      toast.error("Không có thông tin thanh toán", { position: "top-center" });
      navigate(-1);
    }
  }, [sessionResult, navigate]);

  // Fetch payment methods from API
  useEffect(() => {
    const fetchMethods = async () => {
      try {
        const response = await apiClient.get("/api/payment-methods");
        const data = response.data;

        // Handle both direct array response or response with data property
        const methods = Array.isArray(data) ? data : data.data || [];
        setPaymentMethods(methods);
      } catch (err) {
        console.error("❌ Lỗi khi tải phương thức thanh toán:", err);
        toast.error("Không thể tải phương thức thanh toán", {
          position: "top-center",
        });
      } finally {
        setLoadingMethods(false);
      }
    };
    fetchMethods();
  }, []);

  const handlePayment = async () => {
    // Check if payment method is selected
    if (!selectedMethod) {
      toast.warning("Vui lòng chọn phương thức thanh toán", {
        position: "top-center",
      });
      return;
    }

    try {
      setPaymentProcessing(true);

      // Lấy thông tin phương thức hiện tại
      const method = paymentMethods.find((m) => m.methodId === selectedMethod);

      if (!method) {
        toast.error("Không tìm thấy phương thức thanh toán!", {
          position: "top-center",
        });
        return;
      }

      // Gọi API thanh toán cho tất cả các phương thức
      const response = await apiClient.post(
        `/api/payment/vnpay/create?sessionId=${session.sessionId}&paymentMethodId=${selectedMethod}`
      );

      // Xử lý response dựa trên loại phương thức
      if (method.provider === "VNPAY" || method.methodType === "EWALLET") {
        // VNPay/E-Wallet: redirect đến trang thanh toán
        if (response.data?.paymentUrl) {
          // 💾 Lưu thông tin thanh toán vào sessionStorage trước khi redirect
          sessionStorage.setItem(
            "pendingPayment",
            JSON.stringify({
              amount: session.cost || 0,
              currency: session.currency || "VND",
              orderInfo: `Thanh toán phiên sạc #${session.sessionId}`,
              stationName: session.stationName,
              vehiclePlate: session.vehiclePlate,
              energyKWh: session.energyKWh,
              durationMinutes: session.durationMinutes,
            })
          );
          window.location.href = response.data.paymentUrl;
          return;
        } else {
          toast.error("Không nhận được liên kết thanh toán từ server!", {
            position: "top-center",
          });
        }
      } else if (method.methodType === "CASH" || method.provider === "EVM") {
        // CASH/EVM: xử lý thanh toán nội bộ, backend đã lưu vào DB
        if (response.data?.message) {
          toast.success("Thanh toán thành công! Hóa đơn đã được lưu.", {
            position: "top-center",
            autoClose: 2000,
          });
          setTimeout(() => {
            setPaymentCompleted(true);
            // Chuyển về trang chủ sau khi thanh toán thành công
            navigate("/");
          }, 2000);
        } else {
          toast.error("Thanh toán thất bại!", {
            position: "top-center",
          });
        }
      } else {
        // Phương thức không được hỗ trợ
        toast.warning("Phương thức thanh toán chưa được hỗ trợ!", {
          position: "top-center",
        });
      }
    } catch (error) {
      console.error("❌ Lỗi khi gọi API thanh toán:", error);
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
              {session.endTime || session.actualEndTime
                ? new Date(
                    session.endTime || session.actualEndTime
                  ).toLocaleString("vi-VN")
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

          {/* Hiển thị đơn giá theo kWh */}
          {session.pricePerKWh != null && session.pricePerKWh > 0 && (
            <div className="info-row">
              <span className="info-label">Đơn giá năng lượng:</span>
              <span className="info-value">
                {session.pricePerKWh.toLocaleString("vi-VN")}{" "}
                {session.currency ?? "VND"}/kWh
              </span>
            </div>
          )}

          {/* Tính và hiển thị đơn giá theo phút nếu có (ngược từ cost - energyCost) */}
          {(() => {
            const energyCost =
              session.pricePerKWh && session.energyKWh
                ? session.pricePerKWh * session.energyKWh
                : 0;
            const timeCost = (session.cost ?? 0) - energyCost;
            const pricePerMin =
              session.durationMinutes > 0 && timeCost > 0
                ? Math.round(timeCost / session.durationMinutes)
                : 0;

            return pricePerMin > 0 ? (
              <div className="info-row">
                <span className="info-label">Đơn giá thời gian:</span>
                <span className="info-value">
                  {pricePerMin.toLocaleString("vi-VN")}{" "}
                  {session.currency ?? "VND"}/phút
                </span>
              </div>
            ) : null;
          })()}

          <div className="info-row">
            <span className="info-label">Năng lượng sạc:</span>
            <span className="info-value">{session.energyKWh ?? 0} kWh</span>
          </div>

          <div className="info-row">
            <span className="info-label">Thời lượng:</span>
            <span className="info-value">
              {session.durationMinutes ?? 0} phút
            </span>
          </div>

          <div className="total-row">
            <span className="total-label">Tổng cộng:</span>
            <span className="total-value">
              {(session.cost ?? 0).toLocaleString("vi-VN")}{" "}
              {session.currency ?? "VND"}
            </span>
          </div>
        </div>

        {/* Payment Methods Section */}
        {!paymentCompleted && (
          <div className="payment-section">
            <h3 className="section-title">💳 Phương thức thanh toán</h3>
            {loadingMethods ? (
              <p style={{ textAlign: "center", color: "#666" }}>
                Đang tải phương thức thanh toán...
              </p>
            ) : paymentMethods.length === 0 ? (
              <p style={{ textAlign: "center", color: "#f44336" }}>
                Không có phương thức thanh toán khả dụng
              </p>
            ) : (
              <div className="method-list">
                {paymentMethods.map((method) => (
                  <button
                    key={method.methodId}
                    className={`method-btn ${
                      selectedMethod === method.methodId ? "selected" : ""
                    }`}
                    onClick={() => setSelectedMethod(method.methodId)}
                    disabled={paymentProcessing}
                  >
                    <div className="method-name">
                      💳 {method.provider} ({method.methodType})
                    </div>
                    {method.accountNo && (
                      <div className="method-description">
                        Tài khoản: {method.accountNo}
                      </div>
                    )}
                  </button>
                ))}
              </div>
            )}
          </div>
        )}

        <div className="payment-actions">
          {!paymentCompleted ? (
            <button
              className="btn-payment"
              onClick={handlePayment}
              disabled={paymentProcessing || !selectedMethod}
            >
              {paymentProcessing ? "Đang xử lý..." : "💳 Thanh toán ngay"}
            </button>
          ) : (
            <button className="btn-payment" onClick={() => navigate("/")}>
              ✅ Về trang chủ
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
