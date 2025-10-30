import userIcon from "../../assets/icon/admin/manage_user.png";
import modelIcon from "../../assets/icon/admin/model_car.png";
import statisticIcon from "../../assets/icon/admin/statistic.png";
import dashBoardIcon from "../../assets/icon/admin/ad_dashboard.png";
import accident from "../../assets/icon/admin/accident.png";
import station from "../../assets/icon/admin/ad_charging-station.png";
import charger from "../../assets/icon/admin/charger_ad.png";
import price from "../../assets/icon/admin/best-price.png";
import chargingPoint from "../../assets/icon/admin/charging-building.png";
import { NavLink, useLocation } from "react-router-dom";
import "./AdminNavigate.css";
import classed from "../../assets/css/Main.module.css";
import { useNavigate } from "react-router-dom";
import paths from "../../path/paths.jsx";
import { useLogout } from '../../hooks/useAuth.js';


export default function AdminNavigate() {
    const location = useLocation();
    const navigate = useNavigate();
    const { logout, loading } = useLogout();

    const handleLogout = async () => {
        const result = await logout();
        if (result.success) {
          navigate(paths.login);
        }
      };

    const navItems = [
    {
      path: paths.adminDashboard,
      icon: dashBoardIcon,
      label: "Bảng điều khiển",
    },
    {
      path: paths.userManagement,
      icon: userIcon,
      label: "Quản lý người dùng",
    },
    {
      path: paths.stationManagement,
      icon: station,
      label: "Quản lý trạm sạc",
    },
    {
      path: paths.chargingPointManagement,
      icon: chargingPoint,
      label: "Quản lý trụ sạc",
    },
    {
      path: paths.chargerManagement,
      icon: charger,
      label: "Quản lý cổng sạc",
    },
    {
      path: paths.modelManagement,
      icon: modelIcon,
      label: "Quản lý mẫu xe",
    },
    {
      path: paths.chargingPriceConfiguration,
      icon: price,
      label: "Cấu hình giá sạc",
    },
    {
      path: paths.businessStatistics,
      icon: statisticIcon,
      label: "Thống kê doanh thu",
    },
    {
      path: paths.accidentReports,
      icon: accident,
      label: "Quản lý sự cố",
    },
  ];

  return (
      <div className="navContainer">
        {navItems.map((item, index) => {
          const isHomeActive = item.path === "/" && location.pathname === "/";
          const isOtherActive =
            item.path !== "/" && location.pathname === item.path;
          const isActive = isHomeActive || isOtherActive;

          return (
            <NavLink
              key={index}
              to={item.path}
              className={`navItem ${isActive ? "navItemActive" : ""}`}
              title={item.label}
            >
              <span className="navIcon">
                <img src={item.icon} alt={item.label} />
              </span>
              <span className="navLabel">{item.label}</span>
            </NavLink>
          );
        })}
        <button className={classed.button} onClick={handleLogout} disabled={loading}>{loading ? 'Đang đăng xuất...' : 'Đăng xuất'}</button>
      </div>
  );
}