import React, { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { toast } from "react-toastify";
import apiClient from "../../api/apiUrls.js";
import "./PaymentSuccess.css";

export default function PaymentSuccess() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const invoiceId = searchParams.get("invoiceId");

  const [invoiceData, setInvoiceData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchInvoiceData = async () => {
      try {
        const response = await apiClient.get(`/api/invoices/${invoiceId}`);
        setInvoiceData(response.data);
        toast.success("Thanh toán thành công!", { position: "top-center" });
      } catch (error) {
        console.error("❌ Lỗi khi tải thông tin hóa đơn:", error);
        toast.warning("Không thể tải thông tin hóa đơn chi tiết", {
          position: "top-center",
        });
      } finally {
        setLoading(false);
      }
    };

    // Nếu có invoiceId từ VNPay callback, fetch thông tin hóa đơn
    if (invoiceId) {
      fetchInvoiceData();
    } else {
      // Nếu không có invoiceId (thanh toán Cash), chỉ hiển thị thông báo thành công
      setLoading(false);
    }
  }, [invoiceId]);

  if (loading) {
    return (
      <div className="payment-success-container">
        <div className="loading-spinner">
          <div className="spinner"></div>
          <p>Đang tải thông tin thanh toán...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="payment-success-container">
      <div className="success-card">
        <div className="success-icon">✅</div>
        <h1 className="success-title">Thanh toán thành công!</h1>
        <p className="success-message">
          Cảm ơn bạn đã sử dụng dịch vụ sạc điện của chúng tôi
        </p>

        {invoiceData && (
          <div className="invoice-details">
            <h2 className="section-title">📄 Thông tin hóa đơn</h2>

            <div className="info-row">
              <span className="info-label">Mã hóa đơn:</span>
              <span className="info-value">#{invoiceData.invoiceId}</span>
            </div>

            {invoiceData.sessionId && (
              <div className="info-row">
                <span className="info-label">Mã phiên sạc:</span>
                <span className="info-value">{invoiceData.sessionId}</span>
              </div>
            )}

            {invoiceData.amount != null && (
              <div className="info-row">
                <span className="info-label">Số tiền:</span>
                <span className="info-value highlight-green">
                  {invoiceData.amount.toLocaleString("vi-VN")}{" "}
                  {invoiceData.currency || "VND"}
                </span>
              </div>
            )}

            {invoiceData.paymentMethod && (
              <div className="info-row">
                <span className="info-label">Phương thức:</span>
                <span className="info-value">{invoiceData.paymentMethod}</span>
              </div>
            )}

            {invoiceData.paidAt && (
              <div className="info-row">
                <span className="info-label">Thời gian:</span>
                <span className="info-value">
                  {new Date(invoiceData.paidAt).toLocaleString("vi-VN")}
                </span>
              </div>
            )}

            {invoiceData.status && (
              <div className="info-row">
                <span className="info-label">Trạng thái:</span>
                <span className="info-value status-badge success">
                  {invoiceData.status}
                </span>
              </div>
            )}
          </div>
        )}

        {!invoiceData && (
          <div className="simple-success">
            <p className="success-note">
              💰 Thanh toán tiền mặt đã được ghi nhận
            </p>
            <p className="success-note">
              📧 Thông tin chi tiết đã được gửi đến email của bạn
            </p>
          </div>
        )}

        <div className="success-actions">
          <button className="btn-home" onClick={() => navigate("/")}>
            🏠 Về trang chủ
          </button>

          {invoiceData && (
            <button
              className="btn-secondary"
              onClick={() => navigate("/profile")}
            >
              📋 Xem lịch sử
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
