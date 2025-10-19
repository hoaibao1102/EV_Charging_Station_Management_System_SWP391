import { createBrowserRouter } from "react-router-dom";
import Home from "../pages/home/Home.jsx";
import Rules from "../pages/inNavigate/Rules.jsx";
import Stations from "../pages/inNavigate/Stations.jsx";
import Booking from "../pages/inNavigate/Booking.jsx";
import Profile from "../pages/inNavigate/Profile.jsx";
import Error404 from "../pages/inNavigate/Error404.jsx";
import ResponsiveLayout from "../layouts/ResponsiveLayout.jsx";
import Login from "../pages/auth/Login.jsx";
import Register from "../pages/auth/Register.jsx";
import Verify from "../pages/auth/Verify.jsx";
// import MyVehicle from "../pages/profile/MyVehicle.jsx";
// import MyBookings from "../pages/profile/MyBookings.jsx";
// import EditProfile from "../pages/profile/EditProfile.jsx";
import Information from "../pages/profile/Information.jsx";
// import Notifications from "../pages/profile/Notifications.jsx";
// import ChargeHistory from "../pages/profile/ChargeHistory.jsx";

const routes = [
    {
        path: '/',
        element: <ResponsiveLayout />,
        children: [ 
            //navigate
            {
                index: true, 
                element: <Home />,
            },
            {
                path: 'rules', 
                element: <Rules />,
            },
            {
                path: 'stations', 
                element: <Stations />,
            },
            {
                path: 'bookings', 
                element: <Booking />,
            },
            {
                path: 'profile', 
                element: <Profile />,
            },
            //khác
            {
                path: 'login', 
                element: <Login />,
            },
            {
                path: 'register', 
                element: <Register />,
            },
            {
                path: 'verify-otp', 
                element: <Verify />,
            },
            //trong profile - tạm thời comment để không bị lỗi
            //Uncomment và tạo các component tương ứng khi cần
            // {
            //     path: 'my-vehicle', 
            //     element: <MyVehicle />,
            // },
            // {
            //     path: 'my-bookings', 
            //     element: <MyBookings />,
            // },
            // {
            //     path: 'edit', 
            //     element: <EditProfile />,
            // },
            {
                path: 'profile/information', 
                element: <Information />,
            },
            // {
            //     path: 'notifications', 
            //     element: <Notifications />,
            // },
            // {
            //     path: 'charge-history', 
            //     element: <ChargeHistory />,
            // },
            //404
            {
                path: '*',
                element: <Error404 />,
            },
        ]
    }
];

const router = createBrowserRouter(routes);

export default router;