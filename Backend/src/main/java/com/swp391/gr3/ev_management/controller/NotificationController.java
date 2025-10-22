package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.response.CreateNotificationResponse;
import com.swp391.gr3.ev_management.DTO.response.NotificationResponse;
import com.swp391.gr3.ev_management.service.NotificationsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @Autowired
    private final NotificationsService notificationsService;

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

    @PutMapping("/{notificationId}/read")
    @Operation(summary = "Mark notification as read", description = "Mark a specific notification as read for the logged-in user")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId, Authentication auth) {
        Long userId = Long.valueOf(auth.getName());
        notificationsService.markAsRead(notificationId, userId);
        return ResponseEntity.noContent().build();
    }

}
