package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification,Long> {
    // Tìm tất cả notification của user
    List<Notification> findByUser_UserId(Long userId);

    // Tìm notification theo status
    List<Notification> findByUser_UserIdAndStatus(Long userId, String status);

    // Tìm notification chưa đọc
    @Query("SELECT n FROM Notification n " +
            "WHERE n.user.userId = :userId " +
            "AND n.status = 'unread' " +
            "ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByUserId(@Param("userId") Long userId);

    // Tìm notification theo type
    List<Notification> findByUser_UserIdAndType(Long userId, String type);

    // Đếm notification chưa đọc
    Long countByUser_UserIdAndStatus(Long userId, String status);

    // Tìm notification trong khoảng thời gian
    @Query("SELECT n FROM Notification n " +
            "WHERE n.user.userId = :userId " +
            "AND n.createdAt >= :startDate " +
            "AND n.createdAt <= :endDate " +
            "ORDER BY n.createdAt DESC")
    List<Notification> findByUserAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Đánh dấu tất cả notification là đã đọc
    @Query("UPDATE Notification n SET n.status = 'read', n.readAt = :readAt " +
            "WHERE n.user.userId = :userId AND n.status = 'unread'")
    void markAllAsReadByUserId(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);
}
