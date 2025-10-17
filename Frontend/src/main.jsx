
import { createRoot } from 'react-dom/client';
import { RouterProvider } from 'react-router-dom';
import './index.css';
import { PathProvider } from "./path/paths.jsx";
import router from "./path/router.jsx";

createRoot(document.getElementById('root')).render(
    <PathProvider>
        <RouterProvider router={router} /> 
    </PathProvider>
);
