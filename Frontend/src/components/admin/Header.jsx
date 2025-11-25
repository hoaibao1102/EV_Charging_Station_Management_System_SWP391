import { Form } from 'react-router-dom';
import icon_station from '../../assets/icon/staff/charging-station.png';
import icon_user from '../../assets/logo/user.png';
import './Header.css';
import FormProfile from './FormChangePassword.jsx';
import { useState } from 'react';

export default function Header() {
    const role = localStorage.getItem('role') || null;
    const [formProfile , setFormProfile] = useState(false);

    const closeProfile = () => {
        setFormProfile(false);
    }

  return (
    <>
  {formProfile && <FormProfile onClose={closeProfile} />}
  {!formProfile &&
    <div className="header-section">
        <div className="header-left">
            <img src={icon_station} alt="Icon Station" />
        </div>
        <div className="header-right">
            <img src={icon_user} onClick={() => setFormProfile(true)}  alt="Icon User" />
            <h1>Chào mừng {(role === 'STAFF' ? "nhân viên" : role === "ADMIN" ? "quản trị viên" : "người dùng")} trở lại hệ thống!</h1>             
        </div>
    </div>}
    </>
    
  )
}