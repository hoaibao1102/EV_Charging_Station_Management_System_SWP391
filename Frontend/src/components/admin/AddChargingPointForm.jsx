import { useEffect, useState } from 'react';
import { addChargingPointApi } from '../../api/chargingPointApi.js';
import { getAllStations } from '../../api/stationApi.js';
import { getConnectorTypes } from '../../api/stationApi.js';
import chargingPointIcon from '../../assets/icon/admin/charging-building.png';
import { Form, Button, Row, Col } from 'react-bootstrap';

const statusChargingPoint = {
    available: 'AVAILABLE',
    out_of_service: 'OUT_OF_SERVICE',
    maintenance: 'MAINTENANCE'
};

export default function AddChargingPointForm({ onClose }) {
    const [stations, setStations] = useState([]);
    const [connectors, setConnectors] = useState([]);

    const [formData, setFormData] = useState({
        stationId: '',
        connectorTypeId: '',
        pointNumber: '',
        serialNumber: '',
        maxPowerKW: 1, 
        status: statusChargingPoint.available, 
    });
    const [errors, setErrors] = useState({});
   

    useEffect(() => {
        const fetchStations = async () => {
            try {
                const response = await getAllStations();
                if (response.success) {
                    setStations(response.data);
                    console.log('Fetched stations:', response.data);
                }
            } catch (error) {
                console.error('Error fetching stations:', error);
            }
        };
        const fetchConnectors = async () => {
            try {
                const response = await getConnectorTypes();
                if (response.success) {
                    setConnectors(response.data);
                    console.log('Fetched connectors:', response.data);
                }
            } catch (error) {
                console.error('Error fetching connectors:', error);
            }
        };

        fetchStations();
        fetchConnectors();
    }, []);


    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prevData => ({
            ...prevData,
            [name]: value
        }));
        
        if (errors[name]) {
            setErrors(prevErrors => ({
                ...prevErrors,
                [name]: null
            }));
        }
    };


    const handleBlur = (e) => {
        const { name, value } = e.target;
        if (!value) {
            setErrors(prevErrors => ({
                ...prevErrors,
                [name]: 'Trường này là bắt buộc.'
            }));
        }
    };

    const validateForm = () => {
        const newErrors = {};
        if (!formData.pointNumber) newErrors.pointNumber = 'Mã trụ sạc là bắt buộc.';
        if (!formData.serialNumber) newErrors.serialNumber = 'Mã serial là bắt buộc.';
        if (!formData.stationId) newErrors.stationId = 'Vui lòng chọn trạm.';
        if (!formData.connectorTypeId) newErrors.connectorTypeId = 'Vui lòng chọn cổng sạc.';
        if (formData.maxPowerKW <= 0) newErrors.maxPowerKW = 'Năng lượng phải lớn hơn 0.';
        if (formData.maxPowerKW > 350) newErrors.maxPowerKW = 'Năng lượng phải nhỏ hơn hoặc bằng 350.';
        return newErrors;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        
        const newErrors = validateForm();
        if (Object.keys(newErrors).length > 0) {
            setErrors(newErrors);
            return;
        }

        const payload = {
            stationId: parseInt(formData.stationId, 10), 
            connectorTypeId: parseInt(formData.connectorTypeId, 10), 
            pointNumber: formData.pointNumber,
            serialNumber: formData.serialNumber,
            maxPowerKW: parseFloat(formData.maxPowerKW), 
            status: formData.status,
        };

        console.log("Sending payload:", payload);

        try {
            const response = await addChargingPointApi(payload);
            if (response.success) {
                alert('Thêm trụ sạc thành công!');
                onClose(); 
            } else {
                alert('Thêm thất bại: ' + (response.message || 'Lỗi không xác định'));
                setErrors({ api: response.message || 'Lỗi không xác định' });
            }
        } catch (error) {
            console.error('Error submitting form:', error);
            alert('Đã xảy ra lỗi khi gửi form.');
            setErrors({ api: 'Đã xảy ra lỗi, vui lòng thử lại.' });
        }
    };

    return (
        <Form noValidate onSubmit={handleSubmit} className="add-staff-form">

            <img src={chargingPointIcon} alt="ChargingPoint" className="staff-icon" /> <br />

            <Row className="mb-3">
                <Form.Group as={Col} controlId="pointNumber">
                    <Form.Label>Mã trụ sạc</Form.Label>
                    <Form.Control
                        type="text"
                        placeholder="Nhập mã trụ sạc"
                        name="pointNumber"
                        value={formData.pointNumber} 
                        onChange={handleChange}
                        onBlur={handleBlur}
                        required
                        isInvalid={!!errors.pointNumber}
                    />
                    <Form.Control.Feedback type="invalid">
                        {errors.pointNumber}
                    </Form.Control.Feedback>
                </Form.Group>

                <Form.Group as={Col} controlId="serialNumber">
                    <Form.Label>Mã serial</Form.Label>
                    <Form.Control
                        type="text"
                        placeholder="Nhập mã serial"
                        name="serialNumber"
                        value={formData.serialNumber} 
                        onChange={handleChange} 
                        onBlur={handleBlur}
                        required
                        isInvalid={!!errors.serialNumber}
                    />
                    <Form.Control.Feedback type="invalid">
                        {errors.serialNumber}
                    </Form.Control.Feedback>
                </Form.Group>
            </Row>


            <Row className="mb-3">
                <Form.Group as={Col} controlId="stationId">
                    <Form.Label>Chọn trạm</Form.Label>
                    <Form.Control
                        as="select"
                        name="stationId" 
                        value={formData.stationId} 
                        onChange={handleChange} 
                        onBlur={handleBlur}
                        required
                        isInvalid={!!errors.stationId}
                    >
                        <option value="" disabled>Chọn trạm...</option>
                        {stations.map(station => (
                            <option key={station.stationId} value={station.stationId}>
                                {station.stationName}
                            </option>
                        ))}
                    </Form.Control>
                    <Form.Control.Feedback type="invalid">
                        {errors.stationId}
                    </Form.Control.Feedback>
                </Form.Group>

                <Form.Group as={Col} controlId="connectorTypeId"> 
                    <Form.Label>Chọn cổng sạc</Form.Label>
                    <Form.Control
                        as="select"
                        name="connectorTypeId" 
                        value={formData.connectorTypeId} 
                        onChange={handleChange} 
                        onBlur={handleBlur}
                        required
                        isInvalid={!!errors.connectorTypeId}
                    >
                        <option value="" disabled>Chọn cổng sạc...</option>
                        {connectors.map(connector => (
                            <option key={connector.connectorTypeId} value={connector.connectorTypeId}>
                                {connector.displayName}
                            </option>
                        ))}
                    </Form.Control>
                    <Form.Control.Feedback type="invalid">
                        {errors.connectorTypeId}
                    </Form.Control.Feedback>
                </Form.Group>
            </Row>

            <Form.Group className="mb-3" controlId="maxPowerKW"> 
                <Form.Label>Năng lượng tối đa (kW)</Form.Label>
                <Form.Control
                    type="number"
                    placeholder="Nhập năng lượng tối đa"
                    name="maxPowerKW" 
                    value={formData.maxPowerKW} 
                    onChange={handleChange} 
                    onBlur={handleBlur}
                    min={1}
                    max={350}
                    required
                    isInvalid={!!errors.maxPowerKW} 
                />
                <Form.Control.Feedback type="invalid">
                    {errors.maxPowerKW}
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
                    required
                >
                    {Object.entries(statusChargingPoint).map(([key, value]) => (
                        <option key={key} value={value}>
                            {value === 'AVAILABLE' ? 'Sẵn sàng' : value === 'OUT_OF_SERVICE' ? 'Ngưng hoạt động' : 'Bảo trì'}
                        </option>
                    ))}
                </Form.Select>
            </Form.Group>

            <div className="form-button-group mt-3">
                <Button variant="primary" type="submit" className="me-2">
                    Tạo mới
                </Button>
                <Button variant="primary" type="button" className="me-2" onClick={onClose}>
                    Trở về
                </Button>
            </div>
        </Form>
    )
}