import { Outlet } from "react-router-dom";
import AdminNavigate from "../components/navigate/AdminNavigate";
import StaffNavigate from "../components/navigate/StaffNavigate.jsx";
import "./AdminLayout.css"; 

export default function AdminLayout() {
  const role = localStorage.getItem('role');
  return (
    <div className="admin-layout">
      {/* Sidebar Navigation - Fixed bên trái */}
      <aside className="admin-sidebar">
        {role === 'ADMIN' && <AdminNavigate />}
        {role === 'STAFF' && <StaffNavigate />}
      </aside>

      {/* Main Content Area */}
      <main className="admin-main">
        <Outlet />
      </main>
    </div>
  );
}