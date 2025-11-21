import React, { useEffect, useState, useCallback } from "react";
import { useNavigate, useParams, useLocation } from "react-router-dom";
import { toast } from "react-toastify";
import apiClient from "../../api/apiUrls.js";
import { isAuthenticated } from "../../utils/authUtils.js";
import paths from "../../path/paths.jsx";
import "./TransactionDetail.css";

export default function TransactionDetail() {
  const navigate = useNavigate();
  const { transactionId } = useParams();
  const location = useLocation();
  const [transaction, setTransaction] = useState(
    location.state?.transaction || null
  );
  const [loading, setLoading] = useState(!transaction);

  const fetchTransactionDetail = useCallback(async () => {
    try {
      setLoading(true);
      // Assuming there's an endpoint to get transaction by ID
      const response = await apiClient.get(
        `/api/driver/transactions/${transactionId}`
      );
      setTransaction(response.data);
    } catch (error) {
      console.error("Lỗi khi tải chi tiết giao dịch:", error);
      toast.error("Không thể tải chi tiết giao dịch", {
        position: "top-center",
      });
      navigate(-1);
    } finally {
      setLoading(false);
    }
  }, [transactionId, navigate]);

  useEffect(() => {
    if (!isAuthenticated()) {
      toast.warning("Bạn chưa đăng nhập. Vui lòng đăng nhập!", {
        position: "top-center",
      });
      navigate(paths.login);
      return;
    }

    // If transaction not passed via state, fetch from API
    if (!transaction && transactionId) {
      fetchTransactionDetail();
    }
  }, [navigate, transactionId, transaction, fetchTransactionDetail]);

  const getStatusColor = (status) => {
    switch (status) {
      case "COMPLETED":
        return "#4caf50";
      case "FAILED":
        return "#f44336";
      case "PENDING":
        return "#ff9800";
      default:
        return "#9e9e9e";
    }
  };

  const getStatusText = (status) => {
    switch (status) {
      case "COMPLETED":
        return "Hoàn tất";
      case "FAILED":
        return "Thất bại";
      case "PENDING":
        return "Chờ xử lý";
      default:
        return status;
    }
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case "COMPLETED":
        return "✅";
      case "FAILED":
        return "❌";
      case "PENDING":
        return "⏳";
      default:
        return "📋";
    }
  };

  const formatDateTime = (dateTime) => {
    if (!dateTime) return "-";
    return new Date(dateTime).toLocaleString("vi-VN", {
      weekday: "long",
      year: "numeric",
      month: "long",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
    });
  };

  const formatCurrency = (amount, currency = "VND") => {
    return `${amount.toLocaleString("vi-VN")} ${currency}`;
  };

  if (loading) {
    return (
      <div className="transaction-detail-container">
        <div className="loading-spinner">
          <div className="spinner"></div>
          <p>Đang tải chi tiết giao dịch...</p>
        </div>
      </div>
    );
  }

  if (!transaction) {
    return (
      <div className="transaction-detail-container">
        <div className="error-state">
          <div className="error-icon">⚠️</div>
          <h3>Không tìm thấy giao dịch</h3>
          <button className="btn-back" onClick={() => navigate(-1)}>
            ← Quay lại
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="transaction-detail-container">
      {/* Header */}
      <div className="detail-header">
        <button className="btn-back" onClick={() => navigate(-1)}>
          ← Quay lại
        </button>
        <h1 className="page-title">Chi tiết giao dịch</h1>
        <div></div>
      </div>

      {/* Main Content */}
      <div className="detail-content">
        {/* Status Card */}
        <div className="status-card">
          <div
            className="status-icon-large"
            style={{ color: getStatusColor(transaction.status) }}
          >
            {getStatusIcon(transaction.status)}
          </div>
          <div
            className="status-text-large"
            style={{ color: getStatusColor(transaction.status) }}
          >
            {getStatusText(transaction.status)}
          </div>
          <div className="transaction-id-large">
            Mã giao dịch: #{transaction.transactionId}
          </div>
        </div>

        {/* Amount Card */}
        <div className="amount-card">
          <div className="amount-label">Số tiền thanh toán</div>
          <div className="amount-value-large">
            {formatCurrency(transaction.amount, transaction.currency)}
          </div>
        </div>

        {/* Information Sections */}
        <div className="info-sections">
          {/* Transaction Info */}
          <div className="info-section">
            <h3 className="section-title">📋 Thông tin giao dịch</h3>
            <div className="info-grid">
              {transaction.description && (
                <div className="info-item full-width">
                  <div className="info-label">Mô tả</div>
                  <div className="info-value">{transaction.description}</div>
                </div>
              )}

              <div className="info-item">
                <div className="info-label">Thời gian tạo</div>
                <div className="info-value">
                  {formatDateTime(transaction.createdAt)}
                </div>
              </div>

              {transaction.invoiceId && (
                <div className="info-item">
                  <div className="info-label">Mã hóa đơn</div>
                  <div className="info-value">#{transaction.invoiceId}</div>
                </div>
              )}

              {transaction.sessionId && (
                <div className="info-item">
                  <div className="info-label">Mã phiên sạc</div>
                  <div className="info-value">#{transaction.sessionId}</div>
                </div>
              )}

              {transaction.bookingId && (
                <div className="info-item">
                  <div className="info-label">Mã đặt chỗ</div>
                  <div className="info-value">#{transaction.bookingId}</div>
                </div>
              )}
            </div>
          </div>

          {/* Station Info */}
          {transaction.stationName && (
            <div className="info-section">
              <h3 className="section-title">🏢 Thông tin trạm sạc</h3>
              <div className="info-grid">
                <div className="info-item">
                  <div className="info-label">Tên trạm</div>
                  <div className="info-value">{transaction.stationName}</div>
                </div>
                {transaction.stationId && (
                  <div className="info-item">
                    <div className="info-label">Mã trạm</div>
                    <div className="info-value">#{transaction.stationId}</div>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Vehicle Info */}
          {transaction.vehiclePlate && (
            <div className="info-section">
              <h3 className="section-title">🚗 Thông tin phương tiện</h3>
              <div className="info-grid">
                <div className="info-item">
                  <div className="info-label">Biển số xe</div>
                  <div className="info-value">{transaction.vehiclePlate}</div>
                </div>
                {transaction.vehicleId && (
                  <div className="info-item">
                    <div className="info-label">Mã xe</div>
                    <div className="info-value">#{transaction.vehicleId}</div>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>

        {/* Action Buttons */}
        <div className="action-buttons">
          {transaction.sessionId && (
            <button
              className="btn-action"
              onClick={() =>
                navigate(`/charging-session/${transaction.sessionId}`)
              }
            >
              📊 Xem phiên sạc
            </button>
          )}
          {transaction.invoiceId && (
            <button
              className="btn-action"
              onClick={() => navigate(`/invoice/${transaction.invoiceId}`)}
            >
              🧾 Xem hóa đơn
            </button>
          )}
          <button
            className="btn-action secondary"
            onClick={() => navigate(paths.transactionHistory)}
          >
            📜 Xem tất cả giao dịch
          </button>
        </div>
      </div>
    </div>
  );
}
