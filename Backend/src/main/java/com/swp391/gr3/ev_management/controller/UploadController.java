package com.swp391.gr3.ev_management.controller;

import com.cloudinary.Cloudinary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController // ✅ Đánh dấu đây là REST Controller — trả dữ liệu JSON cho client
@RequestMapping("/api/upload") // ✅ Tất cả endpoint trong controller này bắt đầu bằng /api/upload
@Tag(name = "Upload Controller", description = "APIs for handling file uploads (e.g., getting signatures)")
// ✅ Swagger: nhóm các API phục vụ chức năng upload (lấy chữ ký, xác thực,...)
public class UploadController {

    @Autowired
    private Cloudinary cloudinary;
    // ✅ Inject Cloudinary client (được cấu hình sẵn trong ứng dụng)
    //    Dùng để tạo chữ ký hoặc thực hiện các thao tác upload file qua Cloudinary API

    // ✅ Đọc các giá trị Cloudinary config từ file application.properties (hoặc .env)
    @Value("${cloudinary.api_key}")
    private String apiKey;

    @Value("${cloudinary.api_secret}")
    private String apiSecret;

    @Value("${cloudinary.cloud_name}")
    private String cloudName;

    /**
     * ✅ Endpoint 1: Cung cấp chữ ký upload cho frontend (chữ ký Cloudinary)
     *    - Mục đích: Cho phép frontend (ví dụ: React hoặc Angular) upload trực tiếp lên Cloudinary
     *    - Quyền hạn: Chỉ ADMIN mới được phép gọi API này (để tránh bị lạm dụng)
     */
    @GetMapping("/signature") // 🔗 Endpoint: GET /api/upload/signature
    @PreAuthorize("hasRole('ADMIN')")  // 🔒 Chỉ người có role ADMIN mới được phép lấy chữ ký upload
    @SecurityRequirement(name = "bearerAuth") // 🔐 Swagger yêu cầu xác thực bằng Bearer Token
    @Operation(
            summary = "Get Cloudinary Upload Signature (Admin)",
            description = "Admin only - Get a signature to upload a file directly to Cloudinary"
    )
    public ResponseEntity<Map<String, Object>> getSignature() {
        // 🕒 Tạo timestamp hiện tại (đơn vị: giây)
        //     → Cloudinary yêu cầu tham số "timestamp" khi tạo chữ ký upload
        long timestamp = System.currentTimeMillis() / 1000;

        // 🧾 Tạo một map chứa các tham số sẽ được ký (tùy theo quy tắc upload của Cloudinary)
        Map<String, Object> paramsToSign = new HashMap<>();
        paramsToSign.put("timestamp", timestamp);

        // 💡 Ví dụ: ép các file upload vào folder cụ thể trong Cloudinary
        paramsToSign.put("folder", "vehicle_models");

        try {
            // 🔑 Tạo chữ ký (signature) dựa trên tham số và apiSecret
            //     → Đây là phần xác thực giúp Cloudinary biết yêu cầu upload là hợp lệ
            String signature = cloudinary.apiSignRequest(paramsToSign, apiSecret);

            // 🟢 Tạo response chứa toàn bộ thông tin cần thiết cho frontend để upload trực tiếp lên Cloudinary
            Map<String, Object> response = new HashMap<>();
            response.put("signature", signature);  // ✅ Chữ ký upload hợp lệ
            response.put("folder", "vehicle_models"); // ✅ Folder Cloudinary được chỉ định
            response.put("timestamp", timestamp); // ✅ Thời gian tạo chữ ký
            response.put("api_key", apiKey); // ✅ API key Cloudinary (frontend cần để xác thực)
            response.put("cloud_name", cloudName); // ✅ Tên Cloudinary (frontend cần khi upload)

            // ✅ Trả về HTTP 200 OK cùng với thông tin upload
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // ❌ Nếu có lỗi khi tạo chữ ký → Trả về HTTP 500 cùng thông tin lỗi
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

}