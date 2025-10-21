import { createBrowserRouter } from "react-router-dom";
import Home from "../pages/home/Home.jsx";
import Rules from "../pages/inNavigate/Rules.jsx";
import Stations from "../pages/inNavigate/Stations.jsx";
import StationDetail from "../pages/inNavigate/StationDetail.jsx";
import BookingInfo from "../pages/inNavigate/BookingInformation.jsx";
import Booking from "../pages/inNavigate/Booking.jsx";
import Profile from "../pages/inNavigate/Profile.jsx";
import Error404 from "../pages/inNavigate/Error404.jsx";
import ResponsiveLayout from "../layouts/ResponsiveLayout.jsx";
import Login from "../pages/auth/Login.jsx";
import Register from "../pages/auth/Register.jsx";
import Verify from "../pages/auth/Verify.jsx";
import Vehicles from "../pages/profile/Vehicles.jsx";
// import MyBookings from "../pages/profile/MyBookings.jsx";
import EditProfile from "../pages/profile/EditProfile.jsx";
import Information from "../pages/profile/Information.jsx";
import Notification from "../pages/profile/Notification.jsx";
// import ChargeHistory from "../pages/profile/ChargeHistory.jsx";

const routes = [
  {
    path: "/",
    element: <ResponsiveLayout />,
    children: [
      //navigate
      {
        index: true,
        element: <Home />,
      },
      {
        path: "rules",
        element: <Rules />,
      },
      {
        path: "stations",
        element: <Stations />,
      },
      {
        path: "stations/:id",
        element: <StationDetail />,
      },
      {
        path: "bookings",
        element: <Booking />,
      },
      {
        path: "bookingInformation",
        element: <BookingInfo />,
      },
      {
        path: "profile",
        element: <Profile />,
      },
      //khác
      {
        path: "login",
        element: <Login />,
      },
      {
        path: "register",
        element: <Register />,
      },
      {
        path: "verify-otp",
        element: <Verify />,
      },
      //trong profile - tạm thời comment để không bị lỗi
      //Uncomment và tạo các component tương ứng khi cần
      {
        path: "profile/my-vehicle",
        element: <Vehicles />,
      },
      // {
      //     path: 'my-bookings',
      //     element: <MyBookings />,
      // },
      {
        path: "profile/edit",
        element: <EditProfile />,
      },
      {
        path: "profile/information",
        element: <Information />,
      },
      {
        path: "/profile/notifications",
        element: <Notification />,
      },
      // {
      //     path: 'charge-history',
      //     element: <ChargeHistory />,
      // },
      //404
      {
        path: "*",
        element: <Error404 />,
      },
    ],
  },
];

const router = createBrowserRouter(routes);

export default router;
