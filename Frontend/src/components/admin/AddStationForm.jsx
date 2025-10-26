import Button from 'react-bootstrap/Button';
import Col from 'react-bootstrap/Col';
import Form from 'react-bootstrap/Form';
import Row from 'react-bootstrap/Row';
import Alert from 'react-bootstrap/Alert';
import stationIcon from '../../assets/icon/staff/charging-station.png'; 
import { useState } from 'react';
import { addStationApi } from '../../api/stationApi.js'; 

import './AddStaffForm.css'; 
import { toast } from 'react-toastify';


const initialFormData = {
  stationName: '',
  address: '',
  latitude: '',
  longitude: '',
  operatingHours: '',
  status: '', //  'ACTIVE', 'MAINTENANCE', 'INACTIVE'
};

const initialFormErrors = {
  stationName: '',
  address: '',
  latitude: '',
  longitude: '',
  operatingHours: '',
  status: '',
};


export default function AddStationForm({ onClose, onAddSuccess }) {
  const [formData, setFormData] = useState(initialFormData);
  const [formErrors, setFormErrors] = useState(initialFormErrors);
  const [apiError, setApiError] = useState(''); 

  const validateField = (name, value) => {
    switch (name) {
      case 'stationName':
        return value.trim() ? '' : 'Vui lòng nhập tên trạm.';
      case 'address':
        return value.trim() ? '' : 'Vui lòng nhập địa chỉ trạm.';
      case 'latitude':
        if (!value.trim()) return 'Vui lòng nhập vĩ độ.';
        if (!/^-?([0-9]*[.])?[0-9]+$/.test(value)) return 'Vĩ độ phải là một số hợp lệ.';
        return '';
      case 'longitude':
        if (!value.trim()) return 'Vui lòng nhập kinh độ.';
        if (!/^-?([0-9]*[.])?[0-9]+$/.test(value)) return 'Kinh độ phải là một số hợp lệ.';
        return '';
      case 'operatingHours':
        return value.trim() ? '' : 'Vui lòng nhập giờ hoạt động (VD: 24/7).';
      case 'status':
        return value ? '' : 'Vui lòng chọn trạng thái ban đầu.';
      default:
        return '';
    }
  };

  const handleBlur = (event) => {
    const { name, value } = event.target;
    
    const error = validateField(name, value);
    setFormErrors(prevErrors => ({
      ...prevErrors,
      [name]: error,
    }));
  };


  const handleChange = (event) => {
    const { name, value } = event.target;
    setFormData(prevData => ({
      ...prevData,
      [name]: value,
    }));

    if (formErrors[name]) {
      setFormErrors(prevErrors => ({
        ...prevErrors,
        [name]: '',
      }));
    }
    if(apiError) {
      setApiError('');
    }
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setApiError(''); 

    let newErrors = {};
    let hasError = false;
    for (const key in formData) {
      const error = validateField(key, formData[key]);
      if (error) {
        newErrors[key] = error;
        hasError = true;
      }
    }

    if (hasError) {
      setFormErrors(newErrors); 
      return; 
    }

    const stationData = {
      stationName: formData.stationName,
      address: formData.address,
      latitude: parseFloat(formData.latitude),
      longitude: parseFloat(formData.longitude),
      operatingHours: formData.operatingHours,
      status: formData.status,
    };
    
    console.log("Submitting station data:", stationData);

    try {
      const result = await addStationApi(stationData); 
      if (result.success) {
        toast.success('Thêm trạm sạc thành công!');
        onAddSuccess();
        console.log("Thêm trạm thành công:", result.data);
        onClose();
      } else {
        toast.error(result.message || 'Thêm trạm sạc thất bại.');
        setApiError(result.message || 'Thêm trạm sạc thất bại.');
      }
    } catch (error) {
      console.error("Lỗi hệ thống khi thêm trạm:", error);
      setApiError('Lỗi hệ thống. Vui lòng thử lại sau.');
    }
  };

  const handleBack = () => {
    onClose();
  };

  return (
    <Form noValidate onSubmit={handleSubmit} className="add-staff-form"> 

      <img src={stationIcon} alt="Add Station" className="staff-icon" /> <br />

      {/* HÀNG 1: Tên trạm + Giờ hoạt động */}
      <Row className="mb-3">
        <Form.Group as={Col} controlId="stationName">
          <Form.Label>Tên trạm sạc</Form.Label>
          <Form.Control
            type="text"
            placeholder="Nhập tên trạm sạc"
            name="stationName"
            value={formData.stationName}
            onChange={handleChange}
            onBlur={handleBlur}
            isInvalid={!!formErrors.stationName}
            required
          />
          <Form.Control.Feedback type="invalid">
            {formErrors.stationName}
          </Form.Control.Feedback>
        </Form.Group>

        <Form.Group as={Col} controlId="operatingHours">
          <Form.Label>Giờ hoạt động</Form.Label>
          <Form.Control
            type="text"
            placeholder="VD: 24/7 hoặc 8:00 - 22:00"
            name="operatingHours"
            value={formData.operatingHours}
            onChange={handleChange}
            onBlur={handleBlur}
            isInvalid={!!formErrors.operatingHours}
            required
          />
          <Form.Control.Feedback type="invalid">
            {formErrors.operatingHours}
          </Form.Control.Feedback>
        </Form.Group>
      </Row>

      {/* HÀNG 2: Vĩ độ + Kinh độ */}
      <Row className="mb-3">
        <Form.Group as={Col} controlId="latitude">
          <Form.Label>Vĩ độ (Latitude)</Form.Label>
          <Form.Control
            type="text" 
            placeholder="VD: 10.7769"
            name="latitude"
            value={formData.latitude}
            onChange={handleChange}
            onBlur={handleBlur}
            isInvalid={!!formErrors.latitude}
            required
          />
          <Form.Control.Feedback type="invalid">
            {formErrors.latitude}
          </Form.Control.Feedback>
        </Form.Group>

        <Form.Group as={Col} controlId="longitude">
          <Form.Label>Kinh độ (Longitude)</Form.Label>
          <Form.Control
            type="text" 
            placeholder="VD: 106.6953"
            name="longitude"
            value={formData.longitude}
            onChange={handleChange}
            onBlur={handleBlur}
            isInvalid={!!formErrors.longitude}
            required
          />
          <Form.Control.Feedback type="invalid">
            {formErrors.longitude}
          </Form.Control.Feedback>
        </Form.Group>
      </Row>

      {/* HÀNG 3: Địa chỉ (full width) */}
      <Form.Group className="mb-3" controlId="address">
        <Form.Label>Địa chỉ</Form.Label>
        <Form.Control
          as="textarea" 
          cols={90}
          rows={3}
          placeholder="Nhập địa chỉ chi tiết của trạm"
          name="address"
          value={formData.address}
          onChange={handleChange}
          onBlur={handleBlur}
          isInvalid={!!formErrors.address}
          required
        />
        <Form.Control.Feedback type="invalid">
          {formErrors.address}
        </Form.Control.Feedback>
      </Form.Group>

      {/* HÀNG 4: Trạng thái (dropdown) */}
      <Form.Group className="mb-3" controlId="status">
        <Form.Label>Trạng thái</Form.Label>
        <Form.Select
          name="status"
          value={formData.status}
          onChange={handleChange}
          onBlur={handleBlur}
          isInvalid={!!formErrors.status}
          required
        >
          <option value="" disabled>Chọn trạng thái ban đầu...</option>
          <option value="ACTIVE">Hoạt động </option>
          <option value="MAINTENANCE">Bảo trì </option>
          <option value="INACTIVE">Ngưng hoạt động </option>
        </Form.Select>
        <Form.Control.Feedback type="invalid">
          {formErrors.status}
        </Form.Control.Feedback>
      </Form.Group>


      {apiError && (
        <Alert variant="danger" className="mt-3">
          {apiError}
        </Alert>
      )}

      <div className="form-button-group mt-3">
        <Button variant="primary" type="submit" className="me-2">
          Tạo mới
        </Button>
        <Button variant="primary" type="button" className="me-2" onClick={handleBack}>
          Trở về
        </Button>
      </div>
    </Form>
  );
}