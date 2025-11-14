import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import "./Stations.css";
import { stationAPI } from "../../api/stationApi.js";
import stationsHeroMobile from "../../assets/img/home/home.jpg"; 
import stationsHeroDesktop from "../../assets/img/home/home_lab.jpg";

export default function Stations() {
  const navigate = useNavigate();
  const [stations, setStations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [userLocation, setUserLocation] = useState(null);
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 4;

  // ===== Lấy vị trí hiện tại của người dùng =====
  useEffect(() => {
    if ("geolocation" in navigator) {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          setUserLocation({
            lat: pos.coords.latitude,
            lng: pos.coords.longitude,
          });
        },
        (err) => {
          console.warn("Không thể lấy vị trí:", err.message);
          setUserLocation(null);
        },
        { enableHighAccuracy: true }
      );
    } else {
      console.warn("Trình duyệt không hỗ trợ geolocation.");
    }
  }, []);

  // ===== Lấy danh sách trạm sạc =====
  useEffect(() => {
    fetchStations();
  }, []);

  const fetchStations = async () => {
    try {
      const response = await stationAPI.getAllStations();
      // ✅ response.data đã là array rồi, không cần .json()
      const data = response.data;

      const normalized = data.map((item) => ({
        id: item.id || item.stationId, // ✅ Ưu tiên id (MockAPI) rồi fallback sang StationID
        StationID: item.stationId,
        name: item.stationName || item.name || "Trạm sạc chưa đặt tên",
        address: item.address || item.address || "Chưa có địa chỉ",
        status: item.status || item.status || "unknown",
        lat: parseFloat(item.latitude || item.lat || 0),
        lng: parseFloat(item.longitude || item.lng || 0),
        distance: null,
      }));

      setStations(normalized);
    } catch (error) {
      console.error("Error fetching stations:", error);
    } finally {
      setLoading(false);
    }
  };

  // ===== Tính khoảng cách (km) theo công thức Haversine =====
  const calcDistanceKm = (lat1, lon1, lat2, lon2) => {
    const R = 6371; // bán kính Trái đất (km)
    const dLat = ((lat2 - lat1) * Math.PI) / 180;
    const dLon = ((lon2 - lon1) * Math.PI) / 180;
    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos((lat1 * Math.PI) / 180) *
        Math.cos((lat2 * Math.PI) / 180) *
        Math.sin(dLon / 2) *
        Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return (R * c).toFixed(2);
  };

  // ===== Gắn khoảng cách thực tế cho mỗi trạm =====
  const stationsWithDistance = stations.map((s) => {
    if (userLocation && s.lat && s.lng) {
      const distance = calcDistanceKm(
        userLocation.lat,
        userLocation.lng,
        s.lat,
        s.lng
      );
      return { ...s, distance };
    }
    return s;
  });

  // ===== Hàm mở Google Maps chỉ đường =====
  const handleNavigate = (lat, lng) => {
    if (!lat || !lng) {
      alert("Tọa độ trạm không hợp lệ!");
      return;
    }

    const url = userLocation
      ? `https://www.google.com/maps/dir/?api=1&origin=${userLocation.lat},${userLocation.lng}&destination=${lat},${lng}`
      : `https://www.google.com/maps/dir/?api=1&destination=${lat},${lng}`;

    window.open(url, "_blank");
  };

  // ===== Hàm xem chi tiết trạm sạc =====
  const handleViewDetail = (stationId) => {
    console.log("Navigating to station:", stationId);
    console.log("Navigate path:", `/stations/${stationId}`);
    navigate(`/stations/${stationId}`);
  };

  // ===== Lọc theo từ khóa =====
  const filteredStations = stationsWithDistance.filter(
    (station) =>
      (station.name || "").toLowerCase().includes(searchQuery.toLowerCase()) ||
      (station.address || "").toLowerCase().includes(searchQuery.toLowerCase())
  );

  // ===== PHÂN TRANG =====
  const totalPages = Math.ceil(filteredStations.length / itemsPerPage);
  const indexOfLast = currentPage * itemsPerPage;
  const indexOfFirst = indexOfLast - itemsPerPage;
  const currentStations = filteredStations.slice(indexOfFirst, indexOfLast);

  const handleNextPage = () => {
    setCurrentPage((prev) => (prev < totalPages ? prev + 1 : 1)); // quay vòng
  };

  const handlePrevPage = () => {
    setCurrentPage((prev) => (prev > 1 ? prev - 1 : totalPages)); // quay vòng
  };

  // ===== Màu trạng thái =====
  const getStatusColor = (status) => {
    if (status === "ACTIVE" || status === "active") return "#4CAF50";
    if (status === "MAINTENANCE" || status === "busy") return "#00BCD4";
    if (status === "INACTIVE" || status === "maintenance") return "#F44336";
    return "#9E9E9E";
  };

  // ===== Hiển thị =====
  if (loading) return <div className="stations-loading">Đang tải...</div>;

  return (
    <div className="home-page">
      {/* Hero Section - giống Home */}
      <img
        className="hero-img mobile-only"
        src={stationsHeroMobile}
        alt="Trạm sạc xe điện"
      />
      <img
        className="hero-img desktop-only"
        src={stationsHeroDesktop}
        alt="Trạm sạc xe điện"
      />

      <div className="hero-text">
        <h1 className="hero-title">TRẠM SẠC</h1>
        <p className="hero-subtitle">EV STATIONS</p>
      </div>

      {/* Welcome Card - giống Home */}
      <div className="welcome-card">
        {/* Thanh tìm kiếm */}
        <div className="map-search">
          <div className="search-wrapper">
            <span className="search-icon">🔍</span>
            <input
              type="text"
              placeholder="Tìm kiếm trạm sạc..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="search-input"
            />
            <button className="filter-btn">⚙️</button>
          </div>
        </div>
        {/* Tiêu đề */}
        <h2 className="stations-header" style={{paddingTop:'10px'}}>Trạm sạc gần đây</h2>

        {/* Danh sách trạm */}
        <div className="station-list">
          {currentStations.length === 0 ? (
            <p className="no-stations">Không tìm thấy trạm nào.</p>
          ) : (
            currentStations.map((station) => (
              <div key={station.id} className="station-card">
                <div className="station-header">
                  <h3 className="station-name">{station.name}</h3>
                  <span
                    className="status-dot"
                    style={{ background: getStatusColor(station.status) }}
                  ></span>
                </div>
                <p className="station-address">{station.address}</p>
                <p className="station-distance">
                  ⚡ Cách đây{" "}
                  {station.distance ? `${station.distance} km` : "—"}
                </p>
                <div className="station-actions">
                  <button
                    className="btn-navigate"
                    onClick={() => handleNavigate(station.lat, station.lng)}
                  >
                    🗺️ Chỉ đường
                  </button>
                  <button
                    className="btn-detail"
                    onClick={() => handleViewDetail(station.id)}
                  >
                    Xem chi tiết
                  </button>
                </div>
              </div>
            ))
          )}
        </div>

        {/* ==== PAGINATION CONTROLS ==== */}
        {filteredStations.length > itemsPerPage && (
          <div className="pagination-container">
            <button className="page-btn" onClick={handlePrevPage}>
              ◀
            </button>
            <div className="page-dots">
              {Array.from({ length: totalPages }, (_, i) => (
                <span
                  key={i}
                  className={`dot ${currentPage === i + 1 ? "active" : ""}`}
                  onClick={() => setCurrentPage(i + 1)}
                ></span>
              ))}
            </div>
            <button className="page-btn" onClick={handleNextPage}>
              ▶
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
