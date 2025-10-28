package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.response.NotificationResponse;
import com.swp391.gr3.ev_management.entity.Notification;
import com.swp391.gr3.ev_management.mapper.NotificationMapper;
import com.swp391.gr3.ev_management.repository.NotificationsRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.swp391.gr3.ev_management.DTO.response.CreateNotificationResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationsServiceImpl implements NotificationsService{

    private final NotificationsRepository notificationsRepository;
    private final NotificationMapper notificationMapper;
    private final NotificationMapper mapper;

    @Override
    public List<CreateNotificationResponse> getNotificationsByUser(Long userId) {
        return notificationsRepository.findByUser_UserId(userId)
                .stream()
                .map(mapper::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CreateNotificationResponse> getUnreadNotificationsByUser(Long userId) {
        return notificationsRepository.findUnreadByUserId(userId)
                .stream()
                .map(mapper::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Long getUnreadCount(Long userId) {
        return notificationsRepository.countByUser_UserIdAndStatus(userId, "unread");
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification n = notificationsRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        if (!n.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("No permission to mark this notification");
        }

        n.setStatus("READ");
        n.setReadAt(LocalDateTime.now());
        notificationsRepository.save(n);// update status and readAt
    }

    @Override
    public NotificationResponse getNotificationById(Long notificationId, Long userId) {
        // Tìm thông báo theo ID
        Notification notification = notificationsRepository.findById(notificationId)
                .orElse(null);

        // Nếu không có → trả null, để controller xử lý
        if (notification == null) {
            return null;
        }

        // Kiểm tra quyền sở hữu
        if (!notification.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền truy cập thông báo này");
        }

        // Nếu thông báo chưa đọc → cập nhật trạng thái sang "READ"
        if ("UNREAD".equalsIgnoreCase(notification.getStatus())) {
            notification.setStatus("READ");
            notification.setReadAt(LocalDateTime.now());
            notificationsRepository.save(notification);
        }

        // Trả về DTO thông tin chi tiết
        return notificationMapper.mapToNotificationResponse(notification);
    }
}
