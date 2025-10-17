import "./Login.css";
import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { ToastContainer, toast } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import { useLogin } from "../../hooks/useAuth";
<<<<<<< HEAD
=======
import classed from "../../assets/css/Main.module.css";
>>>>>>> bc3ed5eec34a391b0e6abb0dee90fe315485843e

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
      [name]: type === "checkbox" ? checked : value,
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
    <div className="auth-page">
      <ToastContainer position="top-center" autoClose={2500} theme="colored" />

      {/* Desktop Welcome Section (Hidden on Mobile) */}
      <div className="auth-welcome-section">
        <div className="auth-welcome-content">
          <h1 className="auth-welcome-title">
            Chào mừng
            <br />
            trở lại
          </h1>
          <div className="auth-welcome-divider"></div>
          <p className="auth-welcome-text">
            Để giữ liên lạc với chúng tôi, vui lòng đăng nhập vào hệ thống.
          </p>
          <div className="auth-welcome-icon">
            <svg
              viewBox="0 0 100 100"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
            >
              <circle
                cx="50"
                cy="50"
                r="40"
                stroke="white"
                strokeWidth="3"
                fill="rgba(255,255,255,0.1)"
              />
              <path
                d="M35 45 L50 60 L65 45"
                stroke="white"
                strokeWidth="4"
                strokeLinecap="round"
                strokeLinejoin="round"
                transform="rotate(-90 50 50)"
              />
            </svg>
          </div>
        </div>
      </div>

      <div className="auth-container">
        {/* Logo Section */}
        <div className="auth-logo">
          <div className="auth-logo-icon auth-logo-car">
            <svg
              viewBox="0 0 100 100"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
            >
              {/* Car body */}
              <rect
                x="20"
                y="45"
                width="60"
                height="25"
                rx="8"
                fill="white"
                fillOpacity="0.9"
              />
              {/* Windows */}
              <rect
                x="30"
                y="35"
                width="18"
                height="15"
                rx="3"
                fill="white"
                fillOpacity="0.7"
              />
              <rect
                x="52"
                y="35"
                width="18"
                height="15"
                rx="3"
                fill="white"
                fillOpacity="0.7"
              />
              {/* Wheels */}
              <circle cx="32" cy="72" r="8" fill="white" />
              <circle cx="68" cy="72" r="8" fill="white" />
              {/* Lightning bolt */}
              <path
                d="M50 50 L45 60 L52 60 L47 70 L55 58 L50 58 Z"
                fill="#FFD700"
              />
            </svg>
          </div>
          <h1 className="auth-title">Đăng Nhập</h1>
          <p className="auth-subtitle">
            Không chỉ đơn giản là <span className="highlight">làm việc</span>,
            mà còn là kiến tạo nên <span className="highlight">giá trị</span>
          </p>
        </div>

        {/* Login Form */}
        <form onSubmit={handleSubmit} className="auth-form">
          {/* Phone Input */}
          <div className="auth-input-group">
            <div className="auth-input-wrapper">
              <span className="auth-input-icon">👤</span>
              <input
                type="tel"
                name="phone"
                placeholder="Email hoặc số điện thoại"
                value={form.phone}
                onChange={handleChange}
                className="auth-input"
                required
                autoComplete="username"
              />
            </div>

            {/* Login Form */}
            <form onSubmit={handleSubmit} className="login-form">
              <div className="form-group">
                <div className={classed.inputContainer}>
                  <div className={classed.inputWrapper}>
                    <div className={classed.inputIcon}>📧</div>
                    <input
                      className={classed.input}
                      type="tel"
                      name="phone"
                      placeholder="0** *** ****"
                      value={form.phone}
                      onChange={handleChange}
                      required
                      autoComplete="username"
                    />
                  </div>
                </div>
              </div>

              <div className="form-group">
                <div className={classed.inputContainer}>
                  <div className={classed.inputWrapper}>
                    <div className={classed.inputIcon}>🔒</div>
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
                    <div 
                      className={classed.eyeIcon} 
                      onClick={togglePasswordVisibility}
                    >
                      {showPassword ? "🙈" : "👁"}
                    </div>
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

          {/* Password Input */}
          <div className="auth-input-group">
            <div className="auth-input-wrapper">
              <span className="auth-input-icon">�</span>
              <input
                type={showPassword ? "text" : "password"}
                name="password"
                placeholder="Mật khẩu"
                value={form.password}
                onChange={handleChange}
                className="auth-input"
                required
                autoComplete="current-password"
              />
              <span
                className="auth-toggle-password"
                onClick={togglePasswordVisibility}
                role="button"
                aria-label="Toggle password visibility"
              >
                {showPassword ? "🙈" : "👁"}
              </span>
            </div>
          </div>

          {/* Options Row */}
          <div className="auth-options">
            <label className="auth-checkbox-label">
              <input
                type="checkbox"
                name="rememberMe"
                checked={form.rememberMe}
                onChange={handleChange}
              />
              <span>Remember me</span>
            </label>
            <a href="#forgot" className="auth-link">
              Forget password?
            </a>
          </div>

          {/* Submit Button */}
          <button type="submit" className="auth-button" disabled={loading}>
            {loading ? "Đang đăng nhập..." : "Đăng nhập"}
          </button>

          {/* Social Login */}
          <div className="auth-social-section">
            <div className="auth-divider">
              <span>hoặc</span>
            </div>
            <div className="auth-social-buttons">
              <button
                type="button"
                className="auth-social-btn facebook"
                onClick={() => toast.info("Facebook login chưa khả dụng")}
              >
                f
              </button>
              <button
                type="button"
                className="auth-social-btn google"
                onClick={() => toast.info("Google login chưa khả dụng")}
              >
                G
              </button>
            </div>
          </div>

          {/* Footer Link */}
          <div className="auth-footer">
            <span>Chưa có tài khoản? </span>
            <span
              className="auth-footer-link"
              onClick={() => navigate("/register")}
              role="button"
            >
              Đăng ký tài khoản
            </span>
          </div>
        </form>
      </div>
    </div>
  );
};

export default Login;
