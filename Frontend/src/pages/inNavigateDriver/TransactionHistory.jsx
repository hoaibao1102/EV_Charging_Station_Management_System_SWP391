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
  const [searchTerm, setSearchTerm] = useState("");
  const [dateFilter, setDateFilter] = useState("ALL"); // ALL, TODAY, WEEK, MONTH
  const [sortBy, setSortBy] = useState("DATE_DESC"); // DATE_DESC, DATE_ASC, AMOUNT_DESC, AMOUNT_ASC

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
    // Navigate to transaction detail page (if exists)
    if (paths.transactionDetail) {
      navigate(
        paths.transactionDetail.replace(
          ":transactionId",
          transaction.transactionId
        ),
        {
          state: { transaction },
        }
      );
    } else {
      // Show detail in modal or alert
      toast.info("Chi tiết giao dịch #" + transaction.transactionId, {
        position: "top-center",
      });
    }
  };

  const filterByDate = (transaction) => {
    if (dateFilter === "ALL") return true;
    const txDate = new Date(transaction.createdAt);
    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());

    switch (dateFilter) {
      case "TODAY":
        return txDate >= today;
      case "WEEK": {
        const weekAgo = new Date(today);
        weekAgo.setDate(weekAgo.getDate() - 7);
        return txDate >= weekAgo;
      }
      case "MONTH": {
        const monthAgo = new Date(today);
        monthAgo.setMonth(monthAgo.getMonth() - 1);
        return txDate >= monthAgo;
      }
      default:
        return true;
    }
  };

  // Apply all filters
  let filteredTransactions = transactions
    .filter((t) => filter === "ALL" || t.status === filter)
    .filter(filterByDate)
    .filter(
      (t) =>
        searchTerm === "" ||
        t.transactionId.toString().includes(searchTerm) ||
        t.description?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        t.stationName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        t.vehiclePlate?.toLowerCase().includes(searchTerm.toLowerCase())
    );

  // Apply sorting
  filteredTransactions = [...filteredTransactions].sort((a, b) => {
    switch (sortBy) {
      case "DATE_DESC":
        return new Date(b.createdAt) - new Date(a.createdAt);
      case "DATE_ASC":
        return new Date(a.createdAt) - new Date(b.createdAt);
      case "AMOUNT_DESC":
        return b.amount - a.amount;
      case "AMOUNT_ASC":
        return a.amount - b.amount;
      default:
        return 0;
    }
  });

  // Calculate statistics
  const stats = {
    total: transactions.length,
    completed: transactions.filter((t) => t.status === "COMPLETED").length,
    pending: transactions.filter((t) => t.status === "PENDING").length,
    failed: transactions.filter((t) => t.status === "FAILED").length,
    totalAmount: transactions
      .filter((t) => t.status === "COMPLETED")
      .reduce((sum, t) => sum + t.amount, 0),
  };

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
        <h1 className="page-title">💸 Lịch sử giao dịch</h1>
        <button className="btn-refresh" onClick={fetchTransactions}>
          🔄 Làm mới
        </button>
      </div>

      {/* Statistics Cards */}
      <div className="stats-grid">
        <div className="stat-card all">
          <div className="stat-icon">📊</div>
          <div className="stat-info">
            <div className="stat-label">Tổng số</div>
            <div className="stat-value">{stats.total}</div>
          </div>
        </div>
        <div className="stat-card completed">
          <div className="stat-icon">✅</div>
          <div className="stat-info">
            <div className="stat-label">Hoàn tất</div>
            <div className="stat-value">{stats.completed}</div>
          </div>
        </div>
        <div className="stat-card pending">
          <div className="stat-icon">⏳</div>
          <div className="stat-info">
            <div className="stat-label">Chờ duyệt</div>
            <div className="stat-value">{stats.pending}</div>
          </div>
        </div>
        <div className="stat-card failed">
          <div className="stat-icon">❌</div>
          <div className="stat-info">
            <div className="stat-label">Thất bại</div>
            <div className="stat-value">{stats.failed}</div>
          </div>
        </div>
      </div>

      {/* Total Amount Card */}
      <div className="total-amount-card">
        <div className="total-amount-icon">💰</div>
        <div className="total-amount-info">
          <div className="total-amount-label">Tổng tiền đã thanh toán</div>
          <div className="total-amount-value">
            {formatCurrency(stats.totalAmount)}
          </div>
        </div>
      </div>

      {/* Search & Filter Bar */}
      <div className="search-filter-bar">
        <div className="search-box">
          <span className="search-icon">🔍</span>
          <input
            type="text"
            className="search-input"
            placeholder="Tìm kiếm theo mã GD, trạm, biển số..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
          {searchTerm && (
            <button className="search-clear" onClick={() => setSearchTerm("")}>
              ✕
            </button>
          )}
        </div>
      </div>

      {/* Date Filter & Sort */}
      <div className="filter-sort-bar">
        <div className="date-filter">
          <label className="filter-label-text">📅</label>
          <select
            className="filter-select"
            value={dateFilter}
            onChange={(e) => setDateFilter(e.target.value)}
          >
            <option value="ALL">Tất cả</option>
            <option value="TODAY">Hôm nay</option>
            <option value="WEEK">7 ngày qua</option>
            <option value="MONTH">30 ngày qua</option>
          </select>
        </div>
        <div className="sort-filter">
          <label className="filter-label-text">⇅</label>
          <select
            className="filter-select"
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value)}
          >
            <option value="DATE_DESC">Mới nhất</option>
            <option value="DATE_ASC">Cũ nhất</option>
            <option value="AMOUNT_DESC">Số tiền giảm dần</option>
            <option value="AMOUNT_ASC">Số tiền tăng dần</option>
          </select>
        </div>
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
                <div className="amount-label">Số tiền</div>
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
