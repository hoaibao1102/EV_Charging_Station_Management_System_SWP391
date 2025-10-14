package com.swp391.gr3.ev_management.events;

import com.swp391.gr3.ev_management.entity.Notification;
import com.swp391.gr3.ev_management.repository.NotificationsRepository;
import com.swp391.gr3.ev_management.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEmailListener {
    private final NotificationsRepository notificationsRepository;
    private final EmailService emailService;

    @org.springframework.transaction.event.TransactionalEventListener(
            phase = org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT)
    public void onNotificationCreated(com.swp391.gr3.ev_management.events.NotificationCreatedEvent event) {
        notificationsRepository.findById(event.notificationId())
                .ifPresent(emailService::sendNotificationEmail); // gửi mail ở đây
    }
}
