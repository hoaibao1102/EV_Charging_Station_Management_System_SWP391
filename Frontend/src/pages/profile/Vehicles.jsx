import { useEffect, useState } from "react";
import { Container, Row, Col } from "react-bootstrap";
import { getMyVehiclesApi } from "../../api/driverApi.js";
import VehicleCard from "../../components/VehicleCard.jsx";
import { toast } from "react-toastify";
import { deleteVehicleApi } from '../../api/driverApi.js';
import './Vehicles.css';
import AddVehicle from "./AddVehicle.jsx";
import classCss from '../../assets/css/Main.module.css'

export default function Vehicles() {
  const [vehicles, setVehicles] = useState([]);
  const [loading, setLoading] = useState(true);

  const [showAddVehicle, setShowAddVehicle] = useState(false);

  const handleDelete = async (vehicle) => {
    if (window.confirm("Bạn có chắc chắn muốn xóa xe khỏi danh sách?")) {
      try {
        const response = await deleteVehicleApi(vehicle.vehicleId);
        if (response.success) {
          setVehicles(prevVehicles => prevVehicles.filter(v => v.vehicleId !== vehicle.vehicleId));
          toast.success("Xóa xe thành công!");
        } else {
          toast.error("Xóa xe thất bại!");
        }
      } catch (error) {
        console.error("Error deleting vehicle:", error);
        toast.error("Xóa xe thất bại!");
      }
    }
  }

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
    // Refresh danh sách xe sau khi thêm thành công
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
        ) :
        <div className="vehicle-list">
          {loading ? (
            <div className="loading-spinner">
              <p>Đang tải...</p>
            </div>
          ) : vehicles.length === 0 ? (
            <div className="empty-state">
              <div className="empty-state-icon">🚗</div>
              <p>Không có xe nào trong danh sách. Hãy thêm xe mới!</p>
              <button className="btn btn-primary" onClick={() => setShowAddVehicle(true)}>Thêm xe mới</button>
            </div>
          ) : (
            <>
            <button className={classCss.button} onClick={() => setShowAddVehicle(true)}>Thêm xe mới</button>
            <Row className="g-4">
              {vehicles.map(vehicle => (
                <Col xs={12} sm={6} md={4} lg={3} key={vehicle.vehicleId}>
                  <VehicleCard 
                    vehicle={vehicle} 
                    onDelete={() => handleDelete(vehicle)}
                  />
                </Col>
              ))}
            </Row>
            </>
          )}
        </div>
        }
      </Container>
    </div>
  );
}