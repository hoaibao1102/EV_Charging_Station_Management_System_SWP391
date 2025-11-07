
import Button from 'react-bootstrap/Button';
import Col from 'react-bootstrap/Col';
import Form from 'react-bootstrap/Form';
import Row from 'react-bootstrap/Row';
import Alert from 'react-bootstrap/Alert';
import {createAccidentReportApi} from '../api/reportApi.js';
import icon from '../assets/icon/admin/accident.png';
import { useState } from 'react';
import '../components/admin/AddStaffForm.css'; 
import { toast } from 'react-toastify';


export default function AddReportForm({ onClose }) {
  const initialFormData = {
  title: '',
  description: '',
  severity: 'low',
};

const initialFormErrors = {
  title: '',
  description: '',
  severity: '',
};
  const [formData, setFormData] = useState(initialFormData);
  const [formErrors, setFormErrors] = useState(initialFormErrors);
  const [apiError, setApiError] = useState(''); 

  const validateField = (name, value) => {
    switch (name) {
      case 'title':
        return value.trim() ? '' : 'Vui lòng nhập tiêu đề.';
      case 'description':
        return value.trim() ? '' : 'Vui lòng nhập mô tả.';
      case 'severity':
        return value.trim() ? '' : 'Vui lòng chọn mức độ nghiêm trọng.';
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
      const response = await createAccidentReportApi(formData);
      console.log("DATA:", formData);
      if (response.success) {
        onClose();
        toast.success("Thêm mới báo cáo sự cố thành công.");
      } else {
        toast.error("Thêm mới báo cáo sự cố thất bại.");
        setApiError(response.error);
      }
    };

  const handleBack = () => {
    onClose();
  };

  return (
    <Form noValidate onSubmit={handleSubmit} className="add-staff-form">

      <img src={icon} alt="Add Report" className="staff-icon" /> <br />

      <Row className="mb-3">
        <Form.Group as={Col} controlId="title">
          <Form.Label>Tên báo cáo</Form.Label>
          <Form.Control
            type="text"
            placeholder="Nhập tên báo cáo"
            name="title"
            value={formData.title}
            onChange={handleChange}
            onBlur={handleBlur}
            isInvalid={!!formErrors.title}
            required
          />
          <Form.Control.Feedback type="invalid">
            {formErrors.title}
          </Form.Control.Feedback>
        </Form.Group>

        <Form.Group as={Col} controlId="severity">
        <Form.Label>Mức độ nghiêm trọng</Form.Label>
        <Form.Select
          name="severity"
          value={formData.severity}
          onChange={handleChange}
          onBlur={handleBlur}
          isInvalid={!!formErrors.severity}
          required
        >
          <option value="" disabled>Chọn mức độ nghiêm trọng...</option>
          <option selected={formData.severity === 'low'} value="low"> Thấp </option>
          <option selected={formData.severity === 'medium'} value="medium"> Trung bình </option>
          <option selected={formData.severity === 'high'} value="high"> Cao </option>
        </Form.Select>
        <Form.Control.Feedback type="invalid">
          {formErrors.severity}
        </Form.Control.Feedback>
      </Form.Group>
      </Row>

      <Form.Group className="mb-3" controlId="description">
          <Form.Label>Nội dung báo cáo</Form.Label>
          <Form.Control
            as="textarea"
            placeholder="nội dung..."
            name="description"
            value={formData.description}
            onChange={handleChange}
            onBlur={handleBlur}
            isInvalid={!!formErrors.description}
            required
          />
          <Form.Control.Feedback type="invalid">
            {formErrors.description}
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