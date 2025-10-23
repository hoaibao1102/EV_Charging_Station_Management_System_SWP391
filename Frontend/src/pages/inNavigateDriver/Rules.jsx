import React from "react";

export default function Rules() {
  const styles = {
    container: { 
      padding: 32,
      paddingBottom: 120, 
      maxWidth: 1000,
      margin: "0 auto",
      backgroundColor: "#ffffff",
      minHeight: "100vh"
    },
    header: {
      borderBottom: "3px solid #20b2aa",
      paddingBottom: 16,
      marginBottom: 24
    },
    heading: { 
      fontSize: "2rem",
      fontWeight: 700,
      marginBottom: 8,
      color: "#1a1a1a"
    },
    tableWrap: { 
      overflowX: "auto",
      overflowY: "auto",
      maxHeight: "58vh",
      backgroundColor: "#ffffff",
      borderRadius: 8,
      boxShadow: "0 1px 3px rgba(0,0,0,0.1)",
      // Custom scrollbar
      scrollbarWidth: "thin",
      scrollbarColor: "#20b2aa #f0f0f0"
    },
    table: { 
      width: "100%",
      borderCollapse: "collapse"
    },
    cell: { 
      border: "1px solid #20b2aa",
      padding: 16,
      verticalAlign: "top",
      backgroundColor: "#ffffff"
    },
    th: { 
      background: "#20b2aa",
      fontWeight: 600,
      color: "#ffffff",
      textAlign: "left"
    },
    codeCell: {
      fontWeight: 700,
      color: "#20b2aa",
      fontSize: "0.95rem"
    },
    note: { 
      color: "#666666",
      marginTop: 24,
      fontSize: 14,
      padding: 16,
      backgroundColor: "#f0fffe",
      borderLeft: "4px solid #20b2aa",
      borderRadius: 4
    },
  };

  const rules = [
    {
      id: "BR-01",
      text:
        "Nếu người dùng nhập sai mật khẩu 3 lần liên tiếp, tài khoản sẽ bị khóa đăng nhập trong 3 phút tiếp theo.",
    },
    {
      id: "BR-02",
      text:
        "Khi đặt lịch, tài xế phải chọn chính xác 1 phương tiện từ danh sách phương tiện của mình.",
    },
    {
      id: "BR-03",
      text:
        "Khi thêm phương tiện, tài xế chỉ có thể chọn mẫu xe được hệ thống hỗ trợ.",
    },
    {
      id: "BR-04",
      text:
        "Đặt chỗ chỉ được hủy nếu yêu cầu diễn ra trước ít nhất 30 phút so với giờ bắt đầu sạc đã đặt.",
    },
    {
      id: "BR-05",
      text:
        "Khóa tài khoản nếu vắng mặt nhiều lần: Vắng mặt = không bắt đầu phiên sạc trong vòng 30 phút sau giờ bắt đầu trong khi vẫn giữ slot. Sau 3 lần vắng mặt (tính cuốn chiếu), trạng thái tài khoản sẽ chuyển sang NGƯNG HOẠT ĐỘNG.",
    },
    {
      id: "BR-06",
      text:
        "Để gỡ KHÓA TÀI KHOẢN, người dùng phải nộp phí phạt bằng thời lượng slot bị lãng phí (tính theo phút) nhân với mức phạt theo phút của đầu nối tương ứng.",
    },
    {
      id: "BR-07",
      text:
        "Sau khi phiên sạc kết thúc, phí đỗ xe chiếm chỗ bắt đầu tính từ phút thứ 31 và được tính theo phút dựa trên mức phí của từng cổng sạc.",
    },
    {
      id: "BR-08",
      text:
        "Mỗi nhân viên chỉ được phân công làm việc tại một trạm tại cùng một thời điểm.",
    },
    {
      id: "BR-09",
      text: "Trong thời gian bị cấm, người dùng không thể đặt chỗ nào.",
    },
    {
      id: "BR-10",
      text: "Người dùng phải đăng nhập mỗi khi thực hiện thao tác đặt chỗ và xem thông tin người dùng.",
    },
    {
      id: "BR-12",
      text:
        "Thanh toán bắt buộc ngay sau khi phiên sạc kết thúc hoặc khi người dùng yêu cầu gỡ lệnh cấm; không hỗ trợ thanh toán sau trong các trường hợp này.",
    },
    {
      id: "BR-13",
      text:
        "Một slot đặt chỗ có thể gồm nhiều khung giờ liên tiếp liền kề trên cùng một đầu nối, tối đa 3 khung.",
    },
    {
      id: "BR-14",
      text:
        "Số điện thoại phải là duy nhất và không được sử dụng cho nhiều tài khoản.",
    },
  ];

  return (
    <div style={styles.container}>
      <style>{`
        /* Custom scrollbar cho bảng quy định */
        .rules-table-wrap::-webkit-scrollbar {
          width: 10px;
          height: 10px;
        }
        
        .rules-table-wrap::-webkit-scrollbar-track {
          background: #f0f0f0;
          border-radius: 5px;
        }
        
        .rules-table-wrap::-webkit-scrollbar-thumb {
          background: #20b2aa;
          border-radius: 5px;
        }
        
        .rules-table-wrap::-webkit-scrollbar-thumb:hover {
          background: #1a9e98;
        }
      `}</style>

      <div style={styles.header}>
        <h1 style={styles.heading}>Quy định sử dụng hệ thống sạc EV</h1>
      </div>

      <div style={styles.tableWrap} className="rules-table-wrap">
        <table style={styles.table}>
          <thead>
            <tr>
              <th style={{ ...styles.cell, ...styles.th, width: 120 }}>Mã quy định</th>
              <th style={{ ...styles.cell, ...styles.th }}>Quy định</th>
            </tr>
          </thead>
          <tbody>
            {rules.map((r) => (
              <tr key={r.id}>
                <td style={{ ...styles.cell, ...styles.codeCell }}>
                  {r.id}
                </td>
                <td style={styles.cell}>{r.text}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div style={styles.note}>
        <strong>Lưu ý:</strong> Mức phí phạt và thời lượng mỗi slot có thể khác nhau theo loại cổng sạc và cấu hình của từng trạm.
      </div>
    </div>
  );
}