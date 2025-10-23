import React, { useEffect} from 'react';
import { useNavigate } from 'react-router-dom';
import { isAuthenticated } from '../../utils/authUtils.js';
import { toast } from 'react-toastify';
import { useLogout } from '../../hooks/useAuth.js';
import paths from '../../path/paths.jsx';
import girl from '../../assets/icon/girl.png';
import man from '../../assets/icon/man.png';
import './Profile.css';

export default function Profile() {
  const navigate = useNavigate();
  const userName = localStorage.getItem("userName");
  const userMail = localStorage.getItem("userMail");
  const userPhone = localStorage.getItem("userPhone");
  const userSex = localStorage.getItem("userSex");
  // const [userProfile, setUserProfile] = useState(null);
  
  const { logout, loading } = useLogout();

  useEffect(() => {
        if (!isAuthenticated()) {
          toast.warning("Bạn chưa đăng nhập. Vui lòng đăng nhập để có thể đặt chỗ!", {
            position: "top-center",
            autoClose: 3000,
          });
          navigate(paths.login);
          return;
        }
      }, [navigate, paths.login]);

  const handleLogout = async () => {
    const result = await logout();
    if (result.success) {
      navigate(paths.login);
    }
  };
  const menuItems = [
    { label: 'Thông tin chi tiết', icon: '📝', path: paths.myInformation }, 
    { label: 'Phương tiện của tôi', icon: '🚗', path: paths.myVehicle }, 
    { label: 'Giao dịch của tôi', icon: '💸', path: paths.myBookings }, 
    { label: 'Thay đổi thông tin', icon: '🛠️', path: paths.editProfile }, 
    { label: 'Thông báo', icon: '🔔', path: paths.notifications }, 
    { label: 'Lịch sử sạc', icon: '🔋', path: paths.chargeHistory }, 
];

  if (!isAuthenticated()) {
    return <p>Đang kiểm tra trạng thái đăng nhập...</p>;
  }

  return (
    <div className="profile-container">
      <div className="profile-header">
        <div className="ev-background">
        </div>
      </div>
      <div className="profile-card">
        <div className="avatar-container">
          <img 
            src={userSex === 'M' ? man : girl} 
            alt="Profile Avatar" 
            className="avatar"
          />
        </div>

        {/* User Info */}
        <div className="user-info">
          <h2 className="user-name">{userName}</h2>
          <p className="user-email">{userMail} || {userPhone}</p>
        </div>

        {/* Menu Items */}
        <div className="menu-section">
          {menuItems.map((item, index) => (
            <div 
              key={index} 
              className="menu-item"
              onClick={() => navigate(item.path)}
            >
              <span className="menu-label">{item.label}</span>
              <span className="menu-arrow">{item.icon}</span>
            </div>
          ))}
        </div>

        {/* Logout Button */}
        <div className="logout-section">
          <button 
            className="logout-btn" 
            onClick={handleLogout}
            disabled={loading}
          >
            <span className="logout-icon">🚪</span>
            <span className="logout-text">
              {loading ? 'Đang đăng xuất...' : 'Đăng xuất'}
            </span>
          </button>
        </div>
      </div>
    </div>
  );
}

