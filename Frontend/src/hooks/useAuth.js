import { useState } from "react";
import { loginApi, registerApi } from "../api/authApi"; // Import từ authApi

// Custom hook đăng nhập
export const useLogin = () => {
  const [loading, setLoading] = useState(false);

  const validatePhone = (phone) => {
    const phoneRegex = /^0[0-9]{9}$/; // Kiểm tra số điện thoại đúng định dạng
    return phoneRegex.test(phone);
  };

  const validatePassword = (password) => {
    return password.length >= 5; // Kiểm tra mật khẩu có ít nhất 6 ký tự
  };

  const login = async (phone, password) => {
    if (!validatePhone(phone)) {
      return { success: false, message: "Số điện thoại không hợp lệ!" };
    }

    if (!validatePassword(password)) {
      return { success: false, message: "Mật khẩu phải có ít nhất 6 ký tự!" };
    }

    setLoading(true);
    try {
      const response = await loginApi(phone, password); // Gọi API đăng nhập
      if (response.success) {
        localStorage.setItem("isLoggedIn", "true");
        localStorage.setItem("userPhone", phone);
        localStorage.setItem("userId", response.data.userId);
        return { success: true };
      }
      return { success: false, message: "Đăng nhập thất bại!" };
    } catch (error) {
      console.error("Error during login:", error);
      return { success: false, message: "Lỗi trong quá trình đăng nhập!" };
    } finally {
      setLoading(false);
    }
  };

  return { login, loading };
};

// Custom hook đăng ký
export const useRegister = () => {
  const [loading, setLoading] = useState(false);

  const validatePhone = (phone) => {
    const phoneRegex = /^0[0-9]{9}$/;
    return phoneRegex.test(phone);
  };

  const validatePassword = (password) => {
    return password.length >= 6;
  };

  const validateConfirmPassword = (password, confirmPassword) => {
    return password === confirmPassword;
  };

  const register = async (formData) => {
    const { phoneNumber, password, confirmPassword } = formData;

    if (!validatePhone(phoneNumber)) {
      return { success: false, message: "Số điện thoại không hợp lệ!" };
    }

    if (!validatePassword(password)) {
      return { success: false, message: "Mật khẩu phải có ít nhất 6 ký tự!" };
    }

    if (!validateConfirmPassword(password, confirmPassword)) {
      return { success: false, message: "Mật khẩu xác nhận không khớp!" };
    }

    setLoading(true);
    try {
      const response = await registerApi(formData); // Gọi API đăng ký
      if (response.success) {
        return { success: true };
      }
      return { success: false, message: "Đăng ký thất bại!" };
    } catch (error) {
      console.error("Error during registration:", error);
      return { success: false, message: "Lỗi trong quá trình đăng ký!" };
    } finally {
      setLoading(false);
    }
  };

  return { register, loading };
};
