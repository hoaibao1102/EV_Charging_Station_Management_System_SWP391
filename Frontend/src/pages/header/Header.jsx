import React from 'react';
import { Link } from 'react-router-dom';

function Header() {
  return (
    <header style={{ padding: '1rem', background: '#333', color: '#fff' }}>
      <nav>
        <ul style={{ display: 'flex', listStyle: 'none', gap: '1rem' }}>
          <li><Link to="/" style={{ color: '#fff', textDecoration: 'none' }}>HỆ THỐNG SẠC XE ĐIỆN</Link></li>
          <li><Link to="/login" style={{ color: '#fff', textDecoration: 'none' , float: 'right'}}>Đăng nhập</Link></li>
          <li><Link to="/register" style={{ color: '#fff', textDecoration: 'none' }}>Đăng xuất</Link></li>
        </ul>
      </nav>
    </header>
  );
}

export default Header;
