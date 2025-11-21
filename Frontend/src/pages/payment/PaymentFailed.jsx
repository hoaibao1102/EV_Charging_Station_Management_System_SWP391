import React, { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { toast } from "react-toastify";
import paths from "../../path/paths.jsx";
import "./PaymentFailed.css";

export default function PaymentFailed() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  useEffect(() => {
    const responseCode = searchParams.get("vnp_ResponseCode");
    const message = getErrorMessage(responseCode);
    toast.error(message, { position: "top-center" });
  }, [searchParams]);

  const getErrorMessage = (code) => {
    const errorMessages = {
      "07": "Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường).",
      "09": "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng chưa đăng ký dịch vụ InternetBanking tại ngân hàng.",
      10: "Giao dịch không thành công do: Khách hàng xác thực thông tin thẻ/tài khoản không đúng quá 3 lần",
      11: "Giao dịch không thành công do: Đã hết hạn chờ thanh toán. Xin quý khách vui lòng thực hiện lại giao dịch.",
      12: "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng bị khóa.",
      13: "Giao dịch không thành công do Quý khách nhập sai mật khẩu xác thực giao dịch (OTP).",
      24: "Giao dịch không thành công do: Khách hàng hủy giao dịch",
      51: "Giao dịch không thành công do: Tài khoản của quý khách không đủ số dư để thực hiện giao dịch.",
      65: "Giao dịch không thành công do: Tài khoản của Quý khách đã vượt quá hạn mức giao dịch trong ngày.",
      75: "Ngân hàng thanh toán đang bảo trì.",
      79: "Giao dịch không thành công do: KH nhập sai mật khẩu thanh toán quá số lần quy định.",
    };
    return (
      errorMessages[code] || "Giao dịch không thành công. Vui lòng thử lại!"
    );
  };

  const handleRetry = () => {
    navigate(paths.booking);
  };

  const handleGoToHome = () => {
    navigate(paths.home);
  };

  return (
    <div className="payment-failed-container">
      <div className="payment-failed-card">
        {/* Failed Icon */}
        <div className="failed-icon-wrapper">
          <div className="failed-icon">
            <svg viewBox="0 0 52 52" className="crossmark">
              <circle
                cx="26"
                cy="26"
                r="25"
                fill="none"
                className="crossmark-circle"
              />
              <path
                fill="none"
                d="M16 16 36 36 M36 16 16 36"
                className="crossmark-cross"
              />
            </svg>
          </div>
        </div>

        {/* Title */}
        <h1 className="failed-title">Thanh Toán Thất Bại!</h1>
        <p className="failed-subtitle">
          {getErrorMessage(searchParams.get("vnp_ResponseCode"))}
        </p>

        {/* Transaction Info */}
        {searchParams.get("vnp_TxnRef") && (
          <div className="transaction-info">
            <p className="transaction-id">
              Mã giao dịch: <strong>{searchParams.get("vnp_TxnRef")}</strong>
            </p>
          </div>
        )}

        {/* Actions */}
        <div className="action-buttons">
          <button className="btn-retry" onClick={handleRetry}>
            🔄 Thử Lại
          </button>
          <button className="btn-home" onClick={handleGoToHome}>
            🏠 Về Trang Chủ
          </button>
        </div>

        {/* Support */}
        <div className="support-section">
          <p className="support-text">
            Nếu cần hỗ trợ, vui lòng liên hệ: <strong>1900 xxxx</strong>
          </p>
        </div>
      </div>
    </div>
  );
}
