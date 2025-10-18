import { createBrowserRouter } from "react-router-dom";
import Home from "../pages/home/Home.jsx";
import Rules from "../pages/others/Rules.jsx";
import Stations from "../pages/others/Stations.jsx";
import Booking from "../pages/others/Booking.jsx";
import Profile from "../pages/others/Profile.jsx";
import Error404 from "../pages/others/Error404.jsx";
import ResponsiveLayout from "../layouts/ResponsiveLayout.jsx";
import Login from "../pages/auth/Login.jsx";
import Register from "../pages/auth/Register.jsx";

const routes = [
    {
        path: '/',
        element: <ResponsiveLayout />,
        children: [ 
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
            {
                path: 'login', 
                element: <Login />,
            },
            {
                path: 'register', 
                element: <Register />,
            },
            {
                path: '*',
                element: <Error404 />,
            },
        ]
    }
];

const router = createBrowserRouter(routes);

export default router;