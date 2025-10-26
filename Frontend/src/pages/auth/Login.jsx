import "./Login.css";
import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import { useLogin } from "../../hooks/useAuth";

const Login = () => {
  const [form, setForm] = useState({
    phone: "",
    password: "",
    rememberMe: false,
  });
  const [showPassword, setShowPassword] = useState(false);
  const [failedAttempts, setFailedAttempts] = useState(0);
  const [isLocked, setIsLocked] = useState(false);
  const [lockEndTime, setLockEndTime] = useState(null);
  const [countdown, setCountdown] = useState("");

  const { login, loading } = useLogin();
  const navigate = useNavigate();

  // Khôi phục trạng thái khóa và số lần thất bại từ localStorage
  useEffect(() => {
    const savedLockEndTime = localStorage.getItem("loginLockEndTime");
    const savedFailedAttempts = localStorage.getItem("loginFailedAttempts");

    if (savedLockEndTime) {
      const lockEnd = parseInt(savedLockEndTime, 10);
      const now = Date.now();

      if (lockEnd > now) {
        setIsLocked(true);
        setLockEndTime(lockEnd);
        setFailedAttempts(parseInt(savedFailedAttempts || "3", 10));
      } else {
        localStorage.removeItem("loginLockEndTime");
        localStorage.removeItem("loginFailedAttempts");
      }
    } else if (savedFailedAttempts) {
      setFailedAttempts(parseInt(savedFailedAttempts, 10));
    }
  }, []);

  // Effect xử lý đồng hồ đếm ngược
  useEffect(() => {
    let intervalId = null;

    if (isLocked && lockEndTime) {
      const updateTimer = () => {
        const now = Date.now();
        const remainingMs = Math.max(0, lockEndTime - now);
        
        const minutes = Math.floor(remainingMs / 60000);
        const seconds = Math.floor((remainingMs % 60000) / 1000);
        
        setCountdown(`${minutes}:${seconds.toString().padStart(2, "0")}`);

        if (remainingMs === 0) {
          clearInterval(intervalId);
          setFailedAttempts(0);
          setIsLocked(false);
          setLockEndTime(null);
          setCountdown("");
          localStorage.removeItem("loginLockEndTime");
          localStorage.removeItem("loginFailedAttempts");
          toast.info("Tài khoản đã được mở khóa. Bạn có thể đăng nhập lại.");
        }
      };

      updateTimer(); 
      intervalId = setInterval(updateTimer, 1000);
    }

    return () => clearInterval(intervalId);
  }, [isLocked, lockEndTime]);

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

    if (isLocked) {
      toast.error(`Tài khoản bị khóa! Vui lòng thử lại sau ${countdown}`);
      return;
    }

    const { success, message } = await login(form.phone, form.password);

    if (success) {
      setFailedAttempts(0);
      setIsLocked(false);
      setLockEndTime(null);
      localStorage.removeItem("loginLockEndTime");
      localStorage.removeItem("loginFailedAttempts");
    } else {
      const newFailedAttempts = failedAttempts + 1;

      toast.error(
        `${message || "Đăng nhập thất bại!"} (${newFailedAttempts}/3)`
      );

      
      setFailedAttempts(newFailedAttempts);
      localStorage.setItem("loginFailedAttempts", newFailedAttempts.toString());

      if (newFailedAttempts >= 3) {
        setIsLocked(true);
        const lockEnd = Date.now() + 3 * 60 * 1000; 
        setLockEndTime(lockEnd); 
        localStorage.setItem("loginLockEndTime", lockEnd.toString());

        setTimeout(() => {
          toast.warn("Tài khoản bị khóa trong 3 phút.");
        }, 2000);
      } 
    }
  };

  return (
    <div className="auth-page">
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
        <div className="auth-logo">
          <div className="auth-logo-icon auth-logo-car">
            <svg
              viewBox="0 0 100 100"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
            >
              <rect
                x="20"
                y="45"
                width="60"
                height="25"
                rx="8"
                fill="white"
                fillOpacity="0.9"
              />
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
              <circle cx="32" cy="72" r="8" fill="white" />
              <circle cx="68" cy="72" r="8" fill="white" />
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

        <form onSubmit={handleSubmit} className="auth-form">
          <div className="auth-input-group">
            <div className="auth-input-wrapper">
              <span className="auth-input-icon">👤</span>
              <input
                type="tel"
                name="phone"
                placeholder="Số điện thoại"
                onChange={handleChange}
                className="auth-input"
                required
                autoComplete="off"
                value={form.phone}
              />
            </div>
          </div>
          <div className="auth-input-group">
            <div className="auth-input-wrapper">
              <span className="auth-input-icon">🔒</span>
              <input
                type={showPassword ? "text" : "password"}
                name="password"
                placeholder="Mật khẩu"
                onChange={handleChange}
                className="auth-input"
                required
                autoComplete="off"
                value={form.password}
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

          <button
            type="submit"
            className="auth-button"
            disabled={loading || isLocked}
            style={isLocked ? { opacity: 0.5, cursor: "not-allowed" } : {}}
          >
            {loading
              ? "Đang đăng nhập..."
              : isLocked
              ? `Bị khóa (${countdown})`
              : "Đăng nhập"}
          </button>

          {isLocked && (
            <p
              style={{
                color: "red",
                fontSize: "14px",
                textAlign: "center",
                marginTop: "10px",
                fontWeight: "600",
              }}
            >
              ⚠️ Tài khoản bị khóa. Mở lại sau: {countdown}
            </p>
          )}

          <div className="auth-social-section">
            <div className="auth-divider">
              <span>hoặc</span>
            </div>
            <div className="auth-social-buttons">
              <button
                type="button"
                className="auth-social-btn google"
                onClick={() =>
                  (window.location.href =
                    "http://localhost:8080/oauth2/authorization/google")
                }
              >
                G
              </button>
            </div>
          </div>
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