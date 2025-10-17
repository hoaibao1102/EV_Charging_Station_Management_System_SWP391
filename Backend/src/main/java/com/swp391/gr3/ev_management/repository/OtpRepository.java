package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<OtpVerification, Long> {
    Optional<OtpVerification> findTopByEmailOrderByCreatedAtDesc(String email);
}
