import { NavLink, useLocation, useNavigate } from "react-router-dom";
import "./DriverNavigate.css";
import homeIcon from "../../assets/logo/home.png";
import ruleIcon from "../../assets/logo/rule.png";
import stationsIcon from "../../assets/logo/chargingStation.png";
import bookingIcon from "../../assets/logo/booking.png";
import profileIcon from "../../assets/logo/user.png";
import paths from "../../path/paths.jsx";
import {isAuthenticated} from "../../utils/authUtils.js";
import classed from "../../assets/css/Main.module.css";
import { useSelector } from "react-redux";

export default function AppNavigation() {
  const location = useLocation();
  const navigate = useNavigate();
  const isLoggedIn = useSelector(isAuthenticated);

  const navItems = [
    {
      path: paths.home,
      icon: homeIcon,
      label: "Trang chủ",
    },
    {
      path: paths.rules,
      icon: ruleIcon,
      label: "Điều khoản",
    },
    {
      path: paths.stations,
      icon: stationsIcon,
      label: "Trạm sạc",
    },
    {
      path: paths.booking,
      icon: bookingIcon,
      label: "Đặt chỗ",
    },
    {
      path: paths.profile,
      icon: profileIcon,
      label: "Hồ sơ",
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
      {!isLoggedIn && <button className={classed.button} onClick={() => navigate(paths.login)}>ĐĂNG NHẬP</button>}
      </div>
    </nav>
  );
}