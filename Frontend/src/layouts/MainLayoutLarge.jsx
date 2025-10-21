// src/layouts/MainLayout.jsx
import { Outlet } from "react-router-dom";
import AppNavigation from "../components/navigate/Navigate.jsx";

export default function MainLayoutLarge() {
    return (
        <div style={{ 
            display: 'flex', 
            flexDirection: 'column', 
            height: '100vh',
            overflow: 'hidden'
        }}>
            <div style={{
                height: '125px',
                flexShrink: 0
            }}>
                <AppNavigation /> 
            </div>
            <main style={{ 
                flex: 1, 
                overflow: 'auto',
                height: 'calc(100vh - 125px)' 
            }}>
                <Outlet /> 
            </main>
        </div>
   );
}