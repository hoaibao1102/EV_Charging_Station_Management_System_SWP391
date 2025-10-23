import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import ProtectedRoute from './ProtectedRoute.jsx';

// Import Layouts
import AdminLayout from '../layouts/AdminLayout';
import StaffLayout from '../layouts/StaffLayout';
import DriverLayout from '../layouts/DriverLayout';
 
// Public Pages =====================================
import Error404 from "../pages/inNavigateDriver/Error404.jsx";
import Login from "../pages/auth/Login.jsx";
import Register from "../pages/auth/Register.jsx";
import Verify from "../pages/auth/Verify.jsx";

// Driver Pages =====================================
import Home from "../pages/home/Home.jsx";
import Rules from "../pages/inNavigateDriver/Rules.jsx";
import Stations from "../pages/inNavigateDriver/Stations.jsx";
import StationDetail from "../pages/inNavigateDriver/StationDetail.jsx";
import BookingInfo from "../pages/inNavigateDriver/BookingInformation.jsx";
import Booking from "../pages/inNavigateDriver/Booking.jsx";
import Profile from "../pages/inNavigateDriver/Profile.jsx";
import EditProfile from "../pages/profileDriver/EditProfile.jsx";
import Information from "../pages/profileDriver/Information.jsx";
import Notification from "../pages/profileDriver/Notification.jsx";
import Vehicles from "../pages/profileDriver/Vehicles.jsx";
// import ChargeHistory from "../pages/profile/ChargeHistory.jsx";
// import MyBookings from "../pages/profile/MyBookings.jsx";

// Admin Pages ====================================

// Staff Pages ====================================


const AppRouter = () => {
  return (
    <BrowserRouter>
      <Routes>
        {/* ======================= PUBLIC ROUTES (Không cần đăng nhập) ======================= */}
        <Route path="/" element={<DriverLayout />}>
          <Route index element={<Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/verify-otp" element={<Verify />} />
          <Route path="/error" element={<Error404 />} />
          <Route path="stations" element={<Stations />} />
          <Route path="rules" element={<Rules />} />
          <Route path="stations/:id" element={<StationDetail />} />
        </Route>
        
        
        {/* ======================= PROTECTED ROUTES (Cần đăng nhập) ======================= */}

        {/* 1. DRIVER Routes - Chỉ DRIVER mới truy cập được */}
        <Route element={<ProtectedRoute allowedRoles={['DRIVER']} />}>
          <Route path="/" element={<DriverLayout />}> 
            <Route index element={<Home />} />           
            <Route path="bookings" element={<Booking />} />
            <Route path="bookingInformation" element={<BookingInfo />} />
            <Route path="profile" element={<Profile />} />
            <Route path="profile/my-vehicle" element={<Vehicles />} />
            <Route path="profile/edit" element={<EditProfile />} />
            <Route path="profile/information" element={<Information />} />
            <Route path="profile/notifications" element={<Notification />} />
          </Route>
        </Route>

        {/* 2. ADMIN Routes - Chỉ ADMIN mới truy cập được */}
        <Route element={<ProtectedRoute allowedRoles={['ADMIN']} />}>
          <Route path="/admin" element={<AdminLayout />}>
            <Route index element={<div>Admin Dashboard</div>} />
            <Route path="users" element={<div>User Management</div>} />
          </Route>
        </Route>

        {/* 3. STAFF Routes - Chỉ STAFF mới truy cập được */}
        <Route element={<ProtectedRoute allowedRoles={['STAFF']} />}>
          <Route path="/staff" element={<StaffLayout />}>
            <Route index element={<div>Staff Dashboard</div>} />
            <Route path="orders" element={<div>Order Processing</div>} />
          </Route>
        </Route>

        {/* ======================= 404 - Route không tồn tại ======================= */}
        <Route path="*" element={<Error404 />} />
      </Routes>
    </BrowserRouter>
  );
};

export default AppRouter;