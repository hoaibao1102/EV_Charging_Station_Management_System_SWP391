import Header from '../../components/admin/Header.jsx';
import { getAdminStatisticsApi } from '../../api/admin.js';
import { useEffect, useState, useMemo } from 'react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
} from 'recharts';

import './Dashboard.css';
import './ManagementUser.css';

// --- Màu sắc cho biểu đồ Donut ---
const COLORS = {
  high: '#00C49F', // Xanh lá
  medium: '#FFBB28', // Vàng
  low: '#FF8042', // Cam
};
// Định dạng cho Tooltip (khi di chuột)
const formatCurrency = (value) => `${value.toLocaleString()} VND`;

export default function AdminDashboard() {
  const [info, setInfo] = useState(null);

  useEffect(() => {
    const fetchStatistics = async () => {
      try {
        const result = await getAdminStatisticsApi();
        if (result.success) {
          setInfo(result.data);
          console.log('Admin statistics data (raw):', result);
        }
      } catch (error) {
        console.error('Error fetching admin statistics:', error);
      }
    };

    fetchStatistics();
  }, []);

  // --- 2. XỬ LÝ DỮ LIỆU (Giữ nguyên) ---

  const topStations = useMemo(() => {
    if (!info || !info.stationRows) return [];
    return [...info.stationRows]
      .sort((a, b) => b.monthRevenue.amount - a.monthRevenue.amount)
      .slice(0, 4);
  }, [info]);

  const utilizationCounts = useMemo(() => {
    const counts = { high: 0, medium: 0, low: 0 };
    if (!info || !info.stationRows) return counts;
    info.stationRows.forEach(station => {
      if (station.utilization > 0.7) counts.high += 1;
      else if (station.utilization < 0.3) counts.low += 1;
      else counts.medium += 1;
    });
    return counts;
  }, [info]);

  const utilizationDataForChart = useMemo(() => {
    const data = [
      { name: 'Cao (> 70%)', value: utilizationCounts.high },
      { name: 'TB (30-70%)', value: utilizationCounts.medium },
      { name: 'Thấp (< 30%)', value: utilizationCounts.low },
    ];
    return data.filter(item => item.value > 0);
  }, [utilizationCounts]);


  // Trạng thái Loading
  if (!info) {
    return (
      <div className="dashboard-container">
        <Header />
        <h1 className="dashboard-header">Tổng quan mạng lưới sạc trong hệ thống</h1>
        <div>Đang tải dữ liệu...</div>
      </div>
    );
  }

  // --- 3. RENDER GIAO DIỆN (Giữ nguyên) ---
  return (
    <>
      <div className="dashboard-container">
        <Header />
        
        <h1 className="dashboard-header">Tổng quan mạng lưới sạc trong hệ thống</h1>
        
        {/* --- KHU VỰC KPI --- */}
        <div className="kpi-grid">
          <div className="kpi-card">
            <span className="kpi-title">Tổng doanh thu</span>
            <span className="kpi-value">{info?.energyRevenueTotal?.amount.toLocaleString()}</span>
            <span className="kpi-unit">VND</span>
          </div>
          <div className="kpi-card">
            <span className="kpi-title">Tổng năng lượng</span>
            <span className="kpi-value">{info?.totalEnergyKWh?.toLocaleString()}</span>
            <span className="kpi-unit">kWh</span>
          </div>
          <div className="kpi-card">
            <span className="kpi-title">Tổng phiên sạc</span>
            <span className="kpi-value">{info?.totalSessions?.toLocaleString()}</span>
            <span className="kpi-unit">Phiên</span>
          </div>
          <div className="kpi-card">
            <span className="kpi-title">Doanh thu TB/phiên</span>
            <span className="kpi-value">{info?.avgRevenuePerSession?.amount.toLocaleString()}</span>
            <span className="kpi-unit">VND</span>
          </div>
        </div>

        {/* --- KHU VỰC BIỂU ĐỒ VÀ BẢNG --- */}
        <div className="charts-grid">
          
          {/* Biểu đồ 1: Top 5 Trạm (Bar Chart) */}
          <div className="chart-placeholder">
            <h2 className="chart-title">Top Trạm (Doanh thu tháng)</h2>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart
                data={topStations}
                margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
              >
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="stationName" fontSize={12} />
                <YAxis tickFormatter={formatCurrency} fontSize={12} />
                <Tooltip formatter={formatCurrency} />
                <Legend />
                <Bar 
                  dataKey="monthRevenue.amount"
                  name="Doanh thu (VND)"
                  fill="#20b2aa"
                />
              </BarChart>
            </ResponsiveContainer>
          </div>

          {/* Biểu đồ 2: Phân bổ Hiệu suất (Donut Chart) */}
          <div className="chart-placeholder">
            <h2 className="chart-title">Phân Bổ Hiệu Suất (Utilization)</h2>
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie
                  data={utilizationDataForChart}
                  dataKey="value"
                  nameKey="name"
                  cx="50%"
                  cy="50%"
                  innerRadius={60}
                  outerRadius={80}
                  fill="#20b2aa"
                  paddingAngle={5}
                  labelLine={true}
                  label={({
                    cx,
                    cy,
                    midAngle,
                    outerRadius,
                    name,
                    value,
                  }) => {
                    const RADIAN = Math.PI / 180;
                    const radius = outerRadius + 15; // Khoảng cách từ vòng ra
                    const x = cx + radius * Math.cos(-midAngle * RADIAN);
                    const y = cy + radius * Math.sin(-midAngle * RADIAN);
                    // Căn lề dựa trên vị trí (trái/phải)
                    const textAnchor = x > cx ? 'start' : 'end';

                    return (
                      <text
                        x={x}
                        y={y}
                        fill="#333"
                        textAnchor={textAnchor}
                        dominantBaseline="central"
                        style={{ fontSize: '12px', fontWeight: 500 }}
                      >
                        {/* Dòng 1: Hiển thị name */}
                        <tspan x={x} dy="0">
                          {name}
                        </tspan>
                        {/* Dòng 2: Hiển thị value (xuống dòng) */}
                        <tspan x={x} dy="1.2em"> {/* '1.2em' tạo một dòng mới */}
                          {`${value} trạm`}
                        </tspan>
                      </text>
                    );
                  }}
                  // --- KẾT THÚC THAY ĐỔI ---
                
                >
                  {utilizationDataForChart.map((entry, index) => {
                    let colorKey = 'low';
                    if (entry.name.startsWith('Cao')) colorKey = 'high';
                    if (entry.name.startsWith('TB')) colorKey = 'medium';
                    return <Cell key={`cell-${index}`} fill={COLORS[colorKey]} />;
                  })}
                </Pie>
                <Tooltip formatter={(value) => `${value} trạm`} />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          </div>

          {/* --- KHU VỰC BẢNG DỮ LIỆU --- */}
          <div className="table-container">
            <h2 className="chart-title">Chi Tiết Các Trạm Sạc</h2>
            
            <table className="custom-table">
              <thead>
                <tr>
                  <th>Tên Trạm</th>
                  <th>Trạng Thái</th>
                  <th>Hiệu Suất</th>
                  <th>Phiên Sạc (tháng)</th>
                  <th>Tăng Trưởng</th>
                  <th>Doanh Thu (tháng)</th>
                </tr>
              </thead>
              <tbody>
                {info.stationRows.map((station) => (
                  <tr key={station.stationId}>
                    <td>{station.stationName}</td>
                    <td>
                      <span className={`status-badge status-${station.status.toLowerCase()}`}>
                        {station.status}
                      </span>
                    </td>
                    <td>{(station.utilization * 100).toFixed(0)}%</td>
                    <td>{station.sessions.toLocaleString()}</td>
                    <td className={station.growthPercent >= 0 ? 'growth-positive' : 'growth-negative'}>
                      {station.growthPercent >= 0 ? '▲' : '▼'} {station.growthPercent}%
                    </td>
                    <td>{station.monthRevenue.amount.toLocaleString()} VND</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        
        </div> {/* Đóng .charts-grid */}
      </div> {/* Đóng .dashboard-container */}
    </>
  );
}