import React, { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { toast } from "react-toastify";
import apiClient from "../../api/apiUrls.js";
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
    const fetchPaymentInfo = async () => {
      try {
        // Get payment info from URL params
        const invoiceIdParam = searchParams.get("invoiceId");
        const transactionIdParam = searchParams.get("transactionId");
        const transactionNo = searchParams.get("vnp_TransactionNo");
        const bankCode = searchParams.get("vnp_BankCode");
        const payDate = searchParams.get("vnp_PayDate");
        const responseCode = searchParams.get("vnp_ResponseCode");

        // Debug: Log all URL params
        console.log("=== VNPay Payment Success Params ===");
        console.log("invoiceId:", invoiceIdParam);
        console.log("transactionId:", transactionIdParam);
        console.log("transactionNo:", transactionNo);
        console.log("bankCode:", bankCode);
        console.log("payDate:", payDate);
        console.log("responseCode:", responseCode);

        // Check if payment is successful
        if (responseCode && responseCode !== "00") {
          toast.error("Thanh toán thất bại!");
          navigate(paths.paymentFailed + `?vnp_ResponseCode=${responseCode}`);
          return;
        }

        // Fetch transaction details from backend
        let finalAmount = "0";
        let finalOrderInfo = "";
        let finalTransactionId = transactionIdParam || "";

        // 💾 Lấy thông tin thanh toán từ sessionStorage (được lưu từ Payment.jsx)
        const pendingPaymentStr = sessionStorage.getItem("pendingPayment");
        if (pendingPaymentStr) {
          try {
            const pendingPayment = JSON.parse(pendingPaymentStr);
            console.log("Pending payment from sessionStorage:", pendingPayment);

            // Sử dụng số tiền từ sessionStorage
            if (pendingPayment.amount) {
              finalAmount = pendingPayment.amount.toLocaleString("vi-VN");
              finalOrderInfo =
                pendingPayment.orderInfo || `Hóa đơn #${invoiceIdParam}`;
              console.log("Using amount from sessionStorage:", finalAmount);
            }

            // Xóa thông tin sau khi đã sử dụng
            sessionStorage.removeItem("pendingPayment");
          } catch (e) {
            console.error("Error parsing pendingPayment:", e);
          }
        }

        // Fallback: Nếu không có trong sessionStorage, thử lấy từ API
        if (finalAmount === "0" && transactionIdParam) {
          try {
            console.log("Fetching transactions...");
            const txResponse = await apiClient.get(`/api/driver/transactions`);
            const transactions = txResponse.data || [];
            console.log("All transactions:", transactions);
            console.log(
              "Looking for transactionId:",
              parseInt(transactionIdParam)
            );

            const transaction = transactions.find(
              (t) => t.transactionId === parseInt(transactionIdParam)
            );

            console.log("Found transaction:", transaction);

            if (transaction) {
              finalAmount = transaction.amount.toLocaleString("vi-VN");
              finalOrderInfo =
                transaction.description || `Hóa đơn #${invoiceIdParam}`;
              console.log("Final amount from API:", finalAmount);
              console.log("Final orderInfo:", finalOrderInfo);
            } else {
              console.warn(
                "Transaction not found, trying to get from invoice..."
              );
              // Fallback: try to get invoice info
              if (invoiceIdParam) {
                try {
                  const invoiceResponse = await apiClient.get(
                    `/api/invoices/${invoiceIdParam}`
                  );
                  if (invoiceResponse.data) {
                    finalAmount =
                      invoiceResponse.data.amount.toLocaleString("vi-VN");
                    finalOrderInfo = `Hóa đơn #${invoiceIdParam}`;
                    console.log("Got amount from invoice:", finalAmount);
                  }
                } catch (invoiceError) {
                  console.error("Error fetching invoice:", invoiceError);
                }
              }
            }
          } catch (error) {
            console.error("Error fetching transaction:", error);
          }
        }

        setPaymentInfo({
          transactionId: finalTransactionId,
          amount: finalAmount,
          orderInfo: finalOrderInfo,
          transactionNo: transactionNo || "",
          bankCode: bankCode || "",
          payDate: payDate ? formatPayDate(payDate) : "",
          responseCode: responseCode || "",
        });

        // Show success toast
        toast.success("Thanh toán thành công!", { position: "top-center" });
      } catch (error) {
        console.error("Error in fetchPaymentInfo:", error);
        toast.error("Có lỗi khi tải thông tin thanh toán");
      }
    };

    fetchPaymentInfo();
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

          {paymentInfo.amount !== "" && (
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
