package com.swp391.gr3.ev_management.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.swp391.gr3.ev_management.dto.request.AddVehicleRequest;
import com.swp391.gr3.ev_management.dto.request.DriverUpdateRequest;
import com.swp391.gr3.ev_management.dto.request.UpdatePasswordRequest;
import com.swp391.gr3.ev_management.dto.request.UpdateVehicleRequest;
import com.swp391.gr3.ev_management.dto.response.ChargingSessionBriefResponse;
import com.swp391.gr3.ev_management.dto.response.DriverInvoiceDetail;
import com.swp391.gr3.ev_management.dto.response.DriverResponse;
import com.swp391.gr3.ev_management.dto.response.TransactionBriefResponse;
import com.swp391.gr3.ev_management.dto.response.UnpaidInvoiceResponse;
import com.swp391.gr3.ev_management.dto.response.VehicleResponse;
import com.swp391.gr3.ev_management.service.DriverService;
import com.swp391.gr3.ev_management.service.InvoiceService;
import com.swp391.gr3.ev_management.service.TokenService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController // ✅ Đánh dấu class là REST Controller (tự động trả JSON thay vì view)
@RequestMapping("/api/driver") // ✅ Đặt prefix cho toàn bộ endpoint: /api/driver/...
@Tag(name = "Drivers", description = "APIs for driver management") // ✅ Dùng cho Swagger (hiển thị mô tả nhóm API)
@RequiredArgsConstructor // ✅ Lombok: tự động tạo constructor cho các field final (Dependency Injection)
public class DriverController {

    private final DriverService driverService;      // ✅ Service xử lý nghiệp vụ liên quan đến tài xế
    private final TokenService tokenService;        // ✅ Service dùng để lấy userId từ token đăng nhập
    private final InvoiceService invoiceService;    // ✅ Service xử lý nghiệp vụ liên quan đến hóa đơn

    // =========================================================================
    // ✅ 1. DRIVER CẬP NHẬT HỒ SƠ CỦA CHÍNH MÌNH
    // =========================================================================
    @PreAuthorize("hasRole('DRIVER')") // 🔒 Chỉ người có quyền DRIVER mới được thực hiện
    @PutMapping("/profile") // 🔗 PUT /api/driver/profile
    @Operation(summary = "Update own driver profile", description = "Driver updates their own profile information")
    public ResponseEntity<DriverResponse> updateOwnProfile(
            HttpServletRequest request, // ✅ Dùng để lấy token từ header
            @Valid @RequestBody DriverUpdateRequest updateRequest // ✅ Dữ liệu cập nhật hồ sơ (được validate)
    ) {
        Long userId = tokenService.extractUserIdFromRequest(request); // 🟢 Lấy userId từ token đăng nhập
        DriverResponse updated = driverService.updateDriverProfile(userId, updateRequest); // 🟢 Gọi service cập nhật
        return ResponseEntity.ok(updated); // 🟢 Trả về hồ sơ sau khi cập nhật thành công
    }

    // =========================================================================
    // ✅ 2. DRIVER CẬP NHẬT MẬT KHẨU
    // =========================================================================
    @PreAuthorize("hasRole('DRIVER')") // 🔒 Chỉ DRIVER được phép đổi mật khẩu của chính mình
    @PutMapping("/password") // 🔗 PUT /api/driver/password
    @Operation(summary = "Update own driver Password", description = "Driver updates their own Password")
    public ResponseEntity<DriverResponse> updateOwnPassword(
            HttpServletRequest request, // ✅ Dùng để xác định tài xế từ token
            @Valid @RequestBody UpdatePasswordRequest updateRequest // ✅ Gồm mật khẩu cũ và mới
    ) {
        Long userId = tokenService.extractUserIdFromRequest(request); // 🟢 Lấy userId của driver từ token
        // 🟢 Gọi service để đổi mật khẩu (có kiểm tra mật khẩu cũ và xác nhận mật khẩu mới)
        DriverResponse updated = driverService.updateDriverPassword(
                userId,
                updateRequest
        );
        return ResponseEntity.ok(updated); // 🟢 Trả về phản hồi thành công
    }

