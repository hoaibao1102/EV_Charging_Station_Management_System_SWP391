import { Modal, Button } from "react-bootstrap";
import Form from 'react-bootstrap/Form';
import { useState } from "react";
import { updateAdminPasswordApi } from '../../api/admin.js';
import { updateStaffPasswordApi } from '../../api/staffApi.js';
import { toast } from "react-toastify";
import {changePasswordDriverApi} from '../../api/driverApi.js';

const dataInitial = {
    oldPassword: '',
    newPassword: '',
    confirmPassword: '',
};

export default function FormProfile({ onClose }) {
    const role = localStorage.getItem('role') || null;
    const [data, setData] = useState(dataInitial);
    const [errors, setErrors] = useState({});
    const [showOldPassword, setShowOldPassword] = useState(false);
    const [showNewPassword, setShowNewPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);

    // Hàm cập nhật state chung
    const handleChange = (e) => {
        const { name, value } = e.target;
        setData({
            ...data,
            [name]: value
        });
        
        // Xóa lỗi khi người dùng bắt đầu gõ
        if (errors[name]) {
            setErrors({
                ...errors,
                [name]: null
            });
        }
    };

    // Hàm validate dữ liệu
    const validate = () => {
        const newErrors = {};

        if (!data.oldPassword) {
            newErrors.oldPassword = 'Vui lòng nhập mật khẩu cũ.';
        }
        if (!data.newPassword) {
            newErrors.newPassword = 'Vui lòng nhập mật khẩu mới.';
        } else if (data.newPassword.length < 6) {
            newErrors.newPassword = 'Mật khẩu mới phải có ít nhất 6 ký tự.';
        }
        if (!data.confirmPassword) {
            newErrors.confirmPassword = 'Vui lòng xác nhận mật khẩu mới.';
        } else if (data.newPassword && data.newPassword !== data.confirmPassword) {
            newErrors.confirmPassword = 'Mật khẩu xác nhận không khớp.';
        }

        setErrors(newErrors);
        // Trả về true nếu không có lỗi
        return Object.keys(newErrors).length === 0;
    };

    const handleUpdate = async () => {
        // Bước 1: Validate
        if (!validate()) {
            toast.warning('Vui lòng kiểm tra lại thông tin.');
            return;
        }

        // Bước 2: Chọn API và tạo payload
        const apiToCall = role === 'ADMIN' ? updateAdminPasswordApi : role === 'STAFF' ? updateStaffPasswordApi : role === 'DRIVER' ? changePasswordDriverApi : changePasswordDriverApi;
        
        const payload = data;
        console.log("data", payload)
        
        // Bước 3: Gọi API
        try {
            const result = await apiToCall(payload);
            
            // Kiểm tra success từ handleApiCall
            if (result.success) {
                toast.success('Cập nhật mật khẩu thành công!');
                onClose();
            } else {
                // Hiển thị lỗi từ server
                const errorMessage = result.message || 'Cập nhật mật khẩu thất bại!';
                toast.error(errorMessage);
            }
        } catch (error) {
            // Fallback error handling
            const errorMessage = error.response?.data?.message || 'Cập nhật mật khẩu thất bại!';
            toast.error(errorMessage);
        }
    }

    return (
        <>
            <Modal show={true} onHide={onClose} backdrop="static" keyboard={false}>
                
                <Modal.Header 
                    style={{
                        background: 'linear-gradient(135deg, #20b2aa 0%, #1a8f89 100%)',
                        color: 'white',
                        borderBottom: '3px solid white',
                        padding: '16px 20px',
                        position: 'relative',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center'
                    }}
                >
                    <Button 
                        variant="light"
                        onClick={onClose}
                        style={{
                            position: 'absolute',
                            left: '16px',
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '6px',
                            padding: '6px 12px',
                            borderRadius: '8px',
                            fontWeight: '600',
                            fontSize: '13px',
                            background: 'white',
                            border: 'none',
                            color: '#20b2aa',
                            boxShadow: '0 2px 8px rgba(0,0,0,0.15)'
                        }}
                    >
                        <span style={{ fontSize: '16px' }}>←</span>
                        Quay về
                    </Button>
                    <Modal.Title style={{ 
                        margin: 0, 
                        fontSize: '18px', 
                        fontWeight: '700',
                        color: 'white',
                        textAlign: 'center'
                    }}>
                        Đổi mật khẩu
                    </Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    {/* Old Password */}
                    <Form.Group className="mb-3" controlId="formOldPassword">
                        <Form.Label>Mật khẩu cũ</Form.Label>
                        <div style={{ position: 'relative' }}>
                            <Form.Control
                                type={showOldPassword ? "text" : "password"}
                                name="oldPassword"
                                value={data.oldPassword}
                                onChange={handleChange}
                                isInvalid={!!errors.oldPassword}
                                autoComplete="current-password"
                                style={{ paddingRight: '40px' }}
                            />
                            <Button
                                variant="link"
                                onClick={() => setShowOldPassword(!showOldPassword)}
                                style={{
                                    position: 'absolute',
                                    right: '5px',
                                    top: '50%',
                                    transform: 'translateY(-50%)',
                                    padding: '4px 8px',
                                    fontSize: '18px',
                                    color: '#6c757d',
                                    textDecoration: 'none'
                                }}
                            >
                                {showOldPassword ? '👁️' : '👁️‍🗨️'}
                            </Button>
                        </div>
                        <Form.Control.Feedback type="invalid">
                            {errors.oldPassword}
                        </Form.Control.Feedback>
                    </Form.Group>

                    {/* New Password */}
                    <Form.Group className="mb-3" controlId="formNewPassword">
                        <Form.Label>Mật khẩu mới</Form.Label>
                        <div style={{ position: 'relative' }}>
                            <Form.Control
                                type={showNewPassword ? "text" : "password"}
                                name="newPassword"
                                value={data.newPassword}
                                onChange={handleChange}
                                isInvalid={!!errors.newPassword}
                                aria-describedby="passwordHelpBlock"
                                autoComplete="new-password"
                                style={{ paddingRight: '40px' }}
                            />
                            <Button
                                variant="link"
                                onClick={() => setShowNewPassword(!showNewPassword)}
                                style={{
                                    position: 'absolute',
                                    right: '5px',
                                    top: '50%',
                                    transform: 'translateY(-50%)',
                                    padding: '4px 8px',
                                    fontSize: '18px',
                                    color: '#6c757d',
                                    textDecoration: 'none'
                                }}
                            >
                                {showNewPassword ? '👁️' : '👁️‍🗨️'}
                            </Button>
                        </div>
                        <Form.Text id="passwordHelpBlock" muted>
                            Mật khẩu của bạn phải có ít nhất 6 ký tự.
                        </Form.Text>
                        <Form.Control.Feedback type="invalid">
                            {errors.newPassword}
                        </Form.Control.Feedback>
                    </Form.Group>

                    {/* Confirm New Password */}
                    <Form.Group className="mb-3" controlId="formConfirmPassword">
                        <Form.Label>Xác nhận mật khẩu mới</Form.Label>
                        <div style={{ position: 'relative' }}>
                            <Form.Control
                                type={showConfirmPassword ? "text" : "password"}
                                name="confirmPassword"
                                value={data.confirmPassword}
                                onChange={handleChange}
                                isInvalid={!!errors.confirmPassword}
                                autoComplete="new-password"
                                style={{ paddingRight: '40px' }}
                            />
                            <Button
                                variant="link"
                                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                                style={{
                                    position: 'absolute',
                                    right: '5px',
                                    top: '50%',
                                    transform: 'translateY(-50%)',
                                    padding: '4px 8px',
                                    fontSize: '18px',
                                    color: '#6c757d',
                                    textDecoration: 'none'
                                }}
                            >
                                {showConfirmPassword ? '👁️' : '👁️‍🗨️'}
                            </Button>
                        </div>
                        <Form.Control.Feedback type="invalid">
                            {errors.confirmPassword}
                        </Form.Control.Feedback>
                    </Form.Group>
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="primary" onClick={handleUpdate}>
                        Lưu
                    </Button>
                </Modal.Footer>
            </Modal>
        </>
    )
}