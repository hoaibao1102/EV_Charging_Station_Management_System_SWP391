package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.CreateNotificationRequest;
import com.swp391.gr3.ev_management.DTO.response.CreateNotificationResponse;
import com.swp391.gr3.ev_management.entity.Notification;
import com.swp391.gr3.ev_management.entity.User;
import com.swp391.gr3.ev_management.mapper.NotificationMapper;
import com.swp391.gr3.ev_management.repository.NotificationRepository;
import com.swp391.gr3.ev_management.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffNotificationServiceImpl implements StaffNotificationService{
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationMapper mapper;


    @Override
    @Transactional
    public CreateNotificationResponse createNotification(CreateNotificationRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Notification noti = new Notification();
        noti.setUser(user);
        noti.setType(request.getType());
        noti.setTitle(request.getTitle());
        noti.setContentNoti(request.getContent());
        noti.setStatus("unread");
        noti.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(noti);

        return mapper.mapToResponse(noti);
    }

    @Override
    public List<CreateNotificationResponse> getNotificationsByUser(Long userId) {
        return notificationRepository.findByUser_UserId(userId)
                .stream()
                .map(mapper::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CreateNotificationResponse> getUnreadNotificationsByUser(Long userId) {
        return notificationRepository.findUnreadByUserId(userId)
                .stream()
                .map(mapper::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countByUser_UserIdAndStatus(userId, "unread");
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        if (!n.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("No permission to mark this notification");
        }

        n.setStatus("read");
        n.setReadAt(LocalDateTime.now());
        notificationRepository.save(n);// sửa lại update
    }
}
