import React from "react";

export default function Rules() {
  const styles = {
    container: { padding: 16, maxWidth: 1000, margin: "0 auto" },
    heading: { fontSize: "1.5rem", fontWeight: 700, marginBottom: 8 },
    updated: { color: "#6b7280", marginBottom: 16 },
    tableWrap: { overflowX: "auto" },
    table: { width: "100%", borderCollapse: "collapse" },
    cell: { border: "1px solid #e5e7eb", padding: 8, verticalAlign: "top" },
    th: { background: "#f9fafb", fontWeight: 600 },
    note: { color: "#6b7280", marginTop: 12, fontSize: 14 },
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
        "Cấm vì vắng mặt nhiều lần: Vắng mặt = không bắt đầu phiên sạc trong vòng 30 phút sau giờ bắt đầu trong khi vẫn giữ slot. Sau 3 lần vắng mặt (tính cuốn chiếu), trạng thái sẽ chuyển sang BỊ CẤM.",
    },
    {
      id: "BR-06",
      text:
        "Để gỡ lệnh cấm, người dùng phải nộp phí phạt bằng thời lượng slot bị lãng phí (tính theo phút) nhân với mức phạt theo phút của đầu nối tương ứng.",
    },
    {
      id: "BR-07",
      text:
        "Sau khi phiên sạc kết thúc, phí đỗ xe chiếm chỗ bắt đầu tính từ phút thứ 31 và được tính theo phút dựa trên mức phí của từng đầu nối.",
    },
    {
      id: "BR-08",
      text:
        "Mỗi nhân viên chỉ được phân công làm việc tại một trạm tại cùng một thời điểm.",
    },
    {
      id: "BR-09",
      text: "Trong thời gian bị cấm, người dùng không thể tạo bất kỳ đặt chỗ nào.",
    },
    {
      id: "BR-10",
      text: "Người dùng phải đăng nhập mỗi khi thực hiện thao tác đặt chỗ.",
    },
    {
      id: "BR-11",
      text:
        "Khi quản trị viên thêm nhân viên, hệ thống tạo tài khoản nhân viên và cấp thông tin đăng nhập để sử dụng.",
    },
    {
      id: "BR-12",
      text:
        "Thanh toán bắt buộc ngay sau khi phiên sạc kết thúc hoặc khi người dùng yêu cầu gỡ lệnh cấm; không hỗ trợ thanh toán sau trong các trường hợp này.",
    },
    {
      id: "BR-13",
      text:
        "Một đặt chỗ có thể gồm nhiều khung giờ liên tiếp liền kề trên cùng một đầu nối, tối đa 3 khung; độ dài mặc định của mỗi khung là 60 phút và có thể cấu hình.",
    },
    {
      id: "BR-14",
      text:
        "Số điện thoại phải là duy nhất và không được sử dụng cho nhiều tài khoản.",
    },
  ];

  return (
    <div style={styles.container}>
      <h1 style={styles.heading}>Quy định sử dụng hệ thống sạc EV</h1>
      <p style={styles.updated}>Cập nhật: 18/10/2025</p>

      <div style={styles.tableWrap}>
        <table style={styles.table}>
          <thead>
            <tr>
              <th style={{ ...styles.cell, ...styles.th, width: 96 }}>ID</th>
              <th style={{ ...styles.cell, ...styles.th }}>Quy định</th>
            </tr>
          </thead>
          <tbody>
            {rules.map((r) => (
              <tr key={r.id}>
                <td style={styles.cell}>
                  <strong>{r.id}</strong>
                </td>
                <td style={styles.cell}>{r.text}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <p style={styles.note}>
        Lưu ý: Mức phí phạt có thể khác nhau theo loại đầu nối và cấu hình của từng trạm.
      </p>
    </div>
  );
}