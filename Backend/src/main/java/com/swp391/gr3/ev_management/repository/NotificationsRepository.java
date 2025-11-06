package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.Notification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationsRepository extends JpaRepository<Notification, Long> {

    long countByUser_UserIdAndStatus(Long userId, String status);

    // Tìm tất cả notification của user
    List<Notification> findByUser_UserId(Long userId);

    // Tìm notification chưa đọc
    @Query("SELECT n FROM Notification n " +
            "WHERE n.user.userId = :userId " +
            "AND n.status = 'unread' " +
            "ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByUserId(@Param("userId") Long userId);

    List<Notification> findByUserUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"user", "booking"})
    Optional<Notification> findById(Long id);

}
