package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.DriverWallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface DriverWalletRepository extends JpaRepository <DriverWallet, Long>{
    Optional<DriverWallet> findByDriverId(Long driverId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM DriverWallet w WHERE w.driver.driverId = :driverId")
    Optional<DriverWallet> findByDriverIdWithLock(Long driverId);
}
