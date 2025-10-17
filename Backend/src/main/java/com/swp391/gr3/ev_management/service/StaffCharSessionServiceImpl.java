package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.StartCharSessionRequest;
import com.swp391.gr3.ev_management.DTO.request.StopCharSessionRequest;
import com.swp391.gr3.ev_management.DTO.response.StartCharSessionResponse;
import com.swp391.gr3.ev_management.DTO.response.StopCharSessionResponse;
import com.swp391.gr3.ev_management.DTO.response.ViewCharSessionResponse;
import com.swp391.gr3.ev_management.enums.BookingStatus;
import com.swp391.gr3.ev_management.entity.*;
import com.swp391.gr3.ev_management.enums.ChargingSessionStatus;
import com.swp391.gr3.ev_management.enums.InvoiceStatus;
import com.swp391.gr3.ev_management.enums.NotificationTypes;
import com.swp391.gr3.ev_management.mapper.ChargingSessionMapper;
import com.swp391.gr3.ev_management.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    private final NotificationsRepository notificationsRepository;


    @Override
    @Transactional
    public StartCharSessionResponse startChargingSession(StartCharSessionRequest request) {

        Booking booking = bookingsRepository.findByBookingIdAndStatus(request.getBookingId(), BookingStatus.CONFIRMED)
                .orElseThrow(() -> new RuntimeException("Booking not found or not confirmed"));// check booking

        sessionRepository.findByBooking_BookingId(booking.getBookingId())
                .ifPresent(s -> { throw new IllegalStateException("Session already exists for this booking"); });

        // Tạo mới phiên sạc
        ChargingSession session = new ChargingSession();
        session.setBooking(booking);
        session.setStartTime(LocalDateTime.now());
        session.setStatus(ChargingSessionStatus.IN_PROGRESS);
        sessionRepository.save(session);

        booking.setStatus(BookingStatus.BOOKED);
        bookingsRepository.save(booking);

        // Tìm noti gần nhất để update (hoặc tạo mới nếu không thấy)
        Notification noti = notificationsRepository
                .findTopByBookingAndTypeOrderByCreatedAtDesc(booking, NotificationTypes.BOOKING_CONFIRMED)
                .orElseGet(() -> {
                    Notification n = new Notification();
                    n.setUser(booking.getVehicle().getDriver().getUser());
                    n.setBooking(booking);
                    n.setStatus("UNREAD");
                    n.setTitle("Bắt đầu sạc #" + booking.getBookingId());
                    return n;
                });
        noti.setSession(session);
        notificationsRepository.save(noti);

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

        ChargingSession chargingSession = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (!"in_progress".equalsIgnoreCase(String.valueOf(chargingSession.getStatus()))) {
            throw new RuntimeException("Session is not currently active");
        }

        LocalDateTime endTime = LocalDateTime.now();
        chargingSession.setEndTime(endTime);
        chargingSession.setEnergyKWh(request.getFinalEnergyKWh());

        long minutes = ChronoUnit.MINUTES.between(chargingSession.getStartTime(), endTime);
        chargingSession.setDurationMinutes((int) minutes);

        double pricePerKWh = 3500.0;
        double cost = pricePerKWh * request.getFinalEnergyKWh();
        chargingSession.setCost(cost);
        chargingSession.setStatus(ChargingSessionStatus.COMPLETED);
        sessionRepository.save(chargingSession);

        Booking booking = chargingSession.getBooking();
        booking.setStatus(BookingStatus.COMPLETED);
        bookingsRepository.save(booking);

        Optional<Invoice> existingInvoice = invoiceRepository.findBySession_SessionId(chargingSession.getSessionId());
        if (existingInvoice.isPresent()) {
            throw new RuntimeException("Invoice already exists for this session");
        }

        Invoice invoice = new Invoice();
        invoice.setSession(chargingSession);
        invoice.setAmount(cost);
        invoice.setCurrency("VND");
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setIssuedAt(LocalDateTime.now());

        invoice.setDriver(chargingSession.getBooking().getVehicle().getDriver()); // hoặc booking.getDriver()
        invoiceRepository.save(invoice);

        return StopCharSessionResponse.builder()
                .sessionId(chargingSession.getSessionId())
                .stationName(booking.getStation().getStationName())
                .vehiclePlate(booking.getVehicle().getVehiclePlate())
                .startTime(chargingSession.getStartTime())
                .endTime(chargingSession.getEndTime())
                .energyKWh(chargingSession.getEnergyKWh())
                .cost(chargingSession.getCost())
                .status(chargingSession.getStatus())
                .build();
    }

    @Override
    public ViewCharSessionResponse getCharSessionById(Long sessionId) {

        //  Tìm session theo ID
        ChargingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Charging session not found"));
        //  Kiểm tra quyền trạm
        Long sessionStationId = session.getBooking().getStation().getStationId();

        //  Mapping sang DTO trả về
        return mapper.toResponse(session);
    }

    @Override
    public List<ViewCharSessionResponse> getCharSessionsByStation(Long stationId) {

        //  Lấy toàn bộ session của trạm
        List<ChargingSession> sessions = sessionRepository.findByBooking_Station_StationId(stationId);

        //  Map sang DTO
        return sessions.stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public List<ViewCharSessionResponse> getActiveCharSessionsByStation(Long stationId) {

        //  Lấy các session đang hoạt động
        List<ChargingSession> activeSessions = sessionRepository.findActiveSessionsByStation(stationId);

        //  Map sang DTO
        return activeSessions.stream()
                .map(mapper::toResponse)
                .toList();
    }

    public List<ChargingSession> getAll() {
        return sessionRepository.findAll();
    }
}
