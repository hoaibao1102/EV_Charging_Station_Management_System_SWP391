
import Nav from 'react-bootstrap/Nav';
import { useEffect, useState, useMemo } from 'react';
import Table from 'react-bootstrap/Table';
import { useNavigate } from 'react-router-dom';
import paths from '../../path/paths.jsx';
import '../admin/ManagementUser.css';
import Header from '../../components/admin/Header.jsx';
import {getAllAccidentReportsApi} from '../../api/reportApi.js';
import AddReportForm from '../../components/AddReportForm.jsx';

export default function ReportAccidents() {
  const navigator = useNavigate();
  const user = JSON.parse(localStorage.getItem('userDetails'));
  if (!user) {
    navigator(paths.login);
  }

  const [activeTab, setActiveTab] = useState('allReports');
  const [reports, setReports] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [showAddReportForm, setShowAddReportForm] = useState(false);
  const [loading, setLoading] = useState(false);
  const [resetData, setResetData] = useState(false);

  useEffect(() => {
    const fetchReports = async () => {
      setLoading(true);
      const response = await getAllAccidentReportsApi();
      if (response.success) {
        setReports(response.data);
        console.log('Fetched reports:', response.data);
      }
      setLoading(false);
    };
    fetchReports();
  }, [resetData]);

  const handleSelect = (selectedKey) => {
    setActiveTab(selectedKey);
  };


  const handleSearchChange = (e) => {
    setSearchTerm(e.target.value.toLowerCase());
  };

  const handleAddReport = () => {
    setShowAddReportForm(true);
  };

  const handleCloseForm = () => {
    setShowAddReportForm(false);
    setResetData(pre => !pre);
  };

  const totalReports = reports.length;
  const totalResolved = reports.filter(u => u.status === 'RESOLVED').length;
  const totalUnresolved = reports.filter(u => u.status === 'REPORTED').length;


  // Tính toán danh sách hiển thị
  const displayedReports = useMemo(() => {
    let filtered = reports;

    // Lọc theo Tab
    if (activeTab !== 'allReports') {
      filtered = filtered.filter(report => report.status === activeTab.toUpperCase());
    }

    // Lọc theo Search
    if (searchTerm) {
      filtered = filtered.filter(report => 
        report.stationName?.toLowerCase().includes(searchTerm) ||
        report.title?.toLowerCase().includes(searchTerm) ||
        report.description?.toLowerCase().includes(searchTerm)
      );
    }

    return filtered;
  }, [reports, activeTab, searchTerm]);

 
  return (
    <>
      {showAddReportForm && <AddReportForm onClose={handleCloseForm} />}
      {loading && (
        <div className="loading-overlay">
          Loadding....
        </div>
      )}
      {!showAddReportForm && (
        <div className="management-user-container">
          <Header />

          {/* Action Section */}
          <div className="action-section">
            <h2>Báo cáo sự cố</h2>
            <button className="btn-add-staff" onClick={handleAddReport}>
              + Thêm báo cáo
            </button>
          </div>

          {/* Statistics Section */}
          <ul className="statistics-section">
            <li className="stat-card">
              Tổng báo cáo
              <strong>{totalReports}</strong>
            </li>
            <li className="stat-card">
              Báo cáo chưa giải quyết
              <strong>{totalUnresolved}</strong>
            </li>
            <li className="stat-card">
              Báo cáo đã giải quyết
              <strong>{totalResolved}</strong>
            </li>
            
          </ul>

          {/* Table Section */}
          <div className="table-section">
            <div className="table-scroll-container">
              
              {/* Filter Section */}
              <div className="filter-section">
                <Nav justify variant="tabs" activeKey={activeTab} onSelect={handleSelect}>
                  <Nav.Item>
                    <Nav.Link eventKey="allReports">Tất cả báo cáo</Nav.Link>
                  </Nav.Item>
                  <Nav.Item>
                    <Nav.Link eventKey="REPORTED">Báo cáo chưa giải quyết</Nav.Link>
                  </Nav.Item>
                  <Nav.Item>
                    <Nav.Link eventKey="RESOLVED">Báo cáo đã giải quyết</Nav.Link>
                  </Nav.Item>
                </Nav>
                
                <div style={{ marginTop: '15px' }}>
                  <input 
                    type="text"
                    className="search-input"
                    placeholder="🔍 Tìm kiếm theo chủ đề, mô tả..." 
                    value={searchTerm}
                    onChange={handleSearchChange}
                  />
                </div>
              </div>
              <Table className="custom-table">
                <thead>
                  <tr>
                    <th>TIÊU ĐỀ</th>
                    <th>TRẠM LIÊN QUAN</th>
                    <th>NỘI DUNG</th>
                    <th>MỨC ĐỘ</th>
                    <th>BÁO CÁO NGÀY</th>
                    <th>TRẠNG THÁI</th>
                  </tr>
                </thead>
                <tbody>
                  {displayedReports.length > 0 ? (
                    displayedReports.map((report) => (
                      <tr key={report.incidentId}>
                        <td>{report.title}</td>
                        <td>{report.stationName}</td>
                        <td>{report.description}</td>
                        <td>{report.severity}</td>
                        <td>{report.reportedAt.split('T')[0]}</td>
                        <td>{report.status === 'RESOLVED' ? 'Đã giải quyết' : 'Chưa giải quyết'}</td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan="5" style={{ textAlign: 'center', padding: '30px' }}>
                        Không tìm thấy báo cáo phù hợp với yêu cầu.
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