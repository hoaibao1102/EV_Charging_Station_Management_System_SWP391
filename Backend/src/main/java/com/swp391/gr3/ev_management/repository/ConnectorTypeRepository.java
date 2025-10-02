package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.ConnectorType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConnectorTypeRepository extends JpaRepository<ConnectorType, Long> {
    public ConnectorType findcode(String code);
}
