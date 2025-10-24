import icon_station from '../../assets/icon/staff/charging-station.png';
import icon_user from '../../assets/logo/user.png';
import { useSelector } from 'react-redux';
import { selectUser } from '../../redux/slices/authSlice';
import Nav from 'react-bootstrap/Nav';
import { useEffect, useState, useMemo } from 'react';
import { getAllUsersApi } from '../../api/admin.js';
import Table from 'react-bootstrap/Table';
import AddStaffForm from '../../components/AddStaffForm.jsx';
import { useNavigate } from 'react-router-dom';
import paths from '../../path/paths.jsx';

export default function ManagementUser() {
  const navigator = useNavigate();
  const user = useSelector(selectUser);
  if (!user) {
    navigator(paths.login);
  }

  const [activeTab, setActiveTab] = useState('allUsers');
  const [usersList, setUsersList] = useState([]); // Danh sách GỐC
  const [searchTerm, setSearchTerm] = useState(''); // State cho ô tìm kiếm
  //biến lưu có mở form thêm nhân viên hay không
  const [showAddStaffForm, setShowAddStaffForm] = useState(false);
  

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
  }, []);

  // --- 3. HÀM CHỈ CẬP NHẬT STATE ---
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
  };


    // Tính toán thống kê 
  const totalUsers = usersList.length;
  const totalStaff = usersList.filter(u => u.roleName === 'STAFF').length;
  const totalDrivers = usersList.filter(u => u.roleName === 'DRIVER').length;

  // --- 4. TÍNH TOÁN DANH SÁCH HIỂN THỊ (SỬA LỖI LOGIC) ---
  // useMemo sẽ TỰ ĐỘNG chạy lại khi usersList, activeTab, hoặc searchTerm thay đổi
  const displayedUsers = useMemo(() => {
    let filtered = usersList;

    // Lọc theo Tab
    if (activeTab !== 'allUsers') {
      filtered = filtered.filter(user => user.roleName === activeTab.toUpperCase());
    }

    // Lọc theo Search (trên kết quả đã lọc của tab)
    if (searchTerm) {
      filtered = filtered.filter(user => 
        user.name?.toLowerCase().includes(searchTerm) ||
        user.email?.toLowerCase().includes(searchTerm) ||
        user.phoneNumber?.includes(searchTerm)
      );
    }

    return filtered;
  }, [usersList, activeTab, searchTerm]); // Phụ thuộc vào 3 state này

  
  return (
    <>
      {showAddStaffForm && <AddStaffForm onClose={handleCloseForm} />}
      {!showAddStaffForm && (
        <>
      <div>
         <img src={icon_station} style={{ width: '50px', height: '50px', color:'#20b2aa' }} alt="Icon Station" />
         <img src={icon_user} style={{ width: '50px', height: '50px', color:'#20b2aa' }} alt="Icon User" />
         <h1>Chào mừng {user.name||"quản trị viên"} trở lại hệ thống!</h1>
      </div>
      <div>
       <h2>Quản lý người dùng</h2>
       <button onClick={handleAddStaff}>Thêm nhân viên</button>
       </div>
       {/* khối thống kê nhanh */}
      <ul>
         <li>Tổng người dùng: {totalUsers} </li>
        <li>Tổng nhân viên: {totalStaff} </li>
         <li>Tổng tài xế: {totalDrivers} </li>
       </ul>

      {/* khối lọc và tìm kiếm */}
      <div>
        <div>
          <Nav justify variant="tabs" activeKey={activeTab} onSelect={handleSelect}>
            <Nav.Item>
              <Nav.Link eventKey="allUsers">Tất cả người dùng</Nav.Link>
            </Nav.Item>
            <Nav.Item>
              <Nav.Link eventKey="STAFF">Nhân viên</Nav.Link>
            </Nav.Item>
            <Nav.Item>
              <Nav.Link eventKey="DRIVER">Tài xế</Nav.Link>
            </Nav.Item>
          </Nav>
        </div>
        <div>
          {/* {ô tìm kiếm} */}
          <input 
            type="text" 
            placeholder="Tìm kiếm theo tên, email, sđt..." 
            value={searchTerm}
            onChange={handleSearchChange}
          />
        </div>
      </div>

      {/* khối bảng hiển thị danh sách người dùng */}
      <div>
        <Table>
          <thead>
            <tr>
              <th>TÊN</th>
              <th>VAI TRÒ</th>
              <th>SỐ ĐIỆN THOẠI</th>
              <th>EMAIL</th>
              <th>ĐỊA CHỈ</th>
              <th>NGÀY SINH</th>
              <th>GIỚI TÍNH</th>
            </tr>
      </thead>
          <tbody>
            {displayedUsers.map((user) => (
              <tr key={user.phone}>
                <td>{user.name}</td>
                <td>{user.roleName}</td>
                <td>{user.phoneNumber}</td>
                <td>{user.email}</td>
                <td>{user.address}</td>
                <td>{user.dateOfBirth}</td>
                <td>{user.gender}</td>
              </tr>
            ))}
          </tbody>
        </Table>
      </div>
      </>)}
    </>
  );
}