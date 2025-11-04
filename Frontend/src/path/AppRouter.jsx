import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import ProtectedRoute from "./ProtectedRoute.jsx";
import paths from "./paths.jsx";

// Import Layouts
import AdminLayout from "../layouts/AdminLayout";
import DriverLayout from "../layouts/DriverLayout";

// Public Pages =====================================
import Error404 from "../pages/auth/Error404.jsx";
import Login from "../pages/auth/Login.jsx";
import Register from "../pages/auth/Register.jsx";
import Verify from "../pages/auth/Verify.jsx";

// Driver Pages =====================================
import Home from "../pages/home/Home.jsx";
import Rules from "../pages/inNavigateDriver/Rules.jsx";
import Stations from "../pages/inNavigateDriver/Stations.jsx";
import StationDetail from "../pages/inNavigateDriver/StationDetail.jsx";
import ChargingSession from "../pages/inNavigateDriver/ChargingSession.jsx";
import Booking from "../pages/inNavigateDriver/Booking.jsx";
import BookingDetail from "../pages/inNavigateDriver/BookingDetail.jsx";
import BookingQRCode from "../pages/inNavigateDriver/BookingQRCode.jsx";
import Payment from "../pages/inNavigateDriver/Payment.jsx";
import Profile from "../pages/inNavigateDriver/Profile.jsx";
import EditProfile from "../pages/profileDriver/EditProfile.jsx";
import Information from "../pages/profileDriver/Information.jsx";
import Notification from "../pages/profileDriver/Notification.jsx";
import Vehicles from "../pages/profileDriver/Vehicles.jsx";
// import ChargeHistory from "../pages/profile/ChargeHistory.jsx";
// import MyBookings from "../pages/profile/MyBookings.jsx";

// Admin Pages ====================================
import AdminDashboard from "../pages/admin/AdminDashboard.jsx";
import ManagementStation from "../pages/admin/ManagementStation.jsx";
import ManagementUser from "../pages/admin/ManagementUser.jsx";
import BusinessStatistics from "../pages/admin/BusinessStatistics.jsx";
import ManagementAccident from "../pages/admin/ManagementAccident.jsx";
import ManagementCharger from "../pages/admin/ManagementCharger.jsx";
import ManagementChargingPoint from "../pages/admin/ManagementChargingPoint.jsx";
import PriceConfiguration from "../pages/admin/PriceConfiguration.jsx";
import ManagementModel from "../pages/admin/ManagementModel.jsx";

// Staff Pages ====================================
import ReportAccidents from "../pages/staff/ReportAccident.jsx";
import ManagementTransaction from "../pages/staff/ManagementTransaction.jsx";
import SessionCharging from "../pages/staff/SessionCharging.jsx";
import SessionChargingCreate from "../pages/staff/SessionChargingCreate.jsx";
import StaffDashboard from "../pages/staff/StaffDashboard.jsx";

const AppRouter = () => {
  return (
    <BrowserRouter>
      <Routes>
        <Route path={paths.error} element={<Error404 />} />

        {/* ======================= PUBLIC ROUTES (Không cần đăng nhập) ======================= */}
        <Route path="/" element={<DriverLayout />}>
          <Route index element={<Home />} />
          <Route path={paths.login} element={<Login />} />
          <Route path={paths.register} element={<Register />} />
          <Route path={paths.verify} element={<Verify />} />

          <Route path={paths.stations} element={<Stations />} />
          <Route path={paths.rules} element={<Rules />} />
          <Route path={paths.stationDetail} element={<StationDetail />} />
        </Route>

        {/* ======================= PROTECTED ROUTES (Cần đăng nhập) ======================= */}

        {/* 1. DRIVER Routes - Chỉ DRIVER mới truy cập được */}
        <Route element={<ProtectedRoute allowedRoles={["DRIVER"]} />}>
          <Route path="/" element={<DriverLayout />}>
            <Route index element={<Home />} />
            <Route path={paths.booking} element={<Booking />} />
            <Route path={paths.bookingDetail} element={<BookingDetail />} />
            <Route path={paths.bookingQr} element={<BookingQRCode />} />
            <Route path={paths.chargingSession} element={<ChargingSession />} />
            <Route path={paths.payment} element={<Payment />} />
            <Route path={paths.profile} element={<Profile />} />
            <Route path={paths.myVehicle} element={<Vehicles />} />
            <Route path={paths.editProfile} element={<EditProfile />} />
            <Route path={paths.information} element={<Information />} />
            <Route path={paths.notifications} element={<Notification />} />
          </Route>
        </Route>

        {/* 2. ADMIN Routes - Chỉ ADMIN mới truy cập được */}
        <Route element={<ProtectedRoute allowedRoles={["ADMIN"]} />}>
          <Route path={paths.adminDashboard} element={<AdminLayout />}>
            <Route index element={<AdminDashboard />} />
            <Route path={paths.userManagement} element={<ManagementUser />} />
            <Route
              path={paths.stationManagement}
              element={<ManagementStation />}
            />
            <Route path={paths.modelManagement} element={<ManagementModel />} />
            <Route
              path={paths.chargerManagement}
              element={<ManagementCharger />}
            />
            <Route
              path={paths.businessStatistics}
              element={<BusinessStatistics />}
            />
            <Route
              path={paths.accidentReports}
              element={<ManagementAccident />}
            />
            <Route
              path={paths.chargingPriceConfiguration}
              element={<PriceConfiguration />}
            />
            <Route
              path={paths.chargingPointManagement}
              element={<ManagementChargingPoint />}
            />
          </Route>
        </Route>

        {/* 3. STAFF Routes - Chỉ STAFF mới truy cập được */}
        <Route element={<ProtectedRoute allowedRoles={["STAFF"]} />}>
          <Route path={paths.staffDashboard} element={<AdminLayout />}>
            <Route index element={<StaffDashboard />} />
            <Route
              path={paths.manageSessionCharging}
              element={<SessionCharging />}
            />
            <Route
              path={paths.manageSessionChargingCreate}
              element={<SessionChargingCreate />}
            />
            <Route
              path={paths.manageTransaction}
              element={<ManagementTransaction />}
            />
            <Route path={paths.reportAccidents} element={<ReportAccidents />} />
          </Route>
        </Route>

        {/* ======================= 404 - Route không tồn tại ======================= */}
        <Route path="*" element={<Error404 />} />
      </Routes>
    </BrowserRouter>
  );
};

export default AppRouter;
