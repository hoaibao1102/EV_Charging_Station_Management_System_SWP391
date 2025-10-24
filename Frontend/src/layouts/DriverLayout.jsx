import AppNavigation from "../components/navigate/DriverNavigate.jsx";
import { useState, useEffect } from "react";
import { Outlet } from "react-router-dom";
import './DriverLayout.css';

export default function DriverLayout() {
  const [isMobile, setIsMobile] = useState(false);

  function MainLayoutLarge() {
    return (
      <div className="driver-layout-large">
        <header className="driver-layout-header">
          <AppNavigation />
        </header>

        <main className="driver-layout-main">
          <Outlet />
        </main>
      </div>
    );
  }

  // Layout cho màn hình NHỎ (Mobile) - Navigation ở DƯỚI
  function MainLayout() {
    return (
      <div className="driver-layout-mobile">
        <main className="driver-layout-main">
          <Outlet />
        </main>

        <footer className="driver-layout-footer">
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