import Form from 'react-bootstrap/Form';
import {getAllStations} from '../../api/stationApi.js';
import {useEffect, useState} from 'react';
import {transferStaffApi} from '../../api/admin.js';
import './AddStaffForm.css';
import Button from 'react-bootstrap/Button';

export default function SelectStationForm({onClose, onAddSuccess, staff}) {
  const [stations, setStations] = useState([]);

  useEffect(() => {
    const fetchStations = async () => {
      const response = await getAllStations();
      if (response.success) {
        setStations(response.data);
      }
    };
    fetchStations();
  }, []);

  const currentStationName = stations.find(
    station => station.stationId === staff.stationId 
  )?.stationName;


  const handleTransferStaff = async (e) => {
    e.preventDefault();
    const formData = new FormData(e.target);
    const selectedStation = formData.get('station');

    if (!selectedStation) {
      alert('Vui lòng chọn một trạm sạc.');
      return;
    }

    const response = await transferStaffApi(staff.userId, selectedStation);
    if (response.success) {
      alert('Chuyển công tác thành công');
      onAddSuccess();
    } else {
      alert('Chuyển công tác thất bại');
    }
  };

  return (
    <>
      <Form onSubmit={handleTransferStaff} className="add-staff-form">
        <h2>
          Chuyển công tác nhân viên {staff.name}
          {currentStationName && ` (hiện công tác ở trạm ${currentStationName})`}
        </h2>

        <Form.Group className="mb-3" controlId="station">
          <Form.Select aria-label="select station" name="station" required>
            <option value="">Chọn trạm sạc mới</option>
            {stations
              .filter(station => station.stationId != staff.stationId) 
              .map(station => (
                <option key={station.stationId} value={station.stationId}>
                  {station.stationName}
                </option>
              ))}
          </Form.Select>
          <Form.Text className="text-muted">
            Thay đổi trạm làm việc sẽ áp dụng cho nhân viên này ngay lập tức.
          </Form.Text>
        </Form.Group>

        <div className="form-button-group mt-3">
          <Button variant="primary" type="submit" className="me-2">
            XÁC NHẬN CHUYỂN CÔNG TÁC
          </Button>
          <Button variant="primary" type="button" className="me-2" onClick={onClose}>
            Trở về
          </Button>
        </div>
      </Form>
    </>
  )
}