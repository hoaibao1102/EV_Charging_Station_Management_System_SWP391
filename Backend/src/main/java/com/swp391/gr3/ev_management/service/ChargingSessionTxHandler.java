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

@Service // Đánh dấu class là 1 Spring Service (xử lý logic nghiệp vụ ở tầng TX)
@RequiredArgsConstructor // Tự động tạo constructor cho các field final để DI
@Slf4j // Cung cấp logger (log.info, log.warn, log.error, ...)
public class ChargingSessionTxHandler {

    // ====== Dependencies cần cho luồng dừng/auto-stop phiên sạc ======
    private final ChargingSessionRepository chargingSessionRepository;      // Truy vấn & lưu ChargingSession
    private final BookingService bookingService;                            // Cập nhật trạng thái Booking liên quan
    private final TariffService tariffService;                              // Lấy biểu giá (tariff) theo connector & thời gian
    private final InvoiceService invoiceService;                            // Lưu & kiểm tra hoá đơn
    private final NotificationsService notificationsService;                // Tạo Notification cho user
    private final SessionSocCache sessionSocCache;                          // Cache SOC tạm thời theo session
    private final ApplicationEventPublisher eventPublisher;                 // Publish event (vd: gửi email)
    private final StopCharSessionResponseMapper stopResponseMapper;         // Map entity -> DTO phản hồi
    private final SlotAvailabilityService slotAvailabilityService;          // thêm repo này để giải phóng slot

