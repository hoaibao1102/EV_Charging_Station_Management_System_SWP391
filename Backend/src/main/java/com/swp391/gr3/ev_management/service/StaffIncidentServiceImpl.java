package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.CreateIncidentRequest;
import com.swp391.gr3.ev_management.DTO.response.IncidentResponse;
import com.swp391.gr3.ev_management.entity.Incident;
import com.swp391.gr3.ev_management.entity.StationStaff;
import com.swp391.gr3.ev_management.mapper.IncidentMapper;
import com.swp391.gr3.ev_management.repository.IncidentRepository;
import com.swp391.gr3.ev_management.repository.StationStaffRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffIncidentServiceImpl implements StaffIncidentService{
    private final IncidentRepository incidentRepository;
    private final StationStaffRepository staffRepository;
    private final IncidentMapper mapper;

    @Override
    @Transactional
    public IncidentResponse createIncident(CreateIncidentRequest request) {
        StationStaff staff = staffRepository.findById(request.getStationStaffId())
                .orElseThrow(() -> new RuntimeException("Station staff not found"));
        if (!"active".equalsIgnoreCase(staff.getStatus())) {
            throw new RuntimeException("Staff is not active");
        }

        Incident incident = new Incident();
        incident.setStationStaff(staff);
        incident.setStation(staff.getStation());
        incident.setTitle(request.getTitle());
        incident.setDescription(request.getDescription());
        incident.setSeverity(request.getSeverity());
        incident.setStatus("Reported");
        incident.setReportedAt(LocalDateTime.now());
        incidentRepository.save(incident);

        return mapper.mapToIncident(incident);
    }

    @Override
    public List<IncidentResponse> getIncidentsByStation(Long stationId, Long staffId) {
        StationStaff staff = staffRepository.findActiveByUserId(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found or not active"));
        if (!staff.getStation().getStationId().equals(stationId)) {
            throw new RuntimeException("No permission for this station");
        }

        return incidentRepository.findByStationId(stationId)
                .stream()
                .map(mapper::mapToIncident)
                .collect(Collectors.toList());
    }

    @Override
    public List<IncidentResponse> getUnresolvedIncidentsByStation(Long stationId, Long staffId) {
        //  Kiểm tra quyền nhân viên
        StationStaff staff = staffRepository.findActiveByUserId(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found or not active"));

        if (!staff.getStation().getStationId().equals(stationId)) {
            throw new RuntimeException("No permission for this station");
        }

        // Lấy danh sách sự cố có status khác "Resolved" hoặc "Closed"
        List<Incident> incidents = incidentRepository.findByStationId(stationId)
                .stream()
                .filter(i -> !"Resolved".equalsIgnoreCase(i.getStatus())
                        && !"Closed".equalsIgnoreCase(i.getStatus()))
                .toList();

        //  Map sang DTO trả về
        return incidents.stream()
                .map(mapper::mapToIncident)
                .toList();
    }

    @Override
    public IncidentResponse getIncidentById(Long incidentId, Long staffId) {
        //Tìm sự cố
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Incident not found"));

        //  Kiểm tra quyền của staff
        Long stationId = incident.getStationStaff().getStation().getStationId();
        StationStaff staff = staffRepository.findActiveByUserId(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found or not active"));

        if (!staff.getStation().getStationId().equals(stationId)) {
            throw new RuntimeException("No permission to view this incident");
        }

        //  Map sang DTO
        return mapper.mapToIncident(incident);
    }
}
