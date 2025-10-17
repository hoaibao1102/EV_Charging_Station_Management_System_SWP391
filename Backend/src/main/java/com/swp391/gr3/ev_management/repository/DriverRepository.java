package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.enums.DriverStatus;
import com.swp391.gr3.ev_management.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    // Kiểm tra tồn tại driver theo userId (đúng property path)
    boolean existsByUser_UserId(Long userId);

    // Lấy driver theo userId (không fetch join)
    Optional<Driver> findByUser_UserId(Long userId);

    // JOIN FETCH theo userId (dùng khi cần chắc chắn có User đã load)
    @Query("""
           select d from Driver d 
           join fetch d.user u 
           where u.userId = :userId
           """)
    Optional<Driver> findByUserIdWithUser(@Param("userId") Long userId);

    // JOIN FETCH theo driverId (khi bạn có driverId)
    @Query("""
           select d from Driver d 
           join fetch d.user u 
           where d.driverId = :driverId
           """)
    Optional<Driver> findByDriverIdWithUser(@Param("driverId") Long driverId);

    // Các finder khác bạn đang dùng
    List<Driver> findByStatus(DriverStatus status);

    List<Driver> findByUser_NameContainingIgnoreCase(String name);

    List<Driver> findByUser_PhoneNumberContaining(String phoneNumber);
}