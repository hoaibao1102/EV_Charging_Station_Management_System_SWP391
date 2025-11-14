import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getProfileApi } from '../../api/driverApi.js';
import './Information.css';
import man from '../../assets/icon/man.png';
import girl from '../../assets/icon/girl.png';
import FormProfile from '../../components/admin/FormChangePassword.jsx';

export default function Information() {
    const navigate = useNavigate();
    const [profile, setProfile] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [formProfile, setFormProfile] = useState(false);

    const closeProfile = () => {
        setFormProfile(false);
    }

    useEffect(() => {
        const fetchProfile = async () => {
            setLoading(true);
            setError(null);
            
            const response = await getProfileApi();
            
            if (response.success) {
                console.log('Profile data:', response.data);
                setProfile(response.data);
            } else {
                console.error('Failed to fetch profile:', response.message);
                setError(response.message || 'Không thể tải thông tin');
            }
            
            setLoading(false);
        };
        
        fetchProfile();
    }, []);

    // Format date
    const formatDate = (dateString) => {
        if (!dateString) return '';
        const date = new Date(dateString);
        const day = String(date.getDate()).padStart(2, '0');
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const year = date.getFullYear();
        return `${day}/${month}/${year}`;
    };

    return (
        <div className="information-page">
            {formProfile && <FormProfile onClose={closeProfile} />}
            {!formProfile && (
            <>
            {loading ? (
                <div className="information-container">
                    <div className="info-loading">
                        <div className="info-spinner"></div>
                        <p>Đang tải thông tin...</p>
                    </div>
                </div>
            ) : error ? (
                <div className="information-container">
                    <div className="info-error">
                        <p>❌ Lỗi: {error}</p>
                        <button 
                            className="info-retry-btn"
                            onClick={() => window.location.reload()}
                        >
                            Thử lại
                        </button>
                    </div>
                </div>
            ) : profile ? (
                <div className="information-container">
                    {/* Back Button */}
                    <button 
                        className="info-back-btn"
                        onClick={() => navigate(-1)}
                        aria-label="Quay lại"
                    >
                        ←
                    </button>

                    {/* Avatar Section */}
                    <div className="info-avatar-section">
                        <div className="info-avatar-wrapper">
                            <img 
                                src={profile.gender === 'M' ? man : girl} 
                                alt={profile.name}
                                className="info-avatar"
                            />
                        </div>
                        <h1 className="info-name">{profile.name}</h1>
                        
                        {/* Button Group */}
                        <div className="info-button-group">
                            <button 
                                className="info-edit-btn"
                                onClick={() => navigate('/profile/edit', { state: { profile } })}
                            >
                                <span className="info-edit-icon">✏️</span>
                                Chỉnh sửa
                            </button>

                            <button 
                                className="info-edit-btn"
                                onClick={() => setFormProfile(true)}
                            >
                                <span className="info-edit-icon">🔒</span>
                                Đổi mật khẩu
                            </button>
                        </div>
                    </div>

                    {/* Information Fields */}
                    <div className="info-fields">
                        <div className="info-field">
                            <label className="info-label">Email:</label>
                            <div className="info-value">{profile.email}</div>
                        </div>

                        <div className="info-field">
                            <label className="info-label">Địa chỉ:</label>
                            <div className="info-value">{profile.address || 'Chưa cập nhật'}</div>
                        </div>

                        <div className="info-field">
                            <label className="info-label">SĐT:</label>
                            <div className="info-value">{profile.phoneNumber}</div>
                        </div>

                        <div className="info-field">
                            <label className="info-label">Ngày sinh:</label>
                            <div className="info-value">{formatDate(profile.dateOfBirth)}</div>
                        </div>

                        <div className="info-field">
                            <label className="info-label">Ngày đăng ký:</label>
                            <div className="info-value">{formatDate(profile.createdAt)}</div>
                        </div>

                        <div className="info-field">
                            <label className="info-label">Trạng thái tài khoản:</label>
                            <div>
                                <span className={`info-status-badge ${profile.status === 'ACTIVE' ? 'active' : 'inactive'}`}>
                                    <span className="info-status-dot"></span>
                                    {profile.status === 'ACTIVE' ? 'Đang hoạt động' : 'Đang ngưng hoạt động'}
                                </span>
                            </div>
                            {profile.status !== 'ACTIVE' && (
                                <button className="info-activate-btn">Kích hoạt tài khoản</button>
                            )}
                        </div>
                    </div>
                </div>
            ) : (
                <div className="information-container">
                    <div className="info-empty">
                        <p>Không có thông tin</p>
                    </div>
                </div>
            )}
            </>
            )}
        </div>
    );
}