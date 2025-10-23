import AppNavigation from "../components/navigate/DriverNavigate.jsx";
import { useState, useEffect } from "react";
import { Outlet } from "react-router-dom";
import './MainLayout.css';

export default function DriverLayout() {
  const [isMobile, setIsMobile] = useState(false);

  function MainLayoutLarge() {
    return (
      <div
        style={{
          display: "flex",
          flexDirection: "column",
          height: "100vh",
          overflow: "hidden", 
        }}
      >
        <header
          style={{
            height: "125px",
            flexShrink: 0, 
          }}
        >
          <AppNavigation />
        </header>

        <main
          style={{
            flex: 1, 
            overflowY: "auto", 
          }}
        >
          <Outlet />
        </main>
      </div>
    );
  }

  // Layout cho màn hình NHỎ (Mobile) - Navigation ở DƯỚI
  function MainLayout() {
    return (
      <div style={{
          display: 'flex',
          flexDirection: 'column',
          height: '100vh',
          overflow: 'hidden'
      }}>
        <main style={{
            flex: 1, 
            overflowY: 'auto', 
        }}>
          <Outlet />
        </main>

        <footer style={{
            height: '70px',
            flexShrink: 0 
        }}>
          <AppNavigation />
        </footer>
      </div>
    );
  }

  // Logic kiểm tra kích thước màn hình
  useEffect(() => {
    const checkScreenSize = () => {
      // 768px là breakpoint phổ biến cho mobile
      setIsMobile(window.innerWidth <= 768); 
    };

    checkScreenSize();
    window.addEventListener('resize', checkScreenSize);

    return () => window.removeEventListener('resize', checkScreenSize);
  }, []);

  // Render layout phù hợp
  if (isMobile) {
    return <MainLayout />; // Mobile: Navigation dưới
  } else {
    return <MainLayoutLarge />; // Large: Navigation trên
  }
}