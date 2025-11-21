import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import apiClient from "../../api/apiUrls.js";
import { isAuthenticated } from "../../utils/authUtils.js";
import paths from "../../path/paths.jsx";
import "./TransactionHistory.css";

export default function TransactionHistory() {
  const navigate = useNavigate();
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState("ALL"); // ALL, COMPLETED, FAILED, PENDING

  useEffect(() => {
    if (!isAuthenticated()) {
      toast.warning("Bạn chưa đăng nhập. Vui lòng đăng nhập!", {
        position: "top-center",
        autoClose: 3000,
      });
      navigate(paths.login);
      return;
    }
    fetchTransactions();
  }, [navigate]);

  const fetchTransactions = async () => {
    try {
      setLoading(true);
      const response = await apiClient.get("/api/driver/transactions");

      // Sort by createdAt (newest first)
      const sortedData = (response.data || []).sort(
        (a, b) => new Date(b.createdAt) - new Date(a.createdAt)
      );

      setTransactions(sortedData);
    } catch (error) {
      console.error("Lỗi khi tải lịch sử giao dịch:", error);
      toast.error("Không thể tải lịch sử giao dịch", {
        position: "top-center",
      });
    } finally {
      setLoading(false);
    }
  };

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
        return "Đang xử lý";
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

  const handleTransactionClick = (transaction) => {
    // Navigate to transaction detail page
    navigate(
      paths.transactionDetail.replace(
        ":transactionId",
        transaction.transactionId
      ),
      {
        state: { transaction },
      }
    );
  };

  const filteredTransactions = transactions.filter(
    (t) => filter === "ALL" || t.status === filter
  );

  const formatDateTime = (dateTime) => {
    if (!dateTime) return "-";
    return new Date(dateTime).toLocaleString("vi-VN", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  const formatCurrency = (amount, currency = "VND") => {
    return `${amount.toLocaleString("vi-VN")} ${currency}`;
  };

  if (loading) {
    return (
      <div className="transaction-history-container">
        <div className="loading-spinner">
          <div className="spinner"></div>
          <p>Đang tải lịch sử giao dịch...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="transaction-history-container">
      {/* Header */}
      <div className="transaction-header">
        <button className="btn-back" onClick={() => navigate(-1)}>
          ← Quay lại
        </button>
        <h1 className="page-title">💸 Lịch sử giao dịch</h1>
        <button className="btn-refresh" onClick={fetchTransactions}>
          🔄 Làm mới
        </button>
      </div>

      {/* Filter Tabs */}
      <div className="filter-tabs">
        {["ALL", "COMPLETED", "PENDING", "FAILED"].map((status) => (
          <button
            key={status}
            className={`filter-tab ${filter === status ? "active" : ""}`}
            onClick={() => setFilter(status)}
          >
            <span className="filter-label">
              {status === "ALL"
                ? "Tất cả"
                : status === "COMPLETED"
                ? "Hoàn tất"
                : status === "PENDING"
                ? "Chờ duyệt"
                : "Thất bại"}
            </span>
            <span className="filter-count">
              {status === "ALL"
                ? transactions.length
                : transactions.filter((t) => t.status === status).length}
            </span>
          </button>
        ))}
      </div>

      {/* Statistics Summary */}
      <div className="stats-summary">
        <div className="stat-card">
          <div className="stat-icon">💰</div>
          <div className="stat-info">
            <div className="stat-label">Tổng giao dịch</div>
            <div className="stat-value">{transactions.length}</div>
          </div>
        </div>
        <div className="stat-card success">
          <div className="stat-icon">✅</div>
          <div className="stat-info">
            <div className="stat-label">Hoàn tất</div>
            <div className="stat-value">
              {transactions.filter((t) => t.status === "COMPLETED").length}
            </div>
          </div>
        </div>
        <div className="stat-card pending">
          <div className="stat-icon">⏳</div>
          <div className="stat-info">
            <div className="stat-label">Đang xử lý</div>
            <div className="stat-value">
              {transactions.filter((t) => t.status === "PENDING").length}
            </div>
          </div>
        </div>
        <div className="stat-card failed">
          <div className="stat-icon">❌</div>
          <div className="stat-info">
            <div className="stat-label">Thất bại</div>
            <div className="stat-value">
              {transactions.filter((t) => t.status === "FAILED").length}
            </div>
          </div>
        </div>
      </div>

      {/* Transaction List */}
      {filteredTransactions.length === 0 ? (
        <div className="empty-state">
          <div className="empty-icon">📭</div>
          <h3>Chưa có giao dịch nào</h3>
          <p>
            {filter === "ALL"
              ? "Bạn chưa thực hiện giao dịch nào."
              : `Không có giao dịch nào ở trạng thái "${getStatusText(
                  filter
                )}".`}
          </p>
        </div>
      ) : (
        <div className="transaction-list">
          {filteredTransactions.map((transaction) => (
            <div
              key={transaction.transactionId}
              className="transaction-card"
              onClick={() => handleTransactionClick(transaction)}
            >
              {/* Card Header */}
              <div className="card-header">
                <div className="transaction-id">
                  <span className="id-label">Mã GD:</span>
                  <span className="id-value">#{transaction.transactionId}</span>
                </div>
                <div
                  className="status-badge"
                  style={{
                    backgroundColor: `${getStatusColor(transaction.status)}20`,
                    color: getStatusColor(transaction.status),
                    border: `2px solid ${getStatusColor(transaction.status)}`,
                  }}
                >
                  {getStatusIcon(transaction.status)}{" "}
                  {getStatusText(transaction.status)}
                </div>
              </div>

              {/* Amount */}
              <div className="transaction-amount">
                <div className="amount-value">
                  {formatCurrency(transaction.amount, transaction.currency)}
                </div>
              </div>

              {/* Description */}
              {transaction.description && (
                <div className="transaction-description">
                  <span className="desc-icon">📝</span>
                  <span className="desc-text">{transaction.description}</span>
                </div>
              )}

              {/* Details Grid */}
              <div className="transaction-details">
                {transaction.stationName && (
                  <div className="detail-item">
                    <span className="detail-icon">🏢</span>
                    <div className="detail-content">
                      <div className="detail-label">Trạm sạc</div>
                      <div className="detail-value">
                        {transaction.stationName}
                      </div>
                    </div>
                  </div>
                )}

                {transaction.vehiclePlate && (
                  <div className="detail-item">
                    <span className="detail-icon">🚗</span>
                    <div className="detail-content">
                      <div className="detail-label">Biển số xe</div>
                      <div className="detail-value">
                        {transaction.vehiclePlate}
                      </div>
                    </div>
                  </div>
                )}

                {transaction.invoiceId && (
                  <div className="detail-item">
                    <span className="detail-icon">🧾</span>
                    <div className="detail-content">
                      <div className="detail-label">Mã hóa đơn</div>
                      <div className="detail-value">
                        #{transaction.invoiceId}
                      </div>
                    </div>
                  </div>
                )}

                {transaction.sessionId && (
                  <div className="detail-item">
                    <span className="detail-icon">⚡</span>
                    <div className="detail-content">
                      <div className="detail-label">Mã phiên sạc</div>
                      <div className="detail-value">
                        #{transaction.sessionId}
                      </div>
                    </div>
                  </div>
                )}
              </div>

              {/* Footer */}
              <div className="card-footer">
                <div className="transaction-date">
                  <span className="date-icon">🕒</span>
                  <span className="date-text">
                    {formatDateTime(transaction.createdAt)}
                  </span>
                </div>
                <div className="view-detail-btn">Xem chi tiết →</div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
