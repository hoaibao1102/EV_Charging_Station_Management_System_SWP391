import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import Nav from 'react-bootstrap/Nav';
import Table from 'react-bootstrap/Table';
import apiClient from "../../api/apiUrls.js";
import { isAuthenticated } from "../../utils/authUtils.js";
import paths from "../../path/paths.jsx";
import Header from '../../components/admin/Header.jsx';
import "../admin/ManagementUser.css";

export default function TransactionHistory() {
  const navigate = useNavigate();
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState("ALL");
  const [searchTerm, setSearchTerm] = useState("");

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
      console.log("Raw transaction data:", response.data);
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

  // Apply filters
  const filteredTransactions = transactions
    .filter((t) => activeTab === "ALL" || t.status === activeTab)
    .filter(
      (t) =>
        searchTerm === "" ||
        t.transactionId.toString().includes(searchTerm) ||
        t.description?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        t.stationName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        t.vehiclePlate?.toLowerCase().includes(searchTerm.toLowerCase())
    );

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

  return (
    <div className="management-user-container">
      <div className="action-section">
        <h2>Lịch sử giao dịch</h2>
        <div>
          <button className="btn-add-staff" onClick={fetchTransactions}>
            🔄 Làm mới
          </button>
        </div>
      </div>

      <ul className="statistics-section">
        <li className="stat-card">
          Tổng giao dịch
          <strong>{stats.total}</strong>
        </li>
        <li className="stat-card">
          Hoàn tất
          <strong>{stats.completed}</strong>
        </li>
        <li className="stat-card">
          Chờ duyệt
          <strong>{stats.pending}</strong>
        </li>
        <li className="stat-card">
          Thất bại
          <strong>{stats.failed}</strong>
        </li>
        <li className="stat-card">
          Tổng tiền
          <strong>{formatCurrency(stats.totalAmount)}</strong>
        </li>
      </ul>

      <div className="table-section">
        <div className="table-scroll-container">
          
          <div className="filter-section">
            <Nav justify variant="tabs" activeKey={activeTab} onSelect={(k) => setActiveTab(k)}>
              <Nav.Item>
                <Nav.Link eventKey="ALL">Tất cả</Nav.Link>
              </Nav.Item>
              <Nav.Item>
                <Nav.Link eventKey="COMPLETED">Hoàn tất</Nav.Link>
              </Nav.Item>
              <Nav.Item>
                <Nav.Link eventKey="PENDING">Chờ duyệt</Nav.Link>
              </Nav.Item>
              <Nav.Item>
                <Nav.Link eventKey="FAILED">Thất bại</Nav.Link>
              </Nav.Item>
            </Nav>
            
            <div style={{ marginTop: '15px' }}>
              <input 
                type="text"
                className="search-input"
                placeholder="🔍 Tìm kiếm theo mã GD, trạm, biển số..." 
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>
          </div>

          {loading ? (
            <div style={{ textAlign: 'center', padding: '30px' }}>
              Đang tải...
            </div>
          ) : (
            <Table className="custom-table">
              <thead>
                <tr>
                  <th>MÃ GIAO DỊCH</th>
                  <th>THỜI GIAN</th>
                  <th>SỐ TIỀN</th>
                  <th>TRẠNG THÁI</th>
                  <th>TRẠM SẠC</th>
                  <th>BIỂN SỐ XE</th>
                  <th>MÔ TẢ</th>
                </tr>
              </thead>
              <tbody>
                {filteredTransactions.length > 0 ? (
                  filteredTransactions.map((transaction) => (
                    <tr key={transaction.transactionId}>
                      <td>#{transaction.transactionId}</td>
                      <td>{formatDateTime(transaction.createdAt)}</td>
                      <td>{formatCurrency(transaction.amount, transaction.currency)}</td>
                      <td>{getStatusText(transaction.status)}</td>
                      <td>{transaction.stationName || "-"}</td>
                      <td>{transaction.vehiclePlate || "-"}</td>
                      <td>{transaction.description || "-"}</td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="7" style={{ textAlign: 'center', padding: '30px' }}>
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
