import { useEffect, useState } from "react";
import { Container, Row, Col } from "react-bootstrap";
import { getMyVehiclesApi } from "../../api/driverApi.js";
import VehicleCard from "../../components/driver/VehicleCard.jsx";
import { toast } from "react-toastify";
import { updateVehicleApi } from "../../api/driverApi.js";
import "./Vehicles.css";
import AddVehicle from "./AddVehicle.jsx";
import classCss from "../../assets/css/Main.module.css";

export default function Vehicles() {
  const [vehicles, setVehicles] = useState([]);
  const [loading, setLoading] = useState(true);

  const [showAddVehicle, setShowAddVehicle] = useState(false);

  const handleUpdate = async (vehicle) => {
    const newStatus = vehicle.vehicleStatus === "ACTIVE" ? "INACTIVE" : "ACTIVE";
    const confirmMessage = newStatus === "INACTIVE" 
      ? "Bạn có chắc chắn muốn ngưng hoạt động xe này?" 
      : "Bạn có chắc chắn muốn cho xe này hoạt động trở lại?";
    
    if (window.confirm(confirmMessage)) {
      try {
        const response = await updateVehicleApi(vehicle.vehicleId, newStatus);
        if (response.success) {
          // Cập nhật lại trạng thái xe trong danh sách
          setVehicles((prevVehicles) =>
            prevVehicles.map((v) =>
              v.vehicleId === vehicle.vehicleId
                ? { ...v, vehicleStatus: newStatus }
                : v
            )
          );
          const successMessage = newStatus === "INACTIVE" 
            ? "Ngưng hoạt động xe thành công!" 
            : "Xe đã hoạt động trở lại!";
          toast.success(successMessage);
        } else {
          toast.error("Cập nhật trạng thái xe thất bại!");
        }
      } catch (error) {
        console.error("Error updating vehicle status:", error);
        toast.error("Cập nhật trạng thái xe thất bại!");
      }
    }
  };

  const fetchVehicles = async () => {
    try {
      setLoading(true);
      const vehicles = await getMyVehiclesApi();
      if (vehicles.success) {
        setVehicles(vehicles.data);
        console.log("My vehicles:", vehicles.data);
      }
    } catch (error) {
      console.error("Failed to fetch my vehicles:", error);
      toast.error("Không thể tải danh sách xe!");
    } finally {
      setLoading(false);
    }
  };

  const handleAddSuccess = () => {
    fetchVehicles();
  };

  useEffect(() => {
    fetchVehicles();
  }, []);

  return (
    <div className="my-vehicles-container">
      <Container>
        <div className="my-vehicles-header">
          {showAddVehicle ? "Thêm Xe" : "DANH SÁCH XE CỦA TÔI"}
        </div>
        {showAddVehicle ? (
          <AddVehicle
            onClose={() => setShowAddVehicle(false)}
            onSuccess={handleAddSuccess}
          />
        ) : (
          <div className="vehicle-list">
            {loading ? (
              <div className="loading-spinner">
                <p>Đang tải...</p>
              </div>
            ) : vehicles.length === 0 ? (
              <div className="empty-state">
                <div className="empty-state-icon">🚗</div>
                <p>Không có xe nào trong danh sách. Hãy thêm xe mới!</p>
                <button
                  className="btn btn-primary"
                  style={{width:'200px' , marginRight:'10px'}}
                  onClick={() => setShowAddVehicle(true)}
                >
                  Thêm xe mới
                </button>
              </div>
            ) : (
              <>
                <div style={{ marginBottom: '1.5rem' }}>
                  <button
                    className={classCss.button}
                    onClick={() => setShowAddVehicle(true)}
                    style={{ width: 'auto', padding: '0.5rem 1.5rem' }}
                  >
                    Thêm xe mới
                  </button>
                </div>
                
                {/* Xe đang hoạt động */}
                <div className="vehicle-section">
                  <h3 className="section-title">Xe đang hoạt động</h3>
                  <Row className="g-4">
                    {vehicles.filter(v => v.vehicleStatus === "ACTIVE").length === 0 ? (
                      <p className="text-muted">Không có xe nào đang hoạt động</p>
                    ) : (
                      vehicles
                        .filter(v => v.vehicleStatus === "ACTIVE")
                        .map((vehicle) => (
                          <Col xs={12} sm={6} md={4} lg={3} key={vehicle.vehicleId}>
                            <VehicleCard
                              vehicle={vehicle}
                              onUpdate={() => handleUpdate(vehicle)}
                            />
                          </Col>
                        ))
                    )}
                  </Row>
                </div>

                {/* Xe ngưng hoạt động */}
                <div className="vehicle-section mt-5">
                  <h3 className="section-title">Xe ngưng hoạt động</h3>
                  <Row className="g-4">
                    {vehicles.filter(v => v.vehicleStatus === "INACTIVE").length === 0 ? (
                      <p className="text-muted">Không có xe nào ngưng hoạt động</p>
                    ) : (
                      vehicles
                        .filter(v => v.vehicleStatus === "INACTIVE")
                        .map((vehicle) => (
                          <Col xs={12} sm={6} md={4} lg={3} key={vehicle.vehicleId}>
                            <VehicleCard
                              vehicle={vehicle}
                              onUpdate={() => handleUpdate(vehicle)}
                            />
                          </Col>
                        ))
                    )}
                  </Row>
                </div>
              </>
            )}
          </div>
        )}
      </Container>
    </div>
  );
}
