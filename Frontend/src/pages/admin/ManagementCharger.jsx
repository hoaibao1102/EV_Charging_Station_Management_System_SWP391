import Nav from 'react-bootstrap/Nav';
import { useEffect, useState, useMemo } from 'react';
import Table from 'react-bootstrap/Table';
import { useNavigate } from 'react-router-dom';
import paths from '../../path/paths.jsx';
import './ManagementUser.css';
import Header from '../../components/admin/Header.jsx';
import {getConnectorTypes} from '../../api/stationApi.js';
import AddChargerForm from '../../components/admin/AddChargerForm.jsx';

export default function ManagementCharger() {
  const navigator = useNavigate();
  const user = JSON.parse(localStorage.getItem('userDetails'));
  if (!user) {
    navigator(paths.login);
  }

  const [activeTab, setActiveTab] = useState('allChargers');
  const [chargers, setChargers] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [showAddChargerForm, setShowAddChargerForm] = useState(false);
  const [loading, setLoading] = useState(false);
  const [currentCharger, setCurrentCharger] = useState(null);
  
  // ❌ Đã xóa state phân trang
  // const [currentPage, setCurrentPage] = useState(1);
  // const [itemsPerPage] = useState(3); 

  useEffect(() => {
    const fetchChargers = async () => {
      try {
        const response = await getConnectorTypes();
        setCurrentCharger(null);
        if (response.success) {
          setChargers(response.data);
        }
      } catch (error) {
        console.error('Error fetching chargers:', error);
      }
    };
    fetchChargers();
  }, [loading]);

  const handleSelect = (selectedKey) => {
    setActiveTab(selectedKey);
    // ❌ Đã xóa setCurrentPage(1); 
  };

  const handleSetLoading = () => {
    setLoading(pre => !pre);
  };

  const handleSearchChange = (e) => {
    setSearchTerm(e.target.value.toLowerCase());
    // ❌ Đã xóa setCurrentPage(1); 
  };

  const handleEditCharger = (charger) => {
    setCurrentCharger(charger);
    setShowAddChargerForm(true);
    console.log('Editing charger with ID:', charger.connectorTypeId);
  };

  const handleAddCharger = () => {
    setShowAddChargerForm(true);
  };

  const handleCloseForm = () => {
    setShowAddChargerForm(false);
    handleSetLoading();
  };

  // Tính toán thống kê 
  const totalChargers = chargers.length;
  const totalDeprecated = chargers.filter(u => u.isDeprecated).length;
  const totalModern = chargers.filter(u => !u.isDeprecated).length;


  // Tính toán danh sách hiển thị
  const displayedChargers = useMemo(() => {
    let filtered = chargers;

    // Lọc theo Tab
    if (activeTab !== 'allChargers') {
      let valueDeprecated = null;
      if (activeTab === 'true') valueDeprecated = false;
      else if (activeTab === 'false') valueDeprecated = true; 
      filtered = filtered.filter(charger => charger.isDeprecated === valueDeprecated);
    }

    // Lọc theo Search
    if (searchTerm) {
      filtered = filtered.filter(charger => 
        charger.code?.toLowerCase().includes(searchTerm) ||
        charger.mode?.toLowerCase().includes(searchTerm) ||
        charger.displayName?.toLowerCase().includes(searchTerm)
      );
    }

    return filtered;
  }, [chargers, activeTab, searchTerm]);

 
  return (
    <>
      {showAddChargerForm && <AddChargerForm onClose={handleCloseForm} onAddSuccess={handleSetLoading} charger={currentCharger} />}
      {!showAddChargerForm && (
        <div className="management-user-container">
          {/* Header Section */}
          <Header />

          {/* Action Section */}
          <div className="action-section">
            <h2>Quản lý cổng sạc</h2>
            <button className="btn-add-staff" onClick={handleAddCharger}>
              + Thêm cổng sạc
            </button>
          </div>

          {/* Statistics Section */}
          <ul className="statistics-section">
            <li className="stat-card">
              Tổng cổng sạc
              <strong>{totalChargers}</strong>
            </li>
            <li className="stat-card">
              Cổng sạc hiện đại
              <strong>{totalModern}</strong>
            </li>
            <li className="stat-card">
              Cổng sạc lỗi thời
              <strong>{totalDeprecated}</strong>
            </li>
          </ul>

          {/* Table Section */}
          <div className="table-section">
            <div className="table-scroll-container">
              
              {/* Filter Section */}
              <div className="filter-section">
                <Nav justify variant="tabs" activeKey={activeTab} onSelect={handleSelect}>
                  <Nav.Item>
                    <Nav.Link eventKey="allChargers">Tất cả cổng sạc</Nav.Link>
                  </Nav.Item>
                  <Nav.Item>
                    <Nav.Link eventKey="true">Cổng sạc hiện đại</Nav.Link>
                  </Nav.Item>
                  <Nav.Item>
                    <Nav.Link eventKey="false">Cổng sạc lỗi thời</Nav.Link>
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
              <Table className="custom-table">
                <thead>
                  <tr>
                    <th>TÊN</th>
                    <th>MÃ CỔNG</th>
                    <th>LOẠI CỔNG</th>
                    <th>NĂNG LƯỢNG TỐI ĐA</th>
                    <th>TRẠNG THÁI</th>
                    <th>THAO TÁC</th>
                  </tr>
                </thead>
                <tbody>
                  {displayedChargers.length > 0 ? (
                    displayedChargers.map((charger) => (
                      <tr key={charger.connectorTypeId}>
                        <td>{charger.displayName}</td>
                        <td>{charger.code}</td>
                        <td>{charger.mode}</td>
                        <td>{charger.defaultMaxPowerKW} kW</td>
                        <td>{charger.isDeprecated ? 'Đã lỗi thời' : 'Mới'}</td>
                        <td>
                          <button className="btn-edit" onClick={() => handleEditCharger(charger)}>
                            Chỉnh sửa
                          </button>
                        </td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan="5" style={{ textAlign: 'center', padding: '30px' }}>
                        Không tìm thấy cổng sạc phù hợp với yêu cầu.
                      </td>
                    </tr>
                  )}
                </tbody>
              </Table>
              
            </div>
          </div>
        </div>
        

      )}
    </>
  );
}