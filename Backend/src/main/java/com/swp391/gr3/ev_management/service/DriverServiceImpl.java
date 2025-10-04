package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.DriverRequest;
import com.swp391.gr3.ev_management.DTO.response.DriverResponse;

import java.util.List;

public interface DriverServiceImpl {
    DriverResponse create(DriverRequest request);
    DriverResponse getById(Long driverId);
    List<DriverResponse> getAll();
    DriverResponse updateStatus(Long driverId, String status);
    void delete(Long driverId);
}
