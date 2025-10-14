package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.response.NotificationResponse;
import com.swp391.gr3.ev_management.entity.Notification;
import com.swp391.gr3.ev_management.service.NotificationsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationsController {

    private final NotificationsServiceImpl notificationsService;

    @GetMapping("/me")
    public List<NotificationResponse> myNotifications(@RequestParam Long userId) {
        return notificationsService.findNotificationsByUserId(userId)
                .stream().map(NotificationResponse::from).toList();
    }

    @PostMapping("/mark-read")
    public int markRead(@RequestBody List<Long> notiIds) {
        return notificationsService.markAsRead(notiIds);
    }

    @GetMapping("/user/{userId}")
    public List<NotificationResponse> getByUser(@PathVariable Long userId) {
        return notificationsService.findNotificationsByUserId(userId)
                .stream()
                .map(NotificationResponse::from) // Dùng DTO
                .toList();
    }
}
