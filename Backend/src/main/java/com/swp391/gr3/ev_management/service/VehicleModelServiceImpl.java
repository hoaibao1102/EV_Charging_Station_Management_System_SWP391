package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.VehicleModelCreateRequest;
import com.swp391.gr3.ev_management.DTO.request.VehicleModelUpdateRequest;
import com.swp391.gr3.ev_management.DTO.response.VehicleModelResponse;
import com.swp391.gr3.ev_management.entity.ConnectorType;
import com.swp391.gr3.ev_management.entity.VehicleModel;
import com.swp391.gr3.ev_management.exception.ConflictException;
import com.swp391.gr3.ev_management.exception.NotFoundException;
import com.swp391.gr3.ev_management.mapper.VehicleModelMapper;
import com.swp391.gr3.ev_management.repository.ConnectorTypeRepository;
import com.swp391.gr3.ev_management.repository.VehicleModelRepository;
import com.swp391.gr3.ev_management.repository.VehicleRepisitory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleModelServiceImpl implements VehicleModelService {

    private final VehicleModelRepository vehicleModelRepository;
    private final ConnectorTypeRepository connectorTypeRepository;
    private final VehicleRepisitory vehicleRepisitory;
    private final VehicleModelMapper vehicleModelMapper;

    @Override
    @Transactional
    public VehicleModelResponse create(VehicleModelCreateRequest request) {
        // uniqueness check: brand+model+year
        if (vehicleModelRepository.existsByBrandIgnoreCaseAndModelIgnoreCaseAndYear(
                request.getBrand(), request.getModel(), request.getYear())) {
            throw new ConflictException("Vehicle model already exists for brand/model/year");
        }
        ConnectorType connectorType = connectorTypeRepository.findById(request.getConnectorTypeId())
                .orElseThrow(() -> new NotFoundException("ConnectorType not found with id " + request.getConnectorTypeId()));

        VehicleModel entity = VehicleModel.builder()
                .brand(request.getBrand())
                .model(request.getModel())
                .year(request.getYear())
                .imageUrl(request.getImageUrl())
                .connectorType(connectorType)
                .build();

        VehicleModel saved = vehicleModelRepository.save(entity);
        return vehicleModelMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleModelResponse getById(Long id) {
    VehicleModel vm = vehicleModelRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("VehicleModel not found with id " + id));
    return vehicleModelMapper.toResponse(vm);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleModelResponse> getAll() {
        return vehicleModelRepository.findAll().stream().map(vehicleModelMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleModelResponse> search(String brand, String model, Integer year, Integer connectorTypeId) {
    return vehicleModelRepository.search(brand, model, year, connectorTypeId)
        .stream().map(vehicleModelMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public VehicleModelResponse update(Long id, VehicleModelUpdateRequest request) {
        VehicleModel vm = vehicleModelRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("VehicleModel not found with id " + id));

        String newBrand = request.getBrand() != null ? request.getBrand() : vm.getBrand();
        String newModel = request.getModel() != null ? request.getModel() : vm.getModel();
        int newYear = request.getYear() != null ? request.getYear() : vm.getYear();

        if (vehicleModelRepository.existsByBrandIgnoreCaseAndModelIgnoreCaseAndYearAndModelIdNot(
                newBrand, newModel, newYear, id)) {
            throw new ConflictException("Another VehicleModel with same brand/model/year already exists");
        }

        if (request.getBrand() != null) vm.setBrand(request.getBrand());
        if (request.getModel() != null) vm.setModel(request.getModel());
        if (request.getYear() != null) vm.setYear(request.getYear());
        if (request.getImageUrl() != null) vm.setImageUrl(request.getImageUrl());

        if (request.getConnectorTypeId() != null) {
            ConnectorType ct = connectorTypeRepository.findById(request.getConnectorTypeId())
                    .orElseThrow(() -> new NotFoundException("ConnectorType not found with id " + request.getConnectorTypeId()));
            vm.setConnectorType(ct);
        }

        VehicleModel saved = vehicleModelRepository.save(vm);
        return vehicleModelMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        VehicleModel vm = vehicleModelRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("VehicleModel not found with id " + id));

        long usage = vehicleRepisitory.countByModel_ModelId(id);
        if (usage > 0) {
            throw new ConflictException("Cannot delete VehicleModel in use by " + usage + " vehicle(s)");
        }
        vehicleModelRepository.delete(vm);
    }

    
}

