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

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        const stationRes = await axios.get(
          `https://68f35999fd14a9fcc4288a78.mockapi.io/stationcharging?StationID=${id}`
        );
        const pointsRes = await axios.get(
          "https://68f35999fd14a9fcc4288a78.mockapi.io/Charging_Points"
        );
        const connectorsRes = await axios.get(
          "https://68f6f46af7fb897c66141d83.mockapi.io/Connector_types"
        );

        const st = stationRes.data.length ? stationRes.data[0] : null;
        const pts = pointsRes.data.filter(
          (p) => String(p.StationID) === String(id)
        );

        setStation(st);
        setChargingPoints(pts);
        setConnectorTypes(connectorsRes.data);
      } catch (error) {
        console.error("❌ Error:", error);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [id]);

  const getConnectorsByPoint = (pointId) =>
    connectorTypes.filter((c) => String(c.PointID) === String(pointId));

  const toggleExpand = (pointId) =>
    setExpandedPoint(expandedPoint === pointId ? null : pointId);

  const handleBooking = (pointId, connectorId) => {
    console.log(`Booking Point: ${pointId}, Connector: ${connectorId}`);
    navigate(`/booking/${id}/${pointId}/${connectorId}`);
  };

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

  return (
    <div className="station-container">
      <button className="btn-back" onClick={() => navigate(-1)}>
        ← Quay lại danh sách
      </button>

      <h1 className="station-title">{station.StationName}</h1>
      <p className="station-address">📍 {station.Address}</p>

      <div className="point-list">
        {chargingPoints.map((point) => {
          const connectors = getConnectorsByPoint(point.PointID);
          const expanded = expandedPoint === point.PointID;

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

              {/* === Khi mở rộng trụ === */}
              {expanded && (
                <div className="connector-panel">
                  <h4>🔌 Các cổng sạc ({connectors.length})</h4>

                  {connectors.length === 0 && (
                    <p className="no-connector">Chưa có thông tin đầu nối</p>
                  )}

                  {connectors.map((c, index) => (
                    <div key={c.ConnectorTypeID} className="connector-item">
                      <div className="connector-info">
                        <h5>Cổng {String.fromCharCode(65 + index)}</h5>
                        <p>
                          <strong>{c.DisplayName}</strong>
                        </p>
                        <p>Mã: {c.Code}</p>
                        <p>Chế độ: {c.Mode}</p>
                        <p>⚡ Công suất: {c.DefaultMaxPowerKW} kW</p>
                      </div>

                      <div className="connector-actions">
                        <div className={`mode-tag ${c.Mode?.toLowerCase()}`}>
                          {c.Mode}
                        </div>
                        <button
                          className="btn-book-small"
                          onClick={(e) => {
                            e.stopPropagation(); // tránh click trùng toggle
                            handleBooking(point.PointID, c.ConnectorTypeID);
                          }}
                        >
                          📅 Đặt chỗ
                        </button>
                      </div>
                    </div>
                  ))}
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
