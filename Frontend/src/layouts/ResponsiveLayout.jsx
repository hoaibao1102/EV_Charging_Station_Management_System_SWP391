// src/layouts/ResponsiveLayout.jsx
import { useState, useEffect } from "react";
import MainLayout from "./MainLayout.jsx";
import MainLayoutLarge from "./MainLayoutLarge.jsx";

export default function ResponsiveLayout() {
  const [isMobile, setIsMobile] = useState(false);

  useEffect(() => {
    // Kiểm tra kích thước màn hình
    const checkScreenSize = () => {
      setIsMobile(window.innerWidth <= 768); 
    };

    // Kiểm tra ngay khi component mount
    checkScreenSize();

    // Listen cho resize events
    window.addEventListener('resize', checkScreenSize);

    // Cleanup listener
    return () => window.removeEventListener('resize', checkScreenSize);
  }, []);

  // Render layout phù hợp
  if (isMobile) {
    return <MainLayout />; // Navigation ở dưới cho mobile
  } else {
    return <MainLayoutLarge />; // Navigation ở trên cho desktop
  }
}