    /**
     * DỪNG PHIÊN SẠC (TX độc lập):
     * - Kiểm tra session đang IN_PROGRESS
     * - Xác định final SoC (từ cache nếu có, nếu không thì ước lượng)
     * - Tính năng lượng tiêu thụ & chi phí theo tariff tại thời điểm kết thúc
     * - Cập nhật session -> COMPLETED, booking -> COMPLETED
     * - Tạo notification và hoá đơn (invoice)
     * - Trả về DTO kết quả
     */
    @Transactional
    public StopCharSessionResponse stopSessionInternalTx(
            Long sessionId,
            Integer finalSocIfAny,
            LocalDateTime endTime,
            StopInitiator initiator // 🆕
    ) {
        // 1️⃣ Lấy ChargingSession kèm theo Booking, Vehicle, Driver, User để dùng cho tính toán & notification
        ChargingSession cs = chargingSessionRepository
                .findByIdWithBookingVehicleDriverUser(sessionId)
                .orElseThrow(() -> new ErrorException("Session not found"));

        // 2️⃣ Kiểm tra trạng thái phiên sạc, chỉ cho phép dừng nếu đang IN_PROGRESS
        if (cs.getStatus() != ChargingSessionStatus.IN_PROGRESS) {
            throw new ErrorException("Session is not currently active");
        }

        // 3️⃣ Lấy Booking & User liên quan đến phiên sạc (dùng để cập nhật & gửi thông báo)
        Booking booking = cs.getBooking();
        User user = booking.getVehicle().getDriver().getUser();

        // 4️⃣ Đảm bảo đã có SOC ban đầu, nếu chưa có -> dữ liệu không hợp lệ
        Integer initialSoc = Optional.ofNullable(cs.getInitialSoc())
                .orElseThrow(() -> new ErrorException("Initial SoC not recorded"));

        // 5️⃣ Xác định SOC cuối
        int finalSoc = (finalSocIfAny != null) ? clampSoc(finalSocIfAny) : estimateFinalSoc(cs, endTime);
        if (finalSoc < initialSoc) finalSoc = initialSoc;

        // ================== WINDOW / SLOT CONFIG ==================

        // 6️⃣ Slot window gốc
        LocalDateTime rawWindowStart = resolveWindowStartForTx(booking);
        LocalDateTime windowEnd      = resolveWindowEndForTx(booking);

        // Thời điểm tạo booking
        LocalDateTime bookingCreatedAt = booking.getCreatedAt();

        // 🔥 Mốc tính phí = max(slotStart, bookingCreatedAt, startTime thực tế)
        LocalDateTime windowStart = rawWindowStart;
        if (bookingCreatedAt != null && bookingCreatedAt.isAfter(windowStart)) {
            windowStart = bookingCreatedAt;
        }
        if (cs.getStartTime() != null && cs.getStartTime().isAfter(windowStart)) {
            windowStart = cs.getStartTime();
        }

        long sessionMinutes = Math.max(0, ChronoUnit.MINUTES.between(cs.getStartTime(), endTime));
        long totalWindowMinutes = Math.max(0, ChronoUnit.MINUTES.between(windowStart, windowEnd)); // (hiện tại chưa dùng tới nhưng để lại cho dễ debug)

        // ================== TARIFF + CONNECTOR ==================

        var firstSlot = booking.getBookingSlots().stream()
                .findFirst()
                .orElseThrow(() -> new ErrorException("No slot found for booking"));
        ChargingPoint point = firstSlot.getSlot().getChargingPoint();
        String pointNumber = (point != null) ? point.getPointNumber() : "Unknown";

        ConnectorType connectorType = (point != null && point.getConnectorType() != null)
                ? point.getConnectorType()
                : booking.getVehicle().getModel().getConnectorType();

        LocalDateTime pricingTime = endTime;

        Tariff tariff = tariffService
                .findTopByConnectorType_ConnectorTypeIdAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqualOrderByEffectiveFromDesc(
                        connectorType.getConnectorTypeId(), pricingTime, pricingTime)
                .orElseGet(() -> tariffService.findActiveByConnectorType(connectorType.getConnectorTypeId(), pricingTime)
                        .stream().findFirst().orElse(null));

        if (tariff == null) {
            log.warn("[STOP] No active tariff for connectorTypeId={} at {}. Force complete with cost=0.",
                    connectorType.getConnectorTypeId(), pricingTime);
            return forceCompleteWithoutBilling(cs, booking, user, pointNumber, initialSoc, finalSoc,
                    round2(((finalSoc - initialSoc) / 100.0) * booking.getVehicle().getModel().getBatteryCapacityKWh()),
                    sessionMinutes);
        }

        // ================== NĂNG LƯỢNG / SLOT ==================

        double batteryCapacityKWh = booking.getVehicle().getModel().getBatteryCapacityKWh();
        double deltaSoc = finalSoc - initialSoc;
        double energyKWh = round2((deltaSoc / 100.0) * batteryCapacityKWh);

        long slotMinutes = getSlotMinutes(booking);               // vd: 5 hoặc 60
        int bookedSlots = booking.getBookingSlots() != null ? booking.getBookingSlots().size() : 0;

        long elapsedFromWindowStart = Math.max(0, ChronoUnit.MINUTES.between(windowStart, endTime));

        double ratedKW = 11.0;
        if (booking.getBookingSlots() != null && !booking.getBookingSlots().isEmpty()) {
            var bs0 = booking.getBookingSlots().get(0);
            if (bs0.getSlot() != null && bs0.getSlot().getChargingPoint() != null) {
                Double p = bs0.getSlot().getChargingPoint().getMaxPowerKW();
                if (p != null && p > 0) ratedKW = p;
            }
        }

        double efficiency = 0.90;
        long chargingMinutesFromEnergy = (long) Math.ceil((energyKWh / (ratedKW * efficiency)) * 60.0);
        long activeChargingMinutes = Math.min(sessionMinutes, chargingMinutesFromEnergy);

        // ================== PRICING BLOCK ==================

        double timeCost = 0.0;
        double energyCost = 0.0;

        if (initiator == StopInitiator.STAFF) {
            // STAFF: tính đúng toàn bộ thời gian thực
            timeCost = round2(sessionMinutes * tariff.getPricePerMin());
            energyCost = 0.0;

        } else if (initiator == StopInitiator.DRIVER) {
            // DRIVER: hybrid (time + energy)

            if (slotMinutes <= 0 || bookedSlots <= 0) {
                long timeMinutes = Math.max(0, sessionMinutes - activeChargingMinutes);
                timeCost = round2(timeMinutes * tariff.getPricePerMin());
                energyCost = round2(energyKWh * tariff.getPricePerKWh());

            } else {
                // ⏱ Thời điểm xe FULL 100% (nếu có) – sau thời điểm này không phạt thêm time
                LocalDateTime fullTime = null;
                if (finalSoc >= 100 && deltaSoc > 0) {
                    // phút để tăng từ initial -> final (đã clamp max 100)
                    double socGainFraction = deltaSoc / 100.0;
                    long fullMinutes = Math.round(
                            (socGainFraction * batteryCapacityKWh) / (ratedKW * efficiency) * 60.0
                    );
                    fullTime = cs.getStartTime().plusMinutes(fullMinutes);
                }
                final LocalDateTime finalFullTime = fullTime;

                // Thời điểm tối đa để tính phạt time = min(endTime, fullTime nếu có)
                LocalDateTime endForPenalty =
                        (finalFullTime != null && finalFullTime.isBefore(endTime)) ? finalFullTime : endTime;

                // 🔹 Đếm slot thực sự bị "đụng tới"
                long usedSlots = booking.getBookingSlots().stream()
                        .map(bs -> bs.getSlot())
                        .map(slot -> slot.getDate().with(slot.getTemplate().getStartTime()))
                        .filter(slotStart -> endForPenalty.isAfter(slotStart))   // phải qua startTime slot
                        .count();

                // 🔹 Slot theo thời gian trôi qua (từ windowStart)
                long elapsedForPenalty =
                        Math.max(0, ChronoUnit.MINUTES.between(windowStart, endForPenalty));
                long roundedSlotsByTime = (long) Math.ceil(
                        (double) elapsedForPenalty / (double) slotMinutes
                );

                // 🔥 Số slot bị tính tiền = min(usedSlots, roundedSlotsByTime, bookedSlots)
                long roundedSlots = Math.min(bookedSlots, Math.min(usedSlots, roundedSlotsByTime));
                long roundedMinutes = roundedSlots * slotMinutes;

                // activeChargingMinutes cũng không được vượt quá khoảng [startTime, endForPenalty]
                long maxChargingWindow =
                        Math.max(0, ChronoUnit.MINUTES.between(cs.getStartTime(), endForPenalty));
                long effectiveActiveChargingMinutes = Math.min(activeChargingMinutes, maxChargingWindow);

                long timeMinutes = Math.max(0, roundedMinutes - effectiveActiveChargingMinutes);

                timeCost = round2(timeMinutes * tariff.getPricePerMin());
                energyCost = round2(energyKWh * tariff.getPricePerKWh());

                log.info("[PRICING DRIVER] usedSlots={} roundedSlotsByTime={} roundedSlots={} " +
                                "slotMinutes={} elapsedFromWindowStart={} elapsedForPenalty={} " +
                                "activeChargingMinutes={} effectiveActiveChargingMinutes={} timeMinutes={} fullTime={}",
                        usedSlots, roundedSlotsByTime, roundedSlots,
                        slotMinutes, elapsedFromWindowStart, elapsedForPenalty,
                        activeChargingMinutes, effectiveActiveChargingMinutes, timeMinutes, finalFullTime);
            }

        } else { // SYSTEM_AUTO
            // Chỉ tính theo kWh
            timeCost = 0.0;
            energyCost = round2(tariff.getPricePerKWh() * energyKWh);
        }

        double totalCost = round2(timeCost + energyCost);

        // ================== GIẢI PHÓNG SLOT & LƯU DB ==================

        if (initiator == StopInitiator.DRIVER || initiator == StopInitiator.STAFF) {
            releaseUnusedFutureSlots(booking, endTime);
        }

        cs.setEndTime(endTime);
        cs.setDurationMinutes((int) sessionMinutes);
        cs.setFinalSoc(finalSoc);
        cs.setEnergyKWh(energyKWh);
        cs.setCost(totalCost);
        cs.setStatus(ChargingSessionStatus.COMPLETED);
        chargingSessionRepository.save(cs);
        sessionSocCache.remove(cs.getSessionId());

        booking.setStatus(BookingStatus.COMPLETED);
        bookingService.save(booking);

        Notification done = new Notification();
        done.setUser(user);
        done.setBooking(booking);
        done.setSession(cs);
        done.setTitle("Kết thúc sạc #" + booking.getBookingId());
        done.setContentNoti(
                "Điểm sạc: " + pointNumber +
                        " | Thời lượng: " + sessionMinutes + " phút" +
                        " | Tăng SOC: " + initialSoc + "% → " + finalSoc + "%" +
                        " | Năng lượng: " + energyKWh + " kWh" +
                        " | Phí thời gian: " + timeCost + " " + tariff.getCurrency() +
                        " | Phí điện năng: " + energyCost + " " + tariff.getCurrency() +
                        " | Tổng: " + totalCost + " " + tariff.getCurrency()
        );
        done.setType(NotificationTypes.CHARGING_COMPLETED);
        done.setStatus(Notification.STATUS_UNREAD);
        done.setCreatedAt(LocalDateTime.now());
        notificationsService.save(done);
        eventPublisher.publishEvent(new NotificationCreatedEvent(done.getNotiId()));

        invoiceService.findBySession_SessionId(cs.getSessionId())
                .ifPresent(i -> { throw new ErrorException("Invoice already exists for this session"); });

        Invoice invoice = new Invoice();
        invoice.setSession(cs);
        invoice.setAmount(totalCost);
        invoice.setCurrency(tariff.getCurrency());
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setIssuedAt(LocalDateTime.now());
        invoice.setDriver(booking.getVehicle().getDriver());
        invoiceService.save(invoice);

        return stopResponseMapper.mapWithTariff(cs, booking, pointNumber, tariff);
    }

