
import { createRoot } from 'react-dom/client';
import { RouterProvider } from 'react-router-dom';
import './index.css';
import router from "./path/router.jsx";
import 'bootstrap/dist/css/bootstrap.min.css';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

createRoot(document.getElementById('root')).render(
        <>
                <RouterProvider router={router} /> 
                <ToastContainer position="top-center" autoClose={2000} />
        </>
);
