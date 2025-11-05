import Nav from 'react-bootstrap/Nav';
import { useEffect, useState, useMemo } from 'react';
import Table from 'react-bootstrap/Table';
import './ManagementUser.css'; 
import Header from '../../components/admin/Header.jsx';
import {toast} from 'react-toastify';
import {getAllAccidentsApi, markAccidentAsResolvedApi} from '../../api/admin.js';
import AccidentDetail from '../../components/admin/AccidentDetail.jsx';


export default function ManagementAccident() {

  const [activeTab, setActiveTab] = useState('allStations');
  const [accidents, setAccidents] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [showAccidentDetail, setShowAccidentDetail] = useState(false);
  const [loading, setLoading] = useState(false);
  const [selectedAccident, setSelectedAccident] = useState(null);
  

  useEffect(() => {
    const fetchAccidents = async () => {
      try {
        const response = await getAllAccidentsApi();
        if (response.success) {
          setAccidents(response.data);
          console.log('Fetched accidents:', response.data);
        }
      } catch (error) {
        console.error('Error fetching stations:', error);
      }
    };

    fetchAccidents();
  }, [loading]);

  const handleSelect = (selectedKey) => {
    setActiveTab(selectedKey);
  };

  const handleSearchChange = (e) => {
    setSearchTerm(e.target.value.toLowerCase());
  };


  const handleShowAccidentDetail = () => {
    setShowAccidentDetail(true);
  };

  const handleCloseForm = () => {
    setShowAccidentDetail(false);
    setLoading(pre => !pre);
  };

  // Tính toán thống kê 
  const totalAccidentsToday = accidents.filter(a => new Date(a.reportedAt).toDateString() === new Date().toDateString()).length;
  const totalUnresolved = accidents.filter(a => a.status === 'REPORTED').length;
  const totalResolved = accidents.filter(a => a.status === 'RESOLVED').length;


  // Tính toán danh sách hiển thị
  const displayedAccidents = useMemo(() => {
    let filtered = accidents;

    if (activeTab !== 'allAccidents') {
      if (activeTab === 'TODAY') {
        const today = new Date().toDateString();
        filtered = filtered.filter(accident => new Date(accident.reportedAt).toDateString() === today);
      } else {
      filtered = filtered.filter(accident => accident.status === activeTab.toUpperCase());
    }

    }

    if (searchTerm) {
      filtered = filtered.filter(accident =>
        accident.stationName?.toLowerCase().includes(searchTerm) ||
        accident.staffName?.toLowerCase().includes(searchTerm)||
        accident.title?.toLowerCase().includes(searchTerm)||
        accident.description?.toLowerCase().includes(searchTerm)
      );
    }

    return filtered;
  }, [accidents, activeTab, searchTerm]);

  const handleMarkAsHandled = async (incidentId) => {
    try {
      const response = await markAccidentAsResolvedApi(incidentId);
      if (response.success) {
        toast.success('Đánh dấu tai nạn đã xử lý thành công');
        setLoading(prev => !prev);
      }
    } catch (error) {
      console.error('Error marking accident as resolved:', error);
    }
  };

  return (
    <>
      {showAccidentDetail && <AccidentDetail handleClose={handleCloseForm} accident={selectedAccident} />}
      {!showAccidentDetail &&(
        <div className="management-user-container">
          {/* Header Section */}
          <Header />

          {/* Action Section */}
          <div className="action-section">
            <h2>Quản lý báo cáo</h2>
          </div>

          {/* Statistics Section */}
          <ul className="statistics-section">
            <li className="stat-card">
              Tổng báo cáo hôm nay
              <strong>{totalAccidentsToday}</strong>
            </li>
            <li className="stat-card">
              Chưa xử lý
              <strong>{totalUnresolved}</strong>
            </li>
            <li className="stat-card">
              Đã xử lý
              <strong>{totalResolved}</strong>
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
                    <Nav.Link eventKey="allAccidents">Tất cả báo cáo</Nav.Link>
                  </Nav.Item>
                  <Nav.Item>
                    <Nav.Link eventKey="TODAY">Báo cáo hôm nay</Nav.Link>
                  </Nav.Item>
                  <Nav.Item>
                    <Nav.Link eventKey="REPORTED">Chưa xử lý</Nav.Link>
                  </Nav.Item>
                  <Nav.Item>
                    <Nav.Link eventKey="RESOLVED">Đã xử lý</Nav.Link>
                  </Nav.Item>
                </Nav>
                
                <div style={{ marginTop: '15px' }}>
                  <input 
                    type="text"
                    className="search-input"
                    placeholder="🔍 Tìm kiếm theo trạm, nhân viên, tiêu đề, mô tả..." 
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
                    <th>TRẠM</th>
                    <th>NHÂN VIÊN BÁO CÁO</th>
                    <th>TIÊU ĐỀ</th>
                    <th>TRẠNG THÁI</th>
                    <th>NGÀY TẠO</th>
                    <th>XEM CHI TIẾT</th>
                    <th>ĐÁNH DẤU ĐÃ XỬ LÝ</th>                  
                  </tr>
                </thead>
                <tbody>
                  {displayedAccidents.length > 0 ? (
                    displayedAccidents.map((accident) => (
                      <tr key={accident.incidentId}>
                        <td>{accident.stationName}</td>
                        <td>{accident.staffName}</td>
                        <td>{accident.title}</td>
                        <td>{accident.status === 'REPORTED' ? 'CHƯA XỬ LÝ' : 'ĐÃ XỬ LÝ'}</td>
                        <td>{accident.reportedAt.split('T')[0]}</td>
                        <td>
                          <button className='btn-edit' onClick={() => {setSelectedAccident(accident); handleShowAccidentDetail();}}>Xem chi tiết</button>
                        </td>
                        <td>
                          {accident.status === 'REPORTED' && (
                            <button className='btn-edit' onClick={() => handleMarkAsHandled(accident.incidentId)}>ĐÁNH DẤU ĐÃ XỬ LÝ</button>
                          )}
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
            </div> 
            {/* (Hết table-scroll-container) */}
          </div>
        </div>
      )}
    </>
  );
}