package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.entity.Notification;
import com.swp391.gr3.ev_management.repository.NotificationsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationsServiceImpl implements NotificationsService{

    @Autowired
    private final NotificationsRepository notificationsRepository;

    @Override
    public List<Notification> findNotificationsByUserId(Long userId) {
        return notificationsRepository.findAllByUser_UserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public Notification createNotification(Notification notification) {
        return notificationsRepository.save(notification);
    }

    @Override
    public Notification updateNotification(Notification notification) {
        return notificationsRepository.save(notification);
    }

    @Override
    public void deleteNotification(Long notificationId) {
        notificationsRepository.deleteById(notificationId);
    }

    @Override
    public Long getNotificationIdByUserId(Long userId) {
        // nếu cần ID gần nhất:
        return notificationsRepository.findAllByUser_UserIdOrderByCreatedAtDesc(userId)
                .stream().findFirst().map(Notification::getNotiId).orElse(null);
    }

    @Transactional
    public int markAsRead(List<Long> notificationIds) {
        return notificationsRepository.bulkUpdateStatusAndReadAt(
                notificationIds, Notification.STATUS_READ, LocalDateTime.now()
        );
    }
}
