
import Nav from 'react-bootstrap/Nav';
import { useEffect, useState, useMemo } from 'react';
import Table from 'react-bootstrap/Table';
import './ManagementUser.css'; 
import Header from '../../components/admin/Header.jsx';

import { getAllTariffs} from '../../api/tariffApi.js';
import {getConnectorTypes} from '../../api/stationApi.js';
import TariffDetail from '../../components/admin/TariffDetail.jsx';

export default function PriceConfiguration() {
  const [tariffs, setTariffs] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [loading, setLoading] = useState(false);
  const [selectedTariff, setSelectedTariff] = useState(null);
  const [activeTab, setActiveTab] = useState('allTariffs');
  const [showTariffDetail, setShowTariffDetail] = useState(false);
  const [connectorTypes, setConnectorTypes] = useState([]);
  const [inactiveConnectorTypes, setInactiveConnectorTypes] = useState([]);

  useEffect(() => {
    const fetchTariffs = async () => {
      try {
        const response = await getAllTariffs();
        if (response.success) {
          setTariffs(response.data);
          console.log('Fetched tariffs:', response.data);
        }
      } catch (error) {
        console.error('Error fetching tariffs:', error);
      }
    };

    const fetchConnectorTypes = async () => {
      try {   
        const response = await getConnectorTypes();
        if (response.success) {
          setConnectorTypes(response.data);
          console.log('Fetched connector types:', response.data);
        }
      } catch (error) {
        console.error('Error fetching connector types:', error);
      }
    };

    fetchTariffs();
    fetchConnectorTypes();
  }, [loading]);

  useEffect(() => {
    const inactiveTypes = connectorTypes.filter(type => 
      !tariffs.some(tariff => tariff.connectorTypeId === type.connectorTypeId)
    );
  
    setInactiveConnectorTypes(inactiveTypes);
    console.log('Inactive connector types ĐÃ ĐƯỢC CẬP NHẬT:', inactiveTypes);
}, [tariffs, connectorTypes]);


  const handleSearchChange = (e) => {
    setSearchTerm(e.target.value.toLowerCase());
  };


  const handleShowTariffDetail = () => {
    setShowTariffDetail(true);
  };

  const handleCloseForm = () => {
    setShowTariffDetail(false);
    setLoading(pre => !pre);
  };


  // Tính toán thống kê 
  const totalTariffs = tariffs.length;
  const totalConnectorTypes = connectorTypes.length;


  // Tính toán danh sách hiển thị
  const displayedTariffs = useMemo(() => {
    let filtered = tariffs;

    if (activeTab !== 'allTariffs') {
      const activeTabId = Number(activeTab); 
       filtered = filtered.filter(tariff => 
       tariff.connectorTypeId === activeTabId
     );
    }

    if (searchTerm) {
      filtered = filtered.filter(tariff =>
        tariff.connectorTypeName?.toLowerCase().includes(searchTerm.toLowerCase())
      );
    }

    return filtered;
  }, [tariffs, activeTab, searchTerm]);

  const handleAddTariff = () => {
    setSelectedTariff(null);
    setShowTariffDetail(true);
  }


  return (
    <>
      {showTariffDetail && <TariffDetail handleClose={handleCloseForm} tariff={selectedTariff} inactiveConnectorTypes={inactiveConnectorTypes} />}
      {!showTariffDetail &&(
        <div className="management-user-container">
          {/* Header Section */}
          <Header />

          {/* Action Section */}
          <div className="action-section">
            <h2>Quản lý cấu hình giá</h2>
            {totalTariffs !== totalConnectorTypes && (
              <button className="btn-add-staff" onClick={handleAddTariff}>
              + Thêm cấu hình giá mới
              </button>
            )}
          </div>

          {/* Statistics Section */}
          <ul className="statistics-section">
            <li className="stat-card">
              Tổng cấu hình giá
              <strong>{totalTariffs}</strong>
            </li>
            <li className="stat-card">
              Tổng loại cổng sạc
              <strong>{totalConnectorTypes}</strong>
            </li>
          </ul>


          {/* Table Section */}
          <div className="table-section">
            <div className="table-scroll-container">
              <div className="filter-section">
                <select 
                  className="form-select" 
                  value={activeTab} 
                  onChange={(e) => setActiveTab(e.target.value)}
                  style={{ marginBottom: '15px' }}
                >
                <option value="allTariffs">Tất cả loại cổng sạc</option>
                 {connectorTypes.map((type) => (
                <option key={type.connectorTypeId} value={type.connectorTypeId}>
                 {type.displayName?.toUpperCase()} 
                </option>
                ))}
              </select>
                
                <div style={{ marginTop: '15px' }}>
                  <input 
                    type="text"
                    className="search-input"
                    placeholder="🔍 Tìm kiếm theo tên cổng sạc " 
                    value={searchTerm}
                    onChange={handleSearchChange}
                  />
                </div>
              </div>
              
              <Table className="custom-table">
                <thead>
                  <tr>
                    <th>TÊN CỔNG SẠC</th>
                    <th>GIÁ MỖI kWh</th>
                    <th>GIÁ MỖI PHÚT(áp dụng khi quá giờ)</th>
                    <th>ĐƠN VỊ</th>
                    <th>CẬP NHẬT</th>               
                  </tr>
                </thead>
                <tbody>
                  {displayedTariffs.length > 0 ? (
                    displayedTariffs.map((tariff) => (
                      <tr key={tariff.id}>
                        <td>{tariff.connectorTypeName}</td>
                        <td>{tariff.pricePerKWh}</td>
                        <td>{tariff.pricePerMin}</td>
                        <td>{tariff.currency}</td>
                        <td>
                          <button className='btn-edit' onClick={() => {setSelectedTariff(tariff); handleShowTariffDetail();}}>Xem chi tiết</button>
                        </td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan="8" style={{ textAlign: 'center', padding: '30px' }}>
                        Không tìm thấy cấu hình giá phù hợp với cổng trên. Vui lòng tạo cấu hình mới.
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