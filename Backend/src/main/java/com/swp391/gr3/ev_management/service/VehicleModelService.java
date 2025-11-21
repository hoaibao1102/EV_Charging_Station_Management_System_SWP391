package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.VehicleModelCreateRequest;
import com.swp391.gr3.ev_management.dto.request.VehicleModelUpdateRequest;
import com.swp391.gr3.ev_management.dto.response.VehicleModelResponse;
import com.swp391.gr3.ev_management.entity.VehicleModel;
import com.swp391.gr3.ev_management.enums.VehicleModelStatus;

import java.util.List;
import java.util.Optional;

public interface VehicleModelService {

    VehicleModelResponse create(VehicleModelCreateRequest request);

    VehicleModelResponse getById(Long id);

    List<VehicleModelResponse> getAll();

    List<VehicleModelResponse> search(String brand, String model, Integer year, Integer connectorTypeId);

    VehicleModelResponse update(Long id, VehicleModelUpdateRequest request);

    void delete(Long id); //hard delete

    VehicleModelResponse updateStatus(Long id, VehicleModelStatus status);

    Optional<VehicleModel> findById(Long modelId);
}

