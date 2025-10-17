// src/layouts/MainLayout.jsx
import { Outlet } from "react-router-dom";
import AppNavigation from "../components/navigate/Navigate.jsx";

export default function MainLayout() {
    return (
        <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
            <main style={{ flex: 1, padding: '0' }}>
                <Outlet /> 
            </main>
            <AppNavigation /> 
        </div>
   );
}
