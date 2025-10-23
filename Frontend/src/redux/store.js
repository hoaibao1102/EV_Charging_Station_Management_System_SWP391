import { configureStore } from '@reduxjs/toolkit';
import authReducer from './slices/authSlice'; 

export const store = configureStore({
  reducer: {
    auth: authReducer, // Đăng ký slice auth
    // có thể thêm các slice khác ở đây
  },
});