    /**
     * AUTO-STOP (TX độc lập):
     * - Khi tới thời điểm windowEnd, nếu session vẫn IN_PROGRESS thì dừng.
     * - Cố gắng dừng chuẩn (tính tiền); nếu lỗi, fallback force-complete (cost=0) để giải phóng tài nguyên.
     */
    @Transactional
    public void autoStopIfStillRunningTx(Long sessionId, LocalDateTime windowEnd) {
        // 1️⃣ Lấy session theo ID, nếu không tồn tại -> không làm gì (có thể đã bị huỷ hoặc dừng tay)
        var opt = chargingSessionRepository.findById(sessionId);
        if (opt.isEmpty()) return;

        var session = opt.get();
        // 2️⃣ Chỉ auto-stop nếu session vẫn IN_PROGRESS
        if (session.getStatus() != ChargingSessionStatus.IN_PROGRESS) return;

        // 3️⃣ Lấy SOC cuối cùng từ cache nếu có và khác initial
        Integer cachedSoc = sessionSocCache.get(sessionId).orElse(null);
        Integer finalSocIfAny = (cachedSoc != null && !cachedSoc.equals(session.getInitialSoc()))
                ? clampSoc(cachedSoc)
                : null;

        try {
            // 4️⃣ Gọi stopSessionInternalTx với initiator = SYSTEM_AUTO để dừng và tính phí chuẩn
            log.info("[AUTO-STOP] sessionId={} windowEnd={} startTime={} initialSoc={} cachedSoc={}",
                    sessionId, windowEnd, session.getStartTime(), session.getInitialSoc(), cachedSoc);
            stopSessionInternalTx(sessionId, finalSocIfAny, windowEnd, StopInitiator.SYSTEM_AUTO);
        } catch (Exception ex) {
            // 5️⃣ Nếu có bất kỳ lỗi nào trong quá trình dừng chuẩn:
            //    - Log error và fallback sang force-complete không tính phí
            log.error("[AUTO-STOP] Failed for sessionId={} at {}: {}", sessionId, windowEnd, ex.getMessage(), ex);

            try {
                // 6️⃣ Lấy lại session kèm Booking/Vehicle/Driver/User để prepare dữ liệu cho fallback
                var cs = chargingSessionRepository
                        .findByIdWithBookingVehicleDriverUser(sessionId)
                        .orElseThrow(() -> new ErrorException("Session not found"));

                // 7️⃣ Ước lượng SOC cuối nếu chưa có finalSocIfAny, dựa trên estimateFinalSoc
                int finalSoc = (finalSocIfAny != null) ? finalSocIfAny : estimateFinalSoc(cs, windowEnd);
                long minutes = Math.max(0, ChronoUnit.MINUTES.between(cs.getStartTime(), windowEnd));
                var booking = cs.getBooking();
                var user = booking.getVehicle().getDriver().getUser();

                // 8️⃣ Lấy pointNumber để log/notification
                var firstSlot = booking.getBookingSlots().stream().findFirst()
                        .orElseThrow(() -> new ErrorException("No slot found for booking"));
                var point = firstSlot.getSlot().getChargingPoint();
                String pointNumber = (point != null) ? point.getPointNumber() : "Unknown";

                // 9️⃣ Gọi forceCompleteWithoutBilling để đánh dấu COMPLETE nhưng không billing
                forceCompleteWithoutBilling(cs, booking, user, pointNumber,
                        cs.getInitialSoc(), finalSoc,
                        round2(((finalSoc - cs.getInitialSoc()) / 100.0) * booking.getVehicle().getModel().getBatteryCapacityKWh()),
                        minutes);
            } catch (Exception nested) {
                // 🔟 Nếu fallback cũng thất bại thì log lại để điều tra thủ công
                log.error("[AUTO-STOP] Force-complete fallback also failed for sessionId={}: {}", sessionId, nested.getMessage(), nested);
            }
        }
    }

