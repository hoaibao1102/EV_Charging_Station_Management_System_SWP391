import "../../assets/css/login.css";
import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { ToastContainer, toast } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";

const Register = () => {
  const [form, setForm] = useState({
    phone: "",
    password: "",
    confirmPassword: "",
  });
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const trimmedPhone = form.phone.trim();
    const trimmedPassword = form.password.trim();
    const trimmedConfirm = form.confirmPassword.trim();

    if (!trimmedPhone || !trimmedPassword || !trimmedConfirm) {
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

    // Chưa có API backend, giả lập đăng ký thành công
    setLoading(true);
    setTimeout(() => {
      toast.success("Đăng ký thành công! (giả lập)", {
        position: "top-center",
        autoClose: 2000,
        theme: "colored",
      });
      setLoading(false);
      setTimeout(() => {
        navigate("/login");
      }, 2000);
    }, 1200);
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
                <label htmlFor="phone">Số điện thoại</label>
                <input
                  type="tel"
                  id="phone"
                  name="phone"
                  placeholder="Nhập số điện thoại"
                  value={form.phone}
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
