package com.swp391.gr3.ev_management.events;

import com.swp391.gr3.ev_management.emuns.NotificationTypes;
import com.swp391.gr3.ev_management.entity.Notification;
import com.swp391.gr3.ev_management.entity.User;
import com.swp391.gr3.ev_management.repository.NotificationsRepository;
import com.swp391.gr3.ev_management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEventHandler {

    private final NotificationsRepository notificationsRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher publisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent event) {

        User user = userRepository.findById(event.userId())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        Notification noti = new Notification();
        noti.setUser(user);
        noti.setType(NotificationTypes.USER_REGISTERED);
        noti.setTitle("Đăng ký thành công");
        noti.setContentNoti("Chào " + event.fullName() + ", tài khoản của bạn đã được tạo thành công.");
        noti.setStatus(Notification.STATUS_UNREAD);

        notificationsRepository.save(noti);
        // ✅ Bây giờ mới publish event thông báo đã được tạo
        publisher.publishEvent(new NotificationCreatedEvent(noti.getNotiId()));
    }
}
