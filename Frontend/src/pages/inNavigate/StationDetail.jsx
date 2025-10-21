import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import axios from "axios";
import "./StationDetail.css";

const StationDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [station, setStation] = useState(null);
  const [chargingPoints, setChargingPoints] = useState([]);
  const [connectorTypes, setConnectorTypes] = useState([]);
  const [expandedPoint, setExpandedPoint] = useState(null);
  const [loading, setLoading] = useState(true);

  // ====== Fetch dữ liệu ======
  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);

        // 1️⃣ Lấy thông tin trạm
        const stationRes = await axios.get(
          `https://68f35999fd14a9fcc4288a78.mockapi.io/stationcharging?StationID=${id}`
        );

        // 2️⃣ Lấy danh sách trụ và loại connector
        const [pointsRes, connectorsRes] = await Promise.all([
          axios.get(
            "https://68f35999fd14a9fcc4288a78.mockapi.io/Charging_Points"
          ),
          axios.get(
            "https://68f6f46af7fb897c66141d83.mockapi.io/Connector_types"
          ),
        ]);

        const st = stationRes.data.length ? stationRes.data[0] : null;
        const pts = pointsRes.data.filter(
          (p) => String(p.StationID) === String(id)
        );

        setStation(st);
        setChargingPoints(pts);
        setConnectorTypes(connectorsRes.data);
      } catch (error) {
        console.error("❌ Lỗi khi tải dữ liệu:", error);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [id]);

  // ====== Tìm thông tin connector theo ConnectorTypeID ======
  const getConnectorDetail = (connectorTypeId) =>
    connectorTypes.find(
      (c) => String(c.ConnectorTypeID) === String(connectorTypeId)
    );

  const toggleExpand = (pointId) =>
    setExpandedPoint(expandedPoint === pointId ? null : pointId);

  const handleBooking = (pointId, connectorId) => {
    console.log(`Booking Point: ${pointId}, Connector: ${connectorId}`);
    navigate(`/bookings`);
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

      <h1 className="station-title">{station.StationName}</h1>
      <p className="station-address">📍 {station.Address}</p>

      <div className="point-list">
        {chargingPoints.map((point) => {
          const expanded = expandedPoint === point.PointID;
          const connector = getConnectorDetail(point.ConnectorTypeID);

          return (
            <div
              key={point.PointID}
              className={`point-card ${expanded ? "expanded" : ""}`}
              onClick={() => toggleExpand(point.PointID)}
            >
              {/* Header */}
              <div className="point-header">
                <div className="point-info">
                  <h3>🔋 {point.PointNumber}</h3>
                  <p>
                    {point.Status === "available" ? "Sẵn sàng" : point.Status}
                  </p>
                </div>
                <div
                  className={`status-dot ${point.Status?.toLowerCase()}`}
                ></div>
              </div>

              <div className="point-meta">
                <span>⚡ Công suất: {point.MaxPowerKW} kW</span>
                <span>🔧 Bảo trì: {point.LastMaintenanceDate || "N/A"}</span>
              </div>

              {/* Chi tiết Connector */}
              {expanded && connector && (
                <div className="connector-panel">
                  <h4>🔌 Thông tin cổng sạc</h4>
                  <div className="connector-item">
                    <div className="connector-info">
                      <p>
                        <strong>{connector.DisplayName}</strong>
                      </p>
                      <p>Mã: {connector.Code}</p>
                      <p>Chế độ: {connector.Mode}</p>
                      <p>⚡ Công suất: {connector.DefaultMaxPowerKW} kW</p>
                    </div>
                    <div className="connector-actions">
                      <div
                        className={`mode-tag ${connector.Mode?.toLowerCase()}`}
                      >
                        {connector.Mode}
                      </div>
                      {point.Status?.toLowerCase() === "available" && (
                        <button
                          className="btn-book-small"
                          onClick={(e) => {
                            e.stopPropagation();
                            handleBooking(
                              point.PointID,
                              connector.ConnectorTypeID
                            );
                          }}
                        >
                          📅 Đặt chỗ
                        </button>
                      )}
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
