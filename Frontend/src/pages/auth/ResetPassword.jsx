import "./login.css";
import React, { useState } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { toast } from "react-toastify";
import { resetPasswordApi } from "../../api/authApi";

const ResetPassword = () => {
  const [otp, setOtp] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [loading, setLoading] = useState(false);

  const navigate = useNavigate();
  const location = useLocation();

  // Get email from navigation state
  const email = location.state?.email;

  // If no email, redirect back to forgot password
  React.useEffect(() => {
    if (!email) {
      toast.error("Phiên làm việc đã hết hạn. Vui lòng thử lại!");
      navigate("/forgot-password");
    }
  }, [email, navigate]);

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!otp) {
      toast.error("Vui lòng nhập mã OTP!");
      return;
    }

    if (!newPassword) {
      toast.error("Vui lòng nhập mật khẩu mới!");
      return;
    }

    if (newPassword.length < 6) {
      toast.error("Mật khẩu phải có ít nhất 6 ký tự!");
      return;
    }

    if (newPassword !== confirmPassword) {
      toast.error("Mật khẩu xác nhận không khớp!");
      return;
    }

    setLoading(true);

    try {
      const response = await resetPasswordApi(otp, email, newPassword);

      if (response.success) {
        toast.success("Đặt lại mật khẩu thành công! Vui lòng đăng nhập.");
        // Navigate back to login page
        setTimeout(() => {
          navigate("/login");
        }, 1500);
      } else {
        toast.error(
          response.message ||
            "Không thể đặt lại mật khẩu. Vui lòng kiểm tra mã OTP!"
        );
      }
    } catch (error) {
      toast.error(error.message || "Có lỗi xảy ra. Vui lòng thử lại!");
    } finally {
      setLoading(false);
    }
  };

  const togglePasswordVisibility = () => {
    setShowPassword(!showPassword);
  };

  const toggleConfirmPasswordVisibility = () => {
    setShowConfirmPassword(!showConfirmPassword);
  };

  return (
    <div className="auth-page">
      <div className="auth-welcome-section">
        <div className="auth-welcome-content">
          <h1 className="auth-welcome-title">
            Đặt lại
            <br />
            mật khẩu
          </h1>
          <div className="auth-welcome-divider"></div>
          <p className="auth-welcome-text">
            Nhập mã OTP đã được gửi đến email <strong>{email}</strong> và mật
            khẩu mới của bạn.
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
              <rect
                x="35"
                y="40"
                width="30"
                height="25"
                rx="3"
                stroke="white"
                strokeWidth="3"
                fill="rgba(255,255,255,0.2)"
              />
              <path
                d="M40 40 L40 32 C40 25 45 20 50 20 C55 20 60 25 60 32 L60 40"
                stroke="white"
                strokeWidth="3"
                fill="none"
              />
              <circle cx="50" cy="52" r="3" fill="white" />
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
          <h1 className="auth-title">Đặt Lại Mật Khẩu</h1>
          <p className="auth-subtitle">
            Nhập <span className="highlight">mã OTP</span> và{" "}
            <span className="highlight">mật khẩu mới</span>
          </p>
        </div>

        <form onSubmit={handleSubmit} className="auth-form">
          <div className="auth-input-group">
            <div className="auth-input-wrapper">
              <span className="auth-input-icon">🔑</span>
              <input
                type="text"
                name="otp"
                placeholder="Mã OTP (6 chữ số)"
                value={otp}
                onChange={(e) => setOtp(e.target.value)}
                className="auth-input"
                required
                autoComplete="off"
                maxLength="6"
                pattern="[0-9]{6}"
              />
            </div>
          </div>

          <div className="auth-input-group">
            <div className="auth-input-wrapper">
              <span className="auth-input-icon">🔒</span>
              <input
                type={showPassword ? "text" : "password"}
                name="newPassword"
                placeholder="Mật khẩu mới"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                className="auth-input"
                required
                autoComplete="new-password"
                minLength="6"
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

          <div className="auth-input-group">
            <div className="auth-input-wrapper">
              <span className="auth-input-icon">🔒</span>
              <input
                type={showConfirmPassword ? "text" : "password"}
                name="confirmPassword"
                placeholder="Xác nhận mật khẩu mới"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                className="auth-input"
                required
                autoComplete="new-password"
                minLength="6"
              />
              <span
                className="auth-toggle-password"
                onClick={toggleConfirmPasswordVisibility}
                role="button"
                aria-label="Toggle confirm password visibility"
              >
                {showConfirmPassword ? "🙈" : "👁"}
              </span>
            </div>
          </div>

          <button
            type="submit"
            className="auth-button"
            disabled={loading}
            style={loading ? { opacity: 0.7, cursor: "not-allowed" } : {}}
          >
            {loading ? "Đang xử lý..." : "Đặt lại mật khẩu"}
          </button>

          <div className="auth-footer" style={{ marginTop: "20px" }}>
            <span>Chưa nhận được mã? </span>
            <span
              className="auth-footer-link"
              onClick={() => navigate("/forgot-password")}
              role="button"
            >
              Gửi lại
            </span>
          </div>
        </form>
      </div>
    </div>
  );
};

export default ResetPassword;
