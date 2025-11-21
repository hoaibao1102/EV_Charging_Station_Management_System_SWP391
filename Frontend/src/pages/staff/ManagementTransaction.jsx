import React, { useEffect, useState } from "react";
import { toast } from "react-toastify";
import {
  getStationTransactionsApi,
  getStationTransactionStatsApi,
} from "../../api/staffApi.js";
import "./ManagementTransaction.css";

export default function ManagementTransaction() {
  const [transactions, setTransactions] = useState([]);
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState(null); // null = ALL, COMPLETED, PENDING, FAILED
  const [searchTerm, setSearchTerm] = useState("");
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [pageSize] = useState(10);
  const [sortBy, setSortBy] = useState("createdAt");
  const [sortDir, setSortDir] = useState("desc");

  useEffect(() => {
    fetchStats();
  }, []);

  useEffect(() => {
    fetchTransactions();
  }, [filter, currentPage, sortBy, sortDir]);

  const fetchStats = async () => {
    try {
      const response = await getStationTransactionStatsApi();
      setStats(response.data);
    } catch (error) {
      console.error("Lỗi khi tải thống kê:", error);
      toast.error("Không thể tải thống kê giao dịch");
    }
  };

  const fetchTransactions = async () => {
    try {
      setLoading(true);
      const response = await getStationTransactionsApi({
        status: filter,
        page: currentPage,
        size: pageSize,
        sortBy,
        sortDir,
      });

      setTransactions(response.data.content || []);
      setTotalPages(response.data.totalPages || 0);
    } catch (error) {
      console.error("Lỗi khi tải giao dịch:", error);
      toast.error("Không thể tải danh sách giao dịch");
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

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
    }).format(amount);
  };

  const formatDateTime = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleString("vi-VN", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  const handleFilterChange = (newFilter) => {
    setFilter(newFilter);
    setCurrentPage(0);
  };

  const handleSortChange = (field) => {
    if (sortBy === field) {
      setSortDir(sortDir === "asc" ? "desc" : "asc");
    } else {
      setSortBy(field);
      setSortDir("desc");
    }
    setCurrentPage(0);
  };

  const handlePageChange = (newPage) => {
    if (newPage >= 0 && newPage < totalPages) {
      setCurrentPage(newPage);
    }
  };

  // Filter by search term (client-side for current page)
  const filteredTransactions = transactions.filter(
    (t) =>
      searchTerm === "" ||
      t.transactionId.toString().includes(searchTerm) ||
      t.description?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      t.vehiclePlate?.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="management-transaction-container">
      <div className="management-transaction-header">
        <h1>📊 Quản lý Giao dịch</h1>
      </div>

      {/* Statistics Cards */}
      {stats && (
        <div className="stats-cards">
          <div className="stat-card total">
            <div className="stat-icon">📊</div>
            <div className="stat-info">
              <div className="stat-value">{stats.totalTransactions}</div>
              <div className="stat-label">Tổng giao dịch</div>
            </div>
          </div>
          <div className="stat-card completed">
            <div className="stat-icon">✅</div>
            <div className="stat-info">
              <div className="stat-value">{stats.completedTransactions}</div>
              <div className="stat-label">Hoàn tất</div>
            </div>
          </div>
          <div className="stat-card pending">
            <div className="stat-icon">⏳</div>
            <div className="stat-info">
              <div className="stat-value">{stats.pendingTransactions}</div>
              <div className="stat-label">Đang xử lý</div>
            </div>
          </div>
          <div className="stat-card failed">
            <div className="stat-icon">❌</div>
            <div className="stat-info">
              <div className="stat-value">{stats.failedTransactions}</div>
              <div className="stat-label">Thất bại</div>
            </div>
          </div>
          <div className="stat-card revenue">
            <div className="stat-icon">💰</div>
            <div className="stat-info">
              <div className="stat-value">
                {formatCurrency(stats.totalRevenue)}
              </div>
              <div className="stat-label">Doanh thu</div>
            </div>
          </div>
        </div>
      )}

      {/* Filters */}
      <div className="transaction-filters">
        <div className="filter-buttons">
          <button
            className={`filter-btn ${filter === null ? "active" : ""}`}
            onClick={() => handleFilterChange(null)}
          >
            Tất cả
          </button>
          <button
            className={`filter-btn ${filter === "COMPLETED" ? "active" : ""}`}
            onClick={() => handleFilterChange("COMPLETED")}
          >
            ✅ Hoàn tất
          </button>
          <button
            className={`filter-btn ${filter === "PENDING" ? "active" : ""}`}
            onClick={() => handleFilterChange("PENDING")}
          >
            ⏳ Đang xử lý
          </button>
          <button
            className={`filter-btn ${filter === "FAILED" ? "active" : ""}`}
            onClick={() => handleFilterChange("FAILED")}
          >
            ❌ Thất bại
          </button>
        </div>

        <div className="search-box">
          <input
            type="text"
            placeholder="🔍 Tìm theo mã GD, biển số..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
      </div>

      {/* Transactions Table */}
      <div className="transactions-table-wrapper">
        {loading ? (
          <div className="loading-spinner">⏳ Đang tải...</div>
        ) : filteredTransactions.length === 0 ? (
          <div className="no-data">Không có giao dịch nào</div>
        ) : (
          <>
            <table className="transactions-table">
              <thead>
                <tr>
                  <th onClick={() => handleSortChange("transactionId")}>
                    Mã GD {sortBy === "transactionId" && (sortDir === "asc" ? "↑" : "↓")}
                  </th>
                  <th onClick={() => handleSortChange("createdAt")}>
                    Thời gian {sortBy === "createdAt" && (sortDir === "asc" ? "↑" : "↓")}
                  </th>
                  <th>Biển số xe</th>
                  <th onClick={() => handleSortChange("amount")}>
                    Số tiền {sortBy === "amount" && (sortDir === "asc" ? "↑" : "↓")}
                  </th>
                  <th onClick={() => handleSortChange("status")}>
                    Trạng thái {sortBy === "status" && (sortDir === "asc" ? "↑" : "↓")}
                  </th>
                  <th>Mô tả</th>
                </tr>
              </thead>
              <tbody>
                {filteredTransactions.map((tx) => (
                  <tr key={tx.transactionId}>
                    <td className="transaction-id">#{tx.transactionId}</td>
                    <td>{formatDateTime(tx.createdAt)}</td>
                    <td className="vehicle-plate">{tx.vehiclePlate}</td>
                    <td className="amount">{formatCurrency(tx.amount)}</td>
                    <td>
                      <span
                        className="status-badge"
                        style={{ backgroundColor: getStatusColor(tx.status) }}
                      >
                        {getStatusIcon(tx.status)} {getStatusText(tx.status)}
                      </span>
                    </td>
                    <td className="description">{tx.description || "N/A"}</td>
                  </tr>
                ))}
              </tbody>
            </table>

            {/* Pagination */}
            <div className="pagination">
              <button
                className="page-btn"
                onClick={() => handlePageChange(currentPage - 1)}
                disabled={currentPage === 0}
              >
                ← Trước
              </button>
              <span className="page-info">
                Trang {currentPage + 1} / {totalPages}
              </span>
              <button
                className="page-btn"
                onClick={() => handlePageChange(currentPage + 1)}
                disabled={currentPage >= totalPages - 1}
              >
                Sau →
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
