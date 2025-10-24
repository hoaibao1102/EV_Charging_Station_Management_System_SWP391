import dashboard from "../../assets/icon/staff/dashboard.png";
import charging from "../../assets/icon/staff/charging-station.png";
import accident from "../../assets/icon/staff/incident-report.png";
import transition from "../../assets/icon/staff/payment-method.png";
import { NavLink, useLocation } from "react-router-dom";
export default function AdminNavigate() {
    const location = useLocation();

    const navItems = [
    {
      path: "/staff",
      icon: dashboard,
      label: "Bảng điều khiển",
    },
    {
      path: "/manage-session-charging",
      icon: charging,
      label: "Quản lý phiên sạc",
    },
    {
      path: "/transaction-management",
      icon: transition,
      label: "Quản lý giao dịch",
    },
    {
      path: "/report-accidents",
      icon: accident,
      label: "Báo cáo sự cố",
    },
  ];

  return (
    <nav className="navigationBar">
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
      </div>
    </nav>
  );
}