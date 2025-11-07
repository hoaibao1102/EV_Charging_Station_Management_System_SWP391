package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.dto.response.NotificationResponse;
import com.swp391.gr3.ev_management.service.NotificationsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController // ✅ Đánh dấu đây là REST controller (tự động trả JSON)
@RequestMapping("/api/notifications") // ✅ Tất cả endpoint bắt đầu với /api/notifications
@RequiredArgsConstructor // ✅ Lombok: tự động tạo constructor cho field final (DI)
@Tag(name = "Notifications", description = "APIs for managing notifications") // ✅ Dùng cho Swagger
public class NotificationController {

    private final NotificationsService notificationsService; // ✅ Service xử lý nghiệp vụ liên quan đến thông báo (notifications)

    // =========================================================================
    // ✅ 1. ĐÁNH DẤU THÔNG BÁO LÀ ĐÃ ĐỌC
    // =========================================================================
    @PutMapping("/{notificationId}/read") // 🔗 Endpoint: PUT /api/notifications/{notificationId}/read
    @Operation(summary = "Mark notification as read", description = "Mark a specific notification as read for the logged-in user")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long notificationId, // ✅ ID của thông báo cần đánh dấu
            Authentication auth // ✅ Đối tượng Authentication chứa thông tin user đang đăng nhập
    ) {
        Long userId = Long.valueOf(auth.getName()); // 🟢 Lấy userId từ auth (vì auth.getName() lưu userId dưới dạng String)
        notificationsService.markAsRead(notificationId, userId); // 🟢 Gọi service để đánh dấu thông báo là "đã đọc"
        return ResponseEntity.noContent().build(); // 🟢 Trả về HTTP 204 (thành công nhưng không có nội dung trả lại)
    }

    // =========================================================================
    // ✅ 2. LẤY CHI TIẾT 1 THÔNG BÁO THEO ID
    // =========================================================================
    @GetMapping("/{notificationId}") // 🔗 Endpoint: GET /api/notifications/{notificationId}
    @Operation(
            summary = "Get notification by ID",
            description = "Retrieve details of a specific notification by its ID for the logged-in user"
    )
    public ResponseEntity<?> getById(
            @Parameter(description = "ID của thông báo", required = true) // 📝 Swagger: mô tả tham số
            @PathVariable Long notificationId, // ✅ Lấy ID thông báo từ URL
            Authentication auth // ✅ Lấy thông tin người dùng hiện tại
    ) {
        Long userId = Long.valueOf(auth.getName()); // 🟢 Trích xuất userId từ Authentication
        NotificationResponse notification = notificationsService.getNotificationById(notificationId, userId); // 🟢 Lấy chi tiết thông báo cho user

        if (notification == null) {
            // ❌ Nếu không tìm thấy thông báo -> trả về HTTP 404 kèm message thân thiện
            Map<String, String> response = new HashMap<>();
            response.put("message", "Không tìm thấy thông báo");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        // ✅ Nếu có -> trả về HTTP 200 OK cùng nội dung thông báo
        return ResponseEntity.ok(notification);
    }

    // =========================================================================
    // ✅ 3. LẤY TẤT CẢ THÔNG BÁO CỦA USER ĐANG ĐĂNG NHẬP
    // =========================================================================
    @GetMapping // 🔗 Endpoint: GET /api/notifications
    @Operation(summary = "Get all notifications", description = "Get all notifications for the logged-in user")
    public ResponseEntity<?> getAllNotifications(Authentication auth) {
        Long userId = Long.valueOf(auth.getName()); // 🟢 Lấy userId của người dùng hiện tại
        var notifications = notificationsService.getNotificationsByUser(userId); // 🟢 Lấy danh sách tất cả thông báo của user

        if (notifications == null || notifications.isEmpty()) {
            // ❌ Nếu không có thông báo -> trả về 200 OK kèm message "Không có thông báo"
            return ResponseEntity.ok(Map.of("message", "Không có thông báo"));
        }

        // ✅ Nếu có thông báo -> trả về danh sách
        return ResponseEntity.ok(notifications);
    }

    // =========================================================================
    // ✅ 4. LẤY SỐ LƯỢNG THÔNG BÁO CHƯA ĐỌC
    // =========================================================================
    @GetMapping("/unread/count") // 🔗 Endpoint: GET /api/notifications/unread/count
    @Operation(summary = "Get unread notification count", description = "Get the count of unread notifications for the logged-in user")
    public ResponseEntity<Long> getUnreadCount(Authentication auth) {
        Long userId = Long.valueOf(auth.getName()); // 🟢 Lấy userId của user đang đăng nhập
        // 🟢 Gọi service để đếm số lượng thông báo có trạng thái "chưa đọc"
        return ResponseEntity.ok(notificationsService.getUnreadCount(userId));
    }

}
