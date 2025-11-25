import { useState, useEffect } from "react";
import { toast } from "react-toastify";
import {
  getChargingPointById,
  updateChargingPoint,
} from "../../api/chargingPointApi.js";
import { getAllStations, getConnectorTypes } from "../../api/stationApi.js";
import "./AddStaffForm.css";

// Thêm CSS animation cho spinner
const spinnerStyle = document.createElement("style");
spinnerStyle.textContent = `
  @keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
  }
  
  @keyframes pulse {
    0%, 100% { transform: scale(1); opacity: 1; }
    50% { transform: scale(1.05); opacity: 0.8; }
  }
`;
if (!document.head.querySelector("style[data-update-form-animation]")) {
  spinnerStyle.setAttribute("data-update-form-animation", "true");
  document.head.appendChild(spinnerStyle);
}

export default function UpdateChargingPointForm({ pointId, onClose }) {
  const [formData, setFormData] = useState({
    stationId: "",
    connectorTypeId: "",
    pointNumber: "",
    serialNumber: "",
    installationDate: "",
    lastMaintenanceDate: "",
    maxPowerKW: "",
    status: "AVAILABLE",
  });

  const [stations, setStations] = useState([]);
  const [connectorTypes, setConnectorTypes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  // ✅ Load dữ liệu ban đầu
  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);

        // ✅ Load stations và connector types TRƯỚC
        const [stationsRes, connectorTypesRes] = await Promise.all([
          getAllStations(),
          getConnectorTypes(),
        ]);

        if (stationsRes.success) {
          setStations(stationsRes.data);
        }

        if (connectorTypesRes.success) {
          setConnectorTypes(connectorTypesRes.data);
          console.log("✅ Connector types loaded:", connectorTypesRes.data);
        }

        // ✅ SAU ĐÓ mới load charging point detail
        const pointResponse = await getChargingPointById(pointId);
        if (pointResponse.success) {
          const point = pointResponse.data;
          console.log("✅ Loaded charging point:", point);

          // ✅ Map connectorType name sang connectorTypeId
          const matchedConnectorType = connectorTypesRes.data?.find(
            (ct) => ct.connectorTypeName === point.connectorType
          );

          console.log("🔍 Matching connector:", {
            pointConnectorType: point.connectorType,
            matchedConnectorType,
            allTypes: connectorTypesRes.data
          });

          // ✅ Format dates for input type="datetime-local"
          const formatDateForInput = (dateString) => {
            if (!dateString) return "";
            const date = new Date(dateString);
            const year = date.getFullYear();
            const month = String(date.getMonth() + 1).padStart(2, "0");
            const day = String(date.getDate()).padStart(2, "0");
            const hours = String(date.getHours()).padStart(2, "0");
            const minutes = String(date.getMinutes()).padStart(2, "0");
            return `${year}-${month}-${day}T${hours}:${minutes}`;
          };

          setFormData({
            stationId: point.stationId || "",
            connectorTypeId: matchedConnectorType?.connectorTypeId || "",
            pointNumber: point.pointNumber || "",
            serialNumber: point.serialNumber || "",
            installationDate: formatDateForInput(point.installationDate),
            lastMaintenanceDate: formatDateForInput(point.lastMaintenanceDate),
            maxPowerKW: point.maxPowerKW || "",
            status: point.status || "AVAILABLE",
          });
        } else {
          toast.error("Không thể tải thông tin trụ sạc");
          onClose();
        }
      } catch (error) {
        console.error("Error loading data:", error);
        toast.error("Lỗi khi tải dữ liệu");
        onClose();
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [pointId, onClose]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    // ✅ Validation
    if (!formData.stationId) {
      toast.error("Vui lòng chọn trạm sạc");
      return;
    }
    if (!formData.connectorTypeId) {
      toast.error("Vui lòng chọn loại đầu nối");
      return;
    }
    if (!formData.pointNumber?.trim()) {
      toast.error("Vui lòng nhập mã trụ sạc");
      return;
    }
    if (!formData.serialNumber?.trim()) {
      toast.error("Vui lòng nhập số serial");
      return;
    }
    if (!formData.maxPowerKW || formData.maxPowerKW <= 0) {
      toast.error("Công suất tối đa phải lớn hơn 0");
      return;
    }
    if (!formData.installationDate) {
      toast.error("Vui lòng chọn ngày lắp đặt");
      return;
    }
    if (!formData.lastMaintenanceDate) {
      toast.error("Vui lòng chọn ngày bảo trì gần nhất");
      return;
    }

    try {
      setSubmitting(true);

      // ✅ Chuẩn bị payload theo đúng format Backend yêu cầu (CreateChargingPointRequest)
      const payload = {
        stationId: Number(formData.stationId),
        connectorTypeId: Number(formData.connectorTypeId),
        pointNumber: formData.pointNumber.trim(),
        serialNumber: formData.serialNumber.trim(),
        installationDate: formData.installationDate, // Backend nhận LocalDateTime
        lastMaintenanceDate: formData.lastMaintenanceDate,
        maxPowerKW: Number(formData.maxPowerKW),
        status: formData.status,
      };

      console.log("Update payload:", payload);

      const response = await updateChargingPoint(pointId, payload);

      if (response.success) {
        toast.success("Cập nhật trụ sạc thành công!");
        onClose(); // Đóng form và refresh danh sách
      } else {
        // Backend trả về lỗi validation
        const errorMsg =
          response.message || "Cập nhật trụ sạc thất bại. Vui lòng thử lại.";
        toast.error(errorMsg);
        console.error("Backend error:", response);
      }
    } catch (error) {
      console.error("Error updating charging point:", error);
      const errorMsg =
        error.response?.data?.message ||
        error.message ||
        "Đã xảy ra lỗi khi cập nhật trụ sạc";
      toast.error(errorMsg);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="form-overlay">
        <div className="form-container" style={{ textAlign: "center", padding: "40px" }}>
          <div style={{ fontSize: "48px", marginBottom: "20px" }}>⚡</div>
          <h3 style={{ color: "#666", marginBottom: "10px" }}>Đang tải thông tin trụ sạc...</h3>
          <div className="spinner" style={{
            border: "4px solid #f3f3f3",
            borderTop: "4px solid #3498db",
            borderRadius: "50%",
            width: "40px",
            height: "40px",
            animation: "spin 1s linear infinite",
            margin: "20px auto"
          }}></div>
        </div>
      </div>
    );
  }

  return (
    <div className="form-overlay">
      <div className="form-container" style={{ 
        maxWidth: "700px",
        maxHeight: "90vh",
        overflowY: "auto"
      }}>
        <div className="form-header" style={{
          background: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)",
          padding: "20px 25px",
          borderRadius: "8px 8px 0 0",
          position: "sticky",
          top: 0,
          zIndex: 10
        }}>
          <h3 style={{ 
            margin: 0, 
            color: "white",
            fontSize: "20px",
            fontWeight: "600",
            display: "flex",
            alignItems: "center",
            gap: "10px"
          }}>
            <span style={{ fontSize: "24px" }}>⚡</span>
            Cập nhật thông tin trụ sạc
          </h3>
          <button 
            className="btn-close-form" 
            onClick={onClose}
            style={{
              background: "rgba(255,255,255,0.2)",
              color: "white",
              border: "none",
              width: "32px",
              height: "32px",
              borderRadius: "50%",
              fontSize: "20px",
              cursor: "pointer",
              transition: "all 0.3s"
            }}
            onMouseOver={(e) => e.target.style.background = "rgba(255,255,255,0.3)"}
            onMouseOut={(e) => e.target.style.background = "rgba(255,255,255,0.2)"}
          >
            ×
          </button>
        </div>

        <form onSubmit={handleSubmit} className="form-body" style={{ padding: "25px" }}>
          {/* Grid layout 2 cột cho các fields */}
          <div style={{ 
            display: "grid", 
            gridTemplateColumns: "1fr 1fr", 
            gap: "20px",
            marginBottom: "20px"
          }}>
            {/* Trạm sạc */}
            <div className="form-group">
              <label style={{ 
                fontWeight: "600", 
                color: "#333",
                marginBottom: "8px",
                display: "block"
              }}>
                <span style={{ color: "#667eea", marginRight: "5px" }}>🏢</span>
                Trạm sạc <span style={{ color: "#e74c3c" }}>*</span>
              </label>
              <select
                name="stationId"
                value={formData.stationId}
                onChange={handleChange}
                required
                disabled={submitting}
                style={{
                  width: "100%",
                  padding: "12px",
                  border: "2px solid #e0e0e0",
                  borderRadius: "8px",
                  fontSize: "14px",
                  transition: "border 0.3s"
                }}
                onFocus={(e) => e.target.style.borderColor = "#667eea"}
                onBlur={(e) => e.target.style.borderColor = "#e0e0e0"}
              >
                <option value="">-- Chọn trạm sạc --</option>
                {stations.map((station) => (
                  <option key={station.stationId} value={station.stationId}>
                    {station.stationName}
                  </option>
                ))}
              </select>
            </div>

            {/* Loại đầu nối */}
            <div className="form-group">
              <label style={{ 
                fontWeight: "600", 
                color: "#333",
                marginBottom: "8px",
                display: "block"
              }}>
                <span style={{ color: "#667eea", marginRight: "5px" }}>🔌</span>
                Loại đầu nối <span style={{ color: "#e74c3c" }}>*</span>
              </label>
              <select
                name="connectorTypeId"
                value={formData.connectorTypeId}
                onChange={handleChange}
                required
                disabled={submitting}
                style={{
                  width: "100%",
                  padding: "12px",
                  border: "2px solid #e0e0e0",
                  borderRadius: "8px",
                  fontSize: "14px",
                  transition: "border 0.3s"
                }}
                onFocus={(e) => e.target.style.borderColor = "#667eea"}
                onBlur={(e) => e.target.style.borderColor = "#e0e0e0"}
              >
                <option value="">-- Chọn loại đầu nối --</option>
                {connectorTypes.map((type) => (
                  <option
                    key={type.connectorTypeId}
                    value={type.connectorTypeId}
                  >
                    {type.connectorTypeName}
                  </option>
                ))}
              </select>
            </div>

            {/* Mã trụ sạc */}
            <div className="form-group">
              <label style={{ 
                fontWeight: "600", 
                color: "#333",
                marginBottom: "8px",
                display: "block"
              }}>
                <span style={{ color: "#667eea", marginRight: "5px" }}>📌</span>
                Mã trụ sạc <span style={{ color: "#e74c3c" }}>*</span>
              </label>
              <input
                type="text"
                name="pointNumber"
                value={formData.pointNumber}
                onChange={handleChange}
                placeholder="VD: CP-001"
                required
                disabled={submitting}
                style={{
                  width: "100%",
                  padding: "12px",
                  border: "2px solid #e0e0e0",
                  borderRadius: "8px",
                  fontSize: "14px",
                  transition: "border 0.3s"
                }}
                onFocus={(e) => e.target.style.borderColor = "#667eea"}
                onBlur={(e) => e.target.style.borderColor = "#e0e0e0"}
              />
            </div>

            {/* Số serial */}
            <div className="form-group">
              <label style={{ 
                fontWeight: "600", 
                color: "#333",
                marginBottom: "8px",
                display: "block"
              }}>
                <span style={{ color: "#667eea", marginRight: "5px" }}>🔢</span>
                Số serial <span style={{ color: "#e74c3c" }}>*</span>
              </label>
              <input
                type="text"
                name="serialNumber"
                value={formData.serialNumber}
                onChange={handleChange}
                placeholder="VD: SN-123456789"
                required
                disabled={submitting}
                style={{
                  width: "100%",
                  padding: "12px",
                  border: "2px solid #e0e0e0",
                  borderRadius: "8px",
                  fontSize: "14px",
                  transition: "border 0.3s"
                }}
                onFocus={(e) => e.target.style.borderColor = "#667eea"}
                onBlur={(e) => e.target.style.borderColor = "#e0e0e0"}
              />
            </div>

            {/* Công suất tối đa */}
            <div className="form-group">
              <label style={{ 
                fontWeight: "600", 
                color: "#333",
                marginBottom: "8px",
                display: "block"
              }}>
                <span style={{ color: "#667eea", marginRight: "5px" }}>⚡</span>
                Công suất tối đa (kW) <span style={{ color: "#e74c3c" }}>*</span>
              </label>
              <input
                type="number"
                name="maxPowerKW"
                value={formData.maxPowerKW}
                onChange={handleChange}
                placeholder="VD: 11"
                step="0.1"
                min="0"
                required
                disabled={submitting}
                style={{
                  width: "100%",
                  padding: "12px",
                  border: "2px solid #e0e0e0",
                  borderRadius: "8px",
                  fontSize: "14px",
                  transition: "border 0.3s"
                }}
                onFocus={(e) => e.target.style.borderColor = "#667eea"}
                onBlur={(e) => e.target.style.borderColor = "#e0e0e0"}
              />
            </div>

            {/* Trạng thái */}
            <div className="form-group">
              <label style={{ 
                fontWeight: "600", 
                color: "#333",
                marginBottom: "8px",
                display: "block"
              }}>
                <span style={{ color: "#667eea", marginRight: "5px" }}>📊</span>
                Trạng thái <span style={{ color: "#e74c3c" }}>*</span>
              </label>
              <select
                name="status"
                value={formData.status}
                onChange={handleChange}
                required
                disabled={submitting}
                style={{
                  width: "100%",
                  padding: "12px",
                  border: "2px solid #e0e0e0",
                  borderRadius: "8px",
                  fontSize: "14px",
                  transition: "border 0.3s"
                }}
                onFocus={(e) => e.target.style.borderColor = "#667eea"}
                onBlur={(e) => e.target.style.borderColor = "#e0e0e0"}
              >
                <option value="AVAILABLE">✅ Sẵn sàng</option>
                <option value="OCCUPIED">🔋 Đang sử dụng</option>
                <option value="MAINTENANCE">🔧 Bảo trì</option>
                <option value="OUT_OF_SERVICE">❌ Ngưng hoạt động</option>
              </select>
            </div>

            {/* Ngày lắp đặt */}
            <div className="form-group">
              <label style={{ 
                fontWeight: "600", 
                color: "#333",
                marginBottom: "8px",
                display: "block"
              }}>
                <span style={{ color: "#667eea", marginRight: "5px" }}>📅</span>
                Ngày lắp đặt <span style={{ color: "#e74c3c" }}>*</span>
              </label>
              <input
                type="datetime-local"
                name="installationDate"
                value={formData.installationDate}
                onChange={handleChange}
                required
                disabled={submitting}
                style={{
                  width: "100%",
                  padding: "12px",
                  border: "2px solid #e0e0e0",
                  borderRadius: "8px",
                  fontSize: "14px",
                  transition: "border 0.3s"
                }}
                onFocus={(e) => e.target.style.borderColor = "#667eea"}
                onBlur={(e) => e.target.style.borderColor = "#e0e0e0"}
              />
            </div>

            {/* Ngày bảo trì gần nhất */}
            <div className="form-group">
              <label style={{ 
                fontWeight: "600", 
                color: "#333",
                marginBottom: "8px",
                display: "block"
              }}>
                <span style={{ color: "#667eea", marginRight: "5px" }}>🔧</span>
                Ngày bảo trì gần nhất <span style={{ color: "#e74c3c" }}>*</span>
              </label>
              <input
                type="datetime-local"
                name="lastMaintenanceDate"
                value={formData.lastMaintenanceDate}
                onChange={handleChange}
                required
                disabled={submitting}
                style={{
                  width: "100%",
                  padding: "12px",
                  border: "2px solid #e0e0e0",
                  borderRadius: "8px",
                  fontSize: "14px",
                  transition: "border 0.3s"
                }}
                onFocus={(e) => e.target.style.borderColor = "#667eea"}
                onBlur={(e) => e.target.style.borderColor = "#e0e0e0"}
              />
            </div>
          </div>

          {/* Action buttons */}
          <div style={{
            display: "flex",
            gap: "15px",
            justifyContent: "flex-end",
            marginTop: "30px",
            paddingTop: "20px",
            borderTop: "2px solid #f0f0f0"
          }}>
            <button
              type="button"
              onClick={onClose}
              disabled={submitting}
              style={{
                padding: "12px 30px",
                border: "2px solid #e0e0e0",
                borderRadius: "8px",
                background: "white",
                color: "#666",
                fontSize: "15px",
                fontWeight: "600",
                cursor: submitting ? "not-allowed" : "pointer",
                transition: "all 0.3s ease",
                opacity: submitting ? 0.6 : 1
              }}
              onMouseOver={(e) => {
                if (!submitting) {
                  e.target.style.background = "#f5f5f5";
                  e.target.style.borderColor = "#999";
                }
              }}
              onMouseOut={(e) => {
                e.target.style.background = "white";
                e.target.style.borderColor = "#e0e0e0";
              }}
            >
              ❌ Hủy
            </button>
            <button
              type="submit"
              disabled={submitting}
              style={{
                padding: "12px 35px",
                border: "none",
                borderRadius: "8px",
                background: submitting 
                  ? "linear-gradient(135deg, #999 0%, #666 100%)" 
                  : "linear-gradient(135deg, #667eea 0%, #764ba2 100%)",
                color: "white",
                fontSize: "15px",
                fontWeight: "600",
                cursor: submitting ? "not-allowed" : "pointer",
                transition: "all 0.3s ease",
                boxShadow: submitting ? "none" : "0 4px 15px rgba(102, 126, 234, 0.4)",
                transform: submitting ? "none" : "translateY(0)"
              }}
              onMouseOver={(e) => {
                if (!submitting) {
                  e.target.style.transform = "translateY(-2px)";
                  e.target.style.boxShadow = "0 6px 20px rgba(102, 126, 234, 0.5)";
                }
              }}
              onMouseOut={(e) => {
                e.target.style.transform = "translateY(0)";
                e.target.style.boxShadow = "0 4px 15px rgba(102, 126, 234, 0.4)";
              }}
            >
              {submitting ? "⏳ Đang lưu..." : "💾 Lưu thay đổi"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
