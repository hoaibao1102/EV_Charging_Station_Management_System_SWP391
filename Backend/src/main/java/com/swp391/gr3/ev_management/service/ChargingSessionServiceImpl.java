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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class ChargingSessionServiceImpl implements ChargingSessionService {

    private final ChargingSessionRepository sessionRepository;
    private final ChargingPointRepository pointRepository;
    private final BookingsRepository bookingsRepository;
    private final InvoiceRepository invoiceRepository;
    private final StaffsRepository staffRepository;
    private final ChargingSessionMapper mapper;
    private final NotificationsRepository notificationsRepository;
    private final SessionSocCache sessionSocCache;

    @Override
    @Transactional
    public StartCharSessionResponse startChargingSession(StartCharSessionRequest request) {
        // 1) Validate staff & quyền tại trạm
        Staffs staff = staffRepository.findById(request.getStaffId())
                .orElseThrow(() -> new RuntimeException("Staff not found or not active"));

        // 2) Lấy booking ở trạng thái CONFIRMED
        Booking booking = bookingsRepository
                .findByBookingIdAndStatus(request.getBookingId(), BookingStatus.CONFIRMED)
                .orElseThrow(() -> new RuntimeException("Booking not found or not confirmed"));

        // 3) Staff phải thuộc cùng station với booking
//        if (!staff.getStationStaffs().equals(booking.getStation())) {
//            throw new RuntimeException("Staff has no permission for this station");
//        }

        // 4) Chưa có session nào gắn với booking này
        sessionRepository.findByBooking_BookingId(booking.getBookingId())
                .ifPresent(s -> { throw new IllegalStateException("Session already exists for this booking"); });

        // 5) Sinh mức pin ban đầu ngẫu nhiên (20–80%)
        int initialSoc = ThreadLocalRandom.current().nextInt(20, 81);

        // 6) Tạo session
        ChargingSession session = new ChargingSession();
        session.setBooking(booking);
        session.setStartTime(LocalDateTime.now());
        session.setStatus(ChargingSessionStatus.IN_PROGRESS);
        sessionRepository.save(session);

        // 6.1) Lưu SoC vào cache theo sessionId
        sessionSocCache.put(session.getSessionId(), initialSoc);

        // 7) Cập nhật trạng thái booking (nếu có IN_PROGRESS thì nên dùng)
        booking.setStatus(BookingStatus.BOOKED);
        bookingsRepository.save(booking);

        // 8) Tạo noti CHARGING_STARTED
        Notification noti = new Notification();
        noti.setUser(booking.getVehicle().getDriver().getUser());
        noti.setBooking(booking);
        noti.setSession(session);
        noti.setTitle("Bắt đầu sạc #" + booking.getBookingId());
        noti.setContentNoti("Pin hiện tại: " + initialSoc + "%");
        noti.setType(NotificationTypes.CHARGING_STARTED);
        noti.setStatus("UNREAD");
        noti.setCreatedAt(LocalDateTime.now());
        notificationsRepository.save(noti);

        // 9) Trả về response (kèm initialSoc)
        return StartCharSessionResponse.builder()
                .sessionId(session.getSessionId())
                .bookingId(booking.getBookingId())
                .stationName(booking.getStation().getStationName())
                .vehiclePlate(booking.getVehicle().getVehiclePlate())
                .startTime(session.getStartTime())
                .status(session.getStatus())
                .initialSoc(initialSoc)
                .build();
    }

    @Override
    @Transactional
    public StopCharSessionResponse stopChargingSession(StopCharSessionRequest request) {

        ChargingSession chargingSession = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new RuntimeException("Session not found"));

        // 🔧 so sánh enum thay vì string
        if (chargingSession.getStatus() != ChargingSessionStatus.IN_PROGRESS) {
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

        // 🧹 Xoá SoC cached vì session đã kết thúc
        sessionSocCache.remove(chargingSession.getSessionId());

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
        invoice.setDriver(chargingSession.getBooking().getVehicle().getDriver());
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
        ChargingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Charging session not found"));
        return mapper.toResponse(session);
    }

    @Override
    public List<ViewCharSessionResponse> getCharSessionsByStation(Long stationId) {
        List<ChargingSession> sessions = sessionRepository.findByBooking_Station_StationId(stationId);
        return sessions.stream().map(mapper::toResponse).toList();
    }

    @Override
    public List<ViewCharSessionResponse> getActiveCharSessionsByStation(Long stationId) {
        List<ChargingSession> activeSessions = sessionRepository.findActiveSessionsByStation(stationId);
        return activeSessions.stream().map(mapper::toResponse).toList();
    }

    public List<ChargingSession> getAll() {
        return sessionRepository.findAll();
    }

    @Override
    public Optional<ChargingSession> findById(Long sessionId) {
        return sessionRepository.findById(sessionId);
    }
}
