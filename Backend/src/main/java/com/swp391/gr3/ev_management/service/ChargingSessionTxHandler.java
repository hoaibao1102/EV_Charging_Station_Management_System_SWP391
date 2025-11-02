package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.response.StopCharSessionResponse;
import com.swp391.gr3.ev_management.entity.*;
import com.swp391.gr3.ev_management.enums.*;
import com.swp391.gr3.ev_management.events.NotificationCreatedEvent;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChargingSessionTxHandler {

    private final ChargingSessionRepository sessionRepository;
    private final BookingsRepository bookingsRepository;
    private final TariffRepository tariffRepository;
    private final InvoiceRepository invoiceRepository;
    private final NotificationsRepository notificationsRepository;
    private final SessionSocCache sessionSocCache;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public StopCharSessionResponse stopSessionInternalTx(Long sessionId, Integer finalSocIfAny, LocalDateTime endTime) {
        ChargingSession cs = sessionRepository
                .findByIdWithBookingVehicleDriverUser(sessionId)
                .orElseThrow(() -> new ErrorException("Session not found"));

        if (cs.getStatus() != ChargingSessionStatus.IN_PROGRESS) {
            throw new ErrorException("Session is not currently active");
        }

        Booking booking = cs.getBooking();
        User user = booking.getVehicle().getDriver().getUser();

        Integer initialSoc = Optional.ofNullable(cs.getInitialSoc())
                .orElseThrow(() -> new ErrorException("Initial SoC not recorded"));

        // Tính final SoC (clamp 0..100). Nếu không có cache khác initial, sẽ ước lượng theo thời gian sạc
        int finalSoc = (finalSocIfAny != null) ? clampSoc(finalSocIfAny) : estimateFinalSoc(cs, endTime);
        if (finalSoc < initialSoc) finalSoc = initialSoc;

        // Năng lượng tiêu thụ
        double batteryCapacityKWh = booking.getVehicle().getModel().getBatteryCapacityKWh();
        double deltaSoc = finalSoc - initialSoc;
        double energyKWh = round2((deltaSoc / 100.0) * batteryCapacityKWh);

        long minutes = Math.max(0, ChronoUnit.MINUTES.between(cs.getStartTime(), endTime));

        // Thông tin điểm sạc & connector
        var firstSlot = booking.getBookingSlots().stream()
                .findFirst()
                .orElseThrow(() -> new ErrorException("No slot found for booking"));
        ChargingPoint point = firstSlot.getSlot().getChargingPoint();
        String pointNumber = (point != null) ? point.getPointNumber() : "Unknown";
        ConnectorType connectorType = (point != null && point.getConnectorType() != null)
                ? point.getConnectorType()
                : booking.getVehicle().getModel().getConnectorType();

        // Lấy Tariff tại thời điểm endTime. Nếu không có bản “cover” đúng biên, thử fallback theo finder khác.
        LocalDateTime pricingTime = endTime;
        Tariff tariff = tariffRepository
                .findTopByConnectorType_ConnectorTypeIdAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqualOrderByEffectiveFromDesc(
                        connectorType.getConnectorTypeId(), pricingTime, pricingTime)
                .orElseGet(() -> {
                    // fallback: dùng method findActiveByConnectorType nếu có (đã có ở ViolationServiceImpl)
                    return tariffRepository.findActiveByConnectorType(connectorType.getConnectorTypeId(), pricingTime)
                            .stream().findFirst().orElse(null);
                });

        if (tariff == null) {
            // Không có tariff hợp lệ → tuỳ chính sách: force complete với cost=0 và cảnh báo.
            log.warn("[STOP] No active tariff for connectorTypeId={} at {}. Force complete with cost=0.",
                    connectorType.getConnectorTypeId(), pricingTime);
            return forceCompleteWithoutBilling(cs, booking, user, pointNumber, initialSoc, finalSoc, energyKWh, minutes);
        }

        double pricePerKWh = tariff.getPricePerKWh();
        double cost = round2(pricePerKWh * energyKWh);

        // Cập nhật session
        cs.setEndTime(endTime);
        cs.setDurationMinutes((int) minutes);
        cs.setFinalSoc(finalSoc);
        cs.setEnergyKWh(energyKWh);
        cs.setCost(cost);
        cs.setStatus(ChargingSessionStatus.COMPLETED);
        sessionRepository.save(cs);

        sessionSocCache.remove(cs.getSessionId());

        // Cập nhật booking
        booking.setStatus(BookingStatus.COMPLETED);
        bookingsRepository.save(booking);

        // Thông báo
        Notification done = new Notification();
        done.setUser(user);
        done.setBooking(booking);
        done.setSession(cs);
        done.setTitle("Kết thúc sạc #" + booking.getBookingId());
        done.setContentNoti(
                "Điểm sạc: " + pointNumber +
                        " | Thời lượng: " + minutes + " phút" +
                        " | Tăng SOC: " + initialSoc + "% → " + finalSoc + "%" +
                        " | Năng lượng: " + energyKWh + " kWh" +
                        " | Chi phí: " + cost + " " + tariff.getCurrency()
        );
        done.setType(NotificationTypes.CHARGING_COMPLETED);
        done.setStatus(Notification.STATUS_UNREAD);
        done.setCreatedAt(LocalDateTime.now());
        notificationsRepository.save(done);
        eventPublisher.publishEvent(new NotificationCreatedEvent(done.getNotiId()));

        // Hóa đơn
        invoiceRepository.findBySession_SessionId(cs.getSessionId())
                .ifPresent(i -> { throw new ErrorException("Invoice already exists for this session"); });

        Invoice invoice = new Invoice();
        invoice.setSession(cs);
        invoice.setAmount(cost);
        invoice.setCurrency(tariff.getCurrency());
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setIssuedAt(LocalDateTime.now());
        invoice.setDriver(booking.getVehicle().getDriver());
        invoiceRepository.save(invoice);

        return StopCharSessionResponse.builder()
                .sessionId(cs.getSessionId())
                .stationName(booking.getStation().getStationName())
                .pointNumber(pointNumber)
                .vehiclePlate(booking.getVehicle().getVehiclePlate())
                .startTime(cs.getStartTime())
                .endTime(cs.getEndTime())
                .durationMinutes(cs.getDurationMinutes())
                .energyKWh(cs.getEnergyKWh())
                .cost(cs.getCost())
                .status(cs.getStatus())
                .initialSoc(cs.getInitialSoc())
                .finalSoc(cs.getFinalSoc())
                .pricePerKWh(tariff.getPricePerKWh())
                .currency(tariff.getCurrency())
                .build();
    }

    /**
     * Auto-stop nếu session vẫn đang IN_PROGRESS khi tới windowEnd.
     * Bọc try/catch để không bị fail im lặng khi thiếu tariff/khác.
     */
    @Transactional
    public void autoStopIfStillRunningTx(Long sessionId, LocalDateTime windowEnd) {
        var opt = sessionRepository.findById(sessionId);
        if (opt.isEmpty()) return;

        var session = opt.get();
        if (session.getStatus() != ChargingSessionStatus.IN_PROGRESS) return;

        Integer cachedSoc = sessionSocCache.get(sessionId).orElse(null);
        Integer finalSocIfAny = (cachedSoc != null && !cachedSoc.equals(session.getInitialSoc()))
                ? clampSoc(cachedSoc)
                : null;

        try {
            log.info("[AUTO-STOP] sessionId={} windowEnd={} startTime={} initialSoc={} cachedSoc={}",
                    sessionId, windowEnd, session.getStartTime(), session.getInitialSoc(), cachedSoc);
            stopSessionInternalTx(sessionId, finalSocIfAny, windowEnd);
        } catch (Exception ex) {
            log.error("[AUTO-STOP] Failed for sessionId={} at {}: {}", sessionId, windowEnd, ex.getMessage(), ex);

            // fallback chính sách: vẫn đóng session với cost=0 để giải phóng tài nguyên, tạo cảnh báo
            try {
                var cs = sessionRepository
                        .findByIdWithBookingVehicleDriverUser(sessionId)
                        .orElseThrow(() -> new ErrorException("Session not found"));
                int finalSoc = (finalSocIfAny != null) ? finalSocIfAny : estimateFinalSoc(cs, windowEnd);
                long minutes = Math.max(0, ChronoUnit.MINUTES.between(cs.getStartTime(), windowEnd));
                var booking = cs.getBooking();
                var user = booking.getVehicle().getDriver().getUser();

                var firstSlot = booking.getBookingSlots().stream().findFirst()
                        .orElseThrow(() -> new ErrorException("No slot found for booking"));
                var point = firstSlot.getSlot().getChargingPoint();
                String pointNumber = (point != null) ? point.getPointNumber() : "Unknown";

                forceCompleteWithoutBilling(cs, booking, user, pointNumber,
                        cs.getInitialSoc(), finalSoc,
                        round2(((finalSoc - cs.getInitialSoc()) / 100.0) * booking.getVehicle().getModel().getBatteryCapacityKWh()),
                        minutes);
            } catch (Exception nested) {
                log.error("[AUTO-STOP] Force-complete fallback also failed for sessionId={}: {}", sessionId, nested.getMessage(), nested);
            }
        }
    }

    // ------------------ Helper methods ------------------

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static int clampSoc(Integer soc) {
        return Math.max(0, Math.min(100, (soc == null ? 0 : soc)));
    }

    private int estimateFinalSoc(ChargingSession session, LocalDateTime endTime) {
        int initial = Optional.ofNullable(session.getInitialSoc()).orElse(20);
        Booking b = session.getBooking();

        double capKWh = (b != null && b.getVehicle() != null && b.getVehicle().getModel() != null)
                ? b.getVehicle().getModel().getBatteryCapacityKWh()
                : 60.0;

        double minutes = Math.max(0, ChronoUnit.MINUTES.between(session.getStartTime(), endTime));
        double hours = minutes / 60.0;

        double ratedKW = 11.0;
        if (b != null && b.getBookingSlots() != null && !b.getBookingSlots().isEmpty()) {
            var bs0 = b.getBookingSlots().get(0);
            if (bs0.getSlot() != null && bs0.getSlot().getChargingPoint() != null) {
                Double p = bs0.getSlot().getChargingPoint().getMaxPowerKW();
                if (p != null && p > 0) ratedKW = p;
            }
        }

        double efficiency = 0.90;
        double estEnergy = round2(hours * ratedKW * efficiency);
        int estFinal = (int) Math.round(initial + (estEnergy / capKWh) * 100.0);

        if (minutes > 0 && estFinal == initial) estFinal = initial + 1;

        log.info("⚡ Estimating SoC: initial={} capKWh={} ratedKW={} minutes={} hours={} estEnergy={} → estFinal={}",
                initial, capKWh, ratedKW, minutes, hours, estEnergy, estFinal);

        return Math.min(100, Math.max(initial, estFinal));
    }

    /**
     * Hoàn tất phiên sạc mà KHÔNG tính tiền (cost=0), dùng khi thiếu tariff hoặc lỗi billing.
     * Tạo notification cảnh báo để admin xử lý sau.
     */
    private StopCharSessionResponse forceCompleteWithoutBilling(
            ChargingSession cs,
            Booking booking,
            User user,
            String pointNumber,
            Integer initialSoc,
            Integer finalSoc,
            double energyKWh,
            long minutes
    ) {
        cs.setEndTime(cs.getEndTime() != null ? cs.getEndTime() : LocalDateTime.now());
        cs.setDurationMinutes((int) minutes);
        cs.setFinalSoc(finalSoc);
        cs.setEnergyKWh(energyKWh);
        cs.setCost(0.0);
        cs.setStatus(ChargingSessionStatus.COMPLETED);
        sessionRepository.save(cs);
        sessionSocCache.remove(cs.getSessionId());

        booking.setStatus(BookingStatus.COMPLETED);
        bookingsRepository.save(booking);

        Notification warn = new Notification();
        warn.setUser(user);
        warn.setBooking(booking);
        warn.setSession(cs);
        warn.setTitle("Kết thúc sạc (không tính phí) #" + booking.getBookingId());
        warn.setContentNoti(
                "Điểm sạc: " + pointNumber +
                        " | Thời lượng: " + minutes + " phút" +
                        " | Tăng SOC: " + initialSoc + "% → " + finalSoc + "%" +
                        " | Năng lượng (ước lượng): " + energyKWh + " kWh" +
                        " | Lưu ý: Không tìm thấy tariff hoặc lỗi billing. Chi phí tạm tính: 0."
        );
        warn.setType(NotificationTypes.CHARGING_COMPLETED);
        warn.setStatus(Notification.STATUS_UNREAD);
        warn.setCreatedAt(LocalDateTime.now());
        notificationsRepository.save(warn);
        eventPublisher.publishEvent(new NotificationCreatedEvent(warn.getNotiId()));

        // Không tạo invoice khi cost=0 (tuỳ chính sách; nếu muốn vẫn tạo, thêm block ở đây)

        return StopCharSessionResponse.builder()
                .sessionId(cs.getSessionId())
                .stationName(booking.getStation().getStationName())
                .pointNumber(pointNumber)
                .vehiclePlate(booking.getVehicle().getVehiclePlate())
                .startTime(cs.getStartTime())
                .endTime(cs.getEndTime())
                .durationMinutes(cs.getDurationMinutes())
                .energyKWh(cs.getEnergyKWh())
                .cost(cs.getCost())
                .status(cs.getStatus())
                .initialSoc(cs.getInitialSoc())
                .finalSoc(cs.getFinalSoc())
                .pricePerKWh(0.0)
                .currency(null)
                .build();
    }
}
