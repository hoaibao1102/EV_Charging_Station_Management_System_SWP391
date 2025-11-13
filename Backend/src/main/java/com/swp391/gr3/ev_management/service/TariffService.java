package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.TariffCreateRequest;
import com.swp391.gr3.ev_management.dto.request.TariffUpdateRequest;
import com.swp391.gr3.ev_management.dto.response.TariffResponse;
import com.swp391.gr3.ev_management.entity.ConnectorType;
import com.swp391.gr3.ev_management.entity.Tariff;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TariffService {

    List<TariffResponse> getAllTariffs();

    TariffResponse getTariffById(long tariffId);

    TariffResponse createTariff(TariffCreateRequest request);

    TariffResponse updateTariff(long tariffId, TariffUpdateRequest request);

    Optional<Tariff> findByConnectorType(ConnectorType connectorType);

    Optional<Tariff> findTopByConnectorType_ConnectorTypeIdAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqualOrderByEffectiveFromDesc(Long connectorTypeId, LocalDateTime pricingTime, LocalDateTime pricingTime1);

    Collection<Tariff> findActiveByConnectorType(Long connectorTypeId, LocalDateTime pricingTime);
}
