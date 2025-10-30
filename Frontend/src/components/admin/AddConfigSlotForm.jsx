import Button from 'react-bootstrap/Button';
import Form from 'react-bootstrap/Form';
import { addSlotConfigApi } from '../../api/stationApi.js';
import {toast} from 'react-toastify';
import './AddStaffForm.css';

export default function AddConfigSlotForm({handleClose, stationId, slotMinutes}) {
    const date = new Date();
    const dateEnd = new Date();
    dateEnd.setFullYear(dateEnd.getFullYear() + 1);
    
    const handleCreateSlotConfig = (e) => {
        e.preventDefault();
        const form = e.target;
        const slotDurationMin = form.configSlot.value;
        const dataForm = {
            stationId: stationId,
            slotDurationMin: parseInt(slotDurationMin) || 60,
            activeFrom: date.toISOString(),
            activeExpire: dateEnd.toISOString(),  
            isActive: "ACTIVE",
        };
        console.log("Dữ liệu gửi lên API:", dataForm);
        const createSlotConfig = async () => {
            try {
                const response = await addSlotConfigApi(dataForm);
                if (response.success) {
                    toast.success('Tạo cấu hình slot thành công!');
                    handleClose();
                } else {
                    toast.error('Tạo cấu hình slot thất bại: ' + response.message);
                }
                
            } catch (error) {
                console.error("Lỗi khi tạo cấu hình slot:", error);
            }
        };
        createSlotConfig();
    };
    return (
    <>
      <Form onSubmit={handleCreateSlotConfig} className="add-staff-form">
      <Form.Group className="mb-3" controlId="configSlot">
        <Form.Label>Thời gian slot (phút)</Form.Label>
        <Form.Control 
          type="number" 
          placeholder="Nhập thời gian slot" 
          min={60} 
          max={120} 
          defaultValue={slotMinutes || 60}
          required
        />
        <Form.Text className="text-muted">
          Thời gian slot được tính bằng phút và sẽ áp dụng cho trạm sạc này ngay lập tức sau khi cấu hình.
        </Form.Text>
      </Form.Group>

      <div className="form-button-group mt-3">
        <Button variant="primary" type="submit" className="me-2">
          Cấu hình
        </Button>
        <Button variant="primary" type="button" className="me-2" onClick={handleClose}>
          Trở về
        </Button>
      </div>
    </Form>
    </>
  );
}