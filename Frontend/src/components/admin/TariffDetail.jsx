import {toast} from 'react-toastify';
import Button from 'react-bootstrap/Button';
import Col from 'react-bootstrap/Col';
import Form from 'react-bootstrap/Form';
import Row from 'react-bootstrap/Row';
import Alert from 'react-bootstrap/Alert';
import { useState, useEffect } from 'react';
import './AddStaffForm.css';
import tariffIcon from '../../assets/icon/admin/best-price.png';
import {addTariffApi, updateTariffApi} from '../../api/tariffApi.js';

const initialFormData = {
  connectorTypeId: '',
  pricePerKWh: '',
  pricePerMin: '',
  currency: 'VND',
};

const initialFormErrors = {
  connectorTypeId: '',
  pricePerKWh: '',
  pricePerMin: '',
  currency: '',
};

export default function TariffDetail({ handleClose, tariff, inactiveConnectorTypes }) {
  const [formData, setFormData] = useState(initialFormData);
  const [formErrors, setFormErrors] = useState(initialFormErrors);
  const [apiError, setApiError] = useState('');
  const connectorTypes = inactiveConnectorTypes || [];
  const tariffReady = tariff || null;

  useEffect(() => {
    if (tariffReady) {
      setFormData({
        connectorTypeId: tariffReady.connectorTypeId,
        pricePerKWh: tariffReady.pricePerKWh,
        pricePerMin: tariffReady.pricePerMin,
        currency: tariffReady.currency,
      });
    } else {
      setFormData(initialFormData);
    }
  }, [tariffReady]);

  const validateField = (name, value) => {
    switch (name) {
      case 'connectorTypeId':
        return value.toString().trim() ? '' : 'Vui lòng chọn loại cổng sạc.';
      case 'pricePerKWh':
        if (!value.toString().trim()) return 'Vui lòng nhập giá cho mỗi kWh.';
        if (Number(value) <= 500) return 'Giá cho mỗi kWh phải lớn hơn 500vnd.';
        return '';
      case 'pricePerMin':
        if (!value.toString().trim()) return 'Vui lòng nhập giá cho mỗi phút.';
        if (Number(value) <= 500) return 'Giá cho mỗi phút phải lớn hơn 500vnd.';
        return '';
      case 'currency':
        return value.trim() ? '' : 'Vui lòng chọn loại tiền tệ.';
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

  const nextYear = new Date();
  nextYear.setFullYear(nextYear.getFullYear() + 1);

  const handleUpdate = async (event) => {
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

    try {
      const modelData = {
        connectorTypeId: formData.connectorTypeId,
        pricePerKWh: formData.pricePerKWh,
        pricePerMin: formData.pricePerMin,
        currency: formData.currency,
        effectiveFrom: new Date().toISOString(),
        effectiveTo: nextYear.toISOString(),
      };

      console.log("Dữ liệu gửi lên API để cập nhật:", modelData);

      const result = await updateTariffApi(tariffReady.tariffId, modelData);
      if (result.success) {
        toast.success('Cập nhật cấu hình giá thành công!');
        handleClose();
      } else {
        toast.error(result.message || 'Cập nhật cấu hình giá thất bại.');
        setApiError(result.message || 'Cập nhật cấu hình giá thất bại.');
      }
    } catch (error) {
      console.error("Lỗi hệ thống khi cập nhật cấu hình giá:", error);
      setApiError('Lỗi hệ thống. Vui lòng thử lại sau.');
    }
  }

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

    const nextYear = new Date();
    nextYear.setFullYear(nextYear.getFullYear() + 1);

    const tariffData = {
      connectorTypeId: formData.connectorTypeId,
      pricePerKWh: formData.pricePerKWh,
      pricePerMin: formData.pricePerMin,
      currency: formData.currency,
      effectiveFrom: new Date().toISOString(),
      effectiveTo: nextYear.toISOString(),
    };
    try {
      const result = await addTariffApi(tariffData);
      if (result.success) {
        toast.success('Thêm cấu hình giá thành công!');
        console.log("Thêm cấu hình giá thành công:", result.data);
        handleClose();
      } else {
        toast.error(result.message || 'Thêm cấuHình giá thất bại.');
        setApiError(result.message || 'Thêm cấu hình giá thất bại.');
      }
    } catch (error) {
      console.error("Lỗi hệ thống khi thêm cấu hình giá:", error);
      setApiError('Lỗi hệ thống. Vui lòng thử lại sau.');
    }
  };

  return (
    <Form noValidate onSubmit={tariffReady ? handleUpdate : handleSubmit} className="add-staff-form">
      <img src={tariffIcon} alt="Add Tariff" className="staff-icon" /> <br />
      <Row className="mb-3">
        <Form.Group className="mb-3" controlId="connectorTypeId">
          <Form.Label>Loại cổng sạc tương ứng</Form.Label>
          <Form.Select
            name="connectorTypeId"
            value={formData.connectorTypeId}
            onChange={handleChange}
            onBlur={handleBlur}
            isInvalid={!!formErrors.connectorTypeId}
            disabled={!!tariffReady}
            required
          >
            {tariffReady ? (
              <option value={tariffReady.connectorTypeId}>
                {tariffReady.connectorTypeName}
              </option>
            ) : (
              <>
                <option value="" disabled>Chọn cổng sạc...</option>
                {connectorTypes.map((type) => (
                  <option
                    key={type.connectorTypeId}
                    value={type.connectorTypeId}
                  >
                    {type.displayName}
                  </option>
                ))}
              </>
            )}
          </Form.Select>
          <Form.Control.Feedback type="invalid">
            {formErrors.connectorTypeId}
          </Form.Control.Feedback>
        </Form.Group>
      </Row>
      <Row className="mb-3">
        <Form.Group as={Col} controlId="pricePerKWh">
          <Form.Label>Giá điện (kWh)</Form.Label>
          <Form.Control
            type="number"
            placeholder="Nhập giá điện (kWh)"
            name="pricePerKWh"
            value={formData.pricePerKWh}
            onChange={handleChange}
            onBlur={handleBlur}
            isInvalid={!!formErrors.pricePerKWh}
            required
          />
          <Form.Control.Feedback type="invalid">
            {formErrors.pricePerKWh}
          </Form.Control.Feedback>
        </Form.Group>
        <Form.Group as={Col} controlId="pricePerMin">
          <Form.Label>Giá điện (phút)</Form.Label>
          <Form.Control
            type="number"
            placeholder="Nhập giá điện (phút)"
            name="pricePerMin"
            value={formData.pricePerMin}
            onChange={handleChange}
            onBlur={handleBlur}
            isInvalid={!!formErrors.pricePerMin}
            required
          />
          <Form.Control.Feedback type="invalid">
            {formErrors.pricePerMin}
          </Form.Control.Feedback>
        </Form.Group>
        <Form.Group as={Col} controlId="currency">
          <Form.Label>Loại tiền tệ</Form.Label>
          <Form.Control
            type="text"
            placeholder="Loại tiền tệ"
            name="currency"
            value={formData.currency}
            onChange={handleChange}
            onBlur={handleBlur}
            isInvalid={!!formErrors.currency}
            required
            readOnly
          />
          <Form.Control.Feedback type="invalid">
            {formErrors.currency}
          </Form.Control.Feedback>
        </Form.Group>
      </Row>

      {apiError && (
        <Alert variant="danger" className="mt-3">
          {apiError}
        </Alert>
      )}

      <div className="form-button-group mt-3">
        <Button variant="primary" type="submit" className="me-2">
          {tariffReady ? 'Cập nhật' : 'Tạo mới'}
        </Button>
        <Button variant="primary" type="button" className="me-2" onClick={handleClose}>
          Trở về
        </Button>
      </div>
    </Form>
  );
}