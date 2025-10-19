// Hàm tiện ích xử lý authentication

// Xóa tất cả thông tin đăng nhập khỏi localStorage
export const clearAuthData = () => {
  localStorage.removeItem("accessToken");
  localStorage.removeItem("userName");
  localStorage.removeItem("isLoggedIn");
  localStorage.removeItem("userMail");
  localStorage.removeItem("userPhone");
  localStorage.removeItem("userSex");
};

// Kiểm tra trạng thái đăng nhập
export const isAuthenticated = () => {
  return localStorage.getItem("isLoggedIn") === "true" && 
         localStorage.getItem("accessToken");
};


