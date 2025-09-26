import "../../assets/css/login.css";
import React, { useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import { ToastContainer, toast } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";

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
    address: ""
  });
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
  setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const trimmedPhone = form.phoneNumber.trim();
    const trimmedPassword = form.password.trim();
    const trimmedConfirm = form.confirmPassword.trim();
    const trimmedEmail = form.email.trim();
    const trimmedFirstName = form.firstName.trim();
    const trimmedLastName = form.lastName.trim();
    const trimmedDOB = form.dateOfBirth.trim();
    const trimmedGender = form.gender.trim();
    const trimmedAddress = form.address.trim();

    if (!trimmedPhone || !trimmedPassword || !trimmedConfirm || !trimmedEmail || !trimmedFirstName || !trimmedLastName || !trimmedDOB || !trimmedGender || !trimmedAddress) {
      toast.error("Vui lòng nhập đầy đủ thông tin!", {
        position: "top-center",
        autoClose: 2500,
        theme: "colored",
      });
      return;
    }
    const phoneRegex = /^0[0-9]{9}$/;
    if (!phoneRegex.test(trimmedPhone)) {
      toast.error("Số điện thoại không hợp lệ!", {
        position: "top-center",
        autoClose: 2500,
        theme: "colored",
      });
      return;
    }
    if (trimmedPassword.length < 6) {
      toast.error("Mật khẩu phải có ít nhất 6 ký tự!", {
        position: "top-center",
        autoClose: 2500,
        theme: "colored",
      });
      return;
    }
    if (trimmedPassword !== trimmedConfirm) {
      toast.error("Mật khẩu xác nhận không khớp!", {
        position: "top-center",
        autoClose: 2500,
        theme: "colored",
      });
      return;
    }

    setLoading(true);
    try {
      const payload = {
        email: trimmedEmail,
        phoneNumber: trimmedPhone,
        password: trimmedPassword,
        firstName: trimmedFirstName,
        lastName: trimmedLastName,
        dateOfBirth: trimmedDOB,
        gender: trimmedGender,
        address: trimmedAddress,
        roleId: 3
      };
      const response = await axios.post("http://localhost:8080/api/users/register", payload, {
        headers: { "Content-Type": "application/json" },
        withCredentials: true
      });
      if (response.data && response.data.message && response.data.message.toLowerCase().includes("thành công")) {
        toast.success("Đăng ký thành công!", {
          position: "top-center",
          autoClose: 2000,
          theme: "colored",
        });
        setTimeout(() => {
          navigate("/login");
        }, 2000);
      } else {
        toast.error(response.data.message || "Đăng ký thất bại!", {
          position: "top-center",
          autoClose: 2500,
          theme: "colored",
        });
      }
    } catch (error: any) {
      if (error.response && error.response.data && error.response.data.message) {
        toast.error(error.response.data.message, {
          position: "top-center",
          autoClose: 2500,
          theme: "colored",
        });
      } else {
        toast.error("Lỗi kết nối đến server!", {
          position: "top-center",
          autoClose: 2500,
          theme: "colored",
        });
      }
    } finally {
      setLoading(false);
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
