<div align="center">

# ⚡ EV Charging Station Management System

Quản lý toàn diện hệ thống trạm sạc xe điện: người dùng (tài xế, nhân viên, quản trị), trạm sạc, bộ sạc, điểm sạc (charging point), phiên sạc, đặt lịch, định giá, thống kê và xử lý sự cố.

</div>

## 📌 Giới thiệu
Hệ thống hỗ trợ doanh nghiệp/đơn vị vận hành mạng lưới trạm sạc EV theo mô hình nhiều vai trò. Người quản trị cấu hình trạm & giá; tài xế đặt lịch / sạc / quản lý phương tiện; nhân viên theo dõi phiên sạc, giao dịch và báo cáo sự cố. Cung cấp dashboard thống kê kinh doanh, an toàn và hiệu suất.

## 👤 Vai trò người dùng
- **Admin**: Quản lý người dùng, trạm, bộ sạc, model xe, cấu hình giá, thống kê, xử lý sự cố.
- **Driver (Tài xế)**: Quản lý hồ sơ, phương tiện, đặt lịch sạc, xem trạm & chi tiết, nhận thông báo.
- **Staff (Nhân viên)**: Theo dõi phiên sạc, giao dịch, báo cáo & xác nhận tai nạn/sự cố.

## 🔑 Chức năng chính
1. **Quản lý tài khoản & phân quyền** – tạo / cập nhật người dùng, xác thực bảo mật (JWT + Security), OAuth2 login Google.
2. **Quản lý trạm sạc** – thêm, cập nhật thông tin trạm, chi tiết điểm sạc (charging point) & bộ sạc (charger) khả dụng.
3. **Cấu hình giá (Pricing)** – thiết lập khung giá / chính sách theo thời gian hoặc loại bộ sạc.
4. **Đặt lịch & Phiên sạc** – tài xế đặt chỗ, hệ thống theo dõi trạng thái phiên (bắt đầu / kết thúc / báo cáo).
5. **Quản lý phương tiện & model** – CRUD model xe điện, phương tiện của tài xế.
6. **Xử lý tai nạn / sự cố** – ghi nhận, báo cáo, xác thực và thống kê.
7. **Thông báo & Email** – gửi email (SMTP), thông báo hệ thống (caffeine cache hỗ trợ hiệu năng).
8. **QR / Mã hoá** – ZXing hỗ trợ tạo/đọc mã (ví dụ QR cho phiên sạc / xác thực).
9. **Thanh toán / VNPAY (sandbox)** – thông số VNPAY tích hợp cổng thanh toán (trả về URL callback).
10. **Quản lý media** – Cloudinary lưu trữ ảnh (xe, trạm, avatar...).
11. **Thống kê & Dashboard** – biểu đồ doanh thu, tần suất phiên sạc, hiệu suất trạm (Recharts ở frontend + endpoints tổng hợp backend).

## 🗂️ Cấu trúc thư mục
```
EV_Charging_Station_Management_System_SWP391/
├── Backend/
│   ├── pom.xml                      # Cấu hình Maven, dependencies Spring Boot
│   ├── src/main/java/com/...        # Mã nguồn ứng dụng (entities, services, controllers, security)
│   ├── src/main/resources/          # application.properties, scripts
│   └── report/                      # SpotBugs HTML & JSON báo cáo
├── Frontend/
│   ├── package.json                 # Scripts Vite (dev/build/preview)
│   ├── src/
│   │   ├── api/                     # axios wrappers, auth, station, driver
│   │   ├── pages/admin/             # ManagementStation / Charger / ChargingPoint / User / Price...
│   │   ├── pages/inNavigateDriver/  # Booking, Stations, StationDetail, Profile...
│   │   ├── pages/profileDriver/     # Vehicles, Notification, AddVehicle...
│   │   ├── pages/staff/             # SessionCharging, ManagementTransaction, ReportAccident
│   │   ├── layouts/                 # AdminLayout, DriverLayout
│   │   ├── redux/                   # store, authSlice
│   │   └── components/              # Shared & domain components
│   └── vite.config.js
└── README.md
```

