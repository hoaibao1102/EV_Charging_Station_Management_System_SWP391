import Nav from 'react-bootstrap/Nav';
import { useEffect, useState, useMemo } from 'react';
import { 
  getAllUsersApi, 
  statusStaffApi, 
  unbanDriverApi, 
  getStaffs_UserApi, 
  getStaffs_StationApi // <-- 1. IMPORT THÊM API
} from '../../api/admin.js';
import { getAllStations } from '../../api/stationApi.js'; // <-- 2. IMPORT THÊM API
import Table from 'react-bootstrap/Table';
import AddStaffForm from '../../components/admin/AddStaffForm.jsx';
import { useNavigate } from 'react-router-dom';
import paths from '../../path/paths.jsx';
import './ManagementUser.css';
import Header from '../../components/admin/Header.jsx';
import SelectStationForm from '../../components/admin/SelectStationForm.jsx';
import { toast } from 'react-toastify';

export default function ManagementUser() {
  const navigator = useNavigate();
  const user = JSON.parse(localStorage.getItem('userDetails'));
  if (!user) {
    navigator(paths.login);
  }

  const [activeTab, setActiveTab] = useState('allUsers');
  const [usersList, setUsersList] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [showAddStaffForm, setShowAddStaffForm] = useState(false);
  const [showSelectStationForm, setShowSelectStationForm] = useState(false);
  const [loading, setLoading] = useState(false);
  const [selectedStaff, setSelectedStaff] = useState(null);
  const [staffsUserData, setStaffsUserData] = useState([]);

  // --- 3. THÊM STATE CHO DỮ LIỆU MỚI ---
  const [staffsStationData, setStaffsStationData] = useState([]);
  const [stations, setStations] = useState([]);
  // ----------------------------------------

  useEffect(() => {
    const fetchAllData = async () => {
      try {
        console.log('Starting to fetch all data...');
        // 1. Chạy tất cả các promise song song
        const [
          usersResponse,
          staffsUserResponse,
          staffsStationResponse,
          stationsResponse
        ] = await Promise.all([
          getAllUsersApi(),
          getStaffs_UserApi(),
          getStaffs_StationApi(),
          getAllStations()
        ]);

        // 2. Cập nhật TẤT CẢ state cùng một lúc (hoặc gần như cùng lúc)
        if (usersResponse.success) {
          setUsersList(usersResponse.data);
        } else {
          console.error('Failed to fetch users');
        }

        if (staffsUserResponse.success) {
          setStaffsUserData(staffsUserResponse.data);
          // Thêm log để kiểm tra
          console.log('Fetched staffs-user data:', staffsUserResponse.data);
        } else {
          console.error('Failed to fetch staffs-user data');
        }

        if (staffsStationResponse.success) {
          setStaffsStationData(staffsStationResponse.data);
          // Thêm log để kiểm tra
          console.log('Fetched staffs-station data:', staffsStationResponse.data);
        } else {
          console.error('Failed to fetch staffs-station data');
        }

        if (stationsResponse.success) {
          setStations(stationsResponse.data);
          // Thêm log để kiểm tra
          console.log('Fetched stations data:', stationsResponse.data);
        } else {
          console.error('Failed to fetch stations');
        }

        console.log('All data fetched and state updated.');

      } catch (error) {
        console.error('Error fetching data with Promise.all:', error);
      }
    };
    fetchAllData();
  }, [loading]); 

  const handleSelect = (selectedKey) => {
    setActiveTab(selectedKey);
  };

  const handleSearchChange = (e) => {
    setSearchTerm(e.target.value.toLowerCase());
  };

  const handleAddStaff = () => {
    setShowAddStaffForm(true);
  };

  const handleCloseForm = () => {
    setShowAddStaffForm(false);
    setShowSelectStationForm(false);
    setSelectedStaff(null);
  };

  // Hàm này ĐÓNG form VÀ TẢI LẠI DỮ LIỆU (dùng khi submit THÀNH CÔNG)
  const handleActionSuccess = () => {
    setShowAddStaffForm(false);
    setShowSelectStationForm(false);
    setSelectedStaff(null);
    
    // 2. THÊM MỘT ĐỘ TRỄ NHỎ để đảm bảo database đã cập nhật
    setTimeout(() => {
      setLoading(pre => !pre); // Trigger useEffect refetch
      console.log('Refetching data after success...');
    }, 1000); 
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
  
  // --- 6. HÀM LOGIC ĐỂ LẤY TÊN TRẠM TỪ USERID ---
  const getStationNameByUserId = (userId) => {
    try {
      // 1. Từ userId -> staffId (qua bảng staffsUserData)
      const staffUser = staffsUserData.find(su => su.userId === userId);
      if (!staffUser || !staffUser.staffId) return null;
  
      // 2. Từ staffId -> stationId (qua bảng staffsStationData)
      const staffStation = staffsStationData.find(ss => ss.staffId === staffUser.staffId);
      if (!staffStation || !staffStation.stationId) return null;
  
      // 3. Từ stationId -> stationName (qua bảng stations)
      const station = stations.find(s => s.stationId === staffStation.stationId);
      return station ? station.stationName : null;
    } catch (e) {
      console.error("Error finding station name:", e);
      return null;
    }
  };
  // -----------------------------------------------

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

  const handleTransferStaff = (staff) => {
    toast.info('Bạn có thể chuyển công tác nhân viên này');
    setSelectedStaff(staff);
    setShowSelectStationForm(true);
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
      {showSelectStationForm && <SelectStationForm onClose={handleCloseForm} onAddSuccess={handleActionSuccess} staff={selectedStaff} stations={stations} staffsStationData={staffsStationData}/>}
      {showAddStaffForm && <AddStaffForm onClose={handleCloseForm} onAddSuccess={handleActionSuccess} />}
      {!showAddStaffForm && !showSelectStationForm && (
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

          {/* Table Section */}
          <div className="table-section">
            <div className="table-scroll-container"> 
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
                
                <div style={{ marginTop: '5px' }}>
                  <input 
                    type="text"
                    className="search-input"
                    placeholder="🔍 Tìm kiếm theo tên, email, số điện thoại..." 
                    value={searchTerm}
                    onChange={handleSearchChange}
                  />
                </div>
              </div>

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
                  {displayedUsers.length > 0 ? (
                    displayedUsers.map((user, index) => {
                      
                      // --- 7. GỌI HÀM HELPER TRƯỚC KHI RENDER ---
                      const stationName = user.roleName === 'STAFF' 
                                          ? getStationNameByUserId(user.userId) 
                                          : null;

                      return (
                        <tr key={user.phoneNumber || index}>
                          <td>{user.name}</td>
                          
                          {/* --- 8. CẬP NHẬT HIỂN THỊ --- */}
                          <td>
                            {user.roleName === 'STAFF' 
                              ? `NHÂN VIÊN ${stationName ? `(${stationName})` : ''}` 
                              : user.roleName === 'ADMIN' ? 'QUẢN TRỊ VIÊN' : 'TÀI XẾ'
                            }
                          </td>
                          {/* --------------------------- */}

                          <td>{user.phoneNumber}</td>
                          <td>{user.email}</td>
                          <td>{user.address}</td>
                          <td>{user.dateOfBirth}</td>
                          <td>{user.gender === 'M' ? 'Nam' : 'Nữ'}</td>
                          <td>
                            {user.roleName === 'STAFF' && user.status === 'ACTIVE' &&(
                              <div className="action-buttons">
                                <button className="btn-delete" onClick={() => handleStatusStaff(user.userId, 'BANNED')}>
                                  Xóa
                                </button> 
                                <button className="btn-transfer" onClick={() => handleTransferStaff(staffsUserData.find(s => s.userId === user.userId))}>
                                  Công tác
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
                              <div className="action-buttons">
                                <button className="btn-unblock" onClick={() => handleDriverUnblock(user.userId)}>
                                  Gỡ lệnh khóa tài khoản
                                </button> 
                              </div>
                            )}
                          </td>
                        </tr>
                      );
                    }) 
                  ) : (
                    <tr>
                      <td colSpan="8" style={{ textAlign: 'center', padding: '30px' }}>
                        Không tìm thấy người dùng phù hợp với yêu cầu.
                      </td>
                    </tr>
                  )}
                </tbody>
              </Table>
            </div>
            {/* (Hết table-scroll-container) */}
          </div>
          {/* (Hết table-section) */}
        </div>
      )}
    </>
  );
}