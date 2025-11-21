import React, { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { toast } from "react-toastify";
import paths from "../../path/paths.jsx";
import "./PaymentSuccess.css";

export default function PaymentSuccess() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [paymentInfo, setPaymentInfo] = useState({
    transactionId: "",
    amount: "",
    orderInfo: "",
    transactionNo: "",
    bankCode: "",
    payDate: "",
    responseCode: "",
  });

  useEffect(() => {
    // Get payment info from URL params
    const transactionId = searchParams.get("vnp_TxnRef");
    const amount = searchParams.get("vnp_Amount");
    const orderInfo = searchParams.get("vnp_OrderInfo");
    const transactionNo = searchParams.get("vnp_TransactionNo");
    const bankCode = searchParams.get("vnp_BankCode");
    const payDate = searchParams.get("vnp_PayDate");
    const responseCode = searchParams.get("vnp_ResponseCode");

    // Check if payment is successful
    if (responseCode && responseCode !== "00") {
      toast.error("Thanh toán thất bại!");
      navigate(paths.paymentFailed + `?vnp_ResponseCode=${responseCode}`);
      return;
    }

    setPaymentInfo({
      transactionId: transactionId || "",
      amount: amount ? (parseInt(amount) / 100).toLocaleString("vi-VN") : "0",
      orderInfo: orderInfo || "",
      transactionNo: transactionNo || "",
      bankCode: bankCode || "",
      payDate: payDate ? formatPayDate(payDate) : "",
      responseCode: responseCode || "",
    });

    // Show success toast
    toast.success("Thanh toán thành công!", { position: "top-center" });
  }, [searchParams, navigate]);

  const formatPayDate = (dateStr) => {
    // Format: YYYYMMDDHHmmss -> DD/MM/YYYY HH:mm:ss
    if (dateStr.length !== 14) return dateStr;

    const year = dateStr.substring(0, 4);
    const month = dateStr.substring(4, 6);
    const day = dateStr.substring(6, 8);
    const hour = dateStr.substring(8, 10);
    const minute = dateStr.substring(10, 12);
    const second = dateStr.substring(12, 14);

    return `${day}/${month}/${year} ${hour}:${minute}:${second}`;
  };

  const handleGoToTransactionHistory = () => {
    navigate(paths.transactionHistory);
  };

  const handleGoToHome = () => {
    navigate(paths.home);
  };

  return (
    <div className="payment-success-container">
      <div className="payment-success-card">
        {/* Success Icon */}
        <div className="success-icon-wrapper">
          <div className="success-icon">
            <svg viewBox="0 0 52 52" className="checkmark">
              <circle
                cx="26"
                cy="26"
                r="25"
                fill="none"
                className="checkmark-circle"
              />
              <path
                fill="none"
                d="M14.1 27.2l7.1 7.2 16.7-16.8"
                className="checkmark-check"
              />
            </svg>
          </div>
        </div>

        {/* Title */}
        <h1 className="success-title">Thanh Toán Thành Công!</h1>
        <p className="success-subtitle">
          Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi
        </p>

        {/* Payment Info */}
        <div className="payment-info-grid">
          {paymentInfo.transactionId && (
            <div className="info-item">
              <span className="info-label">Mã giao dịch:</span>
              <span className="info-value">{paymentInfo.transactionId}</span>
            </div>
          )}

          {paymentInfo.amount && (
            <div className="info-item">
              <span className="info-label">Số tiền:</span>
              <span className="info-value amount">
                {paymentInfo.amount} VND
              </span>
            </div>
          )}

          {paymentInfo.orderInfo && (
            <div className="info-item">
              <span className="info-label">Nội dung:</span>
              <span className="info-value">{paymentInfo.orderInfo}</span>
            </div>
          )}

          {paymentInfo.transactionNo && (
            <div className="info-item">
              <span className="info-label">Mã giao dịch VNPay:</span>
              <span className="info-value">{paymentInfo.transactionNo}</span>
            </div>
          )}

          {paymentInfo.bankCode && (
            <div className="info-item">
              <span className="info-label">Ngân hàng:</span>
              <span className="info-value">{paymentInfo.bankCode}</span>
            </div>
          )}

          {paymentInfo.payDate && (
            <div className="info-item">
              <span className="info-label">Thời gian:</span>
              <span className="info-value">{paymentInfo.payDate}</span>
            </div>
          )}
        </div>

        {/* Actions */}
        <div className="action-buttons">
          <button
            className="btn-primary"
            onClick={handleGoToTransactionHistory}
          >
            📊 Xem Lịch Sử Giao Dịch
          </button>
          <button className="btn-secondary" onClick={handleGoToHome}>
            🏠 Về Trang Chủ
          </button>
        </div>

        {/* Download Receipt */}
        <div className="receipt-section">
          <p className="receipt-text">
            Biên lai thanh toán đã được lưu vào lịch sử giao dịch
          </p>
        </div>
      </div>
    </div>
  );
}
