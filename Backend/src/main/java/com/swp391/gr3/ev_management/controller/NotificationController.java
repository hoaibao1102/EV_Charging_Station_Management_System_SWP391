package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.request.CreateNotificationRequest;
import com.swp391.gr3.ev_management.DTO.response.CreateNotificationResponse;
import com.swp391.gr3.ev_management.service.StaffNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff/notifications")
@RequiredArgsConstructor
@Tag(name = "Staff Notification", description = "APIs for staff to manage notifications")
public class NotificationController {

    private final StaffNotificationService notificationService;

    @PostMapping
    @Operation(summary = "Create notification", description = "Create a new notification for a user")
    public ResponseEntity<CreateNotificationResponse> createNotification(
            @Valid @RequestBody CreateNotificationRequest request
    ) {
        CreateNotificationResponse response = notificationService.createNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all notifications", description = "Get all notifications for a staff member")
    public ResponseEntity<List<CreateNotificationResponse>> getAllNotifications(
            @Parameter(description = "Staff ID") @RequestParam Long staffId
    ) {
        List<CreateNotificationResponse> notifications = notificationService.getNotificationsByUser(staffId);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread")
    @Operation(summary = "Get unread notifications", description = "Get unread notifications for a staff member")
    public ResponseEntity<List<CreateNotificationResponse>> getUnreadNotifications(
            @Parameter(description = "Staff ID") @RequestParam Long staffId
    ) {
        List<CreateNotificationResponse> notifications = notificationService.getUnreadNotificationsByUser(staffId);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread/count")
    @Operation(summary = "Get unread count", description = "Get count of unread notifications")
    public ResponseEntity<Long> getUnreadCount(
            @Parameter(description = "Staff ID") @RequestParam Long staffId
    ) {
        Long count = notificationService.getUnreadCount(staffId);
        return ResponseEntity.ok(count);
    }

    @PutMapping("/{notificationId}/read")
    @Operation(summary = "Mark as read", description = "Mark a notification as read")
    public ResponseEntity<Void> markAsRead(
            @Parameter(description = "Notification ID") @PathVariable Long notificationId,
            @Parameter(description = "Staff ID") @RequestParam Long staffId
    ) {
        notificationService.markAsRead(notificationId, staffId);
        return ResponseEntity.noContent().build();
    }

}
