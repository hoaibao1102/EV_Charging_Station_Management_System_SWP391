package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationsRepository extends JpaRepository<Notification, Long> {
//    public List<Notification> findNotificationsByUserId(Long userId);
//    public Notification createNotification(Notification notification);
//    public Notification updateNotification(Notification notification);
//    public void deleteNotification(Long notificationId);
//    public Long getNotificationIdByUserId(Long userId);
    // field trong Notification là 'user' -> tham chiếu theo 'user.userId'
    List<Notification> findAllByUser_UserIdOrderByCreatedAtDesc(Long userId);

    long countByUser_UserIdAndStatus(Long userId, String status);

    @Modifying
    @Query("update Notification n set n.status = ?2, n.readAt = ?3 where n.notiId in ?1")
    int bulkUpdateStatusAndReadAt(List<Long> notiIds, String status, LocalDateTime readAt);

}
