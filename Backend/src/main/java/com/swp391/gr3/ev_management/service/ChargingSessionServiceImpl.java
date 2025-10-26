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
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
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
    private final TariffRepository tariffRepository;
    private final TaskScheduler taskScheduler;

    @Override
    @Transactional
    public StartCharSessionResponse startChargingSession(StartCharSessionRequest request) {
        Booking booking = bookingsRepository
                .findByBookingIdAndStatus(request.getBookingId(), BookingStatus.CONFIRMED)
                .orElseThrow(() -> new RuntimeException("Booking not found or not confirmed"));

        // (Tuỳ bạn mở lại kiểm tra staff thuộc station)

        sessionRepository.findByBooking_BookingId(booking.getBookingId())
                .ifPresent(s -> { throw new IllegalStateException("Session already exists for this booking"); });

        // === RÀNG BUỘC THỜI GIAN ===
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = getBookingStart(booking);
        LocalDateTime windowEnd   = getBookingEnd(booking);

        if (now.isBefore(windowStart)) {
            throw new IllegalStateException("Chưa đến giờ đặt. Chỉ được bắt đầu từ: " + windowStart);
        }
        if (now.isAfter(windowEnd)) {
            throw new IllegalStateException("Đã quá giờ đặt (đến: " + windowEnd + "). Không thể bắt đầu.");
        }

        int initialSoc = ThreadLocalRandom.current().nextInt(20, 81);

        ChargingSession session = new ChargingSession();
        session.setBooking(booking);
        session.setStartTime(now);
        session.setStatus(ChargingSessionStatus.IN_PROGRESS);
        session.setInitialSoc(initialSoc);
        sessionRepository.save(session);

        sessionSocCache.put(session.getSessionId(), initialSoc);

        // Trạng thái booking nên là IN_PROGRESS (thay vì BOOKED)
        booking.setStatus(BookingStatus.BOOKED);
        bookingsRepository.save(booking);

        // === HẸN GIỜ AUTO-STOP LÚC HẾT GIỜ ===
        Date triggerAt = Date.from(windowEnd.atZone(ZoneId.systemDefault()).toInstant());
        taskScheduler.schedule(() -> autoStopIfStillRunning(session.getSessionId(), windowEnd), triggerAt);

        BookingSlot firstSlot = booking.getBookingSlots().stream()
                .findFirst()
                .orElse(null);
        String pointNumber = (firstSlot != null && firstSlot.getSlot() != null
                && firstSlot.getSlot().getChargingPoint() != null)
                ? firstSlot.getSlot().getChargingPoint().getPointNumber()
                : "Unknown";

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

    @Transactional
    protected StopCharSessionResponse stopSessionInternal(ChargingSession session,
                                                          int finalSoc,
                                                          LocalDateTime endTime) {
        if (session.getStatus() != ChargingSessionStatus.IN_PROGRESS) {
            throw new RuntimeException("Session is not currently active");
        }

        Integer initialSoc = Optional.ofNullable(session.getInitialSoc())
                .orElseThrow(() -> new IllegalStateException("Initial SoC not recorded"));
        if (finalSoc < 0 || finalSoc > 100) {
            throw new IllegalArgumentException("Final SoC must be between 0 and 100");
        }
        if (finalSoc < initialSoc) {
            throw new IllegalStateException("Final SoC is lower than initial SoC");
        }

        Booking booking = session.getBooking();
        double batteryCapacityKWh = booking.getVehicle().getModel().getBatteryCapacityKWh();

        double deltaSoc = finalSoc - initialSoc;
        double energyKWh = (deltaSoc / 100.0) * batteryCapacityKWh;

        long minutes = ChronoUnit.MINUTES.between(session.getStartTime(), endTime);

        // 🔌 Lấy ConnectorType và PointNumber
        BookingSlot firstSlot = booking.getBookingSlots().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No slot found for booking"));

        ChargingPoint point = firstSlot.getSlot().getChargingPoint();
        String pointNumber = point != null ? point.getPointNumber() : "Unknown";

        ConnectorType connectorType = booking.getBookingSlots().stream()
                .findFirst()
                .map(bs -> bs.getSlot().getChargingPoint().getConnectorType())
                .orElseGet(() -> booking.getVehicle().getModel().getConnectorType());

        LocalDateTime pricingTime = endTime;
        Tariff tariff = tariffRepository
                .findTopByConnectorType_ConnectorTypeIdAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqualOrderByEffectiveFromDesc(
                        connectorType.getConnectorTypeId(), pricingTime, pricingTime)
                .orElseThrow(() -> new RuntimeException("No active tariff for connector type"));

        double pricePerKWh = tariff.getPricePerKWh();
        double cost = round2(pricePerKWh * energyKWh);

        session.setEndTime(endTime);
        session.setDurationMinutes((int) minutes);
        session.setFinalSoc(finalSoc);
        session.setEnergyKWh(energyKWh);
        session.setCost(cost);
        session.setStatus(ChargingSessionStatus.COMPLETED);
        sessionRepository.save(session);

        sessionSocCache.remove(session.getSessionId());

        booking.setStatus(BookingStatus.COMPLETED);
        bookingsRepository.save(booking);

        Notification done = new Notification();
        done.setUser(booking.getVehicle().getDriver().getUser());
        done.setBooking(booking);
        done.setSession(session);
        done.setTitle("Kết thúc sạc #" + booking.getBookingId());
        done.setContentNoti(
                "Điểm sạc: " + pointNumber +
                        " | Thời lượng: " + minutes + " phút" +
                        " | Tăng SOC: " + initialSoc + "% → " + finalSoc + "%" +
                        " | Năng lượng: " + round2(energyKWh) + " kWh" +
                        " | Chi phí: " + cost + " " + tariff.getCurrency()
        );
        done.setType(NotificationTypes.CHARGING_COMPLETED); // hoặc CHARGING_ENDED tùy enum của bạn
        done.setStatus(Notification.STATUS_UNREAD);
        done.setCreatedAt(LocalDateTime.now());
        notificationsRepository.save(done);

        invoiceRepository.findBySession_SessionId(session.getSessionId())
                .ifPresent(i -> { throw new RuntimeException("Invoice already exists for this session"); });

        Invoice invoice = new Invoice();
        invoice.setSession(session);
        invoice.setAmount(cost);
        invoice.setCurrency(tariff.getCurrency());
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setIssuedAt(LocalDateTime.now());
        invoice.setDriver(booking.getVehicle().getDriver());
        invoiceRepository.save(invoice);

        return StopCharSessionResponse.builder()
                .sessionId(session.getSessionId())
                .stationName(booking.getStation().getStationName())
                .pointNumber(pointNumber)
                .vehiclePlate(booking.getVehicle().getVehiclePlate())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .durationMinutes(session.getDurationMinutes())
                .energyKWh(session.getEnergyKWh())
                .cost(session.getCost())
                .status(session.getStatus())
                .initialSoc(session.getInitialSoc())
                .finalSoc(session.getFinalSoc())
                .pricePerKWh(tariff.getPricePerKWh())
                .currency(tariff.getCurrency())
                .build();
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    @Override
    @Transactional
    public StopCharSessionResponse stopChargingSession(StopCharSessionRequest request) {
        ChargingSession session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new RuntimeException("Session not found"));

        LocalDateTime endTime = LocalDateTime.now();

        Integer reqSoc = request.getFinalSoc();
        int finalSoc = (reqSoc != null) ? clampSoc(reqSoc) : computeFinalSoc(session, endTime);

        return stopSessionInternal(session, finalSoc, endTime);
    }

    private int clampSoc(Integer soc) {
        if (soc == null) return 0;
        return Math.max(0, Math.min(100, soc));
    }

    /** Ưu tiên lấy từ cache; nếu không có thì ước lượng theo thời gian sạc x công suất x hiệu suất */
    private int computeFinalSoc(ChargingSession session, LocalDateTime endTime) {
        // 1) lấy SoC realtime nếu có
        Integer cached = sessionSocCache.get(session.getSessionId()).orElse(null);
        if (cached != null) return clampSoc(cached);

        // 2) ước lượng
        int initial = Optional.ofNullable(session.getInitialSoc()).orElse(20);
        double capKWh = session.getBooking().getVehicle().getModel().getBatteryCapacityKWh();
        double estEnergy = estimateEnergyKWh(session.getStartTime(), endTime, session.getBooking());

        int estFinal = (int) Math.round(initial + (estEnergy / capKWh) * 100.0);
        if (estFinal < initial) estFinal = initial;        // không cho thấp hơn initial
        return clampSoc(estFinal);
    }

    private double estimateEnergyKWh(LocalDateTime start, LocalDateTime end, Booking booking) {
        double minutes = Math.max(0, ChronoUnit.MINUTES.between(start, end));
        double hours = minutes / 60.0;

        double ratedKW = booking.getBookingSlots().stream()
                .findFirst()
                .map(bs -> {
                    Double p = bs.getSlot().getChargingPoint().getMaxPowerKW();
                    return (p != null && p > 0) ? p : 11.0; // mặc định 11kW
                })
                .orElse(11.0);

        double efficiency = 0.90;
        return round2(hours * ratedKW * efficiency);
    }

    @Transactional
    protected void autoStopIfStillRunning(Long sessionId, LocalDateTime windowEnd) {
        Optional<ChargingSession> opt = sessionRepository.findById(sessionId);
        if (opt.isEmpty()) return;
        ChargingSession session = opt.get();
        if (session.getStatus() != ChargingSessionStatus.IN_PROGRESS) return;

        Integer latestSoc = sessionSocCache.get(sessionId).orElse(null);
        int finalSoc = (latestSoc != null) ? clampSoc(latestSoc) : computeFinalSoc(session, windowEnd);

        stopSessionInternal(session, finalSoc, windowEnd);
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

    private LocalDateTime getBookingStart(Booking booking) {
        return booking.getBookingSlots().stream()
                .map(bs -> bs.getSlot().getDate().with(bs.getSlot().getTemplate().getStartTime()))
                .min(LocalDateTime::compareTo)
                .orElseThrow(() -> new IllegalStateException("Booking has no slot start time"));
    }

    private LocalDateTime getBookingEnd(Booking booking) {
        return booking.getBookingSlots().stream()
                .map(bs -> bs.getSlot().getDate().with(bs.getSlot().getTemplate().getEndTime()))
                .max(LocalDateTime::compareTo)
                .orElseThrow(() -> new IllegalStateException("Booking has no slot end time"));
    }
}
