import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import apiClient from "../../api/apiUrls.js";
import { isAuthenticated } from "../../utils/authUtils.js";
import paths from "../../path/paths.jsx";
import "../admin/ManagementUser.css";
import "./TransactionHistory.css"; // Đảm bảo file CSS nằm cùng thư mục

export default function TransactionHistory() {
  const navigate = useNavigate();
  const [transactions, setTransactions] = useState([]);
  const [unpaidInvoices, setUnpaidInvoices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState("ALL");
  const [searchTerm, setSearchTerm] = useState("");
  const [dateFilter, setDateFilter] = useState("ALL");
  const [sortBy, setSortBy] = useState("DATE_DESC");

  // --- MÀU CHỦ ĐẠO ---
  const THEME_COLOR = "#20b2aa";

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
      // Sắp xếp mặc định
      const sortedData = (response.data || []).sort(
        (a, b) => new Date(b.createdAt) - new Date(a.createdAt)
      );
      setTransactions(sortedData);
    } catch (error) {
      console.error("Lỗi tải lịch sử:", error);
      toast.error("Không thể tải lịch sử giao dịch");
    } finally {
      setLoading(false);
    }
  };

  const fetchUnpaidInvoices = async () => {
    try {
      const response = await apiClient.get("/api/driver/invoices/unpaid");
      setUnpaidInvoices(response.data || []);
    } catch (error) {
      console.error("Lỗi tải hóa đơn nợ:", error);
    }
  };

  // --- HELPER FUNCTIONS ---
  const getStatusColor = (status) => {
    switch (status) {
      case "COMPLETED": return THEME_COLOR;
      case "FAILED": return "#ff6b6b";
      case "PENDING": return "#feca57";
      default: return "#a4b0be";
    }
  };

  const getStatusText = (status) => {
    switch (status) {
      case "COMPLETED": return "Hoàn tất";
      case "FAILED": return "Thất bại";
      case "PENDING": return "Đang xử lý";
      case "UNPAID": return "Chờ thanh toán";
      default: return status;
    }
  };

  const formatDateTime = (dateTime) => {
    if (!dateTime) return "-";
    return new Date(dateTime).toLocaleString("vi-VN", {
      year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit",
    });
  };

  const formatCurrency = (amount, currency = "VND") => {
    return `${amount?.toLocaleString("vi-VN") || 0} ${currency}`;
  };

  const filterByDate = (item) => {
    if (dateFilter === "ALL") return true;
    const itemDate = new Date(item.createdAt);
    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());

    switch (dateFilter) {
      case "TODAY": return itemDate >= today;
      case "WEEK": {
        const weekAgo = new Date(today);
        weekAgo.setDate(weekAgo.getDate() - 7);
        return itemDate >= weekAgo;
      }
      case "MONTH": {
        const monthAgo = new Date(today);
        monthAgo.setMonth(monthAgo.getMonth() - 1);
        return itemDate >= monthAgo;
      }
      default: return true;
    }
  };

  // --- LOGIC XỬ LÝ SỰ KIỆN ---
  const handleItemClick = async (item) => {
    // Nếu là hóa đơn nợ (INVOICE) hoặc GD đang chờ (PENDING) -> Đi đến trang thanh toán
    if (item.type === "INVOICE" || (item.status === "PENDING" && item.invoiceId)) {
        handlePaymentNavigation(item);
    } else {
        // Nếu là GD đã xong -> Xem chi tiết
        if (paths.transactionDetail) {
            navigate(paths.transactionDetail.replace(":transactionId", item.id), {
                state: { transaction: item },
            });
        } else {
            toast.info(`Chi tiết giao dịch #${item.id}`);
        }
    }
  };

  const handlePaymentNavigation = async (item) => {
    try {
        const invoiceId = item.invoiceId || item.id; // Nếu item là invoice thì id chính là invoiceId
        const response = await apiClient.get(`/api/driver/invoices/${invoiceId}`);
        const invoiceData = response.data;

        if (invoiceData.status === "PAID") {
            toast.info("Hóa đơn này đã được thanh toán!");
            fetchData();
            return;
        }

        navigate(paths.payment, {
            state: {
                sessionResult: {
                    sessionId: item.sessionId || invoiceData.sessionId,
                    invoiceId: invoiceId,
                    stationName: item.stationName || invoiceData.stationName,
                    pointNumber: invoiceData.pointNumber || "-",
                    vehiclePlate: item.vehiclePlate || invoiceData.vehiclePlate,
                    startTime: invoiceData.startTime || item.createdAt,
                    endTime: invoiceData.endTime || item.createdAt,
                    energyKWh: invoiceData.energyKWh || 0,
                    cost: item.amount || invoiceData.amount,
                    durationMinutes: invoiceData.durationMinutes || 0,
                    initialSoc: invoiceData.initialSoc || 0,
                    finalSoc: invoiceData.finalSoc || 0,
                    pricePerKWh: invoiceData.pricePerKWh || 0,
                    currency: item.currency || "VND",
                    status: "COMPLETED", // Trạng thái session đã xong, chờ thanh toán
                },
            },
        });
    } catch (error) {
        console.error("Lỗi lấy thông tin hóa đơn:", error);
        toast.error("Không thể tải thông tin thanh toán");
    }
  };

  // --- LOGIC GỘP & LỌC DỮ LIỆU (FIXED) ---
  
  // 1. Chuẩn hóa unpaidInvoices
  const formattedUnpaid = unpaidInvoices.map((inv) => ({
    ...inv,
    type: "INVOICE",
    id: inv.invoiceId,
    createdAt: inv.issuedAt,
    status: "UNPAID",
    description: "Thanh toán hóa đơn sạc"
  }));

  // 2. Chuẩn hóa transactions
  const formattedTransactions = transactions.map((t) => ({
    ...t,
    type: "TRANSACTION",
    id: t.transactionId,
  }));

  let displayItems = [];

  // 3. Logic Filter
  if (filter === "ALL") {
    // Gộp cả hai
    displayItems = [...formattedUnpaid, ...formattedTransactions];
  } else if (filter === "UNPAID") {
    displayItems = formattedUnpaid;
  } else {
    // Các tab còn lại chỉ lọc trong transactions
    displayItems = formattedTransactions.filter((t) => t.status === filter);
  }

  // 4. Apply Common Filters (Date & Search) & Sort
  displayItems = displayItems
    .filter(filterByDate)
    .filter((item) => {
        const s = searchTerm.toLowerCase();
        return (
            searchTerm === "" ||
            item.id.toString().includes(s) ||
            item.stationName?.toLowerCase().includes(s) ||
            item.vehiclePlate?.toLowerCase().includes(s) ||
            (item.description && item.description.toLowerCase().includes(s))
        );
    })
    .sort((a, b) => {
        switch (sortBy) {
            case "DATE_DESC": return new Date(b.createdAt) - new Date(a.createdAt);
            case "DATE_ASC": return new Date(a.createdAt) - new Date(b.createdAt);
            case "AMOUNT_DESC": return b.amount - a.amount;
            case "AMOUNT_ASC": return a.amount - b.amount;
            default: return 0;
        }
    });

  // --- THỐNG KÊ ---
  const stats = {
    total: transactions.length + unpaidInvoices.length, // Tổng bao gồm cả nợ
    completed: transactions.filter((t) => t.status === "COMPLETED").length,
    unpaid: unpaidInvoices.length,
    failed: transactions.filter((t) => t.status === "FAILED").length,
  };

  return (
    <div className="transaction-history-container">
      {/* Header */}
      <div className="transaction-header">
        <h1 className="page-title2" style={{ color: THEME_COLOR }}>Lịch sử giao dịch</h1>
        <button className="btn-refresh" onClick={fetchData}>
          🔄 Làm mới
        </button>
      </div>

      {/* Stats Cards */}
      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-label">Tổng hoạt động</div>
          <div className="stat-value">{stats.total}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Hoàn tất</div>
          <div className="stat-value" style={{ color: THEME_COLOR }}>{stats.completed}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Cần thanh toán</div>
          <div className="stat-value" style={{ color: "#feca57" }}>{stats.unpaid}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Thất bại</div>
          <div className="stat-value" style={{ color: "#ff6b6b" }}>{stats.failed}</div>
        </div>
      </div>

      {/* Controls */}
      <div className="control-bar">
        <div className="search-wrapper">
          <input
            type="text"
            className="search-input"
            placeholder="Tìm kiếm mã, biển số, trạm..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
        <div className="filters-wrapper">
          <select className="filter-select" value={dateFilter} onChange={(e) => setDateFilter(e.target.value)}>
            <option value="ALL">Tất cả thời gian</option>
            <option value="TODAY">Hôm nay</option>
            <option value="WEEK">7 ngày qua</option>
            <option value="MONTH">30 ngày qua</option>
          </select>
          <select className="filter-select" value={sortBy} onChange={(e) => setSortBy(e.target.value)}>
            <option value="DATE_DESC">Mới nhất</option>
            <option value="DATE_ASC">Cũ nhất</option>
            <option value="AMOUNT_DESC">Giá trị cao</option>
            <option value="AMOUNT_ASC">Giá trị thấp</option>
          </select>
        </div>
      </div>

      {/* Tabs */}
      <div className="filter-tabs">
        {["ALL", "COMPLETED", "PENDING", "UNPAID", "FAILED"].map((status) => (
          <button
            key={status}
            className={`filter-tab ${filter === status ? "active" : ""}`}
            onClick={() => setFilter(status)}
          >
            {status === "ALL" ? "Tất cả" :
             status === "COMPLETED" ? "Thành công" :
             status === "PENDING" ? "Chờ duyệt" :
             status === "UNPAID" ? "Chưa thanh toán" : "Thất bại"}
             
            <span className="tab-count">
              {status === "ALL" ? stats.total :
               status === "UNPAID" ? stats.unpaid :
               transactions.filter(t => t.status === status).length}
            </span>
          </button>
        ))}
      </div>

      {/* Alert Notice */}
      {stats.unpaid > 0 && filter === "ALL" && (
        <div className="notice-alert">
            ⚡ Bạn có <strong>{stats.unpaid}</strong> hóa đơn cần thanh toán.
        </div>
      )}

      {/* List Content */}
      <div className="transaction-list">
        {loading ? (
            <div className="text-center py-5">Đang tải dữ liệu...</div>
        ) : displayItems.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon">📭</div>
            <p>Không tìm thấy giao dịch nào</p>
          </div>
        ) : (
          displayItems.map((item) => {
            const isUnpaid = item.status === "UNPAID";
            const color = isUnpaid ? "#feca57" : getStatusColor(item.status);

            return (
              <div
                key={`${item.type}-${item.id}`}
                className="transaction-card"
                onClick={() => handleItemClick(item)}
                style={{ borderLeft: `4px solid ${color}` }}
              >
                <div className="card-main">
                    <div className="card-top">
                        <span className="card-id">#{item.id}</span>
                        <span className="card-date">{formatDateTime(item.createdAt)}</span>
                    </div>

                    <div className="card-body-info">
                        <div className="info-row">
                             <strong style={{ fontSize: '1.05rem', color: '#333' }}>
                                {isUnpaid ? "Thanh toán hóa đơn sạc" : (item.description || "Giao dịch trạm sạc")}
                             </strong>
                        </div>
                        <div className="info-row details">
                             <span>📍 {item.stationName || "N/A"}</span>
                             {item.vehiclePlate && (
                                <>
                                    <span className="dot-separator">•</span>
                                    <span>🚗 {item.vehiclePlate}</span>
                                </>
                             )}
                        </div>
                    </div>
                </div>

                <div className="card-side">
                    <div className="price-tag" style={{ color: color }}>
                        {formatCurrency(item.amount, item.currency)}
                    </div>
                    
                    <div className="status-badge-clean" 
                         style={{ 
                            color: color, 
                            backgroundColor: isUnpaid ? "#fff8e1" : `${color}15` 
                         }}>
                        {getStatusText(item.status)}
                    </div>
                    
                    {(isUnpaid || item.status === "PENDING") && (
                        <button className="btn-action-small">Thanh toán</button>
                    )}
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}