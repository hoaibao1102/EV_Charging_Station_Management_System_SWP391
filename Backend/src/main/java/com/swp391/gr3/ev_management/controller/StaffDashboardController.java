package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.dto.response.*;
import com.swp391.gr3.ev_management.entity.*;
import com.swp391.gr3.ev_management.enums.*;
import com.swp391.gr3.ev_management.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/staff/dashboard")
@RequiredArgsConstructor // ✅ Lombok tự động tạo constructor cho các field final (Dependency Injection)
public class StaffDashboardController {

    private final ChargingStationService chargingStationService;
    private final ChargingSessionService chargingSessionService;
    private final BookingService bookingService;
    private final TransactionService transactionService;
    private final ChargingPointService chargingPointService;

    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    @GetMapping("/stats")
    public ResponseEntity<StaffDashboardStatsResponse> getTodayStats() {
        // Count active stations
        long activeStations = chargingStationService.countByStatus(ChargingStationStatus.ACTIVE);

        // Count active sessions
        long activeSessions = chargingSessionService.countByStatus(ChargingSessionStatus.IN_PROGRESS);

        // Count today's bookings
        LocalDate today = LocalDate.now();
        long todayBookings = bookingService.countByCreatedAtBetween(
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        );

        // Calculate today's revenue
        double todayRevenue = transactionService.sumAmountByCreatedAtBetween(
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        );

        // Count charging points by status
        Map<String, Long> pointStats = chargingPointService
                .countGroupByStatus();

        return ResponseEntity.ok(new StaffDashboardStatsResponse(
                activeStations,
                activeSessions,
                todayBookings,
                todayRevenue,
                pointStats
        ));
    }

    @GetMapping("/stations-status")
    public ResponseEntity<List<StationStatusResponse>> getStationsStatus() {

        List<ChargingStation> stations = chargingStationService.findAll();

        List<StationStatusResponse> response = stations.stream().map(station -> {
            List<ChargingPoint> points = station.getPoints();

            long total = points.size();

            long available = points.stream()
                    .filter(p -> p.getStatus() == ChargingPointStatus.AVAILABLE)
                    .count();

            long inUse = points.stream()
                    .filter(p -> p.getStatus() == ChargingPointStatus.OCCUPIED)
                    .count();

            long maintenance = points.stream()
                    .filter(p -> p.getStatus() == ChargingPointStatus.MAINTENANCE)
                    .count();

            return new StationStatusResponse(
                    station.getStationId(),
                    station.getStationName(),
                    total,
                    available,
                    inUse,
                    maintenance,
                    station.getStatus().name()
            );
        }).toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/pending-bookings")
    public ResponseEntity<List<PendingBookingResponse>> getPendingBookings() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next2Hours = now.plusHours(2);

        List<Booking> pendingBookings = bookingService
                .findByStatusAndStartTimeBetween(
                        BookingStatus.PENDING,
                        now,
                        next2Hours
                );

        List<PendingBookingResponse> response = pendingBookings.stream()
                .map(booking -> new PendingBookingResponse(
                        booking.getBookingId(),
                        booking.getVehicle().getDriver().getUser().getName(),
                        booking.getVehicle().getVehiclePlate(),
                        booking.getStation().getStationName(),
                        booking.getScheduledStartTime(),
                        booking.getStatus()
                ))
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/recent-activities")
    public ResponseEntity<List<ActivityResponse>> getRecentActivities(
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<ActivityResponse> activities = new ArrayList<>();

        // ===== 1) Recent charging sessions =====
        List<ChargingSession> recentSessions =
                chargingSessionService.findTop5ByOrderByStartTimeDesc();

        recentSessions.forEach(session -> {
            activities.add(new ActivityResponse(
                    session.getSessionId(),                     // id
                    "SESSION_START",                            // type
                    "Phiên sạc #" + session.getSessionId()
                            + " bắt đầu tại "
                            + session.getBooking().getStation().getStationName(), // description
                    session.getStartTime(),                     // timestamp
                    "SESSION",                                  // relatedEntityType
                    session.getSessionId()                      // relatedEntityId
            ));
        });

        // ===== 2) Recent bookings =====
        List<Booking> recentBookings =
                bookingService.findTop5ByOrderByCreatedAtDesc();

        recentBookings.forEach(booking -> {
            activities.add(new ActivityResponse(
                    booking.getBookingId(),
                    "BOOKING_NEW",
                    "Booking mới #" + booking.getBookingId()
                            + " từ " + booking.getVehicle().getDriver().getUser().getName(),
                    booking.getCreatedAt(),
                    "BOOKING",
                    booking.getBookingId()
            ));
        });

        // ===== 3) Recent transactions =====
        List<Transaction> recentTransactions =
                transactionService.findTop5ByStatusOrderByCreatedAtDesc(TransactionStatus.COMPLETED);

        recentTransactions.forEach(tx -> {
            activities.add(new ActivityResponse(
                    tx.getTransactionId(),
                    "PAYMENT_SUCCESS",
                    "Thanh toán thành công " + tx.getAmount() + " VND",
                    tx.getCreatedAt(),
                    "TRANSACTION",
                    tx.getTransactionId()
            ));
        });

        // ===== 4) Sort DESC + limit =====
        List<ActivityResponse> sortedLimited =
                activities.stream()
                        .sorted(Comparator.comparing(ActivityResponse::getTimestamp).reversed())
                        .limit(limit)
                        .toList();

        return ResponseEntity.ok(sortedLimited);
    }

    @GetMapping("/chart/sessions-per-hour")
    public ResponseEntity<List<SessionsPerHourResponse>> getSessionsPerHour() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        // Lấy tất cả phiên sạc bắt đầu trong hôm nay
        List<ChargingSession> todaySessions = chargingSessionService
                .findByStartTimeBetween(startOfDay, endOfDay);

        // Group theo giờ (0–23) và đếm
        Map<Integer, Long> sessionsByHour = todaySessions.stream()
                .collect(Collectors.groupingBy(
                        session -> session.getStartTime().getHour(),
                        Collectors.counting()
                ));

        // Tạo dữ liệu theo từng khung 4 tiếng: 00:00, 04:00, 08:00, ...
        List<SessionsPerHourResponse> response = new ArrayList<>();
        for (int hour = 0; hour < 24; hour += 4) {
            response.add(new SessionsPerHourResponse(
                    String.format("%02d:00", hour),
                    sessionsByHour.getOrDefault(hour, 0L)
            ));
        }

        return ResponseEntity.ok(response);
    }
}
