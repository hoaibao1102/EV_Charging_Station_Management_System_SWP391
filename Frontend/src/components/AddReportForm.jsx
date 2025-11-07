
import Button from 'react-bootstrap/Button';
import Col from 'react-bootstrap/Col';
import Form from 'react-bootstrap/Form';
import Row from 'react-bootstrap/Row';
import Alert from 'react-bootstrap/Alert';
import {createAccidentReportApi} from '../api/reportApi.js';
import { useState } from 'react';
import '../components/admin/AddStaffForm.css'; 
import { toast } from 'react-toastify';


export default function AddReportForm({ onClose, charger }) {
  const initialFormData = {
  code: charger ? charger.code : '',
  mode: charger ? charger.mode : '',
  displayName: charger ? charger.displayName : '',
  defaultMaxPowerKW: charger ? charger.defaultMaxPowerKW : '',
  isDeprecated: charger ? charger.isDeprecated : false,
};

const initialFormErrors = {
  code: '',
  mode: '',
  displayName: '',
  defaultMaxPowerKW: '',
  isDeprecated: false,
};
  const [formData, setFormData] = useState(initialFormData);
  const [formErrors, setFormErrors] = useState(initialFormErrors);
  const [apiError, setApiError] = useState(''); 

  const validateField = (name, value) => {
    switch (name) {
      case 'code':
        return value.trim() ? '' : 'Vui lòng nhập mã cổng sạc.';
      case 'mode':
        return value.trim() ? '' : 'Vui lòng nhập loại cổng sạc.';
      case 'displayName':
        return value.trim() ? '' : 'Vui lòng nhập tên hiển thị.';
      case 'defaultMaxPowerKW':
        if (!value.trim()) return 'Vui lòng nhập công suất tối đa (kW).';
        if (isNaN(value) || Number(value) <= 10) return 'Công suất tối đa phải là một số lớn hơn 10.';
        return '';
      case 'isDeprecated':
        return value ? '' : 'Vui lòng phân loại cổng sạc.';
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

  const handleUpdate = async (event) => {
    event.preventDefault();
    console.log("Charger ID:", charger.connectorTypeId);
    const response = await updateConnectorTypeApi(charger.connectorTypeId, formData);
    console.log("DATA:", formData);
    if (response.success) {
      onClose();
      toast.success("Cập nhật cổng sạc thành công.");
    } else {
      toast.error("Cập nhật cổng sạc thất bại.");
      setApiError(response.error);
    }
  };

    const handleSubmit = async (event) => {
      event.preventDefault();
      const response = await addConnectorTypeApi(formData);
      console.log("DATA:", formData);
      if (response.success) {
        onClose();
        toast.success("Thêm mới cổng sạc thành công.");
      } else {
        toast.error("Thêm mới cổng sạc thất bại.");
        setApiError(response.error);
      }
    };

  const handleBack = () => {
    onClose();
  };

  return (
    <Form noValidate onSubmit={charger ? handleUpdate : handleSubmit} className="add-staff-form">

      <img src={"chargingIcon"} alt="Add Charger" className="staff-icon" /> <br />

      {/* HÀNG 1: Tên trạm + Giờ hoạt động */}
      <Row className="mb-3">
        <Form.Group as={Col} controlId="chargerName">
          <Form.Label>Tên cổng sạc</Form.Label>
          <Form.Control
            type="text"
            placeholder="Nhập tên cổng sạc"
            name="displayName"
            value={formData.displayName}
            onChange={handleChange}
            onBlur={handleBlur}
            isInvalid={!!formErrors.displayName}
            required
          />
          <Form.Control.Feedback type="invalid">
            {formErrors.displayName}
          </Form.Control.Feedback>
        </Form.Group>

        <Form.Group as={Col} controlId="code">
          <Form.Label>Mã cổng sạc</Form.Label>
          <Form.Control
            type="text"
            placeholder="VD: Type2 / CCS2 / CHAdeMO /..."
            name="code"
            value={formData.code }
            onChange={handleChange}
            onBlur={handleBlur}
            isInvalid={!!formErrors.code}
            required
          />
          <Form.Control.Feedback type="invalid">
            {formErrors.code}
          </Form.Control.Feedback>
        </Form.Group>
      </Row>

      <Row className="mb-3">
        <Form.Group as={Col} controlId="mode">
          <Form.Label>Loại cổng sạc</Form.Label>
          <Form.Control
            type="text" 
            placeholder="VD: AC / DC"
            name="mode"
            value={formData.mode }
            onChange={handleChange}
            onBlur={handleBlur}
            isInvalid={!!formErrors.mode}
            required
          />
          <Form.Control.Feedback type="invalid">
            {formErrors.mode}
          </Form.Control.Feedback>
        </Form.Group>

        <Form.Group as={Col} controlId="longitude">
          <Form.Label>Năng lượng tối đa (kW)</Form.Label>
          <Form.Control
            type="number"
            placeholder="VD: 106"
            name="defaultMaxPowerKW"
            value={formData.defaultMaxPowerKW }
            onChange={handleChange}
            onBlur={handleBlur}
            isInvalid={!!formErrors.defaultMaxPowerKW}
            required
          />
          <Form.Control.Feedback type="invalid">
            {formErrors.defaultMaxPowerKW}
          </Form.Control.Feedback>
        </Form.Group>
      </Row>

    
      {/* HÀNG 4: Trạng thái có lỗi thời hay không */}
      <Form.Group className="mb-3" controlId="status">
        <Form.Label>Trạng thái</Form.Label>
        <Form.Select
          name="isDeprecated"
          value={formData.isDeprecated}
          onChange={handleChange}
          onBlur={handleBlur}
          isInvalid={!!formErrors.isDeprecated}
          required
        >
          <option value="" disabled>Chọn trạng thái ban đầu...</option>
          <option selected={charger?.isDeprecated === false} value="false"> Đang hoạt động </option>
          <option selected={charger?.isDeprecated === true} value="true"> Không còn phục vụ </option>
        </Form.Select>
        <Form.Control.Feedback type="invalid">
          {formErrors.isDeprecated}
        </Form.Control.Feedback>
      </Form.Group>


      {apiError && (
        <Alert variant="danger" className="mt-3">
          {apiError}
        </Alert>
      )}

      <div className="form-button-group mt-3">
        <Button variant="primary" type="submit" className="me-2">
          {charger ? 'Cập nhật' : 'Tạo mới'}
        </Button>
        <Button variant="primary" type="button" className="me-2" onClick={handleBack}>
          Trở về
        </Button>
      </div>
    </Form>
  );
}