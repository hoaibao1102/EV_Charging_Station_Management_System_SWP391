import "../../assets/css/login.css";
import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { ToastContainer, toast } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import { useRegister } from "../../hooks/useAuth"; // Thêm import này

const Register = () => {
  const [form, setForm] = useState({
    email: "",
    phoneNumber: "",
    password: "",
    confirmPassword: "",
    firstName: "",
    lastName: "",
    dateOfBirth: "",
    gender: "",
    address: "",
  });

  const { register, loading } = useRegister(); // Sử dụng custom hook đăng ký
  const navigate = useNavigate();

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const { success, message } = await register(form);

    if (success) {
      toast.success("Đăng ký thành công!");
      setTimeout(() => {
        navigate("/login");
      }, 2000);
    } else {
      toast.error(message || "Đăng ký thất bại!");
    }
  };

  return (
    <div className="login-page-wrapper">
      <ToastContainer position="top-center" autoClose={2500} theme="colored" />
      <div className="login-container">
        <div className="left-section">
          <h1>Đăng ký tài khoản</h1>
          <p>Vui lòng nhập thông tin để tạo tài khoản mới</p>
          <div className="illustration"></div>
        </div>
        <div className="right-section">
          <div className="right-content">
            <div className="form-header">
              <h2>Đăng ký</h2>
              <p>Tạo tài khoản mới</p>
            </div>
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label htmlFor="email">Email</label>
                <input
                  type="email"
                  id="email"
                  name="email"
                  placeholder="Nhập email"
                  value={form.email}
                  onChange={handleChange}
                />
              </div>
              <div className="form-group">
                <label htmlFor="phoneNumber">Số điện thoại</label>
                <input
                  type="tel"
                  id="phoneNumber"
                  name="phoneNumber"
                  placeholder="Nhập số điện thoại"
                  value={form.phoneNumber}
                  onChange={handleChange}
                />
              </div>
              <div className="form-group">
                <label htmlFor="firstName">Họ</label>
                <input
                  type="text"
                  id="firstName"
                  name="firstName"
                  placeholder="Nhập họ"
                  value={form.firstName}
                  onChange={handleChange}
                />
              </div>
              <div className="form-group">
                <label htmlFor="lastName">Tên</label>
                <input
                  type="text"
                  id="lastName"
                  name="lastName"
                  placeholder="Nhập tên"
                  value={form.lastName}
                  onChange={handleChange}
                />
              </div>
              <div className="form-group">
                <label htmlFor="dateOfBirth">Ngày sinh</label>
                <input
                  type="date"
                  id="dateOfBirth"
                  name="dateOfBirth"
                  value={form.dateOfBirth}
                  onChange={handleChange}
                />
              </div>
              <div className="form-group">
                <label htmlFor="gender">Giới tính</label>
                <input
                  type="text"
                  id="gender"
                  name="gender"
                  placeholder="Nam/Nữ/Khác"
                  value={form.gender}
                  onChange={handleChange}
                />
              </div>
              <div className="form-group">
                <label htmlFor="address">Địa chỉ</label>
                <input
                  type="text"
                  id="address"
                  name="address"
                  placeholder="Nhập địa chỉ"
                  value={form.address}
                  onChange={handleChange}
                />
              </div>
              <div className="form-group">
                <label htmlFor="password">Mật khẩu</label>
                <input
                  type="password"
                  id="password"
                  name="password"
                  placeholder="Nhập mật khẩu"
                  value={form.password}
                  onChange={handleChange}
                />
              </div>
              <div className="form-group">
                <label htmlFor="confirmPassword">Xác nhận mật khẩu</label>
                <input
                  type="password"
                  id="confirmPassword"
                  name="confirmPassword"
                  placeholder="Nhập lại mật khẩu"
                  value={form.confirmPassword}
                  onChange={handleChange}
                />
              </div>
              <button type="submit" className="login-btn" disabled={loading}>
                {loading ? "Đang đăng ký..." : "Đăng ký"}
              </button>
              <div className="register-link">
                <span
                  style={{ cursor: "pointer", color: "#007bff" }}
                  onClick={() => navigate("/login")}
                >
                  Đã có tài khoản? Đăng nhập
                </span>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Register;