    // =========================================================================
    // ✅ 3. DRIVER THÊM XE MỚI VÀO HỒ SƠ
    // =========================================================================
    /**
     * UC-04: Driver thêm xe vào hồ sơ
     * BR-02: Xe phải thuộc về driver đang đăng nhập
     * BR-03: Kiểm tra model tồn tại và license plate chưa được đăng ký
     */
    @PreAuthorize("hasRole('DRIVER')") // 🔒 Chỉ DRIVER mới được thêm xe
    @PostMapping("/vehicles") // 🔗 POST /api/driver/vehicles
    @Operation(summary = "Add vehicle", description = "Driver adds a new vehicle to their profile")
    public ResponseEntity<VehicleResponse> addVehicle(
            HttpServletRequest request, // ✅ Lấy token để xác định driver
            @Valid @RequestBody AddVehicleRequest addRequest // ✅ Thông tin xe mới (modelId, biển số,...)
    ) {
        Long userId = tokenService.extractUserIdFromRequest(request); // 🟢 Lấy userId driver
        VehicleResponse vehicle = driverService.addVehicle(userId, addRequest); // 🟢 Gọi service để thêm xe
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicle); // 🟢 Trả về HTTP 201 CREATED
    }

    // =========================================================================
    // ✅ 4. DRIVER CẬP NHẬT THÔNG TIN MỘT XE
    // =========================================================================
    @PreAuthorize("hasRole('DRIVER')") // 🔒 Chỉ DRIVER có quyền
    @PatchMapping("/vehicles/{vehicleId}") // 🔗 PATCH /api/driver/vehicles/{vehicleId}
    @Operation(summary = "Update my vehicle", description = "Driver updates their own vehicle (model, license plate)")
    public ResponseEntity<VehicleResponse> updateMyVehicle(
            HttpServletRequest request, // ✅ Lấy userId từ token
            @PathVariable Long vehicleId, // ✅ ID xe cần cập nhật
            @Valid @RequestBody UpdateVehicleRequest updateRequest // ✅ Dữ liệu cập nhật (model, license plate,...)
    ) {
        Long userId = tokenService.extractUserIdFromRequest(request); // 🟢 Lấy userId driver
        VehicleResponse updated = driverService.updateVehicle(userId, vehicleId, updateRequest); // 🟢 Gọi service cập nhật
        return ResponseEntity.ok(updated); // 🟢 Trả về thông tin xe sau khi cập nhật
    }

    // =========================================================================
    // ✅ 5. DRIVER THAY ĐỔI TRẠNG THÁI XE (ACTIVE/INACTIVE)
    // =========================================================================
    @PreAuthorize("hasRole('DRIVER')") // 🔒 Chỉ DRIVER có quyền
    @PatchMapping("/vehicles/{vehicleId}/status") // 🔗 PATCH /api/driver/vehicles/{vehicleId}/status?status=ACTIVE
    @Operation(summary = "Update my vehicle status", description = "Driver updates status of their own vehicle")
    public ResponseEntity<VehicleResponse> updateMyVehicleStatus(
            HttpServletRequest request, // ✅ Lấy userId driver
            @PathVariable Long vehicleId, // ✅ ID xe
            @RequestParam com.swp391.gr3.ev_management.enums.UserVehicleStatus status // ✅ Trạng thái mới (ACTIVE/INACTIVE)
    ) {
        Long userId = tokenService.extractUserIdFromRequest(request); // 🟢 Lấy userId driver
        VehicleResponse updated = driverService.updateVehicleStatus(userId, vehicleId, status); // 🟢 Gọi service để đổi trạng thái
        return ResponseEntity.ok(updated); // 🟢 Trả về thông tin xe đã cập nhật
    }

    // =========================================================================
    // ✅ 6. DRIVER XEM HỒ SƠ CỦA CHÍNH MÌNH
    // =========================================================================
    @PreAuthorize("hasRole('DRIVER')") // 🔒 Chỉ DRIVER
    @GetMapping("/profile") // 🔗 GET /api/driver/profile
    @Operation(summary = "Get own driver profile", description = "Driver retrieves their own profile information")
    public ResponseEntity<DriverResponse> getOwnProfile(HttpServletRequest request) {
        Long userId = tokenService.extractUserIdFromRequest(request); // 🟢 Lấy userId driver từ token
        DriverResponse driver = driverService.getByUserId(userId); // 🟢 Lấy thông tin chi tiết hồ sơ
        return ResponseEntity.ok(driver); // 🟢 Trả về dữ liệu hồ sơ driver
    }

    // =========================================================================
    // ✅ 7. DRIVER XEM DANH SÁCH XE CỦA CHÍNH MÌNH
    // =========================================================================
    /**
     * UC-04: Xem danh sách xe của driver
     */
    @PreAuthorize("hasRole('DRIVER')") // 🔒 Chỉ DRIVER
    @GetMapping("/vehicles") // 🔗 GET /api/driver/vehicles
    @Operation(summary = "Get my vehicles", description = "Driver retrieves list of their vehicles")
    public ResponseEntity<List<VehicleResponse>> getMyVehicles(HttpServletRequest request) {
        Long userId = tokenService.extractUserIdFromRequest(request); // 🟢 Lấy userId driver
        List<VehicleResponse> vehicles = driverService.getMyVehicles(userId); // 🟢 Lấy danh sách xe
        return ResponseEntity.ok(vehicles); // 🟢 Trả về danh sách xe của tài xế
    }

    // =========================================================================
    // ✅ 8. DRIVER XEM LỊCH SỬ GIAO DỊCH
    // =========================================================================
    @PreAuthorize("hasRole('DRIVER')") // 🔒 Chỉ DRIVER
    @GetMapping("/transactions") // 🔗 GET /api/driver/transactions
    @Operation(summary = "Get my transactions", description = "Driver retrieves all their transactions")
    public ResponseEntity<List<TransactionBriefResponse>> myTransactions(HttpServletRequest request) {
        Long userId = tokenService.extractUserIdFromRequest(request); // 🟢 Lấy userId driver
        List<TransactionBriefResponse> result = driverService.getMyTransactions(userId); // 🟢 Lấy danh sách giao dịch
        return ResponseEntity.ok(result); // 🟢 Trả về danh sách
    }

    // =========================================================================
    // ✅ 9. DRIVER XEM DANH SÁCH CÁC PHIÊN SẠC (CHARGING SESSIONS)
    // =========================================================================
    @PreAuthorize("hasRole('DRIVER')") // 🔒 Chỉ DRIVER
    @GetMapping("/sessions") // 🔗 GET /api/driver/sessions
    @Operation(summary = "Get my charging sessions", description = "Driver retrieves all their charging sessions")
    public ResponseEntity<List<ChargingSessionBriefResponse>> mySessions(HttpServletRequest request) {
        Long userId = tokenService.extractUserIdFromRequest(request); // 🟢 Lấy userId driver
        List<ChargingSessionBriefResponse> result = driverService.getMyChargingSessions(userId); // 🟢 Lấy danh sách phiên sạc của driver
        return ResponseEntity.ok(result); // 🟢 Trả về danh sách các phiên sạc
    }


    // =========================================================================
    // ✅ 10. DRIVER XEM CHI TIẾT HÓA ĐƠN
    // =========================================================================
    @GetMapping("/invoices/{invoiceId}") // 🔗 GET /api/driver/invoices/{invoiceId}
    @PreAuthorize("hasRole('DRIVER')") // 🔒 Chỉ DRIVER mới được xem chi tiết hóa đơn của chính mình
    @Operation(
            summary = "Get invoice detail",
            description = "Driver retrieves detailed information of a specific invoice by its ID"
    )
    public DriverInvoiceDetail getInvoiceDetail(
            @PathVariable Long invoiceId,
            HttpServletRequest request) {

        // Lấy userId từ Access Token
        Long userId = tokenService.extractUserIdFromRequest(request);

        return invoiceService.getDetail(invoiceId, userId); // Trả về chi tiết hóa đơn
    }

    // =========================================================================
    // ✅ 11. DRIVER XEM DANH SÁCH HÓA ĐƠN CHƯA THANH TOÁN
    // =========================================================================
    @GetMapping("/invoices/unpaid")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Get unpaid invoices", description = "Driver retrieves all unpaid invoices")
    public ResponseEntity<List<UnpaidInvoiceResponse>> getUnpaidInvoices(
            HttpServletRequest request) {
        Long userId = tokenService.extractUserIdFromRequest(request);
        List<UnpaidInvoiceResponse> invoices = invoiceService.getUnpaidInvoices(userId);
        return ResponseEntity.ok(invoices);
    }

}
