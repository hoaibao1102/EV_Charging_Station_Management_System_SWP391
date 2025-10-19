import React, { useEffect, useState } from 'react';
import { getProfileApi } from '../../api/profileApi.js';

export default function Information() {
    const [profile, setProfile] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

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

  return (
    <>
      {loading ? (
        <p>Đang tải thông tin...</p>
      ) : error ? (
        <div>
          <p style={{ color: 'red' }}>Lỗi: {error}</p>
          <button onClick={() => window.location.reload()}>Thử lại</button>
        </div>
      ) : profile ? (
        <div>
          <h2>Thông tin cá nhân</h2>
          <p><strong>Tên:</strong> {profile.name}</p>
          <p><strong>Email:</strong> {profile.email}</p>
          <p><strong>Số điện thoại:</strong> {profile.phone}</p>
          <p><strong>Giới tính:</strong> {profile.sex}</p>
        </div>
      ) : (
        <p>Không có thông tin</p>
      )}
    </>
  )
}