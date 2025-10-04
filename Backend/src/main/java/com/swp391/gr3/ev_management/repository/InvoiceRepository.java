package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByDriver_DriverId(Long driverId);
}