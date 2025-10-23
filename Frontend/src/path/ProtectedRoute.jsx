import React from 'react';
import { useSelector } from 'react-redux';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { selectRole, selectIsLoggedIn } from '../redux/slices/authSlice.js';

const ProtectedRoute = ({ allowedRoles }) => {
  const isLoggedIn = useSelector(selectIsLoggedIn) || false;
  const userRole = useSelector(selectRole) || null;
  const location = useLocation();


  // Kiểm tra đăng nhập
  if (!isLoggedIn) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // Kiểm tra quyền truy cập
  if (allowedRoles && allowedRoles.length > 0) {
    if (!userRole || !allowedRoles.includes(userRole)) {
      return <Navigate to="/error" replace />;
    }
  }
  // Cho phép truy cập
  return <Outlet />;
};

export default ProtectedRoute;