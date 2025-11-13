package com.swp391.gr3.ev_management.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration // ☁️ Đây là class cấu hình Spring, dùng để khai báo bean Cloudinary
public class CloudinaryConfig {

    @Value("${cloudinary.cloud_name}")  // 🔧 Inject giá trị từ application.properties (hoặc .env)
    private String cloudName;

    @Value("${cloudinary.api_key}")     // 🔧 Inject API Key Cloudinary
    private String apiKey;

    @Value("${cloudinary.api_secret}")  // 🔧 Inject API Secret Cloudinary
    private String apiSecret;

    @Bean  // ⭐ Khai báo bean Cloudinary để Spring quản lý và inject vào các service khác khi cần
    public Cloudinary cloudinary() {
        // 🧩 Tạo Map config chứa thông tin đăng nhập Cloudinary
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", cloudName);   // ☁️ Tên Cloud
        config.put("api_key", apiKey);         // 🔑 API Key
        config.put("api_secret", apiSecret);   // 🔐 API Secret

        // 🏗️ Tạo đối tượng Cloudinary với config trên và trả về để Spring inject
        return new Cloudinary(config);
    }
}
