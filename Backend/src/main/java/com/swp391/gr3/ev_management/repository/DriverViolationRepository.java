package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.DriverViolation;
import com.swp391.gr3.ev_management.enums.ViolationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DriverViolationRepository extends JpaRepository<DriverViolation, Long> {
    List<DriverViolation> findByDriver_DriverId(Long driverId);

    //Đếm vi phạm theo userId và status

    @Query("""
        SELECT COUNT(dv) FROM DriverViolation dv 
        JOIN dv.driver d 
        WHERE d.user.userId = :userId 
        AND dv.status = :status
    """)
    int countByUserIdAndStatus(@Param("userId") Long userId,
                               @Param("status") ViolationStatus status);


    //Lấy danh sách vi phạm theo userId và status

    @Query("""
        SELECT dv FROM DriverViolation dv 
        JOIN FETCH dv.driver d 
        JOIN FETCH d.user u 
        WHERE u.userId = :userId 
        AND dv.status = :status
    """)
    List<DriverViolation> findByUserIdAndStatus(@Param("userId") Long userId,
                                                @Param("status") ViolationStatus status);

     //Đếm vi phạm theo driverId và status (backup method)

    int countByDriver_DriverIdAndStatus(Long driverId, ViolationStatus status);

      //Lấy vi phạm theo driverId và status
    List<DriverViolation> findByDriver_DriverIdAndStatus(Long driverId, ViolationStatus status);
}