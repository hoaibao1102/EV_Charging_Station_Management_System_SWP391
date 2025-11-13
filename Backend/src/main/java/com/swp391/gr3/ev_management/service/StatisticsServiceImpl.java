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
@Transactional(readOnly = true) // Mặc định tất cả method chỉ đọc (không ghi DB) → tối ưu và an toàn hơn
@RequiredArgsConstructor // Lombok tạo constructor với tất cả field final để DI
public class StatisticsServiceImpl implements StatisticsService {

    // Tham số mặc định để ước lượng thời lượng 1 phiên sạc (phút) khi tính utilization
    private static final int DEFAULT_AVG_SESSION_MINUTES = 45;

    // ====== Các service/phụ thuộc được inject ======
    private final InvoiceService invoiceService;                     // Làm việc với Invoice (doanh thu)
    private final ChargingSessionService chargingSessionService;     // Thống kê phiên sạc (energy, số session)
    private final ChargingStationService chargingStationService;     // Lấy danh sách các trạm sạc
    private final UserService userService;                           // Đếm tổng số user
    private final DriverService driverService;                       // Thống kê Driver
    private final StaffService staffService;                         // Thống kê Staff
    private final StatisticsResponseMapper statisticsResponseMapper; // Mapper cho response thống kê tổng

    /**
     * Tổng hợp dữ liệu cho Dashboard:
     * - Tổng doanh thu / năng lượng / số session / trung bình mỗi session
     * - Doanh thu theo ngày / tuần / tháng / năm hiện tại
     * - KPI chi tiết theo từng trạm (doanh thu, số session, utilization, growth%)
     */
    @Override
    public DashboardStatsResponse getDashboard() {
        // ------ 1) Xây dựng các mốc thời gian (day/week/month/year hiện tại) ------

        // Lấy ZoneId hệ thống (thường cấu hình là Asia/Ho_Chi_Minh)
        ZoneId zone = ZoneId.systemDefault();
        // "today" theo zone hiện tại
        LocalDate today = LocalDate.now(zone);

        // Khoảng ngày hôm nay: [00:00, 23:59:59.999999999]
        LocalDateTime dayFrom   = today.atStartOfDay();
        LocalDateTime dayTo     = today.plusDays(1).atStartOfDay().minusNanos(1);

        // Tính thứ Hai đầu tuần hiện tại (ISO: MONDAY → start-of-week)
        LocalDate weekStart     = today.with(DayOfWeek.MONDAY);
        // Khoảng tuần hiện tại: [Thứ 2 00:00, Chủ nhật 23:59:59.999999999]
        LocalDateTime weekFrom  = weekStart.atStartOfDay();
        LocalDateTime weekTo    = weekFrom.plusDays(7).minusNanos(1);

        // Tháng hiện tại: từ ngày 1 đến hết ngày cuối tháng
        LocalDate monthStart    = today.withDayOfMonth(1);
        LocalDateTime monthFrom = monthStart.atStartOfDay();
        LocalDateTime monthTo   = monthFrom.plusMonths(1).minusNanos(1);

        // Năm hiện tại: từ ngày đầu tiên trong năm đến hết ngày cuối năm
        LocalDate yearStart     = today.withDayOfYear(1);
        LocalDateTime yearFrom  = yearStart.atStartOfDay();
        LocalDateTime yearTo    = yearFrom.plusYears(1).minusNanos(1);

        // ------ 2) Tháng trước (previous month) để tính tăng trưởng MoM ------
        // prevMonthFrom: bắt đầu từ tháng trước, cùng ngày với monthFrom nhưng lùi 1 tháng
        LocalDateTime prevMonthFrom = monthFrom.minusMonths(1);
        // prevMonthTo: ngay trước thời điểm monthFrom (tức là cuối tháng trước)
        LocalDateTime prevMonthTo   = monthFrom.minusNanos(1);

        // ------ 3) Các tổng số liệu toàn hệ thống ------

        // Tổng doanh thu từ tất cả Invoice (toàn thời gian)
        double totalRevenue  = invoiceService.sumAll();
        // Tổng năng lượng sạc (kWh) của tất cả session
        double totalEnergy   = chargingSessionService.sumEnergyAll();
        // Tổng số phiên sạc
        long totalSessions   = chargingSessionService.countAll();
        // Doanh thu trung bình trên mỗi session (nếu chưa có session nào thì = 0)
        double avgPerSession = totalSessions == 0 ? 0.0 : (totalRevenue / totalSessions);

        // ------ 4) Doanh thu theo từng khoảng thời gian ------

        double dayRevenue    = invoiceService.sumAmountBetween(dayFrom, dayTo);
        double weekRevenue   = invoiceService.sumAmountBetween(weekFrom, weekTo);
        double monthRevenue  = invoiceService.sumAmountBetween(monthFrom, monthTo);
        double yearRevenue   = invoiceService.sumAmountBetween(yearFrom, yearTo);

        // ------ 5) Thống kê chi tiết theo từng trạm (Station KPI rows) ------

        // Lấy tất cả trạm, sau đó map từng trạm thành StationKpiRowDto
        List<StationKpiRowDto> rows = chargingStationService.findAll().stream().map(st -> {
            // 5.1) Doanh thu trạm theo ngày/tuần/tháng/năm
            double rDay   = invoiceService.sumByStationBetween(st.getStationId(), dayFrom, dayTo);
            double rWeek  = invoiceService.sumByStationBetween(st.getStationId(), weekFrom, weekTo);
            double rMonth = invoiceService.sumByStationBetween(st.getStationId(), monthFrom, monthTo);
            double rYear  = invoiceService.sumByStationBetween(st.getStationId(), yearFrom, yearTo);

            // 5.2) Doanh thu tháng trước cho trạm này (để tính % tăng trưởng MoM)
            double prevMonthRevenue = invoiceService.sumByStationBetween(
                    st.getStationId(), prevMonthFrom, prevMonthTo);

            // 5.3) Tính % tăng trưởng doanh thu tháng hiện tại so với tháng trước
            double growthPercent = calcGrowthPercent(rMonth, prevMonthRevenue);

            // 5.4) Số phiên sạc trong năm cho trạm (để ước lượng utilization)
            long sessionsYear = chargingSessionService.countByStationBetween(st.getStationId(), yearFrom, yearTo);

            // 5.5) Ước lượng mức độ sử dụng (utilization) theo % trong năm
            double utilizationPercent = estimateUtilizationPercent(st, sessionsYear);

            // 5.6) Lấy tên trạm an toàn (tránh NullPointer)
            String stationName = safeStationName(st);

            // 5.7) Gán nhãn xu hướng theo growthPercent (Tốt / Khá / Ổn / Chậm)
            String trendStatus = growthLabel(growthPercent);

            // 5.8) Build DTO cho từng trạm
            return StationKpiRowDto.builder()
                    .stationId(st.getStationId())
                    .stationName(stationName)
                    .dayRevenue(money(rDay, stationCurrency(st)))     // bọc doanh thu trong MoneyDto
                    .weekRevenue(money(rWeek, stationCurrency(st)))
                    .monthRevenue(money(rMonth, stationCurrency(st)))
                    .yearRevenue(money(rYear, stationCurrency(st)))
                    .sessions(sessionsYear)
                    .utilization(utilizationPercent)
                    .growthPercent(round1(growthPercent))             // làm tròn 1 chữ số thập phân
                    .status(trendStatus)
                    .build();
        }).toList();

        // ------ 6) Dùng mapper để build DashboardStatsResponse tổng hợp cho FE ------
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
                baseCurrency() // currency mặc định toàn hệ thống
        );
    }

    /**
     * Thống kê tổng quan user/driver/staff:
     * - Tổng số user
     * - Tổng số driver, số driver đang ACTIVE
     * - Tổng số staff, số staff đang ACTIVE
     */
    @Override
    public UserTotalsResponse getTotals() {
        long totalUsers    = userService.count();
        long totalDrivers  = driverService.count();
        long activeDrivers = driverService.countByStatus(DriverStatus.ACTIVE);
        long totalStaffs   = staffService.count();
        long activeStaffs  = staffService.countByStatus(StaffStatus.ACTIVE);

        return statisticsResponseMapper.toUserTotalsResponse(
                totalUsers, totalDrivers, activeDrivers, totalStaffs, activeStaffs
        );
    }

    // ===== Helpers =====

    /**
     * Tạo MoneyDto từ amount + currency.
     * Nếu currency null thì dùng baseCurrency() (VND).
     */
    private MoneyDto money(double amount, String currency) {
        return MoneyDto.builder()
                .amount(amount)
                .currency(currency == null ? baseCurrency() : currency)
                .build();
    }

    /**
     * Đơn vị tiền tệ mặc định cho toàn hệ thống (hiện tại là VND).
     */
    private String baseCurrency() {
        return "VND";
    }

    /**
     * Currency theo từng trạm.
     * Hiện tại luôn trả về baseCurrency(), nhưng method tách riêng để dễ mở rộng sau này
     * (ví dụ: trạm ở quốc gia khác dùng USD/EUR,...).
     */
    private String stationCurrency(ChargingStation st) {
        return baseCurrency();
    }

    /**
     * Lấy tên trạm an toàn, tránh NPE.
     * Thử gọi getStationName() 2 lần trong try-catch; nếu vẫn lỗi → fallback "Station #id".
     */
    private String safeStationName(ChargingStation st) {
        try { return st.getStationName(); }
        catch (Exception ignore) { /* fallthrough */ }
        try { return st.getStationName(); }
        catch (Exception ignore) { /* fallthrough */ }
        return "Station #" + st.getStationId();
    }

    /**
     * Tính % tăng trưởng doanh thu tháng hiện tại so với tháng trước:
     *   growth(%) = (current - previous) / previous * 100
     * - Nếu previous <= 0:
     *   + current > 0 → coi như 100% (tăng mạnh từ 0)
     *   + current = 0 → 0% (không tăng)
     */
    private double calcGrowthPercent(double current, double previous) {
        if (previous <= 0) return current > 0 ? 100.0 : 0.0; // tránh chia 0
        return (current - previous) * 100.0 / previous;
    }

    /**
     * Gán nhãn cho % tăng trưởng để hiển thị giao diện:
     * - >= 20% : "Tốt"
     * - >= 10% : "Khá"
     * - >  0%  : "Ổn"
     * - còn lại: "Chậm"
     */
    private String growthLabel(double growthPercent) {
        if (growthPercent >= 20) return "Tốt";
        if (growthPercent >= 10) return "Khá";
        if (growthPercent > 0)  return "Ổn";
        return "Chậm";
    }

    /**
     * Làm tròn 1 chữ số thập phân.
     * Ví dụ: 25.56 → 25.6
     */
    private double round1(double val) {
        return Math.round(val * 10.0) / 10.0;
    }

    /**
     * Ước tính % utilization của trạm trong 1 năm:
     *   utilization = (tổng phút sạc ước tính / tổng phút khả dụng của tất cả cổng) * 100
     *
     * - minutesCharged ~ sessionsInYear * DEFAULT_AVG_SESSION_MINUTES
     * - minutesCapacityYear ~ sốConnector * 365 * 24 * 60
     *
     * Lưu ý:
     *  - Đây là cách ước lượng nếu không có duration thực tế. Nếu có dữ liệu durationMinutes thực,
     *    nên thay bằng sum(durationMinutes) để chính xác hơn.
     */
    private double estimateUtilizationPercent(ChargingStation st, long sessionsInYear) {
        int connectors = 1;
        try {
            // Nếu station có danh sách points → lấy size, đảm bảo >= 1
            connectors = Math.max(1, st.getPoints() == null ? 1 : st.getPoints().size());
        } catch (Exception e) {
            // Nếu lần đầu lỗi thì thử lại 1 lần, nếu vẫn lỗi thì fallback = 1
            try {
                connectors = Math.max(1, st.getPoints() == null ? 1 : st.getPoints().size());
            } catch (Exception ignore) {
                connectors = 1;
            }
        }

        // Tổng phút sạc ước tính trong năm (dựa trên số sessions và avg duration)
        double minutesCharged = sessionsInYear * DEFAULT_AVG_SESSION_MINUTES;
        // Tổng phút khả dụng trong năm của tất cả connector
        double minutesCapacityYear = connectors * 365.0 * 24.0 * 60.0;

        // Tính %: (phút sử dụng / phút capacity) * 100
        double percent = (minutesCharged / Math.max(1.0, minutesCapacityYear)) * 100.0;

        // Clamp kết quả về [0, 100]
        if (percent < 0) percent = 0;
        if (percent > 100) percent = 100;
        return percent;
    }
}
