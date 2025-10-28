package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.VehicleModelCreateRequest;
import com.swp391.gr3.ev_management.DTO.request.VehicleModelUpdateRequest;
import com.swp391.gr3.ev_management.DTO.response.VehicleModelResponse;

import java.util.List;

public interface VehicleModelService {
    VehicleModelResponse create(VehicleModelCreateRequest request);
    VehicleModelResponse getById(Long id);
    List<VehicleModelResponse> getAll();
    List<VehicleModelResponse> search(String brand, String model, Integer year, Integer connectorTypeId);
    VehicleModelResponse update(Long id, VehicleModelUpdateRequest request);
    void delete(Long id); //hard delete
}

