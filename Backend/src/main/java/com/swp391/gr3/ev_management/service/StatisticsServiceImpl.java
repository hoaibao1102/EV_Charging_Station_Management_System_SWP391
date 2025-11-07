package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.response.*;
import com.swp391.gr3.ev_management.entity.ChargingStation;
import com.swp391.gr3.ev_management.enums.DriverStatus;
import com.swp391.gr3.ev_management.enums.StaffStatus;
import com.swp391.gr3.ev_management.mapper.DashboardStatsMapper;
import com.swp391.gr3.ev_management.mapper.StatisticsResponseMapper;
import com.swp391.gr3.ev_management.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private static final int DEFAULT_AVG_SESSION_MINUTES = 45;

    private final InvoiceRepository invoiceRepo;
    private final ChargingSessionRepository sessionRepo;
    private final ChargingStationRepository chargingStationRepository;
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final StaffsRepository staffsRepository;
    private final StatisticsResponseMapper statisticsResponseMapper;

    @Override
    public DashboardStatsResponse getDashboard() {
        // ------ Build time ranges ------
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);

        LocalDateTime dayFrom   = today.atStartOfDay();
        LocalDateTime dayTo     = today.plusDays(1).atStartOfDay().minusNanos(1);

        LocalDate weekStart     = today.with(DayOfWeek.MONDAY);
        LocalDateTime weekFrom  = weekStart.atStartOfDay();
        LocalDateTime weekTo    = weekFrom.plusDays(7).minusNanos(1);

        LocalDate monthStart    = today.withDayOfMonth(1);
        LocalDateTime monthFrom = monthStart.atStartOfDay();
        LocalDateTime monthTo   = monthFrom.plusMonths(1).minusNanos(1);

        LocalDate yearStart     = today.withDayOfYear(1);
        LocalDateTime yearFrom  = yearStart.atStartOfDay();
        LocalDateTime yearTo    = yearFrom.plusYears(1).minusNanos(1);

        // ------ Previous month range (for growth) ------
        LocalDateTime prevMonthFrom = monthFrom.minusMonths(1);
        LocalDateTime prevMonthTo   = monthFrom.minusNanos(1);

        // ------ Totals ------
        double totalRevenue  = invoiceRepo.sumAll();
        double totalEnergy   = sessionRepo.sumEnergyAll();
        long totalSessions   = sessionRepo.countAll();
        double avgPerSession = totalSessions == 0 ? 0.0 : (totalRevenue / totalSessions);

        // ------ Period revenues ------
        double dayRevenue    = invoiceRepo.sumAmountBetween(dayFrom, dayTo);
        double weekRevenue   = invoiceRepo.sumAmountBetween(weekFrom, weekTo);
        double monthRevenue  = invoiceRepo.sumAmountBetween(monthFrom, monthTo);
        double yearRevenue   = invoiceRepo.sumAmountBetween(yearFrom, yearTo);

        // ------ Per-station rows ------
        List<StationKpiRowDto> rows = chargingStationRepository.findAll().stream().map(st -> {
            double rDay   = invoiceRepo.sumByStationBetween(st.getStationId(), dayFrom, dayTo);
            double rWeek  = invoiceRepo.sumByStationBetween(st.getStationId(), weekFrom, weekTo);
            double rMonth = invoiceRepo.sumByStationBetween(st.getStationId(), monthFrom, monthTo);
            double rYear  = invoiceRepo.sumByStationBetween(st.getStationId(), yearFrom, yearTo);

            // Doanh thu tháng trước để tính tăng trưởng MoM
            double prevMonthRevenue = invoiceRepo.sumByStationBetween(
                    st.getStationId(), prevMonthFrom, prevMonthTo);

            double growthPercent = calcGrowthPercent(rMonth, prevMonthRevenue);

            long sessionsYear = sessionRepo.countByStationBetween(st.getStationId(), yearFrom, yearTo);
            double utilizationPercent = estimateUtilizationPercent(st, sessionsYear);

            String stationName = safeStationName(st);

            // trạng thái hiển thị theo growth %
            String trendStatus = growthLabel(growthPercent);

            return StationKpiRowDto.builder()
                    .stationId(st.getStationId())
                    .stationName(stationName)
                    .dayRevenue(money(rDay, stationCurrency(st)))
                    .weekRevenue(money(rWeek, stationCurrency(st)))
                    .monthRevenue(money(rMonth, stationCurrency(st)))
                    .yearRevenue(money(rYear, stationCurrency(st)))
                    .sessions(sessionsYear)
                    .utilization(utilizationPercent)
                    .growthPercent(round1(growthPercent))
                    .status(trendStatus)
                    .build();
        }).toList();

        return DashboardStatsMapper.toDashboardStatsResponse(
                totalRevenue,
                totalEnergy,
                totalSessions,
                avgPerSession,
                dayRevenue,
                weekRevenue,
                monthRevenue,
                yearRevenue,
                rows,
                baseCurrency()
        );
    }

    @Override
    public UserTotalsResponse getTotals() {
        long totalUsers    = userRepository.count();
        long totalDrivers  = driverRepository.count();
        long activeDrivers = driverRepository.countByStatus(DriverStatus.ACTIVE);
        long totalStaffs   = staffsRepository.count();
        long activeStaffs  = staffsRepository.countByStatus(StaffStatus.ACTIVE);

        return statisticsResponseMapper.toUserTotalsResponse(
                totalUsers, totalDrivers, activeDrivers, totalStaffs, activeStaffs
        );
    }

    // ===== Helpers =====

    private MoneyDto money(double amount, String currency) {
        return MoneyDto.builder()
                .amount(amount)
                .currency(currency == null ? baseCurrency() : currency)
                .build();
    }

    private String baseCurrency() {
        return "VND";
    }

    private String stationCurrency(ChargingStation st) {
        return baseCurrency();
    }

    private String safeStationName(ChargingStation st) {
        try { return st.getStationName(); }
        catch (Exception ignore) { /* fallthrough */ }
        try { return st.getStationName(); }
        catch (Exception ignore) { /* fallthrough */ }
        return "Station #" + st.getStationId();
    }

    /** Tính % tăng trưởng MoM: ((current - previous)/previous)*100 */
    private double calcGrowthPercent(double current, double previous) {
        if (previous <= 0) return current > 0 ? 100.0 : 0.0; // tránh chia 0
        return (current - previous) * 100.0 / previous;
    }

    /** Nhãn trạng thái theo % tăng trưởng (giống UI: Tốt/Khá/Chậm) */
    private String growthLabel(double growthPercent) {
        if (growthPercent >= 20) return "Tốt";
        if (growthPercent >= 10) return "Khá";
        if (growthPercent > 0)  return "Ổn";
        return "Chậm";
    }

    /** Làm tròn 1 chữ số thập phân (cho đẹp UI: 25,5%) */
    private double round1(double val) {
        return Math.round(val * 10.0) / 10.0;
    }

    /**
     * Ước tính utilization theo năm: (tổng phút sạc ước tính / (số cổng * tổng phút trong năm)).
     * Nếu bạn có dữ liệu duration thực, hãy thay bằng SUM(durationMinutes) ở repository.
     */
    private double estimateUtilizationPercent(ChargingStation st, long sessionsInYear) {
        int connectors = 1;
        try {
            connectors = Math.max(1, st.getPoints() == null ? 1 : st.getPoints().size());
        } catch (Exception e) {
            try {
                connectors = Math.max(1, st.getPoints() == null ? 1 : st.getPoints().size());
            } catch (Exception ignore) {
                connectors = 1;
            }
        }

        double minutesCharged = sessionsInYear * DEFAULT_AVG_SESSION_MINUTES;
        double minutesCapacityYear = connectors * 365.0 * 24.0 * 60.0;

        double percent = (minutesCharged / Math.max(1.0, minutesCapacityYear)) * 100.0;
        if (percent < 0) percent = 0;
        if (percent > 100) percent = 100;
        return percent;
    }
}
