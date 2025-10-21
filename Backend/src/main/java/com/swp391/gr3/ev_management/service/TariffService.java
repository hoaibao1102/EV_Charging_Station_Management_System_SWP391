package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.TariffCreateRequest;
import com.swp391.gr3.ev_management.DTO.request.TariffUpdateRequest;
import com.swp391.gr3.ev_management.DTO.response.TariffResponse;

import java.util.List;

public interface TariffService {

    List<TariffResponse> getAllTariffs();

    TariffResponse getTariffById(long tariffId);

    TariffResponse createTariff(TariffCreateRequest request);

    TariffResponse updateTariff(long tariffId, TariffUpdateRequest request);

}
