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
    private final ChargingSessionRepository sessionRepository;     // Truy vấn & lưu ChargingSession
    private final BookingsRepository bookingsRepository;           // Cập nhật trạng thái Booking liên quan
    private final TariffRepository tariffRepository;               // Lấy biểu giá (tariff) theo connector & thời gian
    private final InvoiceRepository invoiceRepository;             // Lưu & kiểm tra hoá đơn
    private final NotificationsRepository notificationsRepository; // Tạo Notification cho user
    private final SessionSocCache sessionSocCache;                 // Cache SOC tạm thời theo session
    private final ApplicationEventPublisher eventPublisher;        // Publish event (vd: gửi email)
    private final StopCharSessionResponseMapper stopResponseMapper;// Map entity -> DTO phản hồi

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
    public StopCharSessionResponse stopSessionInternalTx(Long sessionId, Integer finalSocIfAny, LocalDateTime endTime) {
        // 1) Tải session kèm booking/vehicle/driver/user để có đủ thông tin tính toán & thông báo
        ChargingSession cs = sessionRepository
                .findByIdWithBookingVehicleDriverUser(sessionId)
                .orElseThrow(() -> new ErrorException("Session not found"));

        // 2) Chỉ dừng khi session đang hoạt động
        if (cs.getStatus() != ChargingSessionStatus.IN_PROGRESS) {
            throw new ErrorException("Session is not currently active");
        }

        Booking booking = cs.getBooking();
        User user = booking.getVehicle().getDriver().getUser();

        // 3) SOC ban đầu là bắt buộc để tính delta SOC
        Integer initialSoc = Optional.ofNullable(cs.getInitialSoc())
                .orElseThrow(() -> new ErrorException("Initial SoC not recorded"));

        // 4) Xác định SOC cuối:
        //    - Nếu finalSocIfAny != null -> dùng (được clamp 0..100)
        //    - Ngược lại -> ước lượng theo thời gian sạc & công suất (estimateFinalSoc)
        int finalSoc = (finalSocIfAny != null) ? clampSoc(finalSocIfAny) : estimateFinalSoc(cs, endTime);
        if (finalSoc < initialSoc) finalSoc = initialSoc; // Không cho giảm so với initial

        // 5) Tính năng lượng tiêu thụ (kWh) từ phần trăm pin thay đổi
        double batteryCapacityKWh = booking.getVehicle().getModel().getBatteryCapacityKWh();
        double deltaSoc = finalSoc - initialSoc;
        double energyKWh = round2((deltaSoc / 100.0) * batteryCapacityKWh);

        // 6) Tính thời lượng phiên (phút)
        long minutes = Math.max(0, ChronoUnit.MINUTES.between(cs.getStartTime(), endTime));

        // 7) Lấy thông tin điểm sạc & connector (phục vụ thông báo & lấy biểu giá)
        var firstSlot = booking.getBookingSlots().stream()
                .findFirst()
                .orElseThrow(() -> new ErrorException("No slot found for booking"));
        ChargingPoint point = firstSlot.getSlot().getChargingPoint();
        String pointNumber = (point != null) ? point.getPointNumber() : "Unknown";
        ConnectorType connectorType = (point != null && point.getConnectorType() != null)
                ? point.getConnectorType()
                : booking.getVehicle().getModel().getConnectorType();

        // 8) Lấy tariff có hiệu lực tại endTime (pricingTime). Nếu không có -> fallback method khác
        LocalDateTime pricingTime = endTime;
        Tariff tariff = tariffRepository
                .findTopByConnectorType_ConnectorTypeIdAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqualOrderByEffectiveFromDesc(
                        connectorType.getConnectorTypeId(), pricingTime, pricingTime)
                .orElseGet(() -> {
                    // fallback: method tuỳ biến để tìm tariff “active” tại thời điểm pricingTime
                    return tariffRepository.findActiveByConnectorType(connectorType.getConnectorTypeId(), pricingTime)
                            .stream().findFirst().orElse(null);
                });

        // 9) Nếu không có tariff phù hợp → force complete (cost=0) và cảnh báo để admin xử lý sau
        if (tariff == null) {
            log.warn("[STOP] No active tariff for connectorTypeId={} at {}. Force complete with cost=0.",
                    connectorType.getConnectorTypeId(), pricingTime);
            return forceCompleteWithoutBilling(cs, booking, user, pointNumber, initialSoc, finalSoc, energyKWh, minutes);
        }

        // 10) Tính chi phí = đơn giá * điện năng
        double pricePerKWh = tariff.getPricePerKWh();
        double cost = round2(pricePerKWh * energyKWh);

        // 11) Ghi nhận kết quả vào session
        cs.setEndTime(endTime);
        cs.setDurationMinutes((int) minutes);
        cs.setFinalSoc(finalSoc);
        cs.setEnergyKWh(energyKWh);
        cs.setCost(cost);
        cs.setStatus(ChargingSessionStatus.COMPLETED);
        sessionRepository.save(cs);

        // Xoá SOC cache vì phiên đã hoàn tất
        sessionSocCache.remove(cs.getSessionId());

        // 12) Cập nhật trạng thái booking -> COMPLETED
        booking.setStatus(BookingStatus.COMPLETED);
        bookingsRepository.save(booking);

        // 13) Tạo Notification cho user về kết quả sạc
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

        // 14) Tạo hoá đơn (Invoice): chặn nếu đã tồn tại trước đó
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

        // 15) Trả về DTO đã map (có kèm thông tin tariff)
        return stopResponseMapper.mapWithTariff(cs, booking, pointNumber, tariff);
    }

    /**
     * AUTO-STOP (TX độc lập):
     * - Khi tới thời điểm windowEnd, nếu session vẫn IN_PROGRESS thì dừng.
     * - Cố gắng dừng chuẩn (tính tiền); nếu lỗi, fallback force-complete (cost=0) để giải phóng tài nguyên.
     */
    @Transactional
    public void autoStopIfStillRunningTx(Long sessionId, LocalDateTime windowEnd) {
        // 1) Kiểm tra session còn tồn tại không
        var opt = sessionRepository.findById(sessionId);
        if (opt.isEmpty()) return;

        var session = opt.get();
        // 2) Chỉ xử lý auto-stop nếu session vẫn đang IN_PROGRESS
        if (session.getStatus() != ChargingSessionStatus.IN_PROGRESS) return;

        // 3) Lấy SOC cuối từ cache (nếu có & khác initial thì dùng)
        Integer cachedSoc = sessionSocCache.get(sessionId).orElse(null);
        Integer finalSocIfAny = (cachedSoc != null && !cachedSoc.equals(session.getInitialSoc()))
                ? clampSoc(cachedSoc)
                : null;

        try {
            // 4) Dừng session bằng flow chuẩn (tính phí theo tariff)
            log.info("[AUTO-STOP] sessionId={} windowEnd={} startTime={} initialSoc={} cachedSoc={}",
                    sessionId, windowEnd, session.getStartTime(), session.getInitialSoc(), cachedSoc);
            stopSessionInternalTx(sessionId, finalSocIfAny, windowEnd);
        } catch (Exception ex) {
            // 5) Nếu dừng chuẩn thất bại -> fallback: kết thúc không tính phí
            log.error("[AUTO-STOP] Failed for sessionId={} at {}: {}", sessionId, windowEnd, ex.getMessage(), ex);

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

                // Force-complete để không giữ tài nguyên, tạo cảnh báo để xử lý sau
                forceCompleteWithoutBilling(cs, booking, user, pointNumber,
                        cs.getInitialSoc(), finalSoc,
                        round2(((finalSoc - cs.getInitialSoc()) / 100.0) * booking.getVehicle().getModel().getBatteryCapacityKWh()),
                        minutes);
            } catch (Exception nested) {
                // 6) Nếu fallback cũng lỗi -> log lại để điều tra
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
        int initial = Optional.ofNullable(session.getInitialSoc()).orElse(20);
        Booking b = session.getBooking();

        // Dung lượng pin (kWh) — fallback 60kWh nếu thiếu thông tin
        double capKWh = (b != null && b.getVehicle() != null && b.getVehicle().getModel() != null)
                ? b.getVehicle().getModel().getBatteryCapacityKWh()
                : 60.0;

        // Thời lượng sạc (giờ)
        double minutes = Math.max(0, ChronoUnit.MINUTES.between(session.getStartTime(), endTime));
        double hours = minutes / 60.0;

        // Công suất danh định (kW) — cố gắng lấy từ ChargingPoint, fallback 11kW
        double ratedKW = 11.0;
        if (b != null && b.getBookingSlots() != null && !b.getBookingSlots().isEmpty()) {
            var bs0 = b.getBookingSlots().get(0);
            if (bs0.getSlot() != null && bs0.getSlot().getChargingPoint() != null) {
                Double p = bs0.getSlot().getChargingPoint().getMaxPowerKW();
                if (p != null && p > 0) ratedKW = p;
            }
        }

        // Hiệu suất (tổn hao)
        double efficiency = 0.90;

        // Điện năng ước lượng (kWh) = giờ * kW * hiệu suất
        double estEnergy = round2(hours * ratedKW * efficiency);

        // Chuyển sang % pin
        int estFinal = (int) Math.round(initial + (estEnergy / capKWh) * 100.0);

        // Nếu có thời gian sạc >0 mà % không đổi -> tăng tối thiểu 1%
        if (minutes > 0 && estFinal == initial) estFinal = initial + 1;

        log.info("⚡ Estimating SoC: initial={} capKWh={} ratedKW={} minutes={} hours={} estEnergy={} → estFinal={}",
                initial, capKWh, ratedKW, minutes, hours, estEnergy, estFinal);

        // Clamp trong  [initial .. 100]
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
        // 1) Cập nhật session với cost=0 & COMPLETED
        cs.setEndTime(cs.getEndTime() != null ? cs.getEndTime() : LocalDateTime.now());
        cs.setDurationMinutes((int) minutes);
        cs.setFinalSoc(finalSoc);
        cs.setEnergyKWh(energyKWh);
        cs.setCost(0.0);
        cs.setStatus(ChargingSessionStatus.COMPLETED);
        sessionRepository.save(cs);
        sessionSocCache.remove(cs.getSessionId()); // xoá cache SOC

        // 2) Booking cũng hoàn tất
        booking.setStatus(BookingStatus.COMPLETED);
        bookingsRepository.save(booking);

        // 3) Gửi thông báo cảnh báo đến user (để admin thấy và xử lý sau)
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

        // 4) Không tạo invoice khi cost=0 (tuỳ chính sách hệ thống). Trả về DTO tương ứng
        return stopResponseMapper.mapNoBilling(cs, booking, pointNumber);
    }
}
