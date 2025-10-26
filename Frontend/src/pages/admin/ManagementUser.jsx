
import { useSelector } from 'react-redux';
import { selectUser } from '../../redux/slices/authSlice';
import Nav from 'react-bootstrap/Nav';
import { useEffect, useState, useMemo } from 'react';
import { getAllUsersApi, statusStaffApi, unbanDriverApi } from '../../api/admin.js';
import Table from 'react-bootstrap/Table';
import AddStaffForm from '../../components/admin/AddStaffForm.jsx';
import { useNavigate } from 'react-router-dom';
import paths from '../../path/paths.jsx';
import './ManagementUser.css';
import Header from '../../components/admin/Header.jsx';

export default function ManagementUser() {
  const navigator = useNavigate();
  const user = useSelector(selectUser);
  if (!user) {
    navigator(paths.login);
  }

  const [activeTab, setActiveTab] = useState('allUsers');
  const [usersList, setUsersList] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [showAddStaffForm, setShowAddStaffForm] = useState(false);
  const [loading, setLoading] = useState(false);
  
  // Pagination states
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage] = useState(4); 

  useEffect(() => {
    const fetchUsers = async () => {
      try {
        const response = await getAllUsersApi();
        if (response.success) {
          setUsersList(response.data); 
        }
      } catch (error) {
        console.error('Error fetching users:', error);
      }
    };
    fetchUsers();
  }, [loading]);

  const handleSelect = (selectedKey) => {
    setActiveTab(selectedKey);
    setCurrentPage(1); // Reset về trang 1 khi chuyển tab
  };

  const handleSetLoading = () => {
    setLoading(pre => !pre);
  };

  const handleSearchChange = (e) => {
    setSearchTerm(e.target.value.toLowerCase());
    setCurrentPage(1); // Reset về trang 1 khi tìm kiếm
  };

  const handleAddStaff = () => {
    setShowAddStaffForm(true);
  };

  const handleCloseForm = () => {
    setShowAddStaffForm(false);
  };

  // Tính toán thống kê 
  const totalUsers = usersList.length;
  const totalStaff = usersList.filter(u => u.roleName === 'STAFF').length;
  const totalDrivers = usersList.filter(u => u.roleName === 'DRIVER').length;

  // Tính toán danh sách hiển thị
  const displayedUsers = useMemo(() => {
    let filtered = usersList;

    // Lọc theo Tab
    if (activeTab !== 'allUsers') {
      filtered = filtered.filter(user => user.roleName === activeTab.toUpperCase());
    }

    // Lọc theo Search
    if (searchTerm) {
      filtered = filtered.filter(user => 
        user.name?.toLowerCase().includes(searchTerm) ||
        user.email?.toLowerCase().includes(searchTerm) ||
        user.phoneNumber?.includes(searchTerm)
      );
    }

    return filtered;
  }, [usersList, activeTab, searchTerm]);

  // Pagination calculations
  const totalPages = Math.ceil(displayedUsers.length / itemsPerPage);
  const indexOfLastItem = currentPage * itemsPerPage;
  const indexOfFirstItem = indexOfLastItem - itemsPerPage;
  const currentUsers = displayedUsers.slice(indexOfFirstItem, indexOfLastItem);

  // Pagination handlers
  const handlePageChange = (pageNumber) => {
    setCurrentPage(pageNumber);
  };

  const handlePrevPage = () => {
    if (currentPage > 1) {
      setCurrentPage(currentPage - 1);
    }
  };

  const handleNextPage = () => {
    if (currentPage < totalPages) {
      setCurrentPage(currentPage + 1);
    }
  };

  // Generate page numbers
  const getPageNumbers = () => {
    const pages = [];
    const maxVisiblePages = 5;
    
    if (totalPages <= maxVisiblePages) {
      for (let i = 1; i <= totalPages; i++) {
        pages.push(i);
      }
    } else {
      if (currentPage <= 3) {
        for (let i = 1; i <= 4; i++) {
          pages.push(i);
        }
        pages.push('...');
        pages.push(totalPages);
      } else if (currentPage >= totalPages - 2) {
        pages.push(1);
        pages.push('...');
        for (let i = totalPages - 3; i <= totalPages; i++) {
          pages.push(i);
        }
      } else {
        pages.push(1);
        pages.push('...');
        pages.push(currentPage - 1);
        pages.push(currentPage);
        pages.push(currentPage + 1);
        pages.push('...');
        pages.push(totalPages);
      }
    }
    
    return pages;
  };

  const handleStatusStaff = async (staffId, status) => {
    const confirmed = window.confirm(`Bạn có chắc chắn muốn ${status === 'BANNED' ? 'xóa' : 'kích hoạt lại'} nhân viên này?`);
    if (confirmed) {
      const response = await statusStaffApi(staffId, status);
      if (response.success) {
        alert(`${status === 'BANNED' ? 'Xóa' : 'Kích hoạt lại'} nhân viên có id ${staffId} thành công `);
        setLoading(pre => !pre); 
      } else {
        alert(`${status === 'BANNED' ? 'Xóa' : 'Kích hoạt lại'} nhân viên có id ${staffId} thất bại`);
      }
    }
  };

  const handleTransferStaff = () => {
    alert('Chuyển công tác nhân viên');
  };      

  const handleDriverUnblock = async (driverId) => {
   const confirmed = window.confirm('Bạn có chắc chắn muốn gỡ lệnh khóa tài khoản tài xế này?');
    if (confirmed) {
      const response = await unbanDriverApi(driverId);
      if (response.success) {
        alert(`Gỡ lệnh khóa tài khoản tài xế có id ${driverId} thành công `);
        setLoading(pre => !pre); 
      } else {
        alert(`Gỡ lệnh khóa tài khoản tài xế có id ${driverId} thất bại`);
      }
    }
  };


  return (
    <>
      {showAddStaffForm && <AddStaffForm onClose={handleCloseForm} onAddSuccess={handleSetLoading} />}
      {!showAddStaffForm && (
        <div className="management-user-container">
          {/* Header Section */}
          <Header />

          {/* Action Section */}
          <div className="action-section">
            <h2>Quản lý người dùng</h2>
            <button className="btn-add-staff" onClick={handleAddStaff}>
              + Thêm nhân viên
            </button>
          </div>

          {/* Statistics Section */}
          <ul className="statistics-section">
            <li className="stat-card">
              Tổng người dùng
              <strong>{totalUsers}</strong>
            </li>
            <li className="stat-card">
              Tổng nhân viên
              <strong>{totalStaff}</strong>
            </li>
            <li className="stat-card">
              Tổng tài xế
              <strong>{totalDrivers}</strong>
            </li>
          </ul>

          {/* Filter Section */}
          <div className="filter-section">
            <Nav justify variant="tabs" activeKey={activeTab} onSelect={handleSelect}>
              <Nav.Item>
                <Nav.Link eventKey="allUsers">Tất cả người dùng</Nav.Link>
              </Nav.Item>
              <Nav.Item>
                <Nav.Link eventKey="ADMIN">Quản trị viên</Nav.Link>
              </Nav.Item>
              <Nav.Item>
                <Nav.Link eventKey="STAFF">Nhân viên</Nav.Link>
              </Nav.Item>
              <Nav.Item>
                <Nav.Link eventKey="DRIVER">Tài xế</Nav.Link>
              </Nav.Item>
            </Nav>
            
            <div style={{ marginTop: '15px' }}>
              <input 
                type="text"
                className="search-input"
                placeholder="🔍 Tìm kiếm theo tên, email, số điện thoại..." 
                value={searchTerm}
                onChange={handleSearchChange}
              />
            </div>
          </div>

          {/* Table Section */}
          <div className="table-section">
            <Table className="custom-table">
              <thead>
                <tr>
                  <th>TÊN</th>
                  <th>VAI TRÒ</th>
                  <th>SỐ ĐIỆN THOẠI</th>
                  <th>EMAIL</th>
                  <th>ĐỊA CHỈ</th>
                  <th>NGÀY SINH</th>
                  <th>GIỚI TÍNH</th>
                  <th>THAO TÁC</th>
                </tr>
              </thead>
              <tbody>
                {currentUsers.length > 0 ? (
                  currentUsers.map((user, index) => (
                    <tr key={user.phoneNumber || index}>
                      <td>{user.name}</td>
                      <td>{user.roleName}</td>
                      <td>{user.phoneNumber}</td>
                      <td>{user.email}</td>
                      <td>{user.address}</td>
                      <td>{user.dateOfBirth}</td>
                      <td>{user.gender}</td>
                      <td>
                        {user.roleName === 'STAFF' && user.status === 'ACTIVE' &&(
                          <div className="action-buttons">
                            <button className="btn-delete" onClick={() => handleStatusStaff(user.userId, 'BANNED')}>
                              Xóa
                            </button> 
                            <button className="btn-transfer" onClick={() => handleTransferStaff(user.userId)}>
                              Chuyển công tác
                            </button> 
                          </div>
                        )}
                        {user.roleName === 'STAFF' && user.status === 'BANNED' &&(
                          <div className="action-buttons">
                            <button className="btn-delete" onClick={() => handleStatusStaff(user.userId , 'ACTIVE')}>
                              Quay lại làm việc
                            </button> 
                          </div>
                        )}
                        {user.roleName === 'DRIVER' && user.status === 'BANNED' && (
                          <button className="btn-unblock" onClick={() => handleDriverUnblock(user.userId)}>
                            Gỡ lệnh khóa tài khoản
                          </button> 
                        )}
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="8" style={{ textAlign: 'center', padding: '30px' }}>
                      Không tìm thấy người dùng phù hợp với yêu cầu.
                    </td>
                  </tr>
                )}
              </tbody>
            </Table>

            {/* Pagination */}
            {displayedUsers.length > 0 && (
              <div className="pagination-section">
                <button 
                  className="btn-page" 
                  onClick={handlePrevPage}
                  disabled={currentPage === 1}
                >
                  ‹ Trước
                </button>

                {getPageNumbers().map((page, index) => (
                  page === '...' ? (
                    <span key={`ellipsis-${index}`} style={{ padding: '0 5px' }}>...</span>
                  ) : (
                    <button
                      key={page}
                      className={`btn-page ${currentPage === page ? 'active' : ''}`}
                      onClick={() => handlePageChange(page)}
                    >
                      {page}
                    </button>
                  )
                ))}

                <button 
                  className="btn-page"
                  onClick={handleNextPage}
                  disabled={currentPage === totalPages}
                >
                  Sau ›
                </button>

                <span className="pagination-info">
                  Trang {currentPage} / {totalPages} - Hiển thị {indexOfFirstItem + 1} đến {Math.min(indexOfLastItem, displayedUsers.length)} của {displayedUsers.length} người dùng
                </span>
              </div>
            )}
          </div>
        </div>
      )}
    </>
  );
}