
import Nav from 'react-bootstrap/Nav';
import { useEffect, useState, useMemo } from 'react';
import Table from 'react-bootstrap/Table';
import '../admin/ManagementUser.css';
import Header from '../../components/admin/Header.jsx';
import {toast} from 'react-toastify';
import {getAllTriplets, updateTripletStatus} from '../../api/tripletApi.js';
import AccidentDetail from '../../components/admin/AccidentDetail.jsx';


export default function Incident() {

  const [activeTab, setActiveTab] = useState('allTriplets');
  const [triplets, setTriplets] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [loading, setLoading] = useState(false);
  

  useEffect(() => {
    const fetchTriplets = async () => {
      try {
        const response = await getAllTriplets();
        if (response.success) {
          setTriplets(response.data);
          console.log('Fetched triplets:', response.data);
        }
      } catch (error) {
        console.error('Error fetching triplets:', error);
      }
    };

    fetchTriplets();
  }, [loading]);

  const handleSelect = (selectedKey) => {
    setActiveTab(selectedKey);
  };

  const handleSearchChange = (e) => {
    setSearchTerm(e.target.value.toLowerCase());
  };


  // Tính toán thống kê 
  const totalTriplets = triplets.length;
  const TripletUnresolved = triplets.filter(a => a.status === 'OPEN').length;
  const TripletResolved = triplets.filter(a => a.status === 'PAID').length;


  // Tính toán danh sách hiển thị
  const displayedTriplets = useMemo(() => {
    let filtered = triplets;

    if (activeTab !== 'allTriplets') {
      filtered = filtered.filter(triplet => triplet.status === activeTab.toUpperCase());
    }

    if (searchTerm) {
      filtered = filtered.filter(triplet =>
        triplet.driverName?.toLowerCase().includes(searchTerm) ||
        triplet.phoneNumber?.toLowerCase().includes(searchTerm)
      );
    }

    return filtered;
  }, [triplets, activeTab, searchTerm]);

  const handleMarkAsHandled = async (tripletId) => {
    try {
      const response = await updateTripletStatus(tripletId);
      if (response.success) {
        toast.success('Đánh dấu đóng phạt thành công!');
        setLoading(prev => !prev);
      }
    } catch (error) {
      console.error('Error marking triplet as resolved:', error);
    }
  };

  return (
    <>
        <div className="management-user-container">
          {/* Header Section */}
          <Header />

          {/* Action Section */}
          <div className="action-section">
            <h2>Quản lý đóng phạt</h2>
          </div>

          {/* Statistics Section */}
          <ul className="statistics-section">
            <li className="stat-card">
              Tổng tài xế bị phạt
              <strong>{totalTriplets}</strong>
            </li>
            <li className="stat-card">
              Chưa xử lý
              <strong>{TripletUnresolved}</strong>
            </li>
            <li className="stat-card">
              Đã xử lý
              <strong>{TripletResolved}</strong>
            </li>
          </ul>


          {/* Table Section */}
          <div className="table-section">
            {/* ✅ BỌC TOÀN BỘ BẢNG VÀ FILTER TRONG KHUNG CUỘN NÀY */}
            <div className="table-scroll-container">
              
              {/* ✅ FILTER SECTION ĐÃ ĐƯỢC CHUYỂN VÀO ĐÂY */}
              <div className="filter-section">
                <Nav justify variant="tabs" activeKey={activeTab} onSelect={handleSelect}>
                  <Nav.Item>
                    <Nav.Link eventKey="allTriplets">Tất cả tài khoản bị phạt</Nav.Link>
                  </Nav.Item>
                  <Nav.Item>
                    <Nav.Link eventKey="OPEN">Chưa xử lý</Nav.Link>
                  </Nav.Item>
                  <Nav.Item>
                    <Nav.Link eventKey="PAID">Đã xử lý</Nav.Link>
                  </Nav.Item>
                </Nav>
                
                <div style={{ marginTop: '15px' }}>
                  <input 
                    type="text"
                    className="search-input"
                    placeholder="🔍 Tìm kiếm theo tên hoặc số điện thoại" 
                    value={searchTerm}
                    onChange={handleSearchChange}
                  />
                </div>
              </div>
              {/* (Hết filter section) */}

              {/* Bảng nằm ngay bên dưới filter */}
              <Table className="custom-table">
                <thead>
                  <tr>
                    <th>TÊN NHÂN VIÊN</th>
                    <th>SỐ ĐIỆN THOẠI</th>
                    <th>TỔNG PHÍ PHẠT</th>
                    <th>TRẠNG THÁI</th>
                    <th>NGÀY TẠO</th>
                    <th>ĐÁNH DẤU ĐÃ XỬ LÝ</th>                  
                  </tr>
                </thead>
                <tbody>
                  {displayedTriplets.length > 0 ? (
                    displayedTriplets.map((triplet) => (
                      <tr key={triplet.tripletId}>
                        <td>{triplet.driverName}</td>
                        <td>{triplet.phoneNumber}</td>
                        <td>{triplet.totalPenalty}</td>
                        <td>{triplet.status === 'OPEN' ? 'CHƯA XỬ LÝ' : 'ĐÃ XỬ LÝ'}</td>
                        <td>{triplet.createdAt.split('T')[0]}</td>
                        <td>
                          {triplet.status === 'OPEN' && (
                            <button className='btn-edit' onClick={() => handleMarkAsHandled(triplet.tripletId)}>ĐÃ XỬ LÝ</button>
                          )}
                        </td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan="8" style={{ textAlign: 'center', padding: '30px' }}>
                        Không tìm thấy tài khoản phù hợp với yêu cầu.
                      </td>
                    </tr>
                  )}
                </tbody>
              </Table>
            </div> 
            {/* (Hết table-scroll-container) */}
          </div>
        </div>
    </>
  );
}