package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.StartCharSessionRequest;
import com.swp391.gr3.ev_management.DTO.request.StopCharSessionRequest;
import com.swp391.gr3.ev_management.DTO.response.StartCharSessionResponse;
import com.swp391.gr3.ev_management.DTO.response.StopCharSessionResponse;
import com.swp391.gr3.ev_management.DTO.response.ViewCharSessionResponse;
import com.swp391.gr3.ev_management.entity.*;
import com.swp391.gr3.ev_management.mapper.ChargingSessionMapper;
import com.swp391.gr3.ev_management.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StaffCharSessionServiceImpl implements StaffCharSessionService {

    private final ChargingSessionRepository sessionRepository;
    private final ChargingPointRepository pointRepository;
    private final BookingsRepository bookingsRepository;
    private final InvoiceRepository invoiceRepository;
    private final StationStaffRepository staffRepository;
    private final ChargingSessionMapper mapper;


    @Override
    @Transactional
    public StartCharSessionResponse startChargingSession(StartCharSessionRequest request) {
        StationStaff staff = staffRepository.findActiveByUserId(request.getStaffId())
                .orElseThrow(() -> new RuntimeException("Staff not found or not active")); // xác thực nhân viên

        Booking booking = bookingsRepository.findByBookingIdAndStatus(request.getBookingId(), "confirmed")
                .orElseThrow(() -> new RuntimeException("Booking not found or not confirmed"));// check booking

        if (!staff.getStation().getStationId().equals(booking.getStation().getStationId())) {
            throw new RuntimeException("Staff has no permission for this station"); // check quyền mỗi trạm
        }

        ChargingPoint point = pointRepository.findById(request.getPointId())
                .orElseThrow(() -> new RuntimeException("Charging point not found"));// check điểm sạc

        if (!"available".equalsIgnoreCase(point.getStatus())) {
            throw new RuntimeException("Charging point not available");
        } // điểm sạc có đang available hay không



        // tạo mới phiên sạc
        ChargingSession session = new ChargingSession();
        session.setBooking(booking);
        session.setStartTime(LocalDateTime.now());
        session.setStatus("in_progress");
        sessionRepository.save(session);

        point.setStatus("in_use");
        pointRepository.save(point);

        booking.setStatus("in_progress");
        bookingsRepository.save(booking);

        return StartCharSessionResponse.builder()
                .sessionId(session.getSessionId())
                .bookingId(booking.getBookingId())
                .stationName(booking.getStation().getStationName())
                .vehiclePlate(booking.getVehicle().getVehiclePlate())
                .startTime(session.getStartTime())
                .status(session.getStatus())
                .build();
    }

    @Override
    @Transactional
    public StopCharSessionResponse stopChargingSession(StopCharSessionRequest request) {
        StationStaff staff = staffRepository.findActiveByUserId(request.getStaffId())
                .orElseThrow(() -> new RuntimeException("Staff not found or not active"));

        ChargingSession session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (!"in_progress".equalsIgnoreCase(session.getStatus())) {
            throw new RuntimeException("Session is not currently active");
        }

        if (!staff.getStation().getStationId()
                .equals(session.getBooking().getStation().getStationId())) {
            throw new RuntimeException("No permission to manage this session");
        }

        LocalDateTime endTime = LocalDateTime.now();
        session.setEndTime(endTime);
        session.setEnergyKWh(request.getFinalEnergyKWh());

        long minutes = ChronoUnit.MINUTES.between(session.getStartTime(), endTime);
        session.setDurationMinutes((int) minutes);

        double pricePerKWh = 3500.0;
        double cost = pricePerKWh * request.getFinalEnergyKWh();
        session.setCost(cost);
        session.setStatus("completed");
        sessionRepository.save(session);

        Booking booking = session.getBooking();
        booking.setStatus("completed");
        bookingsRepository.save(booking);


        ChargingPoint point = pointRepository.findById(request.getPointId())
                .orElseThrow(() -> new RuntimeException("Point not found"));
        point.setStatus("available");
        pointRepository.save(point);

        Optional<Invoice> existingInvoice = invoiceRepository.findBySession_SessionId(session.getSessionId());
        if (existingInvoice.isPresent()) {
            throw new RuntimeException("Invoice already exists for this session");
        }

        Invoice invoice = new Invoice();
        invoice.setSession(session);
        invoice.setAmount(cost);
        invoice.setCurrency("VND");
        invoice.setStatus("unpaid");
        invoice.setIssuedAt(LocalDateTime.now());
        invoiceRepository.save(invoice);

        return StopCharSessionResponse.builder()
                .sessionId(session.getSessionId())
                .stationName(booking.getStation().getStationName())
                .vehiclePlate(booking.getVehicle().getVehiclePlate())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .energyKWh(session.getEnergyKWh())
                .cost(session.getCost())
                .status(session.getStatus())
                .build();
    }

    @Override
    public ViewCharSessionResponse getCharSessionById(Long sessionId, Long staffId) {
        //  Kiểm tra nhân viên
        StationStaff staff = staffRepository.findActiveByUserId(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found or not active"));

        //  Tìm session theo ID
        ChargingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Charging session not found"));

        //  Kiểm tra quyền trạm
        Long sessionStationId = session.getBooking().getStation().getStationId();
        if (!staff.getStation().getStationId().equals(sessionStationId)) {
            throw new RuntimeException("Staff has no permission to view this session");
        }

        //  Mapping sang DTO trả về
        return mapper.toResponse(session);
    }

    @Override
    public List<ViewCharSessionResponse> getCharSessionsByStation(Long stationId, Long staffId) {
        //  Kiểm tra quyền
        StationStaff staff = staffRepository.findActiveByUserId(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found or not active"));
        if (!staff.getStation().getStationId().equals(stationId)) {
            throw new RuntimeException("Staff has no permission for this station");
        }

        //  Lấy toàn bộ session của trạm
        List<ChargingSession> sessions = sessionRepository.findByBooking_Station_StationId(stationId);

        //  Map sang DTO
        return sessions.stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public List<ViewCharSessionResponse> getActiveCharSessionsByStation(Long stationId, Long staffId) {
        //  Kiểm tra quyền
        StationStaff staff = staffRepository.findActiveByUserId(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found or not active"));
        if (!staff.getStation().getStationId().equals(stationId)) {
            throw new RuntimeException("Staff has no permission for this station");
        }

        //  Lấy các session đang hoạt động
        List<ChargingSession> activeSessions = sessionRepository.findActiveSessionsByStation(stationId);

        //  Map sang DTO
        return activeSessions.stream()
                .map(mapper::toResponse)
                .toList();
    }

//    public List<ChargingSession> getCompletedSessionsToday(Long stationId) {
//        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
//        LocalDateTime endOfDay = startOfDay.plusDays(1);
//
//        return sessionRepository.findCompletedSessionsTodayByStation(
//                stationId,
//                startOfDay,
//                endOfDay
//        );
//    }
}
