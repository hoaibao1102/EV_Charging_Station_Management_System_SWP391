import { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { updateProfileApi } from "../../api/driverApi.js";
import { toast } from "react-toastify";
import { Form, Button } from "react-bootstrap";
import './EditProfile.css';

export default function EditProfile() {
    const navigate = useNavigate(); 
    const location = useLocation();
    const [form, setForm] = useState(location.state?.profile ? location.state.profile : {
        email: "",
        phoneNumber: "",
        name: "",
        address: "",
    });

    const handleChange = (e) => {
        setForm({
            ...form,
            [e.target.name]: e.target.value, 
        });
    };

    // Hàm xử lý submit Form
    const handleSubmit = async (e) => {
        e.preventDefault(); 
        
        try {
            const response = await updateProfileApi(form);
            if (response.success) {
                toast.success("Cập nhật thông tin cá nhân thành công!"); 
                navigate('/profile/information');
            } else {
                toast.error("Cập nhật thông tin cá nhân thất bại: " + response.message);
            }
        } catch (error) {
            toast.error(error.message || "Đã xảy ra lỗi trong quá trình cập nhật thông tin cá nhân");
        }
    };

    return (
        <div className="edit-profile-container">
            <h2>Chỉnh sửa thông tin cá nhân của bạn!</h2>

            <Form onSubmit={handleSubmit}> 
                
                <Form.Group className="mb-3" controlId="formBasicName">
                    <Form.Label>Tên</Form.Label>
                    <Form.Control 
                        type="text" 
                        placeholder="Enter name" 
                        name="name" 
                        value={form.name} 
                        onChange={handleChange} 
                    />
                </Form.Group>

                <Form.Group className="mb-3" controlId="formBasicPhone">
                    <Form.Label>Số điện thoại</Form.Label>
                    <Form.Control 
                        type="text" 
                        placeholder="Enter phone number" 
                        name="phoneNumber" 
                        value={form.phoneNumber} 
                        onChange={handleChange} 
                    />
                </Form.Group>

                <Form.Group className="mb-3" controlId="formBasicEmail">
                    <Form.Label>Email</Form.Label>
                    <Form.Control 
                        type="email" 
                        placeholder="Enter email" 
                        name="email"
                        value={form.email} 
                        onChange={handleChange} 
                    />
                </Form.Group>

                <Form.Group className="mb-3" controlId="formBasicAddress">
                    <Form.Label>Địa chỉ</Form.Label>
                    <Form.Control 
                        type="text" 
                        placeholder="Enter address" 
                        name="address" 
                        value={form.address} 
                        onChange={handleChange} 
                    />
                </Form.Group>

                <div><strong>Vui lòng nhập đúng mail vì lý do bảo mật và tiện ích!</strong></div>

                <Button variant="primary" type="submit"> 
                    CẬP NHẬT
                </Button>
            </Form>
        </div>
    )
}