import icon_station from '../../assets/icon/staff/charging-station.png';
import icon_user from '../../assets/logo/user.png';
import './Header.css';

export default function Header() {
    const role = localStorage.getItem('role') || null;
    const user = localStorage.getItem('userDetails') ? JSON.parse(localStorage.getItem('userDetails')) : null;
  return (
    <div className="header-section">
        <div className="header-left">
            <img src={icon_station} alt="Icon Station" />
        </div>
        <div className="header-right">
            <img src={icon_user} alt="Icon User" />
            <h1>Chào mừng {user?.name || role === 'ADMIN'? "quản trị viên" : "nhân viên"} trở lại hệ thống!</h1>             
        </div>
    </div>
  )
}