package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.dto.request.StartCharSessionRequest;
import com.swp391.gr3.ev_management.dto.request.StopCharSessionRequest;
import com.swp391.gr3.ev_management.dto.response.*;
import com.swp391.gr3.ev_management.entity.ChargingSession;
import com.swp391.gr3.ev_management.enums.ChargingSessionStatus;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.service.ChargingSessionService;
import com.swp391.gr3.ev_management.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController // ✅ Đánh dấu đây là REST Controller (trả về JSON thay vì view)
@RequestMapping("/api/charging-sessions") // ✅ Prefix chung cho tất cả endpoint
@RequiredArgsConstructor // ✅ Lombok: tạo constructor cho các field final (DI)
@Tag(name = "Staff Charging Session", description = "APIs for staff to manage charging sessions") // ✅ Nhóm API cho Swagger
public class ChargingSessionController {

    private final ChargingSessionService chargingSessionService; // ✅ Service xử lý nghiệp vụ phiên sạc
    private final TokenService tokenService; // ✅ Service xử lý token để trích xuất userId

    // =========================================================================
    // 1) STAFF/ADMIN: BẮT ĐẦU PHIÊN SẠC (START)
    // =========================================================================
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')") // 🔒 Chỉ STAFF hoặc ADMIN
    @PostMapping("/start") // 🔗 POST /api/charging-sessions/start
    @Operation(summary = "Start charging session", description = "Staff starts a new charging session for a confirmed booking")
    public ResponseEntity<StartCharSessionResponse> startChargingSession(
            @Valid @RequestBody StartCharSessionRequest request // ✅ Dữ liệu đầu vào để bắt đầu phiên sạc (đã validate)
    ) {
        // 🟢 Gọi service để tạo/bắt đầu phiên sạc mới (liên kết booking/point/vehicle...)
        StartCharSessionResponse response = chargingSessionService.startChargingSession(request);
        // 🟢 Trả về 201 CREATED + thông tin phiên sạc vừa tạo
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =========================================================================
    // 2) STAFF/ADMIN: LẤY TẤT CẢ PHIÊN SẠC
    // =========================================================================
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')") // 🔒 STAFF hoặc ADMIN
    @GetMapping("all-session") // 🔗 GET /api/charging-sessions/all-session
    @Operation(summary = "Get all sessions", description = "Get list of all charging sessions")
    public List<ChargingSessionResponse> getAllSession() {
        // 🟢 Lấy tất cả entity ChargingSession rồi map sang DTO ChargingSessionResponse
        return chargingSessionService.getAll()
                .stream()
                .map(session -> new ChargingSessionResponse(
                        session.getStartTime(),        // thời điểm bắt đầu
                        session.getEndTime(),          // thời điểm kết thúc (nếu có)
                        session.getEnergyKWh(),        // điện năng tiêu thụ (kWh)
                        session.getDurationMinutes(),  // thời lượng (phút)
                        session.getCost(),             // chi phí
                        session.getStatus(),           // trạng thái phiên sạc
                        session.getInvoice()           // hoá đơn (nếu có)
                ))
                .collect(Collectors.toList());
    }

    // =========================================================================
    // 3) STAFF/ADMIN: XEM CHI TIẾT 1 PHIÊN SẠC THEO ID
    // =========================================================================
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')") // 🔒 STAFF hoặc ADMIN
    @GetMapping("/{sessionId}") // 🔗 GET /api/charging-sessions/{sessionId}
    @Operation(summary = "Get session detail", description = "Get detailed information of a charging session")
    public ResponseEntity<ViewCharSessionResponse> getSessionById(
            @Parameter(description = "Session ID") @PathVariable Long sessionId // ✅ ID phiên sạc
    ) {
        // 🟢 Gọi service để lấy DTO chi tiết phiên sạc
        ViewCharSessionResponse response = chargingSessionService.getCharSessionById(sessionId);
        // 🟢 Trả về 200 OK + dữ liệu
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // 4) STAFF/ADMIN: LẤY DANH SÁCH PHIÊN SẠC ĐANG HOẠT ĐỘNG TẠI 1 TRẠM
    // =========================================================================
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')") // 🔒 STAFF hoặc ADMIN
    @GetMapping("/active") // 🔗 GET /api/charging-sessions/active?stationId=...
    @Operation(summary = "Get active sessions", description = "Get list of currently active charging sessions at a station")
    public ResponseEntity<List<ViewCharSessionResponse>> getActiveSessionsByStation(
            @Parameter(description = "Station ID") @RequestParam Long stationId // ✅ ID trạm sạc
    ) {
        // 🟢 Lấy danh sách phiên sạc đang hoạt động (IN_PROGRESS) theo station
        List<ViewCharSessionResponse> sessions = chargingSessionService.getActiveCharSessionsByStation(stationId);
        return ResponseEntity.ok(sessions);
    }

    // =========================================================================
    // 5) STAFF/ADMIN: LẤY TÌNH TRẠNG SẠC THEO THỜI GIAN THỰC (MÔ PHỎNG)
    // =========================================================================
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')") // 🔒 STAFF hoặc ADMIN
    @GetMapping("/session/{sessionId}/status") // 🔗 GET /api/charging-sessions/session/{sessionId}/status?initialSoc=..&connectorType=AC|DC
    @Operation(summary = "Get charging status", description = "Get current charging status (SoC, energy delivered) of an ongoing session")
    public ChargingStatusResponse getChargingStatus(
            @PathVariable Long sessionId, // ✅ ID phiên sạc
            @RequestParam int initialSoc, // ✅ SoC ban đầu (%), đầu vào để mô phỏng
            @RequestParam String connectorType // ✅ Loại đầu nối: "DC" hoặc "AC" (ảnh hưởng tốc độ sạc)
    ) {
        // 🟢 Tìm entity phiên sạc; nếu không có thì ném lỗi
        ChargingSession session = chargingSessionService.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        // 🟢 Tính số phút đã trôi qua từ lúc bắt đầu đến hiện tại
        LocalDateTime start = session.getStartTime();
        long minutes = Duration.between(start, LocalDateTime.now()).toMinutes();

        // 🟢 Tốc độ sạc mô phỏng theo loại connector (đơn vị: % SoC / giờ)
        double ratePerHour = connectorType.equalsIgnoreCase("DC") ? 25.0 : 10.0;
        double ratePerMinute = ratePerHour / 60.0;

        // 🟢 Tính SoC hiện tại (không vượt quá 100%)
        double currentSoc = Math.min(100, initialSoc + minutes * ratePerMinute);

        // 🟢 Mô phỏng năng lượng nạp được (kWh) từ chênh lệch SoC * hệ số (0.5 là giả định)
        double energyKWh = (currentSoc - initialSoc) * 0.5;

        // 🟢 Trả về DTO tình trạng sạc hiện tại
        return ChargingStatusResponse.builder()
                .sessionId(sessionId)
                .currentSoc(currentSoc)
                .energyKWh(energyKWh)
                .minutesElapsed(minutes)
                .build();
    }

    // =========================================================================
    // 6) STAFF/ADMIN: LẤY TẤT CẢ PHIÊN SẠC THEO STATION ID
    // =========================================================================
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')") // 🔒 STAFF hoặc ADMIN
    @GetMapping("/stations/{stationId}/charging-sessions") // 🔗 GET /api/charging-sessions/stations/{stationId}/charging-sessions
    @Operation(summary = "Get all sessions by station", description = "Get all charging sessions for a specific station")
    public ResponseEntity<List<ViewCharSessionResponse>> getAllByStation(@PathVariable Long stationId) {
        // 🟢 Gọi service để lấy tất cả phiên sạc thuộc trạm
        List<ViewCharSessionResponse> res = chargingSessionService.getAllSessionsByStation(stationId);
        return ResponseEntity.ok(res);
    }

    // =========================================================================
    // 7) STAFF/ADMIN: LẤY TẤT CẢ PHIÊN SẠC THEO POIN ID
    // =========================================================================
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    @GetMapping("/by-point/{pointId}")
    @Operation(summary = "Get sessions by charging point",
            description = "Retrieve all charging sessions associated with a specific charging point (via Booking -> Slot -> ChargingPoint)")
    public ResponseEntity<List<ViewCharSessionResponse>> getSessionsByPoint(
            @PathVariable Long pointId
    ) {
        // ✅ Gọi service để lấy danh sách session theo pointId
        return ResponseEntity.ok(chargingSessionService.getSessionsByPoint(pointId));
    }

    // =========================================================================
    // 8) DRIVER: TỰ DỪNG PHIÊN SẠC CỦA CHÍNH MÌNH
    // =========================================================================
    @PreAuthorize("hasRole('DRIVER')") // 🔒 Chỉ tài xế (DRIVER)
    @PostMapping("/driver-stop") // 🔗 POST /api/charging-sessions/driver-stop
    @Operation(summary = "Driver stops their own charging session", description = "Driver stops their own charging session using session ID")
    public ResponseEntity<StopCharSessionResponse> driverStopSession(
            @RequestBody StopCharSessionRequest body, // ✅ Chứa sessionId cần dừng
            HttpServletRequest httpReq // ✅ Dùng để rút userId từ token
    ) {
        // 🟢 Lấy userId từ token trong request (Authorization header/cookie)
        Long userId = tokenService.extractUserIdFromRequest(httpReq);

        // 🟢 Gọi service để dừng phiên sạc thuộc về chính userId này (đảm bảo quyền sở hữu)
        StopCharSessionResponse res =
                chargingSessionService.driverStopSession(body.getSessionId(), userId);

        // 🟢 Trả về 200 OK + thông tin sau khi dừng
        return ResponseEntity.ok(res);
    }

    // =========================================================================
    // 9) DRIVER: LẤY PHIÊN SẠC ĐANG HOẠT ĐỘNG HIỆN TẠI CỦA CHÍNH MÌNH
    // =========================================================================
    @GetMapping("/charging-sessions/current") // 🔗 GET /api/charging-sessions/charging-sessions/current
    @Operation(summary = "Get current active session for driver",
            description = "Driver retrieves their currently active charging session")
    public ResponseEntity<ViewCharSessionResponse> getCurrentSession(HttpServletRequest httpReq) {
        // 🟢 Lấy userId từ token
        Long userId = tokenService.extractUserIdFromRequest(httpReq);

        // 🔎 Lấy tất cả phiên sạc (gợi ý tối ưu: tạo repo method chỉ lấy phiên đang IN_PROGRESS theo user)
        List<ChargingSession> all = chargingSessionService.getAll();

        // 🔎 Tìm phiên sạc đang hoạt động (IN_PROGRESS) thuộc về driver có userId tương ứng
        ChargingSession current = all.stream()
                .filter(s -> s.getStatus() == ChargingSessionStatus.IN_PROGRESS)
                .filter(s -> s.getBooking() != null
                        && s.getBooking().getVehicle() != null
                        && s.getBooking().getVehicle().getDriver() != null
                        && s.getBooking().getVehicle().getDriver().getUser().getUserId().equals(userId))
                .findFirst()
                // ❌ Không có phiên sạc đang hoạt động -> ném ErrorException với thông điệp tiếng Việt
                .orElseThrow(() -> new ErrorException("Bạn không có phiên sạc nào đang hoạt động."));

        // 🟢 Dùng service để lấy DTO chi tiết theo sessionId tìm được
        ViewCharSessionResponse res = chargingSessionService.getCharSessionById(current.getSessionId());
        return ResponseEntity.ok(res);
    }

    // =========================================================================
    // 10) STAFF: XEM DANH SÁCH PHIÊN SẠC Ở CHẾ ĐỘ COMPACT
    // =========================================================================

    @GetMapping("/active/compact")
    @Operation(summary = "Get active sessions in compact view",
            description = "Only sessions at stations where the current staff is actively assigned")
    public ResponseEntity<List<ActiveSessionView>> getActiveCompact(HttpServletRequest request) {
        // 🟢 Lấy userId từ token đăng nhập
        Long userId = tokenService.extractUserIdFromRequest(request);
        return ResponseEntity.ok(chargingSessionService.getActiveSessionsCompact(userId));
    }

    // =========================================================================
    // 11) STAFF: XEM DANH SÁCH PHIÊN SẠC HOÀN THÀNH Ở CHẾ ĐỘ COMPACT
    // =========================================================================

    @GetMapping("/completed/compact")
    @Operation(summary = "Get completed sessions in compact view",
            description = "Only sessions at stations where the current staff is actively assigned")
    public ResponseEntity<List<CompletedSessionView>> getCompletedCompact(HttpServletRequest request) {
        // 🟢 Lấy userId từ token đăng nhập
        Long userId = tokenService.extractUserIdFromRequest(request);
        return ResponseEntity.ok(chargingSessionService.getCompletedSessionsCompactByStaff(userId));
    }
}
