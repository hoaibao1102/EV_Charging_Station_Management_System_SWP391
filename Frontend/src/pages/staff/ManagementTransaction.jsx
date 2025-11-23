import React, { useEffect, useState } from "react";
import { toast } from "react-toastify";
import Nav from "react-bootstrap/Nav";
import Table from "react-bootstrap/Table";
import {
  getStationTransactionsApi,
  getStationTransactionStatsApi,
} from "../../api/staffApi.js";
import Header from "../../components/admin/Header.jsx";
import "../admin/ManagementUser.css";

export default function ManagementTransaction() {
  const [transactions, setTransactions] = useState([]);
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState(null); // null = ALL, COMPLETED, PENDING, FAILED
  const [searchTerm, setSearchTerm] = useState("");

  useEffect(() => {
    fetchStats();
  }, []);

  // ✅ Auto-refresh stats mỗi 30s
  useEffect(() => {
    fetchStats();

    const statsInterval = setInterval(() => {
      console.log("🔄 Auto-refreshing stats...");
      fetchStats();
    }, 15000); // 30 seconds

    return () => clearInterval(statsInterval);
  }, []);

  useEffect(() => {
    fetchTransactions();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filter]);

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
      });

      setTransactions(response.data.content || response.data || []);
    } catch (error) {
      console.error("Lỗi khi tải giao dịch:", error);
      toast.error("Không thể tải danh sách giao dịch");
    } finally {
      setLoading(false);
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
    <div className="management-user-container">
      <Header />
      {/* Action Section */}
      <div className="action-section">
        <h2>Quản lý giao dịch</h2>
      </div>

      {/* Statistics Section */}
      {stats && (
        <ul className="statistics-section">
          <li className="stat-card">
            Tổng giao dịch
            <strong>{stats.totalTransactions}</strong>
          </li>
          <li className="stat-card">
            Hoàn tất
            <strong>{stats.completedTransactions}</strong>
          </li>
          <li className="stat-card">
            Đang xử lý
            <strong>{stats.pendingTransactions}</strong>
          </li>
          <li className="stat-card">
            Thất bại
            <strong>{stats.failedTransactions}</strong>
          </li>
          <li className="stat-card">
            Doanh thu
            <strong>{formatCurrency(stats.totalRevenue)}</strong>
          </li>
        </ul>
      )}

      {/* Table Section */}
      <div className="table-section">
        <div className="table-scroll-container">
          {/* Filter Section */}
          <div className="filter-section">
            <Nav
              justify
              variant="tabs"
              activeKey={filter || "all"}
              onSelect={(k) => handleFilterChange(k === "all" ? null : k)}
            >
              <Nav.Item>
                <Nav.Link eventKey="all">Tất cả giao dịch</Nav.Link>
              </Nav.Item>
              <Nav.Item>
                <Nav.Link eventKey="COMPLETED">Hoàn tất</Nav.Link>
              </Nav.Item>
              <Nav.Item>
                <Nav.Link eventKey="PENDING">Đang xử lý</Nav.Link>
              </Nav.Item>
              <Nav.Item>
                <Nav.Link eventKey="FAILED">Thất bại</Nav.Link>
              </Nav.Item>
            </Nav>

            <div style={{ marginTop: "15px" }}>
              <input
                type="text"
                className="search-input"
                placeholder="🔍 Tìm kiếm theo mã GD, biển số, mô tả..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>
          </div>

          {/* Bảng */}
          {loading ? (
            <div style={{ textAlign: "center", padding: "30px" }}>
              Đang tải...
            </div>
          ) : (
            <Table className="custom-table">
              <thead>
                <tr>
                  <th>MÃ GIAO DỊCH</th>
                  <th>THỜI GIAN</th>
                  <th>BIỂN SỐ XE</th>
                  <th>SỐ TIỀN</th>
                  <th>TRẠNG THÁI</th>
                  <th>MÔ TẢ</th>
                </tr>
              </thead>
              <tbody>
                {filteredTransactions.length > 0 ? (
                  filteredTransactions.map((tx) => (
                    <tr key={tx.transactionId}>
                      <td>#{tx.transactionId}</td>
                      <td>{formatDateTime(tx.createdAt)}</td>
                      <td>{tx.vehiclePlate}</td>
                      <td>{formatCurrency(tx.amount)}</td>
                      <td>{getStatusText(tx.status)}</td>
                      <td>{tx.description || "N/A"}</td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td
                      colSpan="6"
                      style={{ textAlign: "center", padding: "30px" }}
                    >
                      Không tìm thấy giao dịch phù hợp với yêu cầu.
                    </td>
                  </tr>
                )}
              </tbody>
            </Table>
          )}
        </div>
      </div>
    </div>
  );
}
