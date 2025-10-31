package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.DriverViolationTriplet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DriverViolationTripletRepository extends JpaRepository<DriverViolationTriplet, Long> {

    @Query("""
        select t from DriverViolationTriplet t
        where t.driver.driverId = :driverId and t.status = 'OPEN'
        order by t.createdAt desc
    """)
    List<DriverViolationTriplet> findOpenByDriver(@Param("driverId") Long driverId);

    @Query("""
        select case when count(t) > 0 then true else false end
        from DriverViolationTriplet t
        where t.v1.violationId = :violationId
           or t.v2.violationId = :violationId
           or t.v3.violationId = :violationId
    """)
    boolean existsByViolation(@Param("violationId") Long violationId);
}