## 🧱 Kiến trúc tổng quan 
- **Frontend**: React SPA + Vite, phân vai trò qua Router, Redux Toolkit quản lý auth/notify.
- **Backend**: Spring Boot phân tầng rõ ràng (Controller → Service → Repository → Entity) + bảo mật Security/JWT/OAuth2.
- **Tích hợp**: Cloudinary (ảnh), VNPAY (sandbox), SMTP Gmail (mail), ZXing (QR), Caffeine (cache), Springdoc (OpenAPI).
- **Chất lượng**: SpotBugs/FindSecBugs trong pha `verify` để rà soát lỗi & bảo mật.

## 💻 Công nghệ sử dụng

### Frontend
- **React 19 + Vite**: SPA hiệu năng cao, HMR nhanh.
- **React Router v7**: Điều hướng nhiều vai trò (admin/driver/staff).
- **Redux Toolkit**: Quản lý state phiên đăng nhập & thông báo.
- **Axios**: Gọi API chuẩn hóa header/token.
- **Styled Components / Bootstrap / Recharts / ZXing**: UI linh hoạt, biểu đồ thống kê, quét mã QR.

### Backend
- **Spring Boot 3.5.x (Java 17)**: REST API, cấu hình đơn giản.
- **Spring Security + JWT + OAuth2 (Google)**: Xác thực & phân quyền theo vai trò (ADMIN / DRIVER / STAFF).
- **Spring Data JPA (Hibernate)**: Tầng truy cập dữ liệu SQL Server.
- **Spring Validation / Mail / Retry**: Kiểm tra dữ liệu, gửi email, retry ổn định.
- **Springdoc OpenAPI**: Tài liệu & thử nghiệm endpoint.
- **Cloudinary / ZXing / Caffeine**: Media, mã QR, cache nhẹ.
- **jjwt**: Tạo & xác thực token JWT.

### Database
- **SQL Server**: Lưu trữ giao dịch sạc, phiên đặt lịch, cấu hình giá.

### DevOps & Testing
- **Git**: Quản lý phiên bản mã nguồn.
- **Maven Wrapper**: Build nhất quán.
- **Vercel**: Nền tảng triển khai và hosting cho frontend.
- **SpotBugs + FindSecBugs**: Phân tích chất lượng & bảo mật.
- **Swagger UI**: Test và kiểm thử API trực tiếp trên trình duyệt

### 🔐 Bảo mật & Quyền riêng tư
- **JWT / OAuth2**: Bảo vệ API, hạn chế truy cập trái phép.
- **Phân quyền vai trò**: Chỉ ADMIN quản lý giá & trạm; STAFF xử lý phiên; DRIVER thao tác đặt lịch.
- **Validation**: Ngăn dữ liệu xấu (injection, format sai).
- **Báo cáo phân tích**: SpotBugs hỗ trợ phát hiện lỗi tiềm ẩn.
- **Chính sách bảo mật**: Cam kết tuân thủ các quy định về bảo mật và quyền riêng tư dữ liệu theo pháp luật hiện hành.
- **⚠️ Cảnh báo bảo mật**: Các secrets (mail password, Cloudinary API key/secret, VNPAY secretKey, JWT secret) hiện đang xuất hiện trong `application.properties`. Khuyến nghị thay thế bằng biến môi trường / vault trước khi triển khai production hoặc public.

### 🛡️ Yêu cầu phi chức năng
- **Hiệu năng**: Cache nhẹ (Caffeine) giảm truy vấn lặp.
- **Mở rộng**: Kiến trúc phân lớp rõ ràng dễ tách service sau này.
- **Bảo trì**: Tên package & phân tầng chuẩn (controller/service/repository).
- **Kiểm thử**: Có dependencies test (`spring-boot-starter-test`, `spring-security-test`).
- **Di động**: Chạy được trên Windows / Linux / Docker.
- **Trải nghiệm người dùng**: Giao diện phân vai trò rõ ràng.

---

## 👥 Author & Contributors
| Vai trò | Mô tả ngắn |
|---------|-----------|
| Team Lead | Điều phối phát triển, kiến trúc, review mã |
| Backend Dev | Xây dựng API, bảo mật, tích hợp thanh toán & Cloudinary |
| Frontend Dev | UI/UX, Redux, tối ưu hiệu năng & biểu đồ |

---



