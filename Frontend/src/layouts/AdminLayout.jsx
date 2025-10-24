import { Outlet } from "react-router-dom";
import AdminNavigate from "../components/navigate/AdminNavigate";
import "./AdminLayout.css"; 

export default function AdminLayout() {
  return (
    <div className="admin-layout">
      {/* Sidebar Navigation - Fixed bên trái */}
      <aside className="admin-sidebar">
        <AdminNavigate />
      </aside>

      {/* Main Content Area */}
      <main className="admin-main">
        <Outlet />
      </main>
    </div>
  );
}