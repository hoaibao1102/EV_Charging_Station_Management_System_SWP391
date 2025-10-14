import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { ToastContainer, toast } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import { registerApi } from "../../api/authApi";
import "../../assets/css/register-mobile.css";

const Register = () => {
  const navigate = useNavigate();
  const [step, setStep] = useState(1);
  const [loading, setLoading] = useState(false);
  const [acceptTerms, setAcceptTerms] = useState(false);
  const [form, setForm] = useState({
    email: "",
    phoneNumber: "",
    name: "",
    dateOfBirth: "",
    gender: "",
    address: "",
    password: "",
    confirmPassword: "",
  });

  // Handle input change
  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  // === Per-step validation ===
  const validateStep = () => {
    if (step === 1) {
      if (!form.email || !form.phoneNumber) {
        toast.error("Vui lòng nhập Email và Số điện thoại!");
        return false;
      }
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(form.email)) {
        toast.error("Địa chỉ Email không hợp lệ!");
        return false;
      }
      const phoneRegex = /^0[0-9]{9}$/;
      if (!phoneRegex.test(form.phoneNumber)) {
        toast.error("Số điện thoại không hợp lệ!");
        return false;
      }
    }

    if (step === 2) {
      if (!form.name || !form.dateOfBirth || !form.gender) {
        toast.error("Vui lòng điền đầy đủ thông tin cá nhân!");
        return false;
      }
    }

    if (step === 3) {
      if (!form.address || !form.password || !form.confirmPassword) {
        toast.error("Vui lòng điền đầy đủ thông tin tài khoản!");
        return false;
      }
      if (form.password.length < 6) {
        toast.error("Mật khẩu phải có ít nhất 6 ký tự!");
        return false;
      }
      if (form.password !== form.confirmPassword) {
        toast.error("Mật khẩu xác nhận không khớp!");
        return false;
      }
    }

    return true;
  };

  // Next step handler
  const handleNextStep = () => {
    if (validateStep()) {
      setStep(step + 1);
    }
  };

  // Final Submit
  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!acceptTerms) {
      toast.error("Bạn cần đồng ý điều khoản trước khi đăng ký!");
      return;
    }

    const registerData = {
      email: form.email,
      phoneNumber: form.phoneNumber,
      passwordHash: form.password,
      name: form.name,
      dateOfBirth: form.dateOfBirth,
      gender: form.gender,
      address: form.address,
    };

    setLoading(true);
    try {
      const response = await registerApi(registerData);
      if (response && response.message?.toLowerCase().includes("thành công")) {
        toast.success("Đăng ký thành công!");
        setTimeout(() => navigate("/login"), 2000);
      } else toast.error(response.message || "Đăng ký thất bại!");
    } catch (error) {
      toast.error("Có lỗi xảy ra khi đăng ký!");
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  // Step indicator
  const StepDots = () => (
    <div className="register-step-dots">
      {[1, 2, 3, 4].map((n) => (
        <span key={n} className={`dot ${step === n ? "active" : ""}`}></span>
      ))}
    </div>
  );

  return (
    <div className="register-mobile-wrapper">
      <ToastContainer position="top-center" autoClose={2500} theme="colored" />
      <div className="register-mobile-container">
        {/* Back Button */}
        {step > 1 && (
          <button
            type="button"
            className="register-back-btn"
            onClick={() => setStep(step - 1)}
          >
            &#8592;
          </button>
        )}

        {/* Logo */}
        <div className="register-logo">
          <div className="register-logo-icon">⚡</div>
          <h1>Đăng Ký</h1>
        </div>

        {/* Step Dots */}
        <StepDots />

        <form onSubmit={handleSubmit}>
          {/* STEP 1 */}
          {step === 1 && (
            <>
              <h3 className="register-step-title">Thông tin cơ bản</h3>
              <div className="register-form-group" data-icon="📧">
                <input
                  type="email"
                  name="email"
                  placeholder="Nhập email của bạn"
                  value={form.email}
                  onChange={handleChange}
                  required
                />
              </div>
              <div className="register-form-group" data-icon="📱">
                <input
                  type="tel"
                  name="phoneNumber"
                  placeholder="Số điện thoại"
                  value={form.phoneNumber}
                  onChange={handleChange}
                  required
                />
              </div>
              <div className="register-next-btn" onClick={handleNextStep}>
                ➜
              </div>
            </>
          )}

          {/* STEP 2 */}
          {step === 2 && (
            <>
              <h3 className="register-step-title">Thông tin cá nhân</h3>
              <div className="register-form-group" data-icon="👤">
                <input
                  type="text"
                  name="name"
                  placeholder="Họ và tên"
                  value={form.name}
                  onChange={handleChange}
                />
              </div>
              <div className="register-form-group" data-icon="📅">
                <input
                  type="date"
                  name="dateOfBirth"
                  value={form.dateOfBirth}
                  onChange={handleChange}
                />
              </div>
              <div className="register-gender-options">
                <label>
                  <input
                    type="radio"
                    name="gender"
                    value="Nam"
                    checked={form.gender === "Nam"}
                    onChange={handleChange}
                  />
                  <span
                    style={{
                      borderColor:
                        form.gender === "Nam" ? "#00bcd4" : "#e0e0e0",
                      background:
                        form.gender === "Nam"
                          ? "rgba(0, 188, 212, 0.1)"
                          : "#fafafa",
                      color: form.gender === "Nam" ? "#00bcd4" : "#757575",
                      border: "2px solid",
                      borderRadius: "12px",
                      padding: "12px",
                      display: "block",
                    }}
                  >
                    Nam
                  </span>
                </label>
                <label>
                  <input
                    type="radio"
                    name="gender"
                    value="Nữ"
                    checked={form.gender === "Nữ"}
                    onChange={handleChange}
                  />
                  <span
                    style={{
                      borderColor: form.gender === "Nữ" ? "#00bcd4" : "#e0e0e0",
                      background:
                        form.gender === "Nữ"
                          ? "rgba(0, 188, 212, 0.1)"
                          : "#fafafa",
                      color: form.gender === "Nữ" ? "#00bcd4" : "#757575",
                      border: "2px solid",
                      borderRadius: "12px",
                      padding: "12px",
                      display: "block",
                    }}
                  >
                    Nữ
                  </span>
                </label>
              </div>
              <div className="register-next-btn" onClick={handleNextStep}>
                ➜
              </div>
            </>
          )}

          {/* STEP 3 */}
          {step === 3 && (
            <>
              <h3 className="register-step-title">Thông tin tài khoản</h3>
              <div className="register-form-group" data-icon="📍">
                <input
                  type="text"
                  name="address"
                  placeholder="Địa chỉ"
                  value={form.address}
                  onChange={handleChange}
                />
              </div>
              <div className="register-form-group" data-icon="🔒">
                <input
                  type="password"
                  name="password"
                  placeholder="Mật khẩu"
                  value={form.password}
                  onChange={handleChange}
                />
              </div>
              <div className="register-form-group" data-icon="🔒">
                <input
                  type="password"
                  name="confirmPassword"
                  placeholder="Xác nhận mật khẩu"
                  value={form.confirmPassword}
                  onChange={handleChange}
                />
              </div>
              <div className="register-next-btn" onClick={handleNextStep}>
                ➜
              </div>
            </>
          )}

          {/* STEP 4 */}
          {step === 4 && (
            <>
              <h3 className="register-step-title">Xác nhận đăng ký</h3>
              <div className="register-checkbox-group">
                <input
                  type="checkbox"
                  checked={acceptTerms}
                  onChange={(e) => setAcceptTerms(e.target.checked)}
                />
                <label>
                  Tôi đồng ý với <a href="#terms">Điều khoản Dịch vụ</a> và{" "}
                  <a href="#privacy">Chính sách Bảo mật</a>
                </label>
              </div>
              <button
                type="submit"
                className="register-submit-btn"
                disabled={loading || !acceptTerms}
              >
                {loading ? "Đang đăng ký..." : "Đăng ký"}
              </button>
            </>
          )}

          {/* Social Login */}
          <div className="register-social-btns">
            <button type="button" className="register-social-btn facebook">
              f
            </button>
            <button type="button" className="register-social-btn google">
              G
            </button>
          </div>

          <div className="register-login-link">
            Đã có tài khoản?{" "}
            <span onClick={() => navigate("/login")}>Đăng nhập ngay</span>
          </div>
        </form>
      </div>
    </div>
  );
};

export default Register;
