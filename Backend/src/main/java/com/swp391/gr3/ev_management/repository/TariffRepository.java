package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.ConnectorType;
import com.swp391.gr3.ev_management.entity.Tariff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TariffRepository extends JpaRepository<Tariff,Long> {
    Optional<Tariff> findByConnectorType(ConnectorType connectorType);

    // Lấy tariff active tại một thời điểm cho 1 connector type (ưu tiên bản có EffectiveFrom mới nhất)
    Optional<Tariff> findTopByConnectorType_ConnectorTypeIdAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqualOrderByEffectiveFromDesc(
            Long connectorTypeId, LocalDateTime from, LocalDateTime to
    );
}
