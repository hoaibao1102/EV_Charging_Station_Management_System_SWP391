import { useState } from "react";
import { loginApi, registerApi, logoutApi } from "../api/authApi";
import { setAuthData, clearAuthData } from "../utils/authUtils";
import { useDispatch } from 'react-redux';
import { loginSuccess } from '../redux/slices/authSlice.js';
import { useNavigate } from 'react-router-dom';
import { toast } from "react-toastify";
import { logout as logoutAction } from "../redux/slices/authSlice.js";

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
  const navigate = useNavigate();
  const dispatch = useDispatch();
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
        if (response.data) {
          const {token, roleName, name, email, phone, gender} = response.data;
          console.log("Login successful, role:", roleName);
          //lưu vào localStorage
          setAuthData({ accessToken: token, role: roleName, userDetails: {name, email, phone, gender} });
          //lưu vào store
          dispatch(loginSuccess({ accessToken: token, role: roleName, userDetails: {name, email, phone, gender} }));
          // 3. Chuyển hướng theo Vai trò
          toast.success("Đăng nhập thành công!");
          setTimeout(() => {
            if (roleName === 'ADMIN') {
                navigate('/admin', { replace: true });
              } else if (roleName === 'STAFF') {
                navigate('/staff', { replace: true });
              } else if (roleName === 'DRIVER') {
                navigate('/', { replace: true });
            }
          }, 2000);
          
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
    const dispatch = useDispatch();
    const [loading, setLoading] = useState(false);
    
    // HÀM logout NÀY SẼ CHỊU TRÁCH NHIỆM HOÀN TẤT MỌI VIỆC từ xóa trong store/ localstorage đến gọi API logout
    const logout = async () => {
        setLoading(true);
        try {
            const response = await logoutApi(); 
            if(response.success){
              // Xóa Redux store
              dispatch(logoutAction());
              // Xóa localStorage
              clearAuthData();
              toast.success("Đăng xuất thành công!");
              return { success: true, message: "Đăng xuất thành công" }; 
            } else {
              // Nếu API trả về thất bại nhưng vẫn clear client-side
              dispatch(logoutAction());
              clearAuthData();
              return { success: false, message: response.message || "Đăng xuất thất bại" };
            }
        } catch (error) {
            console.error("Logout API failed, continuing client cleanup.", error);
            // Vẫn clear client-side dù API lỗi
            dispatch(logoutAction());
            clearAuthData();
            return { success: false, message: "Lỗi khi đăng xuất, nhưng đã xóa phiên làm việc" };
        } finally {
            setLoading(false);
        }
    };

    return { logout, loading };
};
