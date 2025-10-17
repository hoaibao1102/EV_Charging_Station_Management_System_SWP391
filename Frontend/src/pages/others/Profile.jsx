import React, { useEffect} from 'react';
import { useNavigate } from 'react-router-dom';
import { isAuthenticated } from '../../utils/authUtils';
import { useLogout } from '../../hooks/useAuth';
import usePaths from '../../hooks/usePath.js';
import girl from '../../assets/icon/girl.png';
// import man from '../../assets/icon/man.png';
import './Profile.css';

export default function Profile() {
  const navigate = useNavigate();
  const paths = usePaths();
  const { logout, loading } = useLogout();

  useEffect(() => {
    if (!isAuthenticated()) {
      alert("Bạn chưa đăng nhập. Vui lòng đăng nhập để tiếp tục!");
      navigate('/login');
      return;
    }
    
    // Lấy thông tin user từ localStorage
    // const profile = getUserProfile();
    // setUserProfile(profile);
  }, [navigate]);

  const handleLogout = async () => {
    const result = await logout();
    if (result.success) {
      navigate(paths.login);
    }
  };
  //thêm các path và router tương ứng , xử lý sau
  const menuItems = [
    { label: 'Thông tin chi tiết', icon: '📝', path: paths.profile }, 
    { label: 'Phương tiện của tôi', icon: '🚗', path: paths.myVehicle }, 
    { label: 'Giao dịch của tôi', icon: '💸', path: paths.myBookings }, 
    { label: 'Thay đổi mật khẩu', icon: '🔑', path: paths.changePassword }, 
    { label: 'Thông báo', icon: '🔔', path: paths.notifications }, 
    { label: 'Lịch sử sạc', icon: '🔋', path: paths.chargeHistory }, 
    { label: 'Cài đặt', icon: '🛠️', path: paths.settings }, 
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
{/* img để tạm */}
      <div className="profile-card">
        <div className="avatar-container">
          <img 
            src={girl} 
            alt="Profile Avatar" 
            className="avatar"
          />
        </div>

        {/* User Info */}
        <div className="user-info">
          <h2 className="user-name">{ 'Nguyen Van A'}</h2>
          <p className="user-email">{'nguyenvana@gmail.com'}</p>
        </div>

        {/* Menu Items */}
        <div className="menu-section">
          {menuItems.map((item, index) => (
            <div key={index} className="menu-item">
              <span className="menu-label">{item.label}</span>
              <span className="menu-arrow">{item.icon}</span>
              <navigate to={item.path} />
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

