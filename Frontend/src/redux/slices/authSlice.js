import { createSlice } from '@reduxjs/toolkit';
import { getAuthData, isAuthenticated } from '../../utils/authUtils.js'; 

// Lấy dữ liệu bền vững từ Local Storage để khôi phục trạng thái
const initialData = getAuthData();

const initialState = {
  isLoggedIn: isAuthenticated(), // Kiểm tra trạng thái đăng nhập
  user: null, 
  role: initialData.role || null, // Vai trò (ADMIN, STAFF, DRIVER)
  accessToken: initialData.accessToken || null,
};

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    loginSuccess: (state, action) => {
      state.isLoggedIn = true;
      state.user = action.payload.userDetails;
      state.role = action.payload.role; 
      state.accessToken = action.payload.accessToken;
    },
    logout: (state) => {
      state.isLoggedIn = false;
      state.user = null;
      state.role = null;
      state.accessToken = null;
    },
  },
});

export const { loginSuccess, logout } = authSlice.actions;

export const selectRole = (state) => state.auth.role;
export const selectIsLoggedIn = (state) => state.auth.isLoggedIn;
export const selectUser = (state) => state.auth.user;
// userDetails: {name, email, phone, gender}

export default authSlice.reducer;