package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.dto.request.ConnectorTypeCreateRequest;
import com.swp391.gr3.ev_management.dto.request.ConnectorTypeUpdateRequest;
import com.swp391.gr3.ev_management.dto.response.ConnectorTypeResponse;
import com.swp391.gr3.ev_management.service.ConnectorTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // ✅ Đánh dấu đây là REST Controller (tự động trả JSON thay vì view)
@RequestMapping("/api/connector-types") // ✅ Định nghĩa prefix chung cho toàn bộ endpoint
@Tag(name = "Connector Type Controller", description = "APIs for managing connector types") // ✅ Nhóm API cho Swagger UI
@RequiredArgsConstructor // ✅ Lombok: tự động tạo constructor cho field final (DI)
public class ConnectorTypeController {

    private final ConnectorTypeService connectorTypeService; // ✅ Service xử lý nghiệp vụ về loại đầu nối (connector type)

    // =========================================================================
    // ✅ 1. ADMIN: CẬP NHẬT LOẠI ĐẦU NỐI (PUT /{connectorTypeId})
    // =========================================================================
    @PutMapping("/{connectorTypeId}") // 🔗 Endpoint: PUT /api/connector-types/{connectorTypeId}
    @PreAuthorize("hasRole('ADMIN')") // 🔒 Chỉ tài khoản có quyền ADMIN mới được cập nhật
    @SecurityRequirement(name = "bearerAuth") // ✅ Swagger: yêu cầu token Bearer để truy cập
    @Operation(summary = "Update connector type", description = "Admin only - Update an existing connector type")
    public ResponseEntity<ConnectorTypeResponse> updateConnectorType(
            @PathVariable Long connectorTypeId, // ✅ Lấy ID loại đầu nối từ URL
            @Valid @RequestBody ConnectorTypeUpdateRequest request // ✅ Dữ liệu cập nhật từ request body, có validation
    ) {
        // 🟢 Gọi service để cập nhật loại đầu nối theo ID
        ConnectorTypeResponse response = connectorTypeService.updateConnectorType(connectorTypeId, request);

        // 🟢 Trả về HTTP 200 OK cùng thông tin loại đầu nối đã cập nhật
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // ✅ 2. ADMIN: TẠO MỚI LOẠI ĐẦU NỐI (POST /)
    // =========================================================================
    @PostMapping // 🔗 Endpoint: POST /api/connector-types
    @PreAuthorize("hasRole('ADMIN')") // 🔒 Chỉ ADMIN được phép tạo
    @SecurityRequirement(name = "bearerAuth") // ✅ Swagger yêu cầu xác thực token
    @Operation(summary = "Create new connector type", description = "Admin only - Create a new connector type")
    public ResponseEntity<ConnectorTypeResponse> createConnectorType(
            @Valid @RequestBody ConnectorTypeCreateRequest request // ✅ Dữ liệu yêu cầu tạo mới, được validate
    ) {
        // 🟢 Gọi service để tạo loại đầu nối mới
        ConnectorTypeResponse response = connectorTypeService.createConnectorType(request);

        // 🟢 Trả về HTTP 201 CREATED cùng với dữ liệu loại đầu nối vừa được tạo
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =========================================================================
    // ✅ 3. PUBLIC: LẤY DANH SÁCH TẤT CẢ LOẠI ĐẦU NỐI (GET /)
    // =========================================================================
    @GetMapping // 🔗 Endpoint: GET /api/connector-types
    @Operation(summary = "Get all connector types", description = "Public endpoint to retrieve all connector types")
    public ResponseEntity<List<ConnectorTypeResponse>> getAllConnectorTypes() {
        // 🟢 Gọi service để lấy danh sách tất cả loại đầu nối hiện có trong hệ thống
        List<ConnectorTypeResponse> list = connectorTypeService.getAllConnectorTypes();

        // 🟢 Trả về danh sách (HTTP 200 OK)
        return ResponseEntity.ok(list);
    }

    // =========================================================================
    // ✅ 4. PUBLIC: LẤY CHI TIẾT MỘT LOẠI ĐẦU NỐI THEO ID (GET /{connectorTypeId})
    // =========================================================================
    @GetMapping("/{connectorTypeId}") // 🔗 Endpoint: GET /api/connector-types/{connectorTypeId}
    @Operation(summary = "Get connector type by ID", description = "Public endpoint to retrieve a specific connector type")
    public ResponseEntity<ConnectorTypeResponse> getConnectorTypeById(
            @PathVariable Long connectorTypeId // ✅ Lấy ID loại đầu nối từ URL
    ) {
        // 🟢 Gọi service để lấy thông tin chi tiết của loại đầu nối theo ID
        ConnectorTypeResponse response = connectorTypeService.getConnectorTypeById(connectorTypeId);

        // 🟢 Trả về dữ liệu (HTTP 200 OK)
        return ResponseEntity.ok(response);
    }

}
