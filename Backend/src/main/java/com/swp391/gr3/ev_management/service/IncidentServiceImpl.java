package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.CreateIncidentRequest;
import com.swp391.gr3.ev_management.DTO.response.IncidentResponse;
import com.swp391.gr3.ev_management.entity.ChargingStation;
import com.swp391.gr3.ev_management.entity.Incident;
import com.swp391.gr3.ev_management.entity.Staffs;
import com.swp391.gr3.ev_management.enums.IncidentStatus;
import com.swp391.gr3.ev_management.enums.StaffStatus;
import com.swp391.gr3.ev_management.exception.ConflictException;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.mapper.IncidentMapper;
import com.swp391.gr3.ev_management.repository.IncidentRepository;
import com.swp391.gr3.ev_management.repository.StaffsRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IncidentServiceImpl implements IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentMapper mapper;
    private final StaffsRepository staffsRepository;

    @Override
    @Transactional
    public IncidentResponse createIncident(CreateIncidentRequest request) {
        Staffs staff = staffsRepository.findById(request.getStaffId())
                .orElseThrow(() -> new ErrorException("Station staff not found"));

        // ✅ Dùng enum thay vì string
        if (staff.getStatus() != StaffStatus.ACTIVE) {
            throw new ConflictException("Staff is not active");
        }

        Incident incident = new Incident();
        incident.setStaffs(staff);
        incident.setStation((ChargingStation) staff.getStationStaffs());
        incident.setTitle(request.getTitle());
        incident.setDescription(request.getDescription());
        incident.setSeverity(request.getSeverity());
        incident.setStatus(IncidentStatus.REPORTED);                 // chuẩn hoá status
        incident.setReportedAt(LocalDateTime.now());   // nếu entity có


        incident = incidentRepository.save(incident);
        return mapper.mapToIncident(incident);
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public IncidentResponse findById(Long incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ErrorException("Incident not found"));
        return mapper.mapToIncident(incident);
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<IncidentResponse> findAll() {
        return incidentRepository.findAll()
                .stream()
                .map(mapper::mapToIncident)
                .toList();
    }

    @Override
    public void updateIncidentStatus(Long incidentId, String status) {
        Optional<Incident> incident = incidentRepository.findById(incidentId);
        if (incident.isPresent()) {
            Incident existingIncident = incident.get();
            existingIncident.setStatus(IncidentStatus.valueOf(status));
            incidentRepository.save(existingIncident);
        } else {
            throw new ErrorException("Incident not found");
        }
    }
}
