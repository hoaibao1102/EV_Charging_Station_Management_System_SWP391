// src/layouts/MainLayout.jsx
import { Outlet } from "react-router-dom";
import AppNavigation from "../components/navigate/Navigate.jsx";

export default function MainLayout() {
    return (
        <div style={{ 
            display: 'flex', 
            flexDirection: 'column', 
            height: '100vh',
            overflow: 'hidden'
        }}>
            <main style={{ 
                flex: 1, 
                overflow: 'auto',
                height: 'calc(100vh - 70px)' // 70px là chiều cao của navigation
            }}>
                <Outlet /> 
            </main>
            <div style={{
                height: '70px',
                flexShrink: 0
            }}>
                <AppNavigation /> 
            </div>
        </div>
   );
}
