package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.ConnectorType;
import com.swp391.gr3.ev_management.entity.Tariff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TariffRepository extends JpaRepository<Tariff,Long> {
    Optional<Tariff> findByConnectorType(ConnectorType connectorType);
}
