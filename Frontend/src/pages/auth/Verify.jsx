import { useState, useRef, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { ToastContainer, toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import './login.css';
import './Verify.css';
import { verifyOtp } from '../../api/authApi.js';
import usePaths from '../../hooks/usePath.js';

export default function OTPVerification() {
  const navigate = useNavigate();
  const location = useLocation();
  const [otp, setOtp] = useState(['', '', '', '', '', '']);
  const [timer, setTimer] = useState(180); 
  const [canResend, setCanResend] = useState(false);
  const inputRefs = useRef([]);

  
  const registerData = location.state?.registerData || null;
  const paths = usePaths();

  // Kiểm tra nếu không có email thì redirect về register
  useEffect(() => {
    if (!registerData) {
      toast.error('Vui lòng đăng ký trước!');
      navigate(usePaths.register);
    }
  }, [registerData, navigate]);

  // Countdown timer
  useEffect(() => {
    if (timer > 0) {
      const interval = setInterval(() => {
        setTimer((prev) => {
          if (prev <= 1) {
            setCanResend(true);
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
      return () => clearInterval(interval);
    }
  }, [timer]);

  // Format timer display (mm:ss)
  const formatTime = (seconds) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  const handleChange = (index, value) => {
    if (!/^\d*$/.test(value)) return;
    
    const newOtp = [...otp];
    newOtp[index] = value.slice(-1);
    setOtp(newOtp);

    if (value && index < 5) {
      inputRefs.current[index + 1]?.focus();
    }
  };

  const handleKeyDown = (index, e) => {
    if (e.key === 'Backspace' && !otp[index] && index > 0) {
      inputRefs.current[index - 1]?.focus();
    }
  };

  const handlePaste = (e) => {
    e.preventDefault();
    const pastedData = e.clipboardData.getData('text').slice(0, 6);
    if (!/^\d+$/.test(pastedData)) return;

    const newOtp = [...otp];
    pastedData.split('').forEach((char, i) => {
      if (i < 6) newOtp[i] = char;
    });
    setOtp(newOtp);
  };

  const handleVerifyOtp = async () => {
    const otpCode = otp.join('');
    if (otpCode.length !== 6) {
      toast.error('Vui lòng nhập đủ 6 số OTP');
      return;
    }

    try {
      // Gửi OTP và registerData để verify
      const response = await verifyOtp(otpCode, registerData);

      if (response.success) {
        toast.success('Xác thực OTP thành công!');
        setTimeout(() => navigate(paths.login), 2000);
      } else {
        toast.error(response.message || 'Mã OTP không đúng. Vui lòng thử lại.');
      }
    } catch (error) {
      console.error('Verify OTP error:', error);
      toast.error('Có lỗi xảy ra khi xác thực OTP!');
    }
  };

  return (
    <div className="verify-page">
      <ToastContainer 
        position="top-center" 
        autoClose={2500} 
        theme="colored"
        style={{ zIndex: 99999 }}
      />
      
      <div className="auth-container verify-container">
        <div className="verify-container-inner">
          {/* Back Button */}
          <button 
            type="button"
            onClick={() => navigate(-1)}
            className="verify-back-btn"
          >
            ‹
          </button>

          {/* Car Icon */}
          <div className="verify-logo-wrapper">
            <div className="verify-logo-circle">
              <svg
                className="verify-logo-icon"
                viewBox="0 0 24 24"
                fill="white"
                xmlns="http://www.w3.org/2000/svg"
              >
                <path d="M18.92 6.01C18.72 5.42 18.16 5 17.5 5h-11c-.66 0-1.21.42-1.42 1.01L3 12v8c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-1h12v1c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-8l-2.08-5.99zM6.5 16c-.83 0-1.5-.67-1.5-1.5S5.67 13 6.5 13s1.5.67 1.5 1.5S7.33 16 6.5 16zm11 0c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zM5 11l1.5-4.5h11L19 11H5z"/>
              </svg>
            </div>
            <h1 className="verify-title">Đăng Ký</h1>
          </div>

          {/* Step Indicator - 3 dots */}
          <div className="verify-step-dots">
            <div className="verify-step-dot"></div>
            <div className="verify-step-dot active"></div>
            <div className="verify-step-dot"></div>
          </div>

          {/* OTP Section */}
          <div className="verify-otp-section">
            <h3 className="verify-otp-title">Xác thực OTP</h3>
            <p className="verify-otp-description">
              Chúng tôi đã gửi mã xác thực qua tài khoản email của bạn. Vui lòng truy cập và lấy mã.
            </p>

            {/* OTP Input Boxes */}
            <div className="verify-otp-inputs">
              {otp.map((digit, index) => (
                <input
                  key={index}
                  ref={(el) => (inputRefs.current[index] = el)}
                  type="text"
                  inputMode="numeric"
                  maxLength={1}
                  value={digit}
                  onChange={(e) => handleChange(index, e.target.value)}
                  onKeyDown={(e) => handleKeyDown(index, e)}
                  onPaste={index === 0 ? handlePaste : undefined}
                  className="verify-otp-input"
                />
              ))}
            </div>

            {/* Resend Link with Timer */}
            <div className="verify-resend-wrapper">
              {canResend ? (
                <button
                  onClick={() => {
                    setTimer(180);
                    setCanResend(false);
                    toast.info('Mã OTP đã được gửi lại');
                  }}
                  className="verify-resend-btn"
                >
                  🔄 Gửi lại mã
                </button>
              ) : (
                <p className="verify-timer-text">
                  <span className="verify-timer-icon">⏱️</span>
                  Gửi lại mã sau {formatTime(timer)}
                </p>
              )}
            </div>
          </div>

          {/* Next Button */}
          <button 
            type="button"
            className="verify-submit-btn"
            onClick={() => {
              handleVerifyOtp();
            }}
          >
            <span className="verify-submit-arrow">›</span>
          </button>

          {/* Login Link */}
          <div className="verify-footer">
            <p className="verify-footer-text">Đã có tài khoản?</p>
            <button
              onClick={() => navigate(paths.login)}
              className="verify-login-btn"
            >
              Đăng nhập ngay
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}