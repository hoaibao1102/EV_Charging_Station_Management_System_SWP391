import { useState } from "react";
import { loginApi, registerApi, logoutApi } from "../api/authApi";
import { clearAuthData } from "../utils/authUtils";

// Các hàm validate dùng chung
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

// Custom hook đăng nhập
export const useLogin = () => {
  const [loading, setLoading] = useState(false);

  const login = async (phone, password) => {
    if (!validatePhone(phone)) {
      return { success: false, message: "Số điện thoại không hợp lệ!" };
    }
    if (!validatePassword(password)) {
      return { success: false, message: "Mật khẩu phải có ít nhất 6 ký tự!" };
    }
    setLoading(true);
    try {
      const response = await loginApi(phone, password);
      if (response.success) {
        // Lưu thông tin đăng nhập vào localStorage
        localStorage.setItem("isLoggedIn", "true");
        if (response.data) {
          // API chỉ trả về token và tt user
          if (response.data.token) localStorage.setItem("accessToken", response.data.token);
          if (response.data.name) localStorage.setItem("userName", response.data.name);
          if (response.data.email) localStorage.setItem("userMail", response.data.email);
          if (response.data.phone) localStorage.setItem("userPhone", response.data.phone);
          if (response.data.sex) localStorage.setItem("userSex", response.data.sex);
        }
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
      const response = await registerApi(formData);
      if (response.success) {
        if (response.data) {
          if (response.data.token) localStorage.setItem("accessToken", response.data.token);
          if (response.data.name) localStorage.setItem("userName", response.data.name);
        }
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

// Custom hook đăng xuất
export const useLogout = () => {
  const [loading, setLoading] = useState(false);

  const logout = async () => {
    setLoading(true);
    try {
      const response = await logoutApi();
      clearAuthData();
      
      if (response.success) {
        return { success: true, message: "Đăng xuất thành công!" };
      } else {
        return { success: true, message: "Đã đăng xuất khỏi thiết bị!" };
      }
    } catch (error) {
      console.error("Error during logout:", error);
      clearAuthData();
      return { success: true, message: "Đã đăng xuất khỏi thiết bị!" };
    } finally {
      setLoading(false);
    }
  };

  return { logout, loading };
};
