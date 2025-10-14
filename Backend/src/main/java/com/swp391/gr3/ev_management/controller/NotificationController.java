package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.response.CreateNotificationResponse;
import com.swp391.gr3.ev_management.DTO.response.NotificationResponse;
import com.swp391.gr3.ev_management.service.NotificationsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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

    @GetMapping
    @Operation(summary = "Get all notifications", description = "Get all notifications for the logged-in user")
    public ResponseEntity<?> getAllNotifications(org.springframework.security.core.Authentication auth) {
        Long userId = Long.valueOf(auth.getName()); // vì principal = userId string
        var notifications = notificationsService.getNotificationsByUser(userId);

        if (notifications == null || notifications.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "Không có thông báo"));
        }
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadNotifications(Authentication auth) {
        Long userId = Long.valueOf(auth.getName());
        var notifications = notificationsService.getUnreadNotificationsByUser(userId);
        if (notifications == null || notifications.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "Không có thông báo chưa đọc"));
        }
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Long> getUnreadCount(Authentication auth) {
        Long userId = Long.valueOf(auth.getName());
        return ResponseEntity.ok(notificationsService.getUnreadCount(userId));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId, Authentication auth) {
        Long userId = Long.valueOf(auth.getName());
        notificationsService.markAsRead(notificationId, userId);
        return ResponseEntity.noContent().build();
    }

}
