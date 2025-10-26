import Button from 'react-bootstrap/Button';
import Col from 'react-bootstrap/Col';
import Form from 'react-bootstrap/Form';
import Row from 'react-bootstrap/Row';
import Alert from 'react-bootstrap/Alert'; 
import staff from '../../assets/icon/admin/staff.png';
import { getAllStations } from '../../api/stationApi.js';
import { useEffect, useState } from 'react';
import { addStaffApi } from '../../api/admin.js';

// Import file CSS
import './AddStaffForm.css';

// --- Cấu trúc state cho form và lỗi ---
const initialFormData = {
  formBasicName: '',
  formBasicPhone: '',
  formBasicBirthday: '',
  gender: '',
  formBasicAddress: '',
  stationID: '',
  formBasicEmail: '',
  formBasicPassword: '',
  formBasicPasswordConfirm: '',
};

// State lỗi, rỗng là không có lỗi
const initialFormErrors = {
  formBasicName: '',
  formBasicPhone: '',
  formBasicBirthday: '',
  gender: '',
  formBasicAddress: '',
  stationID: '',
  formBasicEmail: '',
  formBasicPassword: '',
  formBasicPasswordConfirm: '',
};


export default function AddStaffForm({ onClose, onAddSuccess }) {
  const [stations, setStations] = useState([]);
  const [formData, setFormData] = useState(initialFormData);
  const [formErrors, setFormErrors] = useState(initialFormErrors);
  const [apiError, setApiError] = useState(''); // State riêng cho lỗi từ backend

  useEffect(() => {
    const fetchStations = async () => {
      try {
        const stationList = await getAllStations();
        if (stationList.success) {
          setStations(stationList.data);
          console.log("Fetched stations:", stationList.data);
        } else {
          console.error("Failed to fetch stations:", stationList.message);
        }
      } catch (error) {
        console.error("Error fetching stations:", error);
      }
    };
    fetchStations();
  }, []);

  const validateField = (name, value) => {
    switch (name) {
      case 'formBasicName':
        return value.trim() ? '' : 'Vui lòng nhập tên nhân viên.';
      case 'formBasicPhone':
        return value.trim() ? '' : 'Vui lòng nhập số điện thoại.';
      case 'formBasicBirthday':
        return value ? '' : 'Vui lòng chọn ngày sinh.';
      case 'gender':
        return value ? '' : 'Vui lòng chọn giới tính.';
      case 'formBasicAddress':
        return value.trim() ? '' : 'Vui lòng nhập địa chỉ.';
      case 'stationID':
        return value ? '' : 'Vui lòng chọn trạm sạc.';
      case 'formBasicEmail':
        if (!value.trim()) return 'Vui lòng nhập email.';
        // Một regex đơn giản để check email
        if (!/\S+@\S+\.\S+/.test(value)) return 'Email không hợp lệ.';
        return '';
      case 'formBasicPassword':
        if (!value) return 'Vui lòng nhập mật khẩu.';
        if (value.length < 6) return 'Mật khẩu cần ít nhất 6 ký tự.';
        return '';
      case 'formBasicPasswordConfirm':
        if (!value) return 'Vui lòng nhập lại mật khẩu.';
        // So sánh với giá trị mật khẩu trong state
        if (value !== formData.formBasicPassword) return 'Mật khẩu nhập lại không khớp.';
        return '';
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

    if (name === 'formBasicPassword' && formData.formBasicPasswordConfirm) {
      const confirmError = validateField('formBasicPasswordConfirm', formData.formBasicPasswordConfirm);
      setFormErrors(prevErrors => ({
        ...prevErrors,
        formBasicPasswordConfirm: confirmError,
      }));
    }
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

 
    const user = {
      name: formData.formBasicName,
      phoneNumber: formData.formBasicPhone,
      dateOfBirth: formData.formBasicBirthday,
      address: formData.formBasicAddress,
      email: formData.formBasicEmail,
      passwordHash: formData.formBasicPassword,
      gender: formData.gender,
    };
    const data = { user: user, stationId: formData.stationID };
    
    console.log("Submitting staff data:", data);

    try {
      const result = await addStaffApi(data);
      if (result.success) {
        console.log("Thêm nhân viên thành công:", result.data);
        onAddSuccess(); // Thông báo cho cha biết thêm thành công
        onClose(); // Đóng form
      } else {
        setApiError(result.message || 'Thêm nhân viên thất bại.');
      }
    } catch (error) {
      console.error("Lỗi hệ thống khi thêm nhân viên:", error);
      setApiError('Lỗi hệ thống. Vui lòng thử lại sau.');
    }
  };

  const handleBack = () => {
    onClose();
  };

  return (
    // 'noValidate' RẤT QUAN TRỌNG, nó tắt validation của trình duyệt
    <Form noValidate onSubmit={handleSubmit} className="add-staff-form">

      <img src={staff} alt="Add Staff" className="staff-icon" />

      {/* Cập nhật tất cả các field */}
      
      {/* HÀNG 1: Tên + SĐT */}
      <Row className="mb-3">
        <Form.Group as={Col} controlId="formBasicName">
          <Form.Label>Tên nhân viên</Form.Label>
          <Form.Control
            type="text"
            placeholder="Nhập tên đầy đủ"
            name="formBasicName"
            value={formData.formBasicName}
            onChange={handleChange}
            onBlur={handleBlur}
            isInvalid={!!formErrors.formBasicName} // Hiển thị lỗi
            required
          />
          <Form.Control.Feedback type="invalid">
            {formErrors.formBasicName} {/* Tin nhắn lỗi */}
          </Form.Control.Feedback>
        </Form.Group>

        <Form.Group as={Col} controlId="formBasicPhone">
          <Form.Label>Số điện thoại</Form.Label>
          <Form.Control
            type="tel"
            placeholder="Nhập số điện thoại"
            name="formBasicPhone"
            value={formData.formBasicPhone}
            onChange={handleChange}
            onBlur={handleBlur}
            isInvalid={!!formErrors.formBasicPhone}
            required
          />
          <Form.Control.Feedback type="invalid">
            {formErrors.formBasicPhone}
          </Form.Control.Feedback>
        </Form.Group>
      </Row>

      {/* HÀNG 2: Ngày sinh + Giới tính */}
      <Row className="mb-3">
        <Form.Group as={Col} controlId="formBasicBirthday">
          <Form.Label>Ngày sinh</Form.Label>
          <Form.Control
            type="date"
            name="formBasicBirthday"
            value={formData.formBasicBirthday}
            onChange={handleChange}
            onBlur={handleBlur}
            isInvalid={!!formErrors.formBasicBirthday}
            required
          />
          <Form.Control.Feedback type="invalid">
            {formErrors.formBasicBirthday}
          </Form.Control.Feedback>
        </Form.Group>

        <Form.Group as={Col} controlId="gender">
          <Form.Label>Giới tính</Form.Label>
          <Form.Select
            name="gender"
            value={formData.gender}
            onChange={handleChange}
            onBlur={handleBlur}
            isInvalid={!!formErrors.gender}
            required
          >
            <option value="" disabled>Chọn ...</option>
            <option value="M">Nam</option>
            <option value="F">Nữ</option>
          </Form.Select>
          <Form.Control.Feedback type="invalid">
            {formErrors.gender}
          </Form.Control.Feedback>
        </Form.Group>
      </Row>

      {/* HÀNG 3: Địa chỉ + Trạm sạc */}
      <Row className="mb-3">
        <Form.Group as={Col} controlId="formBasicAddress">
          <Form.Label>Địa chỉ</Form.Label>
          <Form.Control
            type="text"
            placeholder="Nhập địa chỉ"
            name="formBasicAddress"
            value={formData.formBasicAddress}
            onChange={handleChange}
            onBlur={handleBlur}
            isInvalid={!!formErrors.formBasicAddress}
            required
          />
          <Form.Control.Feedback type="invalid">
            {formErrors.formBasicAddress}
          </Form.Control.Feedback>
        </Form.Group>

        <Form.Group as={Col} controlId="stationID">
          <Form.Label>Trạm sạc</Form.Label>
          <Form.Select
            name="stationID"
            value={formData.stationID}
            onChange={handleChange}
            onBlur={handleBlur}
            isInvalid={!!formErrors.stationID}
            required
          >
            <option value="" disabled>Chọn ...</option>
            {stations.map((station) => (
              <option key={station.stationId} value={station.stationId}>
                {station.stationName}
              </option>
            ))}
          </Form.Select>
          <Form.Control.Feedback type="invalid">
            {formErrors.stationID}
          </Form.Control.Feedback>
        </Form.Group>
      </Row>

      {/* HÀNG 4: Email */}
      <Form.Group className="mb-3" controlId="formBasicEmail">
        <Form.Label>Địa chỉ email</Form.Label>
        <Form.Control
          type="email"
          placeholder="Nhập email"
          name="formBasicEmail"
          value={formData.formBasicEmail}
          onChange={handleChange}
          onBlur={handleBlur}
          isInvalid={!!formErrors.formBasicEmail}
          required
        />
        <Form.Control.Feedback type="invalid">
          {formErrors.formBasicEmail}
        </Form.Control.Feedback>
      </Form.Group>

      {/* HÀNG 5: Mật khẩu + Nhập lại */}
      <Row className="mb-3">
        <Form.Group as={Col} controlId="formBasicPassword">
          <Form.Label>Mật khẩu</Form.Label>
          <Form.Control
            type="password"
            placeholder="Nhập mật khẩu"
            name="formBasicPassword"
            value={formData.formBasicPassword}
            onChange={handleChange}
            onBlur={handleBlur}
            isInvalid={!!formErrors.formBasicPassword}
            required
          />
          <Form.Control.Feedback type="invalid">
            {formErrors.formBasicPassword}
          </Form.Control.Feedback>
        </Form.Group>

        <Form.Group as={Col} controlId="formBasicPasswordConfirm">
          <Form.Label>Nhập lại mật khẩu</Form.Label>
          <Form.Control
            type="password"
            placeholder="Nhập lại mật khẩu"
            name="formBasicPasswordConfirm"
            value={formData.formBasicPasswordConfirm}
            onChange={handleChange}
            onBlur={handleBlur}
            isInvalid={!!formErrors.formBasicPasswordConfirm}
            required
          />
          <Form.Control.Feedback type="invalid">
            {formErrors.formBasicPasswordConfirm}
          </Form.Control.Feedback>
        </Form.Group>
      </Row>

      {/* NƠI HIỂN THỊ LỖI TỪ API (BACKEND) */}
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