    // ------------------ Helper methods ------------------

    // Làm tròn 2 chữ số thập phân
    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    // Giới hạn SOC trong khoảng 0..100
    private static int clampSoc(Integer soc) {
        return Math.max(0, Math.min(100, (soc == null ? 0 : soc)));
    }

    /**
     * ƯỚC LƯỢNG SOC CUỐI:
     * - Dựa trên thời gian sạc (start -> endTime), công suất điểm sạc (ratedKW) và hiệu suất.
     * - Chuyển đổi kWh sang % pin theo dung lượng pin của xe.
     * - Đảm bảo không giảm dưới initial và không vượt quá 100.
     */
    private int estimateFinalSoc(ChargingSession session, LocalDateTime endTime) {
        // 1️⃣ Lấy SOC ban đầu, nếu chưa có thì dùng fallback 20%
        int initial = Optional.ofNullable(session.getInitialSoc()).orElse(20);
        Booking b = session.getBooking();

        // 2️⃣ Lấy dung lượng pin (kWh) từ Model của Vehicle, nếu thiếu thì mặc định 60kWh
        double capKWh = (b != null && b.getVehicle() != null && b.getVehicle().getModel() != null)
                ? b.getVehicle().getModel().getBatteryCapacityKWh()
                : 60.0;

        // 3️⃣ Tính thời lượng sạc (phút) và chuyển sang giờ
        double minutes = Math.max(0, ChronoUnit.MINUTES.between(session.getStartTime(), endTime));
        double hours = minutes / 60.0;

        // 4️⃣ Lấy công suất danh định ratedKW
        //    - Ưu tiên lấy từ ChargingPoint của slot đầu tiên
        //    - Nếu không có thì dùng default 11kW
        double ratedKW = 11.0;
        if (b != null && b.getBookingSlots() != null && !b.getBookingSlots().isEmpty()) {
            var bs0 = b.getBookingSlots().get(0);
            if (bs0.getSlot() != null && bs0.getSlot().getChargingPoint() != null) {
                Double p = bs0.getSlot().getChargingPoint().getMaxPowerKW();
                if (p != null && p > 0) ratedKW = p;
            }
        }

        // 5️⃣ Hiệu suất sạc (đã tính tổn hao)
        double efficiency = 0.90;

        // 6️⃣ Ước lượng điện năng nạp được (kWh) = giờ * kW * hiệu suất
        double estEnergy = round2(hours * ratedKW * efficiency);

        // 7️⃣ Chuyển đổi từ kWh sang % pin: (estEnergy / capKWh) * 100
        int estFinal = (int) Math.round(initial + (estEnergy / capKWh) * 100.0);

        // 8️⃣ Nếu có thời gian sạc > 0 mà % không đổi -> tăng tối thiểu 1% cho hợp lý
        if (minutes > 0 && estFinal == initial) estFinal = initial + 1;

        // 9️⃣ Log lại để tiện debug/monitor
        log.info("⚡ Estimating SoC: initial={} capKWh={} ratedKW={} minutes={} hours={} estEnergy={} → estFinal={}",
                initial, capKWh, ratedKW, minutes, hours, estEnergy, estFinal);

        // 🔟 Clamp kết quả trong [initial .. 100] để tránh giảm % hoặc vượt quá 100%
        return Math.min(100, Math.max(initial, estFinal));
    }

