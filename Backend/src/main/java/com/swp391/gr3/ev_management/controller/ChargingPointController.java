package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.dto.request.*;
import com.swp391.gr3.ev_management.dto.response.*;
import com.swp391.gr3.ev_management.service.ChargingPointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // ✅ Đánh dấu đây là REST Controller (trả về JSON thay vì view)
@RequestMapping("/api/charging-points") // ✅ Định nghĩa prefix chung cho tất cả endpoint: /api/charging-points/...
@RequiredArgsConstructor // ✅ Lombok: tự sinh constructor với tất cả field final (D.I.)
@Tag(name = "Staff Charging Point", description = "APIs for staff to manage charging points") // ✅ Dùng để mô tả nhóm API trong Swagger
public class ChargingPointController {

    // ✅ Inject service để xử lý nghiệp vụ liên quan đến "charging points"
    private final ChargingPointService pointService;

    // =====================================================================
    // ✅ 1. ADMIN: TẠO ĐIỂM SẠC MỚI (CREATE CHARGING POINT)
    // =====================================================================
    @PreAuthorize("hasRole('ADMIN')") // 🔒 Chỉ người có role ADMIN được phép tạo
    @PostMapping(value = "/create") // 🔗 Endpoint: POST /api/charging-points/create
    @Operation(summary = "Create a new point", description = "Endpoint to create a new charging point")
    public ResponseEntity<ChargingPointResponse> createPoint(@RequestBody CreateChargingPointRequest request) {
        try {
            // 🟢 Gọi service để tạo điểm sạc mới dựa trên thông tin từ request
            ChargingPointResponse response = pointService.createChargingPoint(request);

            // 🟢 Trả về HTTP 201 CREATED cùng dữ liệu chi tiết điểm sạc vừa tạo
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            // ❌ Nếu có lỗi (VD: station không tồn tại, dữ liệu không hợp lệ...) -> trả 400
            return ResponseEntity.badRequest().build();
        }
    }

    // =====================================================================
    // ✅ 2. ADMIN hoặc STAFF: DỪNG MỘT ĐIỂM SẠC (STOP CHARGING POINT)
    // =====================================================================
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')") // 🔒 Cả ADMIN và STAFF đều có quyền
    @PostMapping("/stop") // 🔗 Endpoint: POST /api/charging-points/stop
    @Operation(summary = "Stop charging point", description = "Staff stops a charging point for maintenance or other reasons")
    public ResponseEntity<ChargingPointResponse> stopChargingPoint(
            @Valid @RequestBody StopChargingPointRequest request // ✅ Dữ liệu yêu cầu dừng (pointId, lý do, ...)
    ) {
        // 🟢 Gọi service để xử lý dừng hoạt động điểm sạc
        ChargingPointResponse response = pointService.stopChargingPoint(request);

        // 🟢 Trả về thông tin điểm sạc sau khi dừng (HTTP 200 OK)
        return ResponseEntity.ok(response);
    }

    // =====================================================================
    // ✅ 3. ADMIN hoặc STAFF: LẤY DANH SÁCH TẤT CẢ CÁC ĐIỂM SẠC
    // =====================================================================
    @GetMapping // 🔗 Endpoint: GET /api/charging-points
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')") // 🔒 Cả ADMIN và STAFF đều có quyền
    @Operation(summary = "Get all charging points", description = "Get list of all charging points at a station")
    public ResponseEntity<List<ChargingPointResponse>> getAllPoints(){
        // 🟢 Gọi service để lấy toàn bộ danh sách các điểm sạc trong hệ thống
        return ResponseEntity.ok(pointService.getAllPoints());
    }

    // =====================================================================
    // ✅ 4. LẤY CÁC ĐIỂM SẠC THEO MỘT TRẠM CỤ THỂ (STATION ID)
    // =====================================================================
    @GetMapping("station/{stationId}") // 🔗 Endpoint: GET /api/charging-points/station/{stationId}
    @Operation(summary = "Get charging points by station", description = "Get all charging points for a specific station")
    public ResponseEntity<List<ChargingPointResponse>> getPointsByStation(
            @Parameter(description = "Charging Station ID") @PathVariable Long stationId // ✅ Truyền ID trạm sạc qua URL
    ) {
        // 🟢 Gọi service để lấy danh sách tất cả điểm sạc thuộc về stationId được chỉ định
        List<ChargingPointResponse> responses = pointService.getPointsByStationId(stationId);

        // 🟢 Trả về danh sách các điểm sạc tương ứng
        return ResponseEntity.ok(responses);
    }

    // =====================================================================
    // ✅ 5. LẤY CHI TIẾT MỘT ĐIỂM SẠC THEO ID
    // =====================================================================
    @GetMapping("/{pointId}") // GET /api/charging-points/{pointId}
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get charging point detail", description = "Get detail information of a specific charging point")
    public ResponseEntity<ChargingPointResponse> getPointById(
            @Parameter(description = "Charging Point ID") @PathVariable Long pointId
    ) {
        ChargingPointResponse response = pointService.getPointById(pointId);
        return ResponseEntity.ok(response);
    }

    // =====================================================================
    // ✅ 6. CẬP NHẬT MỘT ĐIỂM SẠC
    // =====================================================================
    @PutMapping("/{pointId}") // PUT /api/charging-points/{pointId}
    @PreAuthorize("hasRole('ADMIN')") // chỉ ADMIN mới được phép sửa cấu hình điểm sạc
    @Operation(summary = "Update charging point", description = "Update configuration of an existing charging point")
    public ResponseEntity<ChargingPointResponse> updatePoint(
            @Parameter(description = "Charging Point ID") @PathVariable Long pointId,
            @Valid @RequestBody CreateChargingPointRequest request
    ) {
        try {
            ChargingPointResponse response = pointService.updateChargingPoint(pointId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // =====================================================================
    // ✅ 7. XOÁ MỘT ĐIỂM SẠC
    // =====================================================================
    @DeleteMapping("/{pointId}") // DELETE /api/charging-points/{pointId}
    @PreAuthorize("hasRole('ADMIN')") // xoá thiết bị => chỉ ADMIN
    @Operation(summary = "Delete charging point", description = "Delete a charging point by ID")
    public ResponseEntity<Void> deletePoint(
            @Parameter(description = "Charging Point ID") @PathVariable Long pointId
    ) {
        pointService.deleteChargingPoint(pointId);
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}
