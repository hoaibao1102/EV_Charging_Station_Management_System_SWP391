import React, { useEffect, useState } from "react";
import { toast } from "react-toastify";
import Nav from "react-bootstrap/Nav";
import Table from "react-bootstrap/Table";
import Modal from "react-bootstrap/Modal";
import Button from "react-bootstrap/Button";
import {
  getStationTransactionsApi,
  getStationTransactionStatsApi,
  getStationInvoicesApi,
  getMyStationApi,
  getInvoiceDetailApi,
  payInvoiceApi,
} from "../../api/staffApi.js";
import Header from "../../components/admin/Header.jsx";
import "../admin/ManagementUser.css";

export default function ManagementTransaction() {
  const [transactions, setTransactions] = useState([]);
  const [stats, setStats] = useState(null);
  const [paymentStats, setPaymentStats] = useState({
    cashRevenue: 0,
    vnpayRevenue: 0,
    cashCount: 0,
    vnpayCount: 0,
  }); // ✅ Thống kê theo phương thức thanh toán
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState(null); // null = ALL, COMPLETED, PENDING, FAILED
  const [searchTerm, setSearchTerm] = useState("");
  const [invoices, setInvoices] = useState([]);
  const [showInvoiceModal, setShowInvoiceModal] = useState(false);
  const [loadingInvoices, setLoadingInvoices] = useState(false);
  const [myStation, setMyStation] = useState(null);
  const [payingInvoiceId, setPayingInvoiceId] = useState(null);

  useEffect(() => {
    const initData = async () => {
      await fetchMyStation();
      await fetchStats();
    };
    initData();
  }, []);

  const fetchMyStation = async () => {
    try {
      const response = await getMyStationApi();
      console.log("✅ Full API response:", response);
      console.log("✅ Response.data:", response.data);
      
      // API trả về array, lấy phần tử đầu tiên
      const stationData = Array.isArray(response.data) ? response.data[0] : response.data;
      console.log("✅ StationData:", stationData);
      
      if (stationData?.station) {
        console.log("✅ Station object:", stationData.station);
        console.log("✅ Station ID:", stationData.station.stationId);
        setMyStation(stationData.station);
      } else if (stationData?.stationId) {
        // Trường hợp data trực tiếp là station object
        console.log("✅ Direct station data:", stationData);
        setMyStation(stationData);
      } else {
        console.error("❌ Không tìm thấy thông tin station trong response");
        console.error("❌ Response structure:", JSON.stringify(response.data, null, 2));
        toast.error("Không tìm thấy trạm được phân công");
      }
    } catch (error) {
      console.error("❌ Lỗi khi lấy thông tin trạm:", error);
      console.error("❌ Error details:", error.response?.data);
      toast.error("Không thể tải thông tin trạm");
    }
  };

  // ✅ Auto-refresh stats mỗi 15s
  useEffect(() => {
    const statsInterval = setInterval(() => {
      console.log("🔄 Auto-refreshing stats...");
      fetchStats();
    }, 15000); // 15 seconds

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
      console.log("✅ Thống kê giao dịch:", response.data);
    } catch (error) {
      console.error("Lỗi khi tải thống kê:", error);
      toast.error("Không thể tải thống kê giao dịch");
    }
  };

  const fetchTransactions = async () => {
    try {
      setLoading(true);
      console.log("📡 [ManagementTransaction] Fetching transactions with filter:", filter);
      
      const response = await getStationTransactionsApi({
        status: filter,
      });

      console.log("✅ [ManagementTransaction] Full response object:", response);
      console.log("✅ [ManagementTransaction] response.success:", response.success);
      console.log("✅ [ManagementTransaction] response.data:", response.data);
      console.log("✅ [ManagementTransaction] response.data type:", typeof response.data);
      
      // ✅ Check if API call was successful
      if (!response.success) {
        console.error("❌ [ManagementTransaction] API returned success=false:", response.message);
        toast.error(response.message || "Không thể tải danh sách giao dịch");
        setTransactions([]);
        return;
      }
      
      // ✅ Backend trả về Spring Page object: {content: [], totalElements, totalPages, ...}
      let txList = [];
      if (response.data && Array.isArray(response.data.content)) {
        txList = response.data.content;
        console.log("✅ [ManagementTransaction] Extracted from Page.content:", txList);
        console.log("✅ [ManagementTransaction] Total elements in DB:", response.data.totalElements);
        console.log("✅ [ManagementTransaction] Total pages:", response.data.totalPages);
      } else if (Array.isArray(response.data)) {
        txList = response.data;
        console.log("✅ [ManagementTransaction] Direct array:", txList);
      } else {
        console.warn("⚠️ [ManagementTransaction] Unexpected response.data structure:", response.data);
        txList = [];
      }
      
      console.log("✅ [ManagementTransaction] Final txList:", txList);
      console.log("✅ [ManagementTransaction] Total transactions in current page:", txList.length);
      
      if (txList.length === 0) {
        console.warn("⚠️ [ManagementTransaction] No transactions found");
        console.warn("⚠️ [ManagementTransaction] Possible causes:");
        console.warn("   1. Staff not assigned to any station with transactions");
        console.warn("   2. Database has no transactions for this station");
        console.warn("   3. Staff status is not ACTIVE");
        console.warn("   4. Backend query filter issue");
      }
      
      setTransactions(txList);

      // ✅ Tính toán thống kê theo payment method (Cash vs VNPay)
      calculatePaymentStats(txList);
    } catch (error) {
      console.error("❌ [ManagementTransaction] Exception:", error);
      console.error("❌ [ManagementTransaction] Error response:", error.response);
      console.error("❌ [ManagementTransaction] Error message:", error.message);
      toast.error("Không thể tải danh sách giao dịch");
      setTransactions([]);
    } finally {
      setLoading(false);
    }
  };

  // ✅ Tính toán doanh thu theo phương thức thanh toán
  const calculatePaymentStats = (txList) => {
    let cashRevenue = 0;
    let vnpayRevenue = 0;
    let cashCount = 0;
    let vnpayCount = 0;

    txList.forEach((tx) => {
      // Chỉ tính các giao dịch COMPLETED
        console.log(tx);
      if (tx.status === "COMPLETED") {
        // Backend có thể trả về paymentMethodName hoặc description chứa thông tin
        // Giả định: nếu description chứa "VNPay" hoặc "VNPAY" → VNPay, còn lại → Cash
        const isVNPay =
          tx.description?.toUpperCase().includes("VNPAY") ||
          tx.description?.toUpperCase().includes("VN PAY");

        if (isVNPay) {
          vnpayRevenue += tx.amount || 0;
          vnpayCount++;
        } else {
          // Mặc định coi là Cash (EVM)
          cashRevenue += tx.amount || 0;
          cashCount++;
        }
      }
    });

    setPaymentStats({
      cashRevenue,
      vnpayRevenue,
      cashCount,
      vnpayCount,
    });
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

  const fetchInvoices = async () => {
    console.log("🔍 Checking myStation:", myStation);
    console.log("🔍 myStation.stationId:", myStation?.stationId);
    
    if (!myStation?.stationId) {
      console.error("⚠️ myStation is null or missing stationId");
      console.error("⚠️ myStation value:", myStation);
      toast.warning("Đang tải thông tin trạm, vui lòng thử lại sau ít giây...");
      return;
    }

    try {
      setLoadingInvoices(true);
      const stationId = myStation.stationId;
      console.log("📡 Fetching invoices for stationId:", stationId);
      console.log("📡 API URL will be: /api/invoice/station/" + stationId + "/details");
      
      const response = await getStationInvoicesApi(stationId);
      console.log("✅ Invoices response:", response);
      console.log("✅ Invoices data:", response.data);
      
      // Chỉ hiển thị hóa đơn chưa thanh toán
      const unpaidInvoices = (response.data || []).filter(inv => inv.status === "UNPAID");
      console.log("✅ Unpaid invoices:", unpaidInvoices);
      
      setInvoices(unpaidInvoices);
      setShowInvoiceModal(true);
    } catch (error) {
      console.error("❌ Lỗi khi tải hóa đơn:", error);
      console.error("❌ Error response:", error.response?.data);
      console.error("❌ Error message:", error.message);
      toast.error("Không thể tải danh sách hóa đơn: " + (error.response?.data?.message || error.message));
    } finally {
      setLoadingInvoices(false);
    }
  };

  const getInvoiceStatusText = (status) => {
    return status === "PAID" ? "Đã thanh toán" : "Chưa thanh toán";
  };

  const getInvoiceStatusStyle = (status) => {
    return {
      padding: "5px 10px",
      borderRadius: "5px",
      backgroundColor: status === "PAID" ? "#d4edda" : "#f8d7da",
      color: status === "PAID" ? "#155724" : "#721c24",
      fontWeight: "500",
    };
  };

  const handlePayInvoice = async (invoiceId) => {
    if (!window.confirm(`Xác nhận thanh toán hóa đơn #${invoiceId}?`)) {
      return;
    }

    try {
      setPayingInvoiceId(invoiceId);
      console.log("💳 Bắt đầu thanh toán hóa đơn #", invoiceId);
      
      // Bước 1: Lấy chi tiết hóa đơn
      console.log("📡 Lấy chi tiết hóa đơn...");
      const detailResponse = await getInvoiceDetailApi(invoiceId);
      console.log("✅ Chi tiết hóa đơn:", detailResponse.data);
      
      // Bước 2: Thanh toán hóa đơn
      console.log("📡 Gọi API thanh toán...");
      const payResponse = await payInvoiceApi(invoiceId);
      console.log("✅ Kết quả thanh toán:", payResponse.data);
      
      toast.success(`Thanh toán hóa đơn #${invoiceId} thành công!`);
      
      // Bước 3: Cập nhật lại danh sách hóa đơn
      console.log("🔄 Cập nhật lại danh sách hóa đơn...");
      await fetchInvoices();
      
    } catch (error) {
      console.error("❌ Lỗi khi thanh toán hóa đơn:", error);
      console.error("❌ Error response:", error.response?.data);
      toast.error(
        "Thanh toán thất bại: " + 
        (error.response?.data?.message || error.message)
      );
    } finally {
      setPayingInvoiceId(null);
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

          {/* ✅ Phân loại theo phương thức thanh toán */}
          <li className="stat-card" style={{ backgroundColor: "#e8f5e9" }}>
            💵 Tiền mặt (Cash)
            <strong>{formatCurrency(paymentStats.cashRevenue)}</strong>
            <small style={{ fontSize: "0.85em", color: "#666" }}>
              {paymentStats.cashCount} giao dịch
            </small>
          </li>
          <li className="stat-card" style={{ backgroundColor: "#e3f2fd" }}>
            💳 VNPay
            <strong>{formatCurrency(paymentStats.vnpayRevenue)}</strong>
            <small style={{ fontSize: "0.85em", color: "#666" }}>
              {paymentStats.vnpayCount} giao dịch
            </small>
          </li>
        </ul>
      )}

      {/* Table Section */}
      <div className="table-section">
        <div className="table-scroll-container">
          {/* Filter Section */}
          <div className="filter-section">
            <div style={{ display: "flex", gap: "10px", alignItems: "center" }}>
              <Nav
                justify
                variant="tabs"
                activeKey={filter || "all"}
                onSelect={(k) => handleFilterChange(k === "all" ? null : k)}
                style={{ flex: 1 }}
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
              
              <button
                onClick={fetchInvoices}
                className="btn"
                style={{
                  backgroundColor: "#fff3e0",
                  color: "#e65100",
                  border: "1px solid #e65100",
                  padding: "8px 20px",
                  borderRadius: "5px",
                  fontWeight: "500",
                  cursor: "pointer",
                  whiteSpace: "nowrap",
                  transition: "all 0.3s"
                }}
                onMouseEnter={(e) => {
                  e.target.style.backgroundColor = "#e65100";
                  e.target.style.color = "#fff";
                }}
                onMouseLeave={(e) => {
                  e.target.style.backgroundColor = "#fff3e0";
                  e.target.style.color = "#e65100";
                }}
              >
                📄 Chưa thanh toán
              </button>
            </div>

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

      {/* Modal hiển thị hóa đơn */}
      <Modal 
        show={showInvoiceModal} 
        onHide={() => setShowInvoiceModal(false)}
        size="xl"
        centered
      >
        <Modal.Header closeButton>
          <Modal.Title>📄 Danh sách Hóa đơn Station chưa thanh toán</Modal.Title>
        </Modal.Header>
        <Modal.Body style={{ maxHeight: "70vh", overflowY: "auto" }}>
          {loadingInvoices ? (
            <div style={{ textAlign: "center", padding: "30px" }}>
              Đang tải hóa đơn...
            </div>
          ) : (
            <Table striped bordered hover>
              <thead>
                <tr>
                  <th>Mã HĐ</th>
                  <th>Biển số xe</th>
                  <th>Trạm</th>
                  <th>Điểm sạc</th>
                  <th>Năng lượng (kWh)</th>
                  <th>Thời gian (phút)</th>
                  <th>Số tiền</th>
                  <th>Trạng thái</th>
                  <th>Ngày phát hành</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {invoices.length > 0 ? (
                  invoices.map((inv) => (
                    <tr key={inv.invoiceId}>
                      <td>#{inv.invoiceId}</td>
                      <td>{inv.vehiclePlate || "N/A"}</td>
                      <td>{inv.stationName || "N/A"}</td>
                      <td>{inv.pointNumber || "N/A"}</td>
                      <td>{inv.energyKWh?.toFixed(2) || "0.00"}</td>
                      <td>{inv.durationMinutes || 0}</td>
                      <td>{formatCurrency(inv.amount)}</td>
                      <td>
                        <span style={getInvoiceStatusStyle(inv.status)}>
                          {inv.status === "PAID" ? "✅ " : "❌ "}
                          {getInvoiceStatusText(inv.status)}
                        </span>
                      </td>
                      <td>{formatDateTime(inv.issuedAt)}</td>
                      <td>
                        {inv.status === "UNPAID" ? (
                          <button
                            onClick={() => handlePayInvoice(inv.invoiceId)}
                            disabled={payingInvoiceId === inv.invoiceId}
                            style={{
                              backgroundColor: payingInvoiceId === inv.invoiceId ? "#ccc" : "#28a745",
                              color: "#fff",
                              border: "none",
                              padding: "6px 12px",
                              borderRadius: "4px",
                              cursor: payingInvoiceId === inv.invoiceId ? "not-allowed" : "pointer",
                              fontWeight: "500",
                              fontSize: "0.9em",
                              transition: "all 0.3s"
                            }}
                            onMouseEnter={(e) => {
                              if (payingInvoiceId !== inv.invoiceId) {
                                e.target.style.backgroundColor = "#218838";
                              }
                            }}
                            onMouseLeave={(e) => {
                              if (payingInvoiceId !== inv.invoiceId) {
                                e.target.style.backgroundColor = "#28a745";
                              }
                            }}
                          >
                            {payingInvoiceId === inv.invoiceId ? "Đang xử lý..." : "💳 Thanh toán"}
                          </button>
                        ) : (
                          <span style={{ color: "#28a745", fontWeight: "500" }}>✓ Đã thanh toán</span>
                        )}
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="10" style={{ textAlign: "center", padding: "30px" }}>
                      Không có hóa đơn nào
                    </td>
                  </tr>
                )}
              </tbody>
            </Table>
          )}
          {invoices.length > 0 && (
            <div style={{ marginTop: "15px", padding: "10px", backgroundColor: "#fff3e0", borderRadius: "5px", border: "1px solid #e65100" }}>
              <strong style={{ color: "#e65100" }}>📊 Thống kê hóa đơn chưa thanh toán:</strong>
              <ul style={{ marginTop: "10px", marginBottom: "0" }}>
                <li>Tổng số hóa đơn: <strong>{invoices.length}</strong></li>
                <li>Tổng số tiền cần thanh toán: <strong style={{ color: "#d32f2f" }}>{formatCurrency(invoices.reduce((sum, i) => sum + (i.amount || 0), 0))}</strong></li>
              </ul>
            </div>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowInvoiceModal(false)}>
            Đóng
          </Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
}
