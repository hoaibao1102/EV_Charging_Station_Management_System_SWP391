import { NavLink, useLocation } from "react-router-dom";
import "./Navigate.css";
import homeIcon from "../../assets/logo/home.png";
import ruleIcon from "../../assets/logo/rule.png";
import stationsIcon from "../../assets/logo/chargingStation.png";
import bookingIcon from "../../assets/logo/booking.png";
import profileIcon from "../../assets/logo/user.png";
import styled from "styled-components";

const NavItem = styled(NavLink)`
  display: flex;
  flex-direction: column;
  align-items: center;
  text-decoration: none;
  padding: 8px;
  transition: all 0.3s ease;
  border-radius: 50%;
  min-width: 50px;
  min-height: 50px;
  justify-content: center;

  &:hover {
    transform: translateY(-2px);
  }

  &.active {
    background-color: #20b2aa;
    color: white;
    justify-content: center;
    align-items: center;

    .navIcon {
      background-color: white;
      transform: scale(1.3);
      margin-bottom: 0;

      img {
        filter: brightness(0) saturate(100%) invert(48%) sepia(79%)
          saturate(2476%) hue-rotate(148deg) brightness(95%) contrast(86%);
      }
    }

    .navLabel {
      display: none;
    }
  }
`;

export default function AppNavigation() {
  const location = useLocation();

  const navItems = [
    {
      path: "/",
      icon: homeIcon,
      label: "Trang chủ",
    },
    {
      path: "/rules",
      icon: ruleIcon,
      label: "Điều khoản",
    },
    {
      path: "/stations",
      icon: stationsIcon,
      label: "Trạm sạc",
    },
    {
      path: "/bookingInformation",
      icon: bookingIcon,
      label: "Đặt chỗ",
    },
    {
      path: "/profile",
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
      </div>
    </nav>
  );
}
