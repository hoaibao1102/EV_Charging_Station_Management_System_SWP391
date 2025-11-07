package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.CreateReportRequest;
import com.swp391.gr3.ev_management.dto.response.ReportResponse;
import com.swp391.gr3.ev_management.entity.ChargingStation;
import com.swp391.gr3.ev_management.entity.Report;
import com.swp391.gr3.ev_management.entity.Staffs;
import com.swp391.gr3.ev_management.entity.StationStaff;
import com.swp391.gr3.ev_management.enums.ReportStatus;
import com.swp391.gr3.ev_management.enums.StaffStatus;
import com.swp391.gr3.ev_management.exception.ConflictException;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.mapper.ReportMapper;
import com.swp391.gr3.ev_management.repository.ReportRepository;
import com.swp391.gr3.ev_management.repository.StaffsRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final ReportMapper mapper;
    private final StaffsRepository staffsRepository;

    @Override
    @Transactional
    public ReportResponse createIncident(Long userId, CreateReportRequest request) {
        Staffs staff = staffsRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new ErrorException("Staff not found for userId: " + userId));

        if (staff.getStatus() != StaffStatus.ACTIVE) {
            throw new ConflictException("Staff is not active");
        }

        // ✅ Lấy station tự động — nếu staff chỉ có 1 station active
        ChargingStation station = staff.getStationStaffs().stream()
                .filter(ss -> ss.getUnassignedAt() == null)
                .map(StationStaff::getStation)
                .findFirst()
                .orElseThrow(() -> new ConflictException("Staff is not assigned to any active station"));

        Report report = new Report();
        report.setStaffs(staff);
        report.setStation(station);
        report.setTitle(request.getTitle());
        report.setDescription(request.getDescription());
        report.setSeverity(request.getSeverity());
        report.setStatus(ReportStatus.REPORTED);
        report.setReportedAt(LocalDateTime.now());

        Report saved = reportRepository.save(report);
        return mapper.mapToReport(saved);
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public ReportResponse findById(Long incidentId) {
        Report report = reportRepository.findById(incidentId)
                .orElseThrow(() -> new ErrorException("Incident not found"));
        return mapper.mapToReport(report);
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<ReportResponse> findAll() {
        return reportRepository.findAll()
                .stream()
                .map(mapper::mapToReport)
                .toList();
    }

    @Override
    public void updateIncidentStatus(Long incidentId, String status) {
        Optional<Report> incident = reportRepository.findById(incidentId);
        if (incident.isPresent()) {
            Report existingReport = incident.get();
            existingReport.setStatus(ReportStatus.valueOf(status));
            reportRepository.save(existingReport);
        } else {
            throw new ErrorException("Incident not found");
        }
    }
}
