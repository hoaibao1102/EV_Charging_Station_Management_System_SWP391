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

@RestController
@RequestMapping("/api/upload")
@Tag(name = "Upload Controller", description = "APIs for handling file uploads (e.g., getting signatures)")
public class UploadController {

    @Autowired
    private Cloudinary cloudinary;

    @Value("${cloudinary.api_key}")
    private String apiKey;

    @Value("${cloudinary.api_secret}")
    private String apiSecret;

    @Value("${cloudinary.cloud_name}")
    private String cloudName;

    /**
     * Endpoint 1: Cung cấp chữ ký cho frontend
     * (Bảo vệ endpoint này giống như các endpoint 'create' của Admin)
     */
    @GetMapping("/signature")
    @PreAuthorize("hasRole('ADMIN')") 
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get Cloudinary Upload Signature (Admin)", 
             description = "Admin only - Get a signature to upload a file directly to Cloudinary")
    public ResponseEntity<Map<String, Object>> getSignature() {
        // Tạo timestamp (tính bằng giây)
        long timestamp = System.currentTimeMillis() / 1000;

        // Tạo Map chứa các tham số cần ký (nếu bạn muốn ký thêm các tham số khác)
        Map<String, Object> paramsToSign = new HashMap<>();
        paramsToSign.put("timestamp", timestamp);
        // Ví dụ: nếu bạn muốn ép upload vào 1 folder cụ thể:
        paramsToSign.put("folder", "vehicle_models"); 

        try {
            // Dùng apiSecret để tạo chữ ký
            String signature = cloudinary.apiSignRequest(paramsToSign, apiSecret);

            // Trả về các thông tin cần thiết cho frontend
            Map<String, Object> response = new HashMap<>();
            response.put("signature", signature);
            response.put("timestamp", timestamp);
            response.put("api_key", apiKey);
            response.put("cloud_name", cloudName);
            response.put("folder", "vehicle_models"); 

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

}