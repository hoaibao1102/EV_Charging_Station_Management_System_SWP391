import { useSelector } from 'react-redux';
import { selectUser } from '../../redux/slices/authSlice';
import Nav from 'react-bootstrap/Nav';
import { useEffect, useState, useMemo } from 'react';
import {getAllStations, updateStationStatus} from '../../api/stationApi.js';
import Table from 'react-bootstrap/Table';
import AddStaffForm from '../../components/admin/AddStaffForm.jsx';
import { useNavigate } from 'react-router-dom';
import paths from '../../path/paths.jsx';
import './ManagementUser.css';
import Header from '../../components/admin/Header.jsx';
import {toast} from 'react-toastify';
import AddStationForm from '../../components/admin/AddStationForm.jsx';

export default function ManagementStation() {
  const navigator = useNavigate();
  const user = useSelector(selectUser);
  if (!user) {
    navigator(paths.login);
  }

  const [activeTab, setActiveTab] = useState('allStations');
  const [stations, setStations] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [showAddStationForm, setShowAddStationForm] = useState(false);
  const [loading, setLoading] = useState(false);
  const status = ['ACTIVE', 'MAINTENANCE', 'INACTIVE'];
  
  // Pagination states
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage] = useState(2); 

  useEffect(() => {
    const fetchStations = async () => {
      try {
        const response = await getAllStations();
        if (response.success) {
          setStations(response.data);
          console.log('Fetched stations:', response.data);
        }
      } catch (error) {
        console.error('Error fetching stations:', error);
      }
    };
    fetchStations();
  }, [loading]);

  const handleSelect = (selectedKey) => {
    setActiveTab(selectedKey);
    setCurrentPage(1); 
  };

  const handleSearchChange = (e) => {
    setSearchTerm(e.target.value.toLowerCase());
    setCurrentPage(1); 
  };


  const handleAddStation = () => {
    setShowAddStationForm(true);
  };

  const handleCloseForm = () => {
    setShowAddStationForm(false);
  };
