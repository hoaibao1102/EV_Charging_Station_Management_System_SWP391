import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import "./StationDetail.css";
import { stationAPI } from "../../api/stationApi.js";
import { getMyVehiclesApi } from "../../api/driverApi.js";

const StationDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [station, setStation] = useState(null);
  const [chargingPoints, setChargingPoints] = useState([]);
  const [myVehicles, setMyVehicles] = useState([]);
  const [selectedVehicle, setSelectedVehicle] = useState(null);
  const [connectorTypes, setConnectorTypes] = useState([]);
  const [expandedPoint, setExpandedPoint] = useState(null);
  const [loading, setLoading] = useState(true);

  // ====== Fetch dữ liệu ======
  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);

        // const stationRes = await stationAPI.getStationById(id);
        const stationRes = await stationAPI.getStationById(id);

        // ✅ Fix: nếu trả về mảng, lấy phần tử đầu tiên
        const st = Array.isArray(stationRes.data)
          ? stationRes.data[0]
          : stationRes.data;
        setStation(st);

        const [pointsRes, connectorsRes] = await Promise.all([
          stationAPI.getChargingPointsByStationId(id),
          stationAPI.getConnectorTypes(),
        ]);

        // console.log("🔋 Charging Points:", pointsRes.data);
        // console.log("🔌 Connector Types:", connectorsRes.data);

        setChargingPoints(pointsRes.data);

        // Normalize connector types to a consistent shape so lookups are reliable
        const normalizedConnectors = (connectorsRes.data || []).map((c) => ({
          // prefer explicit connectorTypeId fields, fallback to id or code when necessary
          connectorTypeId:
            c.connectorTypeId ?? c.id ?? c.ConnectorTypeID ?? c.code ?? null,
          code:
            c.code ??
            c.Code ??
            (c.connectorTypeId ? String(c.connectorTypeId) : null),
          displayName:
            c.displayName ?? c.DisplayName ?? c.name ?? c.TypeName ?? "",
          mode: c.mode ?? c.Mode ?? "",
          defaultMaxPowerKW:
            c.defaultMaxPowerKW ??
            c.defaultPower ??
            c.default_max_power ??
            null,
          raw: c,
        }));

        console.log(
          "🔌 Normalized connector types:",
          normalizedConnectors.slice(0, 10)
        );
        setConnectorTypes(normalizedConnectors);

        // Lấy danh sách xe của tài xế
        const myVehiclesRes = await getMyVehiclesApi();
        // console.log("🚗 My Vehicles:", myVehiclesRes.data);

        setMyVehicles(myVehiclesRes.data);

        // Tự động chọn xe đầu tiên nếu có
        if (myVehiclesRes.data?.length > 0) {
          // console.log("✅ Auto-selecting first vehicle:", myVehiclesRes.data[0]);
          setSelectedVehicle(myVehiclesRes.data[0]);
        }
      } catch (error) {
        console.error("❌ Lỗi khi tải dữ liệu:", error);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [id]);

  // ====== Tìm thông tin connector theo connectorTypeId ======
  const getConnectorDetail = (connectorTypeId) => {
    // Flexible lookup since API field names / formats can vary (id vs code vs displayName)
    if (!connectorTypes || connectorTypes.length === 0) return null;

    const searchRaw = connectorTypeId;
    const search = String(connectorTypeId ?? "")
      .toLowerCase()
      .trim();

    // 1) Try strict id match (connectorTypeId or ConnectorTypeID)
    let result = connectorTypes.find(
      (c) =>
        String(c.connectorTypeId) === String(searchRaw) ||
        String(c.ConnectorTypeID) === String(searchRaw) ||
        String(c.id) === String(searchRaw)
    );
    if (result) return result;

    // 2) Try matching by code / displayName (case-insensitive, includes)
    result = connectorTypes.find((c) => {
      const code = String(c.code ?? c.Code ?? "")
        .toLowerCase()
        .trim();
      const display = String(c.displayName ?? c.DisplayName ?? "")
        .toLowerCase()
        .trim();
      return (
        (code && code === search) ||
        (display && display === search) ||
        (display && display.includes(search)) ||
        (code && code.includes(search))
      );
    });
    if (result) {
      console.log("ℹ️ Matched connector by code/displayName fallback:", {
        search: connectorTypeId,
        matched: result,
      });
      return result;
    }

    // 3) As a last resort try partial numeric/string coercion matches (helpful when API mixes types)
    result = connectorTypes.find((c) => {
      try {
        return (
          String(c.connectorTypeId) === search ||
          String(c.ConnectorTypeID) === search
        );
      } catch {
        return false;
      }
    });
    if (result) return result;

    // Not found — provide helpful debug info in console
    console.log(`⚠️ Connector not found for ID/Code: ${connectorTypeId}`, {
      searchRaw: connectorTypeId,
      available: connectorTypes.map((c) => ({
        id: c.connectorTypeId ?? c.id ?? c.ConnectorTypeID,
        code: c.code,
        displayName: c.displayName,
      })),
    });

    return null;
  };

  // ====== Kiểm tra trụ có tương thích với xe được chọn không ======
  const isPointCompatible = (point) => {
    // Nếu không có xe được chọn → return false (không tương thích)
    if (!selectedVehicle) {
      return false;
    }

    // Lấy thông tin connector của trụ này
    // Points may return a connectorType string (e.g. "CCS Combo 2") or an id field.
    const connector = getConnectorDetail(
      point.connectorType || point.connectorTypeId || point.ConnectorTypeID
    );
    if (!connector) {
      console.log(
        `❌ Không tìm thấy connector cho trụ ${
          point.pointNumber || point.PointNumber
        }`,
        {
          pointConnectorTypeID: point.connectorTypeId || point.ConnectorTypeID,
          availableConnectorTypes: connectorTypes.map((c) => ({
            id: c.connectorTypeId,
            name: c.displayName,
            code: c.code,
          })),
        }
      );
      return false;
    }

    // ✅ Sử dụng connectorTypeName thay vì connectorType
    const vehicleConnectorName =
      selectedVehicle.connectorTypeName || selectedVehicle.connectorType;

    if (!vehicleConnectorName) {
      console.log("❌ Xe không có connectorTypeName:", selectedVehicle);
      return false;
    }

    // So khớp connectorTypeName của xe với connector.displayName
    const vehicleType = vehicleConnectorName.toLowerCase().trim();
    const connectorName = (connector.displayName || "").toLowerCase().trim();
    const connectorCode = (connector.code || "").toLowerCase().trim();

    const isCompatible =
      connectorName.includes(vehicleType) ||
      connectorCode.includes(vehicleType) ||
      connectorName === vehicleType ||
      connectorCode === vehicleType ||
      vehicleType.includes(connectorName) ||
      vehicleType.includes(connectorCode);

    console.log(
      `🔍 Checking compatibility for ${
        point.pointNumber || point.PointNumber
      }:`,
      {
        vehicleConnectorTypeName: vehicleConnectorName,
        connectorDisplayName: connector.displayName,
        connectorCode: connector.code,
        isCompatible,
      }
    );

    return isCompatible;
  };

  // ====== Phân nhóm trụ phù hợp / không phù hợp ======
  const groupChargingPoints = () => {
    console.log("📊 Grouping charging points...", {
      selectedVehicle,
      totalPoints: chargingPoints.length,
      totalConnectorTypes: connectorTypes.length,
    });

    if (!selectedVehicle) {
      console.log("⚠️ No vehicle selected - showing all points");
      return { compatible: chargingPoints, others: [] };
    }

    const compatible = chargingPoints.filter((p) => isPointCompatible(p));
    const others = chargingPoints.filter((p) => !isPointCompatible(p));

    console.log("✅ Grouping result:", {
      compatible: compatible.length,
      others: others.length,
      compatiblePoints: compatible.map((p) => p.PointNumber),
      otherPoints: others.map((p) => p.PointNumber),
    });

    return { compatible, others };
  };

  const { compatible, others } = groupChargingPoints();

  // ====== Toggle mở rộng trụ ======
  const toggleExpand = (pointId) =>
    setExpandedPoint(expandedPoint === pointId ? null : pointId);

  const handleBooking = (pointId, connectorId) => {
    // Kiểm tra xem đã chọn xe chưa
    if (!selectedVehicle) {
      alert("⚠️ Vui lòng chọn xe trước khi đặt chỗ!");
      return;
    }

    // Lấy thông tin chi tiết của point và connector
    const point = chargingPoints.find(
      (p) => (p.pointId || p.PointID) === pointId
    );
    const connector = getConnectorDetail(connectorId);

    console.log(`📅 Booking Point: ${pointId}, Connector: ${connectorId}`);

    // Gửi thông tin sang trang Booking
    navigate(`/bookings`, {
      state: {
        station: {
          id: station?.StationID || station?.stationID,
          name: station?.StationName || station?.stationName,
          address: station?.Address || station?.address,
        },
        chargingPoint: {
          pointId: point?.pointId || point?.PointID,
          pointNumber: point?.pointNumber || point?.PointNumber,
          maxPowerKW: point?.maxPowerKW || point?.MaxPowerKW,
          status: point?.status || point?.Status,
        },
        connector: {
          connectorTypeId: connector?.connectorTypeId,
          displayName: connector?.displayName,
          code: connector?.code,
          mode: connector?.mode,
          defaultMaxPowerKW: connector?.defaultMaxPowerKW,
        },
        vehicle: {
          vehicleId: selectedVehicle?.vehicleId,
          vehicleName: selectedVehicle?.vehicleName,
          brand: selectedVehicle?.brand,
          model: selectedVehicle?.model,
          connectorTypeName: selectedVehicle?.connectorTypeName,
          licensePlate: selectedVehicle?.licensePlate,
        },
      },
    });
  };

  const handleVehicleChange = (e) => {
    const vehicleId = parseInt(e.target.value);
    console.log("🚗 Vehicle changed:", { vehicleId });

    if (!vehicleId) {
      console.log("⚠️ No vehicle selected (showing all)");
      setSelectedVehicle(null);
      return;
    }

    const vehicle = myVehicles.find((v) => v.vehicleId === vehicleId);
    console.log("✅ Selected vehicle:", vehicle);
    setSelectedVehicle(vehicle || null);
  };

  // ====== Loading / Error ======
  if (loading)
    return (
      <div className="station-container">
        <div className="loading">Đang tải dữ liệu...</div>
      </div>
    );

  if (!station)
    return (
      <div className="station-container">
        <h3>Không tìm thấy trạm sạc</h3>
        <button onClick={() => navigate(-1)}>← Quay lại</button>
      </div>
    );

  // ====== Giao diện chính ======
  return (
    <div className="station-container">
      <button className="btn-back" onClick={() => navigate(-1)}>
        ← Quay lại danh sách
      </button>

      <h1 className="station-title">
        {station?.StationName || station?.stationName || "Trạm sạc"}
      </h1>
      <p className="station-address">
        📍 {station?.Address || station?.address || "Đang cập nhật địa chỉ"}
      </p>

      {/* ====== Dropdown chọn xe hoặc thông báo thêm xe ====== */}
      {myVehicles?.length > 0 ? (
        <div className="vehicle-selector">
          <label htmlFor="vehicle-select" className="selector-label">
            🚗 Chọn xe bạn muốn sạc:
          </label>
          <select
            id="vehicle-select"
            value={selectedVehicle?.vehicleId || ""}
            onChange={handleVehicleChange}
            className="vehicle-dropdown"
          >
            <option value="">-- Hiển thị tất cả trụ --</option>
            {myVehicles.map((vehicle) => (
              <option key={vehicle.vehicleId} value={vehicle.vehicleId}>
                {vehicle.vehicleName} ({vehicle.brand} {vehicle.model}) - 🔌{" "}
                {vehicle.connectorTypeName || vehicle.connectorType || "N/A"}
              </option>
            ))}
          </select>
          {!selectedVehicle && (
            <p
              style={{
                color: "#ff9800",
                marginTop: "10px",
                fontSize: "14px",
                fontWeight: "500",
              }}
            >
              ⚠️ Vui lòng chọn xe để xem trụ sạc tương thích và đặt chỗ
            </p>
          )}
        </div>
      ) : (
        <div
          className="vehicle-selector"
          style={{
            background: "linear-gradient(135deg, #fff3e0 0%, #ffe0b2 100%)",
            border: "2px solid #ff9800",
            padding: "20px",
            borderRadius: "12px",
          }}
        >
          <p
            style={{
              color: "#e65100",
              fontSize: "16px",
              fontWeight: "600",
              marginBottom: "15px",
              display: "flex",
              alignItems: "center",
              gap: "8px",
            }}
          >
            🚗 Bạn chưa có xe nào trong danh sách
          </p>
          <p
            style={{
              color: "#f57c00",
              fontSize: "14px",
              marginBottom: "15px",
            }}
          >
            Vui lòng thêm xe của bạn để đặt chỗ sạc
          </p>
          <button
            onClick={() => navigate("/profile/my-vehicle")}
            style={{
              background: "linear-gradient(135deg, #ff9800 0%, #f57c00 100%)",
              color: "white",
              border: "none",
              padding: "12px 24px",
              borderRadius: "8px",
              fontSize: "15px",
              fontWeight: "600",
              cursor: "pointer",
              transition: "all 0.3s ease",
              boxShadow: "0 4px 12px rgba(255, 152, 0, 0.3)",
            }}
            onMouseOver={(e) => {
              e.currentTarget.style.transform = "translateY(-2px)";
              e.currentTarget.style.boxShadow =
                "0 6px 16px rgba(255, 152, 0, 0.4)";
            }}
            onMouseOut={(e) => {
              e.currentTarget.style.transform = "translateY(0)";
              e.currentTarget.style.boxShadow =
                "0 4px 12px rgba(255, 152, 0, 0.3)";
            }}
          >
            ➕ Thêm xe của tôi
          </button>
        </div>
      )}

      {/* ====== Danh sách trụ phù hợp ====== */}
      <div className="point-section">
        {selectedVehicle && compatible.length > 0 && (
          <h3 className="section-title">✅ Phù hợp với xe bạn</h3>
        )}
        {compatible.map((point) => {
          const pointId = point.pointId || point.PointID;
          const pointNumber = point.pointNumber || point.PointNumber;
          const status = point.status || point.Status;
          const maxPowerKW = point.maxPowerKW || point.MaxPowerKW;
          const lastMaintenanceDate =
            point.lastMaintenanceDate || point.LastMaintenanceDate;
          const connectorTypeId =
            point.connectorType ||
            point.connectorTypeId ||
            point.ConnectorTypeID;

          const expanded = expandedPoint === pointId;
          const connector = getConnectorDetail(connectorTypeId);

          return (
            <div
              key={pointId}
              className={`point-card ${expanded ? "expanded" : ""} compatible`}
              onClick={() => toggleExpand(pointId)}
            >
              <div className="point-header">
                <div className="point-info">
                  <h3>🔋 {pointNumber}</h3>
                  <p>{status === "available" ? "Sẵn sàng" : status}</p>
                </div>
                <div className={`status-dot ${status?.toLowerCase()}`}></div>
              </div>

              <div className="point-meta">
                <span>⚡ Công suất: {maxPowerKW} kW</span>
                <span>🔧 Bảo trì: {lastMaintenanceDate || "N/A"}</span>
              </div>

              {expanded && connector && (
                <div className="connector-panel">
                  <h4>🔌 Cổng sạc tương thích</h4>
                  <div className="connector-item">
                    <div className="connector-info">
                      <strong>{connector.displayName}</strong>
                      <p>Mã: {connector.code}</p>
                      <p>Chế độ: {connector.mode}</p>
                      <p>⚡ {connector.defaultMaxPowerKW} kW</p>
                    </div>
                    <div className="connector-actions">
                      <div
                        className={`mode-tag ${connector.mode?.toLowerCase()}`}
                      >
                        {connector.mode}
                      </div>
                      {status?.toLowerCase() === "available" &&
                        selectedVehicle && (
                          <button
                            className="btn-book-small"
                            onClick={(e) => {
                              e.stopPropagation();
                              handleBooking(pointId, connector.connectorTypeId);
                            }}
                          >
                            📅 Đặt chỗ
                          </button>
                        )}
                      {status?.toLowerCase() === "available" &&
                        !selectedVehicle && (
                          <p
                            style={{
                              color: "#ff9800",
                              fontSize: "13px",
                              fontWeight: "500",
                              margin: "5px 0",
                            }}
                          >
                            ⚠️ Chọn xe để đặt chỗ
                          </p>
                        )}
                    </div>
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* ====== Ngăn cách giữa nhóm ====== */}
      {selectedVehicle && others.length > 0 && (
        <div className="divider">
          <h3 className="section-title">⚠️ Không phù hợp với xe bạn</h3>
        </div>
      )}

      {/* ====== Danh sách trụ không phù hợp ====== */}
      <div className="point-section">
        {others.map((point) => {
          const pointId = point.pointId || point.PointID;
          const pointNumber = point.pointNumber || point.PointNumber;
          const status = point.status || point.Status;
          const maxPowerKW = point.maxPowerKW || point.MaxPowerKW;
          const lastMaintenanceDate =
            point.lastMaintenanceDate || point.LastMaintenanceDate;
          const connectorTypeId =
            point.connectorType ||
            point.connectorTypeId ||
            point.ConnectorTypeID;

          const expanded = expandedPoint === pointId;
          const connector = getConnectorDetail(connectorTypeId);

          return (
            <div
              key={pointId}
              className={`point-card ${
                expanded ? "expanded" : ""
              } not-compatible`}
              onClick={() => toggleExpand(pointId)}
            >
              <div className="point-header">
                <div className="point-info">
                  <h3>🔋 {pointNumber}</h3>
                  <p>{status === "available" ? "Sẵn sàng" : status}</p>
                </div>
                <div className={`status-dot ${status?.toLowerCase()}`}></div>
              </div>

              <div className="point-meta">
                <span>⚡ Công suất: {maxPowerKW} kW</span>
                <span>🔧 Bảo trì: {lastMaintenanceDate || "N/A"}</span>
              </div>

              {expanded && connector && (
                <div className="connector-panel">
                  <h4>🔌 Cổng sạc</h4>
                  <div className="connector-item">
                    <div className="connector-info">
                      <strong>{connector.displayName}</strong>
                      <p>Mã: {connector.code}</p>
                      <p>Chế độ: {connector.mode}</p>
                      <p>⚡ {connector.defaultMaxPowerKW} kW</p>
                    </div>
                    <div className="connector-actions">
                      <div
                        className={`mode-tag ${connector.mode?.toLowerCase()}`}
                      >
                        {connector.mode}
                      </div>
                    </div>
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default StationDetail;
