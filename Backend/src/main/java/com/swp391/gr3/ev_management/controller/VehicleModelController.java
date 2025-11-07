package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.dto.request.VehicleModelCreateRequest;
import com.swp391.gr3.ev_management.dto.request.VehicleModelUpdateRequest;
import com.swp391.gr3.ev_management.dto.response.UpdateStatusRequest;
import com.swp391.gr3.ev_management.dto.response.VehicleModelResponse;
import com.swp391.gr3.ev_management.repository.VehicleModelRepository;
import com.swp391.gr3.ev_management.service.VehicleModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController // ✅ REST controller trả JSON
@RequiredArgsConstructor // ✅ Lombok tạo constructor cho các field final (DI)
@Tag(name = "Vehicle Model Controller", description = "APIs for managing vehicle models")
@RequestMapping("/api/vehicle-models") // ✅ Prefix cho tất cả endpoint
public class VehicleModelController {

    private final VehicleModelService vehicleModelService; // ✅ Chứa nghiệp vụ CRUD cho Vehicle Model
    private final VehicleModelRepository vehicleModelRepository; // ✅ Truy vấn DB trực tiếp khi cần (ví dụ lấy brands distinct)

    // ===================== ADMIN ONLY =====================

    // ✅ Xoá model theo ID (204 No Content nếu thành công)
    @PreAuthorize("hasRole('ADMIN')") // 🔒 Chỉ ADMIN
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Vehicle Model (Admin)", description = "Delete a vehicle model by its ID", hidden = false)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        vehicleModelService.delete(id); // Gọi service xoá (có thể kiểm tra ràng buộc/đang sử dụng)
        return ResponseEntity.noContent().build();
    }

    // ✅ Tạo model mới (trả về thông tin model sau khi tạo)
    @PreAuthorize("hasRole('ADMIN')") // 🔒 Chỉ ADMIN
    @PostMapping
    @Operation(summary = "Create Vehicle Model (Admin)", description = "Create a new vehicle model", hidden = false)
    public ResponseEntity<VehicleModelResponse> create(@Valid @RequestBody VehicleModelCreateRequest request) {
        // request gồm các trường như brand, modelName, battery, range, connector types,...
        return ResponseEntity.ok(vehicleModelService.create(request));
    }

    // ✅ Cập nhật model theo ID (PUT: thay đổi toàn bộ/quan trọng)
    @PreAuthorize("hasRole('ADMIN')") // 🔒 Chỉ ADMIN
    @PutMapping("/{id}")
    @Operation(summary = "Update Vehicle Model (Admin)", description = "Update an existing vehicle model by its ID")
    public ResponseEntity<VehicleModelResponse> update(@PathVariable Long id,
                                                       @Valid @RequestBody VehicleModelUpdateRequest request) {
        // request chứa các field cho phép cập nhật (ví dụ: công suất sạc tối đa, thông số pin,...)
        return ResponseEntity.ok(vehicleModelService.update(id, request));
    }

    // ✅ Đổi trạng thái model (ACTIVE/INACTIVE, ...) bằng PATCH
    @PreAuthorize("hasRole('ADMIN')") // 🔒 Chỉ ADMIN
    @PatchMapping("/{id}/status")
    @Operation(
            summary = "Update Vehicle Model Status (Admin)",
            description = "Update status of a vehicle model (e.g. ACTIVE / INACTIVE)"
    )
    public ResponseEntity<VehicleModelResponse> updateStatus(@PathVariable Long id,
                                                             @Valid @RequestBody UpdateStatusRequest body) {
        // body.getStatus() là enum trạng thái; service sẽ kiểm tra hợp lệ trước khi cập nhật
        return ResponseEntity.ok(vehicleModelService.updateStatus(id, body.getStatus()));
    }

    // ===================== PUBLIC (không yêu cầu auth) =====================

    // ✅ Lấy tất cả model (dùng cho admin/list – nhưng đang để public)
    @GetMapping("/models")
    @Operation(summary = "List or Search Vehicle Models (Admin)", description = "Admin endpoint to list all vehicle models")
    public ResponseEntity<List<VehicleModelResponse>> listOrSearchAdmin() {
        return ResponseEntity.ok(vehicleModelService.getAll());
    }

    // ✅ Lấy danh sách brand (distinct) để bước 1: chọn hãng
    @GetMapping("/brands")
    @Operation(
            summary = "Get all vehicle brands",
            description = "Get all distinct vehicle brands (Step 1: User selects a brand)"
    )
    public ResponseEntity<List<String>> getAllBrands() {
        // Lấy toàn bộ model rồi map -> brand, distinct, sort (có thể tối ưu bằng query distinct ở repo)
        List<String> brands = vehicleModelRepository.findAll()
                .stream()
                .map(vm -> vm.getBrand())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        return ResponseEntity.ok(brands);
    }

    // ✅ Lấy danh sách model theo brand đã chọn (bước 2 sau khi user chọn brand)
    @GetMapping("/brand/models")
    @Operation(
            summary = "Get vehicle models by brand",
            description = "Get all vehicle models for a specific brand (Step 2: After user clicks a brand)"
    )
    public ResponseEntity<List<VehicleModelResponse>> getModelsByBrand(
            @RequestParam(required = true) String brand // brand bắt buộc qua query param
    ) {
        // search(brand, modelName, battery, connectorType) — ở đây chỉ filter theo brand
        return ResponseEntity.ok(vehicleModelService.search(brand, null, null, null));
    }

    // ✅ Lấy chi tiết 1 model theo ID
    @GetMapping("/{id}")
    @Operation(summary = "Get Vehicle Model by ID", description = "Get a vehicle model by its ID")
    public ResponseEntity<VehicleModelResponse> getById(@PathVariable Long id) {
        VehicleModelResponse response = vehicleModelService.getById(id);
        if (response == null) {
            // Không tồn tại -> trả 404 Not Found
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }
}
