// Hàm tiện ích xử lý authentication

// Xóa tất cả thông tin đăng nhập khỏi localStorage
export const clearAuthData = () => {
  localStorage.removeItem("accessToken");
  localStorage.removeItem("userName");
  localStorage.removeItem("isLoggedIn");
};

// Kiểm tra trạng thái đăng nhập
export const isAuthenticated = () => {
  return localStorage.getItem("isLoggedIn") === "true" && 
         localStorage.getItem("accessToken");
};

// Lấy thông tin hồ sơ người dùng từ localStorage
// export const getUserProfile = () => {
//   const userName = localStorage.getItem("userName");
//   const email = localStorage.getItem("email");
//   const phone = localStorage.getItem("phone");
//   return { name: userName, email, phone };
// };
