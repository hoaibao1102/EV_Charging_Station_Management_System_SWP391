package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.response.NotificationResponse;
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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "APIs for managing notifications")
public class NotificationController {

    private final NotificationsService notificationsService;

    @PutMapping("/{notificationId}/read")
    @Operation(summary = "Mark notification as read", description = "Mark a specific notification as read for the logged-in user")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId, Authentication auth) {
        Long userId = Long.valueOf(auth.getName());
        notificationsService.markAsRead(notificationId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{notificationId}")
    @Operation(
            summary = "Get notification by ID",
            description = "Retrieve details of a specific notification by its ID for the logged-in user"
    )
    public ResponseEntity<?> getById(
            @Parameter(description = "ID của thông báo", required = true)
            @PathVariable Long notificationId,
            Authentication auth
    ) {
        Long userId = Long.valueOf(auth.getName());
        NotificationResponse notification = notificationsService.getNotificationById(notificationId, userId);

        if (notification == null) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Không tìm thấy thông báo");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        return ResponseEntity.ok(notification);
    }

    @GetMapping
    @Operation(summary = "Get all notifications", description = "Get all notifications for the logged-in user")
    public ResponseEntity<?> getAllNotifications(Authentication auth) {
        Long userId = Long.valueOf(auth.getName());
        var notifications = notificationsService.getNotificationsByUser(userId);

        if (notifications == null || notifications.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "Không có thông báo"));
        }
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread/count")
    @Operation(summary = "Get unread notification count", description = "Get the count of unread notifications for the logged-in user")
    public ResponseEntity<Long> getUnreadCount(Authentication auth) {
        Long userId = Long.valueOf(auth.getName());
        return ResponseEntity.ok(notificationsService.getUnreadCount(userId));
    }

}