// ============================================================================================
  const handleStatusStation = async (stationId, newStatus) => {
    const confirmChange = window.confirm(`Bạn có chắc chắn muốn thay đổi trạng thái trạm sạc này ?`);
    if (!confirmChange) return;
    try {
      console.log('Updating status for station:', stationId, 'to', newStatus);
      const response = await updateStationStatus(stationId, newStatus);
      if (response.success) {
        setLoading(!loading);
        toast.success('Cập nhật trạng thái trạm sạc thành công');
      }
    } catch (error) {
      toast.error('Cập nhật trạng thái trạm sạc thất bại');
      console.error('Error updating station status:', error);
    }
  };

  const handleUpdateStation = () => {
    console.log("Update station info");
  }

  const handleConfigSlotTime = () => {
    console.log("Config slot time");
  }

   const handleSetLoading = () => {
    setLoading(pre => !pre);
  };

  // =======================================================================================

  // Tính toán thống kê 
  const totalStations = stations.length;
  const totalActive = stations.filter(s => s.status === 'ACTIVE').length;
  const totalMaintenance = stations.filter(s => s.status === 'MAINTENANCE').length;
  const totalInactive = stations.filter(s => s.status === 'INACTIVE').length;

  // Tính toán danh sách hiển thị
  const displayedStations = useMemo(() => {
    let filtered = stations;

    // Lọc theo Tab
    if (activeTab !== 'allStations') {
      filtered = filtered.filter(station => station.roleName === activeTab.toUpperCase());
    }

    // Lọc theo Search
    if (searchTerm) {
      filtered = filtered.filter(station => 
        station.stationName?.toLowerCase().includes(searchTerm) ||
        station.address?.toLowerCase().includes(searchTerm) 
      );
    }

    return filtered;
  }, [stations, activeTab, searchTerm]);

  // Pagination calculations
  const totalPages = Math.ceil(displayedStations.length / itemsPerPage);
  const indexOfLastItem = currentPage * itemsPerPage;
  const indexOfFirstItem = indexOfLastItem - itemsPerPage;
  const currentStations = displayedStations.slice(indexOfFirstItem, indexOfLastItem);

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


  return (
    <>
      {showAddStationForm && <AddStationForm onClose={handleCloseForm} onAddSuccess={handleSetLoading} />}
      {!showAddStationForm && (
        <div className="management-user-container">
          {/* Header Section */}
          <Header />

          {/* Action Section */}
          <div className="action-section">
            <h2>Quản lý trạm sạc</h2>
            <button className="btn-add-staff" onClick={handleAddStation}>
              + Thêm trạm sạc
            </button>
          </div>

          {/* Statistics Section */}
          <ul className="statistics-section">
            <li className="stat-card">
              Tổng trạm sạc
              <strong>{totalStations}</strong>
            </li>
            <li className="stat-card">
              Đang hoạt động
              <strong>{totalActive}</strong>
            </li>
            <li className="stat-card">
              Đang bảo trì
              <strong>{totalMaintenance}</strong>
            </li>
            <li className="stat-card">
              Ngưng hoạt động
              <strong>{totalInactive}</strong>
            </li>
          </ul>

          {/* Filter Section */}
          <div className="filter-section">
            <Nav justify variant="tabs" activeKey={activeTab} onSelect={handleSelect}>
              <Nav.Item>
                <Nav.Link eventKey="allStations">Tất cả trạm sạc</Nav.Link>
              </Nav.Item>
              <Nav.Item>
                <Nav.Link eventKey="ACTIVE">Đang hoạt động</Nav.Link>
              </Nav.Item>
              <Nav.Item>
                <Nav.Link eventKey="MAINTENANCE">Đang bảo trì</Nav.Link>
              </Nav.Item>
              <Nav.Item>
                <Nav.Link eventKey="INACTIVE">Ngưng hoạt động</Nav.Link>
              </Nav.Item>
            </Nav>
            
            <div style={{ marginTop: '15px' }}>
              <input 
                type="text"
                className="search-input"
                placeholder="🔍 Tìm kiếm theo tên, vị trí..." 
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
                  <th>ĐỊA CHỈ</th>
                  <th>CHỈ ĐƯỜNG</th>
                  <th>GIỜ HOẠT ĐỘNG</th>
                  <th>TRẠNG THÁI</th>
                  <th>NGÀY THÀNH LẬP</th>
                  <th style={{ width: '220px' }}>HÀNH ĐỘNG</th>
                  <th>THÔNG TIN VÀ CẤU HÌNH</th>
                </tr>
              </thead>
              <tbody>
                {currentStations.length > 0 ? (
                  currentStations.map((station) => (
                    <tr key={station.stationId}>
                      <td>{station.stationName}</td>
                      <td>{station.address}</td>
                      <td>
                        <a href={`https://www.google.com/maps?q=${station.latitude},${station.longitude}`} 
                        target="_blank" rel="noopener noreferrer">
                            Xem trên bản đồ
                        </a>
                      </td>
                      <td>{station.operatingHours}</td>
                      <td>{station.status}</td>
                      <td>{station.createdAt.split('T')[0]}</td>
                      <td>                        
                        {status.map((s) => (
                          s !== station.status && (
                            <div className="action-buttons" key={s}>
                              <button className={s === 'INACTIVE' ? "btn-delete" : s ==="ACTIVE" ? "btn-unblock" : "btn-transfer"} onClick={() => handleStatusStation(station.stationId, s)}>
                                {s === 'MAINTENANCE' ? 'BẢO TRÌ' : s === 'ACTIVE' ? 'KÍCH HOẠT' : 'NGƯNG HOẠT ĐỘNG'}
                                </button>
                              </div>
                            )
                          ))}
                      </td>
                      <td>
                        <button className='btn-edit' onClick={handleUpdateStation}>Sửa thông tin</button>
                        <button className='btn-edit' onClick={handleConfigSlotTime}>Cấu hình thời gian mỗi slot</button>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="8" style={{ textAlign: 'center', padding: '30px' }}>
                      Không tìm thấy trạm sạc phù hợp với yêu cầu.
                    </td>
                  </tr>
                )}
              </tbody>
            </Table>

            {/* Pagination */}
            {displayedStations.length > 0 && (
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
                  Trang {currentPage} / {totalPages} - Hiển thị {indexOfFirstItem + 1} đến {Math.min(indexOfLastItem, displayedStations.length)} của {displayedStations.length} trạm sạc
                </span>
              </div>
            )}
          </div>
        </div>
      )}
    </>
  );
}