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
  const [unpaidInvoices, setUnpaidInvoices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState("ALL");
  const [searchTerm, setSearchTerm] = useState("");
  const [dateFilter, setDateFilter] = useState("ALL");
  const [sortBy, setSortBy] = useState("DATE_DESC");
  const [activeTab, setActiveTab] = useState("ALL");

  useEffect(() => {
    if (!isAuthenticated()) {
      toast.warning("Bạn chưa đăng nhập. Vui lòng đăng nhập!", {
        position: "top-center",
        autoClose: 3000,
      });
      navigate(paths.login);
      return;
    }
    fetchData();
  }, [navigate]);

  const fetchData = async () => {
    await Promise.all([fetchTransactions(), fetchUnpaidInvoices()]);
  };

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

  const fetchUnpaidInvoices = async () => {
    try {
      const response = await apiClient.get("/api/driver/invoices/unpaid");
      console.log("📝 Unpaid invoices response:", response.data);
      console.log("📊 Number of unpaid invoices:", response.data?.length || 0);
      setUnpaidInvoices(response.data || []);
    } catch (error) {
      console.error("❌ Lỗi khi tải hóa đơn chưa thanh toán:", error);
      console.error("Error details:", error.response?.data || error.message);
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

  const handleTransactionClick = async (transaction) => {
    if (transaction.status === "PENDING" && transaction.invoiceId) {
      try {
        const response = await apiClient.get(
          `/api/driver/invoices/${transaction.invoiceId}`
        );
        const invoiceData = response.data;

        if (invoiceData.status === "PAID") {
          toast.info("Hóa đơn này đã được thanh toán rồi!", {
            position: "top-center",
          });
          fetchData();
          return;
        }

        if (invoiceData.status !== "UNPAID") {
          toast.warning(
            `Hóa đơn ở trạng thái: ${invoiceData.status}. Không thể thanh toán!`,
            {
              position: "top-center",
            }
          );
          return;
        }

        navigate(paths.payment, {
          state: {
            sessionResult: {
              sessionId: transaction.sessionId,
              invoiceId: transaction.invoiceId,
              stationName: transaction.stationName,
              pointNumber: invoiceData.pointNumber || "-",
              vehiclePlate: transaction.vehiclePlate,
              startTime: invoiceData.startTime,
              endTime: invoiceData.endTime,
              energyKWh: invoiceData.energyKWh || 0,
              cost: transaction.amount,
              durationMinutes: invoiceData.durationMinutes || 0,
              initialSoc: invoiceData.initialSoc,
              finalSoc: invoiceData.finalSoc,
              pricePerKWh: invoiceData.pricePerKWh,
              currency: transaction.currency || "VND",
              status: "COMPLETED",
            },
          },
        });
      } catch (error) {
        console.error("❌ Lỗi khi tải thông tin hóa đơn:", error);

        navigate(paths.payment, {
          state: {
            sessionResult: {
              sessionId: transaction.sessionId,
              invoiceId: transaction.invoiceId,
              stationName: transaction.stationName,
              pointNumber: "-",
              vehiclePlate: transaction.vehiclePlate,
              startTime: transaction.createdAt,
              endTime: transaction.createdAt,
              energyKWh: 0,
              cost: transaction.amount,
              durationMinutes: 0,
              initialSoc: 0,
              finalSoc: 0,
              pricePerKWh: 0,
              currency: transaction.currency || "VND",
              status: "COMPLETED",
            },
          },
        });
      }
      return;
    }

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
      toast.info("Chi tiết giao dịch #" + transaction.transactionId, {
        position: "top-center",
      });
    }
  };

  const handleInvoiceClick = async (invoice) => {
    try {
      const response = await apiClient.get(
        `/api/driver/invoices/${invoice.invoiceId}`
      );
      const invoiceData = response.data;

      navigate(paths.payment, {
        state: {
          sessionResult: {
            sessionId: invoice.sessionId,
            invoiceId: invoice.invoiceId,
            stationName: invoice.stationName,
            pointNumber: invoiceData.pointNumber || "-",
            vehiclePlate: invoice.vehiclePlate,
            startTime: invoice.sessionStartTime || invoiceData.startTime,
            endTime: invoice.sessionEndTime || invoiceData.endTime,
            energyKWh: invoiceData.energyKWh || 0,
            cost: invoice.amount,
            durationMinutes: invoiceData.durationMinutes || 0,
            initialSoc: invoiceData.initialSoc || 0,
            finalSoc: invoiceData.finalSoc || 0,
            pricePerKWh: invoiceData.pricePerKWh || 0,
            currency: invoice.currency || "VND",
            status: "COMPLETED",
          },
        },
      });
    } catch (error) {
      console.error("❌ Lỗi khi tải thông tin hóa đơn:", error);
      toast.error("Không thể tải thông tin hóa đơn", {
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

  // Determine what to display based on filter
  let displayItems = [];
  if (filter === "UNPAID") {
    // Show unpaid invoices
    displayItems = unpaidInvoices
      .filter(
        (inv) =>
          searchTerm === "" ||
          inv.invoiceId.toString().includes(searchTerm) ||
          inv.stationName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
          inv.vehiclePlate?.toLowerCase().includes(searchTerm.toLowerCase())
      )
      .map((inv) => ({
        ...inv,
        type: "INVOICE",
        id: inv.invoiceId,
        createdAt: inv.issuedAt,
      }));
  } else {
    // Show transactions
    displayItems = transactions
      .filter((t) => filter === "ALL" || t.status === filter)
      .filter(filterByDate)
      .filter(
        (t) =>
          searchTerm === "" ||
          t.transactionId.toString().includes(searchTerm) ||
          t.description?.toLowerCase().includes(searchTerm.toLowerCase()) ||
          t.stationName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
          t.vehiclePlate?.toLowerCase().includes(searchTerm.toLowerCase())
      )
      .map((t) => ({ ...t, type: "TRANSACTION", id: t.transactionId }));
  }

  // Apply sorting
  displayItems = [...displayItems].sort((a, b) => {
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

  const stats = {
    total: transactions.length,
    completed: transactions.filter((t) => t.status === "COMPLETED").length,
    pending: transactions.filter((t) => t.status === "PENDING").length,
    failed: transactions.filter((t) => t.status === "FAILED").length,
    unpaid: unpaidInvoices.length,
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
    <div className="transaction-history-container">
      {/* Header */}
      <div className="transaction-header">
        <h1 className="page-title2">💸 Lịch sử giao dịch</h1>
        <button className="btn-refresh" onClick={fetchData}>
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
          <div className="stat-icon">💰</div>
          <div className="stat-info">
            <div className="stat-label">Chưa thanh toán</div>
            <div className="stat-value">{stats.unpaid}</div>
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
                {displayItems.length > 0 ? (
                  displayItems.map((transaction) => (
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
        {["ALL", "COMPLETED", "UNPAID", "FAILED"].map((status) => (
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
                : status === "UNPAID"
                ? "Chưa thanh toán"
                : "Thất bại"}
            </span>
            <span className="filter-count">
              {status === "ALL"
                ? transactions.length
                : status === "UNPAID"
                ? stats.unpaid
                : transactions.filter((t) => t.status === status).length}
            </span>
          </button>
        ))}
      </div>

      {/* Unpaid Invoice Notice */}
      {stats.unpaid > 0 && filter === "ALL" && (
        <div className="pending-notice">
          <div className="pending-icon">⚠️</div>
          <div className="pending-text">
            <strong>Bạn có {stats.unpaid} hóa đơn chưa thanh toán!</strong>
            <p>Nhấn vào tab "Chưa thanh toán" để xem và thanh toán ngay.</p>
          </div>
        </div>
      )}

      {/* Transaction/Invoice List */}
      {displayItems.length === 0 ? (
        <div className="empty-state">
          <div className="empty-icon">📭</div>
          <h3>
            {filter === "UNPAID"
              ? "Không có hóa đơn chưa thanh toán"
              : "Chưa có giao dịch nào"}
          </h3>
          <p>
            {filter === "ALL"
              ? "Bạn chưa thực hiện giao dịch nào."
              : filter === "UNPAID"
              ? "Tất cả hóa đơn đã được thanh toán."
              : `Không có giao dịch nào ở trạng thái "${getStatusText(
                  filter
                )}".`}
          </p>
        </div>
      ) : (
        <div className="transaction-list">
          {displayItems.map((item) => {
            const isInvoice = item.type === "INVOICE";
            const handleClick = isInvoice
              ? () => handleInvoiceClick(item)
              : () => handleTransactionClick(item);

            return (
              <div
                key={item.id}
                className="transaction-card"
                onClick={handleClick}
              >
                {/* Card Header */}
                <div className="card-header">
                  <div className="transaction-id">
                    <span className="id-label">
                      {isInvoice ? "Mã HĐ:" : "Mã GD:"}
                    </span>
                    <span className="id-value">#{item.id}</span>
                  </div>
                  {!isInvoice && (
                    <div
                      className="status-badge"
                      style={{
                        backgroundColor: `${getStatusColor(item.status)}20`,
                        color: getStatusColor(item.status),
                        border: `2px solid ${getStatusColor(item.status)}`,
                      }}
                    >
                      {getStatusIcon(item.status)} {getStatusText(item.status)}
                    </div>
                  )}
                  {isInvoice && (
                    <div
                      className="status-badge"
                      style={{
                        backgroundColor: "#ff980020",
                        color: "#ff9800",
                        border: "2px solid #ff9800",
                      }}
                    >
                      💰 Chưa thanh toán
                    </div>
                  )}
                </div>

                {/* Amount */}
                <div className="transaction-amount">
                  <div className="amount-label">Số tiền</div>
                  <div className="amount-value">
                    {formatCurrency(item.amount, item.currency || "VND")}
                  </div>
                </div>

                {/* Description - only for transactions */}
                {!isInvoice && item.description && (
                  <div className="transaction-description">
                    <span className="desc-icon">📝</span>
                    <span className="desc-text">{item.description}</span>
                  </div>
                )}

                {/* Details Grid */}
                <div className="transaction-details">
                  {item.stationName && (
                    <div className="detail-item">
                      <span className="detail-icon">🏢</span>
                      <div className="detail-content">
                        <div className="detail-label">Trạm sạc</div>
                        <div className="detail-value">{item.stationName}</div>
                      </div>
                    </div>
                  )}

                  {item.vehiclePlate && (
                    <div className="detail-item">
                      <span className="detail-icon">🚗</span>
                      <div className="detail-content">
                        <div className="detail-label">Biển số xe</div>
                        <div className="detail-value">{item.vehiclePlate}</div>
                      </div>
                    </div>
                  )}

                  {item.invoiceId && !isInvoice && (
                    <div className="detail-item">
                      <span className="detail-icon">🧾</span>
                      <div className="detail-content">
                        <div className="detail-label">Mã hóa đơn</div>
                        <div className="detail-value">#{item.invoiceId}</div>
                      </div>
                    </div>
                  )}

                  {item.sessionId && (
                    <div className="detail-item">
                      <span className="detail-icon">⚡</span>
                      <div className="detail-content">
                        <div className="detail-label">Mã phiên sạc</div>
                        <div className="detail-value">#{item.sessionId}</div>
                      </div>
                    </div>
                  )}
                </div>

                {/* Footer */}
                <div className="card-footer">
                  <div className="transaction-date">
                    <span className="date-icon">🕒</span>
                    <span className="date-text">
                      {formatDateTime(item.createdAt)}
                    </span>
                  </div>
                  {isInvoice || item.status === "PENDING" ? (
                    <div className="pay-now-btn">💳 Thanh toán ngay →</div>
                  ) : (
                    <div className="view-detail-btn">Xem chi tiết →</div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
