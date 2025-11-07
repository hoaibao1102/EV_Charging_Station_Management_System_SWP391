package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.TariffCreateRequest;
import com.swp391.gr3.ev_management.dto.request.TariffUpdateRequest;
import com.swp391.gr3.ev_management.dto.response.TariffResponse;

import java.util.List;

public interface TariffService {

    List<TariffResponse> getAllTariffs();

    TariffResponse getTariffById(long tariffId);

    TariffResponse createTariff(TariffCreateRequest request);

    TariffResponse updateTariff(long tariffId, TariffUpdateRequest request);

}
