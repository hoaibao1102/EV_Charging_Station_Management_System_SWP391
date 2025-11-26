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
              pricePerKWh: session.pricePerKWh,
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

      // Enhanced error message
      let errorMessage = "Thanh toán thất bại";

      if (error.response) {
        // Server responded with error
        const status = error.response.status;
        const data = error.response.data;

        if (status === 409) {
          errorMessage = "Hóa đơn đã được thanh toán rồi!";
        } else if (status === 404) {
          errorMessage = "Không tìm thấy thông tin phiên sạc hoặc hóa đơn!";
        } else if (data?.message) {
          errorMessage = data.message;
        }
      } else if (error.request) {
        errorMessage = "Không thể kết nối đến server!";
      }

      toast.error(errorMessage, { position: "top-center" });
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
        Quay lại
      </button>

      <h1
        className="payment-header"
        style={{
          textAlign: "center",
          fontSize: "32px",
          fontWeight: "700",
          background: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)",
          WebkitBackgroundClip: "text",
          WebkitTextFillColor: "transparent",
          marginBottom: "30px",
        }}
      >
        💳 Thanh toán phiên sạc
      </h1>

      <div
        className="payment-card"
        style={{
          background: "white",
          borderRadius: "16px",
          boxShadow: "0 10px 40px rgba(0,0,0,0.1)",
          padding: "30px",
          maxWidth: "800px",
          margin: "0 auto",
        }}
      >
        <div
          className="payment-status"
          style={{
            textAlign: "center",
            padding: "30px",
            background: "linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%)",
            borderRadius: "12px",
            marginBottom: "30px",
          }}
        >
          <div
            className="status-icon"
            style={{ fontSize: "64px", marginBottom: "15px" }}
          >
            ✅
          </div>
          <h2
            style={{
              color: "#2e7d32",
              fontSize: "28px",
              fontWeight: "700",
              marginBottom: "10px",
            }}
          >
            Phiên sạc hoàn thành!
          </h2>
          <p
            className="status-text"
            style={{ color: "#558b2f", fontSize: "16px" }}
          >
            Vui lòng thanh toán để hoàn tất giao dịch
          </p>
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

          {/* ✅ HIỂN thị đơn giá năng lượng (Backend: pricePerKWh) */}
          {session.pricePerKWh != null && session.pricePerKWh > 0 && (
            <div className="info-row">
              <span className="info-label">💵 Đơn giá điện năng:</span>
              <span
                className="info-value"
                style={{ fontWeight: "600", color: "#667eea" }}
              >
                {session.pricePerKWh.toLocaleString("vi-VN")}{" "}
                {session.currency ?? "VND"}/kWh
              </span>
            </div>
          )}

          {/* ✅ Năng lượng đã sạc */}
          <div className="info-row">
            <span className="info-label">⚡ Năng lượng tiêu thụ:</span>
            <span
              className="info-value"
              style={{ fontWeight: "600", color: "#27ae60" }}
            >
              {(session.energyKWh ?? 0).toFixed(2)} kWh
            </span>
          </div>

          {/* ✅ Thời lượng sạc */}
          <div className="info-row">
            <span className="info-label">⏱️ Thời gian sạc:</span>
            <span className="info-value">
              {session.durationMinutes ?? 0} phút
            </span>
          </div>

          {/* ✅ Divider */}
          <div
            style={{
              borderTop: "2px dashed #e0e0e0",
              margin: "15px 0",
            }}
          ></div>

          {/* ✅ TỔNG TIỀN (từ Backend, đã tính sẵn) */}
          <div
            className="total-row"
            style={{
              background: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)",
              padding: "15px 20px",
              borderRadius: "10px",
              marginTop: "10px",
            }}
          >
            <span
              className="total-label"
              style={{
                color: "white",
                fontSize: "18px",
                fontWeight: "700",
              }}
            >
              💳 Tổng thanh toán:
            </span>
            <span
              className="total-value"
              style={{
                color: "white",
                fontSize: "24px",
                fontWeight: "800",
              }}
            >
              {(session.cost ?? 0).toLocaleString("vi-VN")}{" "}
              {session.currency ?? "VND"}
            </span>
          </div>
        </div>

        {/* Payment Methods Section */}
        {!paymentCompleted && (
          <div
            className="payment-section"
            style={{
              background: "linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)",
              padding: "25px",
              borderRadius: "12px",
              marginTop: "20px",
            }}
          >
            <h3
              className="section-title"
              style={{
                fontSize: "20px",
                fontWeight: "700",
                marginBottom: "20px",
                color: "#2c3e50",
              }}
            >
              💳 Chọn phương thức thanh toán
            </h3>
            {loadingMethods ? (
              <div
                style={{
                  textAlign: "center",
                  padding: "40px",
                  color: "#666",
                }}
              >
                <div style={{ fontSize: "48px", marginBottom: "15px" }}>⏳</div>
                <p>Đang tải phương thức thanh toán...</p>
              </div>
            ) : paymentMethods.length === 0 ? (
              <div
                style={{
                  textAlign: "center",
                  padding: "40px",
                  color: "#f44336",
                  background: "white",
                  borderRadius: "8px",
                }}
              >
                <div style={{ fontSize: "48px", marginBottom: "15px" }}>⚠️</div>
                <p>Không có phương thức thanh toán khả dụng</p>
              </div>
            ) : (
              <div
                className="method-list"
                style={{ display: "grid", gap: "12px" }}
              >
                {paymentMethods.map((method) => (
                  <button
                    key={method.methodId}
                    className={`method-btn ${
                      selectedMethod === method.methodId ? "selected" : ""
                    }`}
                    onClick={() => setSelectedMethod(method.methodId)}
                    disabled={paymentProcessing}
                    style={{
                      padding: "18px 24px",
                      border:
                        selectedMethod === method.methodId
                          ? "3px solid #667eea"
                          : "2px solid #ddd",
                      borderRadius: "10px",
                      background:
                        selectedMethod === method.methodId
                          ? "linear-gradient(135deg, #667eea 0%, #764ba2 100%)"
                          : "white",
                      color:
                        selectedMethod === method.methodId ? "white" : "#333",
                      cursor: paymentProcessing ? "not-allowed" : "pointer",
                      transition: "all 0.3s ease",
                      textAlign: "left",
                      fontWeight: "600",
                      boxShadow:
                        selectedMethod === method.methodId
                          ? "0 8px 20px rgba(102, 126, 234, 0.4)"
                          : "0 2px 8px rgba(0,0,0,0.1)",
                    }}
                  >
                    <div
                      className="method-name"
                      style={{ fontSize: "16px", marginBottom: "5px" }}
                    >
                      {method.provider === "VNPAY" ? "💳" : "💵"}{" "}
                      {method.provider} ({method.methodType})
                    </div>
                    {method.accountNo && (
                      <div
                        className="method-description"
                        style={{
                          fontSize: "13px",
                          opacity: 0.9,
                        }}
                      >
                        📋 Tài khoản: {method.accountNo}
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
