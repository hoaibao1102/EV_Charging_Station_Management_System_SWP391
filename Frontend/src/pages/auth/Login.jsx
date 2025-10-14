import "./login.css";
import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { ToastContainer, toast } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import { useLogin } from "../../hooks/useAuth";
import classed from "../Main.module.css";

const Login = () => {
  const [form, setForm] = useState({
    phone: "",
    password: "",
    rememberMe: false,
  });
  const [showPassword, setShowPassword] = useState(false);

  const { login, loading } = useLogin();
  const navigate = useNavigate();

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm({ 
      ...form, 
      [name]: type === 'checkbox' ? checked : value 
    });
  };

  const togglePasswordVisibility = () => {
    setShowPassword(!showPassword);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const { success, message } = await login(form.phone, form.password);

    if (success) {
      toast.success("Đăng nhập thành công!");
      setTimeout(() => {
        navigate("/");
      }, 2000);
    } else {
      toast.error(message || "Đăng nhập thất bại!");
    }
  };

  return (
    <div className="login-page-wrapper">
      <ToastContainer position="top-center" autoClose={2500} theme="colored" />
      <div className="login-container">
        {/* Left Section */}
        <div className="left-section">
          <div className="left-content">
            <h1>Chào mừng trở lại</h1>
            <div className="divider"></div>
            <hr style={{color:"white"}}></hr>
            <p style={{color:"white"}}>Để giữ liên lạc với chúng tôi, vui lòng đăng nhập vào hệ thống.</p>
          </div>
        </div>

        {/* Right Section */}
        <div className="right-section">
          <div className="form-container">
            {/* Header with Icon */}
            <div className="form-header">
              <div className="logo-icon">
                <div className="car-icon">
                  <div className="lightning">⚡</div>
                  <p style={{ color: "black", fontWeight: "bold" }}>Không chỉ đơn giản là làm việc, mà còn là kiến tạo nên giá trị</p>
                </div>
              </div>
              <h2>Đăng Nhập</h2>
            </div>

            {/* Login Form */}
            <form onSubmit={handleSubmit} className="login-form">
              <div className="form-group">
                <div className="input-container">
                  <div className="input-icon">�</div>
                  <input
                    className={classed.input}
                    type="tel"
                    name="phone"
                    placeholder="0** *** ****"
                    value={form.phone}
                    onChange={handleChange}
                    required
                    autoComplete="username"
                    style={{
                      color: '#333',
                      fontSize: '16px'
                    }}
                  />
                </div>
              </div>

              <div className="form-group">
                <div className="input-container">
                  <div className="input-icon">🔒</div>
                  <input
                    className={classed.input}
                    type={showPassword ? "text" : "password"}
                    name="password"
                    placeholder="Mật khẩu"
                    value={form.password}
                    onChange={handleChange}
                    required
                    autoComplete="new-password"
                  />
                  <div className="eye-icon" onClick={togglePasswordVisibility}>
                    {showPassword ? "🙈" : "👁"}
                  </div>
                </div>
              </div>

              <div className="form-options">
                <label style={{color:"black"}} className="remember-me">
                  <input
                    type="checkbox"
                    name="rememberMe"
                    checked={form.rememberMe}
                    onChange={handleChange}
                  />
                  <span className="checkmark"></span>
                  Remember me   
                </label>
                <a href="#" className="forgot-password">Quên mật khẩu?</a>
              </div>

              <button type="submit" className={classed.button} disabled={loading}>
                {loading ? "Đang đăng nhập..." : "Đăng nhập"}
              </button>

              <div className="register-link">
                <span>Chưa có tài khoản? </span>
                <span 
                  className="register-text"
                  onClick={() => navigate("/register")}
                >
                  Đăng ký tài khoản
                </span>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Login;