    /**
     * FORCE-COMPLETE KHÔNG TÍNH PHÍ:
     * - Dùng khi thiếu tariff hoặc lỗi billing.
     * - Hoàn tất session/booking, gửi notification cảnh báo (cost=0), không tạo invoice.
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
        // 1️⃣ Cập nhật session ở trạng thái COMPLETED, cost=0
        cs.setEndTime(cs.getEndTime() != null ? cs.getEndTime() : LocalDateTime.now());
        cs.setDurationMinutes((int) minutes);
        cs.setFinalSoc(finalSoc);
        cs.setEnergyKWh(energyKWh);
        cs.setCost(0.0);
        cs.setStatus(ChargingSessionStatus.COMPLETED);
        chargingSessionRepository.save(cs);
        //      Xoá SOC trong cache
        sessionSocCache.remove(cs.getSessionId()); // xoá cache SOC

        // 2️⃣ Booking cũng được chuyển sang COMPLETED (dù chưa billing)
        booking.setStatus(BookingStatus.COMPLETED);
        bookingService.save(booking);

        // 3️⃣ Tạo notification cảnh báo cho user: session đã kết thúc nhưng chưa tính phí
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

        // 4️⃣ Không tạo invoice khi cost=0 (tuỳ chính sách hệ thống)
        //     → Trả về DTO stopResponseMapper.mapNoBilling
        return stopResponseMapper.mapNoBilling(cs, booking, pointNumber);
    }

    // Helper: tính thời gian bắt đầu window cho giao dịch TX (similar với resolveWindowStart ở service)
    private LocalDateTime resolveWindowStartForTx(Booking booking) {
        if (booking.getScheduledStartTime() != null) return booking.getScheduledStartTime();
        return booking.getBookingSlots().stream()
                .map(bs -> bs.getSlot().getDate().with(bs.getSlot().getTemplate().getStartTime()))
                .min(LocalDateTime::compareTo)
                .orElseThrow(() -> new ErrorException("Booking has no slot start time"));
    }

    // Helper: tính thời gian kết thúc window cho giao dịch TX
    private LocalDateTime resolveWindowEndForTx(Booking booking) {
        if (booking.getScheduledEndTime() != null) return booking.getScheduledEndTime();
        return booking.getBookingSlots().stream()
                .map(bs -> bs.getSlot().getDate().with(bs.getSlot().getTemplate().getEndTime()))
                .max(LocalDateTime::compareTo)
                .orElseThrow(() -> new ErrorException("Booking has no slot end time"));
    }

    /** Lấy số phút mỗi slot (giả định đồng nhất theo template) */
    private long getSlotMinutes(Booking booking) {
        // 1️⃣ Lấy bất kỳ BookingSlot nào trong booking (giả định tất cả dùng chung template)
        var any = booking.getBookingSlots().stream()
                .findFirst()
                .orElseThrow(() -> new ErrorException("No slot found for booking"));
        var tpl = any.getSlot().getTemplate();
        var start = tpl.getStartTime();
        var end   = tpl.getEndTime();
        // 2️⃣ Thời lượng 1 slot = chênh lệch phút giữa startTime và endTime
        return ChronoUnit.MINUTES.between(start, end);
    }

    /**
     * releaseUnusedFutureSlots:
     * - Khi driver/staff dừng sớm, những slot có startTime >= endTime
     *   được coi là chưa sử dụng -> trả về AVAILABLE để người khác có thể đặt.
     */
    private void releaseUnusedFutureSlots(Booking booking, LocalDateTime endTime) {
        if (booking.getBookingSlots() == null) return;

        booking.getBookingSlots().forEach(bs -> {
            SlotAvailability slot = bs.getSlot();
            LocalDateTime slotStart = slot.getDate().with(slot.getTemplate().getStartTime());
            // Nếu kết thúc <= thời điểm bắt đầu slot -> slot này chưa bị sử dụng, giải phóng
            if (!endTime.isAfter(slotStart)) { // endTime <= slotStart
                slot.setStatus(SlotStatus.AVAILABLE);
                slotAvailabilityService.save(slot);
                log.info("[RELEASE SLOT] bookingId={} slotId={} released (endTime={} <= slotStart={})",
                        booking.getBookingId(), slot.getSlotId(), endTime, slotStart);
            }
        });
    }
}
