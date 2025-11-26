package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.response.StopCharSessionResponse;
import com.swp391.gr3.ev_management.entity.*;
import com.swp391.gr3.ev_management.enums.*;
import com.swp391.gr3.ev_management.events.NotificationCreatedEvent;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.mapper.StopCharSessionResponseMapper;
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

    private final ChargingSessionRepository chargingSessionRepository;
    private final BookingService bookingService;
    private final TariffService tariffService;
    private final InvoiceService invoiceService;
    private final NotificationsService notificationsService;
    private final SessionSocCache sessionSocCache;
    private final ApplicationEventPublisher eventPublisher;
    private final StopCharSessionResponseMapper stopResponseMapper;
    private final SlotAvailabilityService slotAvailabilityService;

    @Transactional
    public StopCharSessionResponse stopSessionInternalTx(
            Long sessionId,
            Integer finalSocIfAny,
            LocalDateTime endTime,
            StopInitiator initiator
    ) {
        // 1) Lấy session + booking + vehicle + driver + user
        ChargingSession cs = chargingSessionRepository
                .findByIdWithBookingVehicleDriverUser(sessionId)
                .orElseThrow(() -> new ErrorException("Session not found"));

        if (cs.getStatus() != ChargingSessionStatus.IN_PROGRESS) {
            throw new ErrorException("Session is not currently active");
        }

        Booking booking = cs.getBooking();

        // ====== PHÒNG VỆ NPE: VEHICLE / DRIVER / USER ======
        UserVehicle vehicle = booking != null ? booking.getVehicle() : null;
        Driver driver = (vehicle != null) ? vehicle.getDriver() : null;
        User user = (driver != null) ? driver.getUser() : null;

        if (vehicle == null) {
            log.warn("[STOP] Booking {} has NO VEHICLE. Notifications & invoice may lose owner information.",
                    booking != null ? booking.getBookingId() : null);
        }
        if (driver == null) {
            log.warn("[STOP] Vehicle {} has NO DRIVER. Invoice driver = null.",
                    vehicle != null ? vehicle.getVehicleId() : null);
        }
        if (user == null) {
            log.warn("[STOP] No USER found → Notification disabled.");
        }

        // 2) initial soc
        Integer initialSoc = Optional.ofNullable(cs.getInitialSoc())
                .orElseThrow(() -> new ErrorException("Initial SoC not recorded"));

        // 3) final soc
        int finalSoc = (finalSocIfAny != null) ? clampSoc(finalSocIfAny) : estimateFinalSoc(cs, endTime);
        if (finalSoc < initialSoc) finalSoc = initialSoc;

        // 4) Resolve windows
        LocalDateTime rawWindowStart = resolveWindowStartForTx(booking);
        LocalDateTime windowEnd = resolveWindowEndForTx(booking);

        LocalDateTime windowStart = rawWindowStart;
        if (booking.getCreatedAt() != null && booking.getCreatedAt().isAfter(windowStart)) {
            windowStart = booking.getCreatedAt();
        }
        if (cs.getStartTime() != null && cs.getStartTime().isAfter(windowStart)) {
            windowStart = cs.getStartTime();
        }

        long sessionMinutes = Math.max(0, ChronoUnit.MINUTES.between(cs.getStartTime(), endTime));

        // 5) Slot info (để lấy ChargingPoint & pointNumber)
        var firstSlot = booking.getBookingSlots().stream()
                .findFirst()
                .orElseThrow(() -> new ErrorException("No slot found for booking"));

        ChargingPoint point = (firstSlot.getSlot() != null)
                ? firstSlot.getSlot().getChargingPoint()
                : null;

        String pointNumber = (point != null && point.getPointNumber() != null)
                ? point.getPointNumber()
                : "Unknown";

        // ====== PHÒNG VỆ CONNECTOR-TYPE NULL ======
        ConnectorType connectorType =
                (point != null && point.getConnectorType() != null)
                        ? point.getConnectorType()
                        : (vehicle != null && vehicle.getModel() != null)
                        ? vehicle.getModel().getConnectorType()
                        : null;

        if (connectorType == null) {
            log.warn("[STOP] connectorType NULL → free session.");
            return forceCompleteWithoutBilling(cs, booking, user, pointNumber, initialSoc, finalSoc,
                    0, sessionMinutes);
        }

        // ====== TARIFF ======
        LocalDateTime pricingTime = endTime;
        Tariff tariff = tariffService
                .findTopByConnectorType_ConnectorTypeIdAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqualOrderByEffectiveFromDesc(
                        connectorType.getConnectorTypeId(), pricingTime, pricingTime)
                .orElseGet(() -> tariffService
                        .findActiveByConnectorType(connectorType.getConnectorTypeId(), pricingTime)
                        .stream().findFirst().orElse(null));

        if (tariff == null) {
            log.warn("[STOP] No active tariff → free session.");
            return forceCompleteWithoutBilling(cs, booking, user, pointNumber, initialSoc, finalSoc,
                    0, sessionMinutes);
        }

        // ====== ENERGY ======

        double batteryCapacity =
                (vehicle != null && vehicle.getModel() != null)
                        ? vehicle.getModel().getBatteryCapacityKWh()
                        : 40.0; // default tránh NPE

        double deltaSoc = finalSoc - initialSoc;
        double energyKWh = round2((deltaSoc / 100.0) * batteryCapacity);

        long slotMinutes = getSlotMinutes(booking);
        int bookedSlots = booking.getBookingSlots().size();

        double ratedKW = (point != null && point.getMaxPowerKW() > 0)
                ? point.getMaxPowerKW()
                : 11.0;

        long chargingMinutesFromEnergy = (long) Math.ceil((energyKWh / (ratedKW * 0.9)) * 60);
        long activeChargingMinutes = Math.min(sessionMinutes, chargingMinutesFromEnergy);

        // ====== PRICING ======
        double timeCost = 0.0;
        double energyCost = 0.0;

        if (initiator == StopInitiator.STAFF) {
            timeCost = round2(sessionMinutes * tariff.getPricePerMin());

        } else if (initiator == StopInitiator.DRIVER) {
            if (slotMinutes > 0 && bookedSlots > 0) {
                long roundedSlots = (long) Math.ceil((double) sessionMinutes / slotMinutes);
                long roundedMinutes = roundedSlots * slotMinutes;
                long penaltyMinutes = Math.max(0, roundedMinutes - activeChargingMinutes);
                timeCost = round2(penaltyMinutes * tariff.getPricePerMin());
            }
            energyCost = round2(energyKWh * tariff.getPricePerKWh());

        } else { // SYSTEM_AUTO
            energyCost = round2(energyKWh * tariff.getPricePerKWh());
        }

        double totalCost = round2(timeCost + energyCost);

        // Giải phóng các slot chưa dùng nếu DRIVER hoặc STAFF dừng sớm
        if (initiator == StopInitiator.DRIVER || initiator == StopInitiator.STAFF) {
            releaseUnusedFutureSlots(booking, endTime);
        }

        // ====== SAVE SESSION ======
        cs.setEndTime(endTime);
        cs.setDurationMinutes((int) sessionMinutes);
        cs.setFinalSoc(finalSoc);
        cs.setEnergyKWh(energyKWh);
        cs.setCost(totalCost);
        cs.setStatus(ChargingSessionStatus.COMPLETED);
        chargingSessionRepository.save(cs);
        sessionSocCache.remove(cs.getSessionId());

        // ====== UPDATE BOOKING ======
        booking.setStatus(BookingStatus.COMPLETED);
        bookingService.save(booking);

        // ====== NOTIFICATION (NULL-SAFE) ======
        if (user != null) {
            Notification done = new Notification();
            done.setUser(user);
            done.setBooking(booking);
            done.setSession(cs);
            done.setTitle("Kết thúc sạc #" + booking.getBookingId());
            done.setContentNoti(
                    "Điểm sạc: " + pointNumber +
                            " | Thời lượng: " + sessionMinutes + " phút" +
                            " | Tăng SOC: " + initialSoc + " → " + finalSoc +
                            " | Năng lượng: " + energyKWh + " kWh" +
                            " | Tổng phí: " + totalCost + " " + tariff.getCurrency()
            );
            done.setType(NotificationTypes.CHARGING_COMPLETED);
            done.setStatus(Notification.STATUS_UNREAD);
            done.setCreatedAt(LocalDateTime.now());
            notificationsService.save(done);
            eventPublisher.publishEvent(new NotificationCreatedEvent(done.getNotiId()));
        } else {
            log.warn("[STOP] Skip notification because USER == null");
        }

        // ====== INVOICE (driver có thể null) ======
        invoiceService.findBySession_SessionId(cs.getSessionId())
                .ifPresent(i -> { throw new ErrorException("Invoice already exists for this session"); });

        Invoice invoice = new Invoice();
        invoice.setSession(cs);
        invoice.setAmount(totalCost);
        invoice.setCurrency(tariff.getCurrency());
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setIssuedAt(LocalDateTime.now());
        // driver null cũng lưu bình thường (có thể sau này update driver sau)
        invoice.setDriver(driver);
        invoiceService.save(invoice);

        if (driver == null) {
            log.warn("[STOP] Invoice created WITHOUT DRIVER for session {}", cs.getSessionId());
        }

        return stopResponseMapper.mapWithTariff(cs, booking, pointNumber, tariff);
    }

    @Transactional
    public void autoStopIfStillRunningTx(Long sessionId, LocalDateTime windowEnd) {
        var opt = chargingSessionRepository.findById(sessionId);
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
            stopSessionInternalTx(sessionId, finalSocIfAny, windowEnd, StopInitiator.SYSTEM_AUTO);

        } catch (Exception ex) {
            log.error("[AUTO-STOP] Failed for sessionId={} at {}: {}", sessionId, windowEnd, ex.getMessage(), ex);

            try {
                var cs = chargingSessionRepository
                        .findByIdWithBookingVehicleDriverUser(sessionId)
                        .orElseThrow(() -> new ErrorException("Session not found"));

                Booking booking = cs.getBooking();
                UserVehicle vehicle = booking != null ? booking.getVehicle() : null;
                Driver driver = (vehicle != null) ? vehicle.getDriver() : null;
                User user = (driver != null) ? driver.getUser() : null;

                int finalSoc = (finalSocIfAny != null) ? finalSocIfAny : estimateFinalSoc(cs, windowEnd);
                long minutes = Math.max(0, ChronoUnit.MINUTES.between(cs.getStartTime(), windowEnd));

                double capKWh =
                        (vehicle != null && vehicle.getModel() != null)
                                ? vehicle.getModel().getBatteryCapacityKWh()
                                : 40.0;

                double energyKWh = round2(((finalSoc - cs.getInitialSoc()) / 100.0) * capKWh);

                var firstSlot = booking.getBookingSlots().stream().findFirst()
                        .orElseThrow(() -> new ErrorException("No slot found for booking"));
                var point = firstSlot.getSlot().getChargingPoint();
                String pointNumber = (point != null) ? point.getPointNumber() : "Unknown";

                forceCompleteWithoutBilling(cs, booking, user, pointNumber,
                        cs.getInitialSoc(), finalSoc, energyKWh, minutes);

            } catch (Exception nested) {
                log.error("[AUTO-STOP] Force-complete fallback also failed for sessionId={}: {}",
                        sessionId, nested.getMessage(), nested);
            }
        }
    }

    // ===== helper methods y như cũ: round2, clampSoc, estimateFinalSoc, forceCompleteWithoutBilling,
    // resolveWindowStartForTx, resolveWindowEndForTx, getSlotMinutes, releaseUnusedFutureSlots =====

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
        chargingSessionRepository.save(cs);
        sessionSocCache.remove(cs.getSessionId());

        booking.setStatus(BookingStatus.COMPLETED);
        bookingService.save(booking);

        if (user != null) {
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
            notificationsService.save(warn);
            eventPublisher.publishEvent(new NotificationCreatedEvent(warn.getNotiId()));
        } else {
            log.warn("[STOP] Skip FREE notification because USER == null");
        }

        return stopResponseMapper.mapNoBilling(cs, booking, pointNumber);
    }

    private LocalDateTime resolveWindowStartForTx(Booking booking) {
        if (booking.getScheduledStartTime() != null) return booking.getScheduledStartTime();
        return booking.getBookingSlots().stream()
                .map(bs -> bs.getSlot().getDate().with(bs.getSlot().getTemplate().getStartTime()))
                .min(LocalDateTime::compareTo)
                .orElseThrow(() -> new ErrorException("Booking has no slot start time"));
    }

    private LocalDateTime resolveWindowEndForTx(Booking booking) {
        if (booking.getScheduledEndTime() != null) return booking.getScheduledEndTime();
        return booking.getBookingSlots().stream()
                .map(bs -> bs.getSlot().getDate().with(bs.getSlot().getTemplate().getEndTime()))
                .max(LocalDateTime::compareTo)
                .orElseThrow(() -> new ErrorException("Booking has no slot end time"));
    }

    private long getSlotMinutes(Booking booking) {
        var any = booking.getBookingSlots().stream()
                .findFirst()
                .orElseThrow(() -> new ErrorException("No slot found for booking"));
        var tpl = any.getSlot().getTemplate();
        var start = tpl.getStartTime();
        var end   = tpl.getEndTime();
        return ChronoUnit.MINUTES.between(start, end);
    }

    private void releaseUnusedFutureSlots(Booking booking, LocalDateTime endTime) {
        if (booking.getBookingSlots() == null) return;

        booking.getBookingSlots().forEach(bs -> {
            SlotAvailability slot = bs.getSlot();
            LocalDateTime slotStart = slot.getDate().with(slot.getTemplate().getStartTime());
            if (!endTime.isAfter(slotStart)) { // endTime <= slotStart
                slot.setStatus(SlotStatus.AVAILABLE);
                slotAvailabilityService.save(slot);
                log.info("[RELEASE SLOT] bookingId={} slotId={} released (endTime={} <= slotStart={})",
                        booking.getBookingId(), slot.getSlotId(), endTime, slotStart);
            }
        });
    }
}
