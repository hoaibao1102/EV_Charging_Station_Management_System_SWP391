package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.CreateReportRequest;
import com.swp391.gr3.ev_management.dto.response.ReportResponse;
import com.swp391.gr3.ev_management.entity.ChargingStation;
import com.swp391.gr3.ev_management.entity.Report;
import com.swp391.gr3.ev_management.entity.Staffs;
import com.swp391.gr3.ev_management.entity.StationStaff;
import com.swp391.gr3.ev_management.enums.ReportStatus;
import com.swp391.gr3.ev_management.enums.StaffStatus;
import com.swp391.gr3.ev_management.exception.ConflictException;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.mapper.ReportMapper;
import com.swp391.gr3.ev_management.repository.ReportRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service // Đánh dấu đây là một Spring Service (chứa business logic liên quan tới Report/Incident)
@RequiredArgsConstructor // Lombok tạo constructor chứa các field final để DI (dependency injection)
public class ReportServiceImpl implements ReportService {

    // Repository thao tác với bảng Report trong DB (CRUD + query custom)
    private final ReportRepository reportRepository;
    // Mapper để chuyển Entity Report -> DTO ReportResponse
    private final ReportMapper mapper;
    // Service để lấy thông tin Staff dựa trên userId (mối quan hệ user - staff)
    private final StaffService staffService;

    /**
     * Tạo một Incident/Report mới do staff đang đăng nhập thực hiện.
     * Quy trình:
     *  - Lấy Staff theo userId (user đang login)
     *  - Kiểm tra Staff đang ACTIVE
     *  - Tự động lấy Station mà Staff đang được assign (StationStaff.unassignedAt == null)
     *  - Build đối tượng Report từ request
     *  - Lưu DB -> map sang DTO -> trả về
     */
    @Override
    @Transactional // Có thao tác ghi DB (save report) nên cần transaction
    public ReportResponse createIncident(Long userId, CreateReportRequest request) {
        // 1️⃣ Tìm Staff tương ứng với userId (thường lấy từ token)
        Staffs staff = staffService.findByUser_UserId(userId)
                .orElseThrow(() -> new ErrorException("Staff not found for userId: " + userId));

        // 2️⃣ Kiểm tra trạng thái Staff phải đang ACTIVE mới được tạo báo cáo
        if (staff.getStatus() != StaffStatus.ACTIVE) {
            throw new ConflictException("Staff is not active");
        }

        // 3️⃣ Lấy station tự động dựa trên StationStaff
        //    - staff.getStationStaffs(): danh sách quan hệ Staff <-> Station
        //    - filter ss.getUnassignedAt() == null: chỉ lấy những assignment đang còn hiệu lực (chưa bị unassign)
        //    - map sang ss.getStation(): lấy ra đối tượng ChargingStation
        //    - findFirst(): lấy station đầu tiên tìm được
        //    - Nếu không có station active -> ném ConflictException (staff không gắn với trạm nào)
        ChargingStation station = staff.getStationStaffs().stream()
                .filter(ss -> ss.getUnassignedAt() == null)
                .map(StationStaff::getStation)
                .findFirst()
                .orElseThrow(() -> new ConflictException("Staff is not assigned to any active station"));

        // 4️⃣ Khởi tạo entity Report mới từ dữ liệu request + staff + station
        Report report = new Report();
        report.setStaffs(staff);                        // gán staff tạo báo cáo
        report.setStation(station);                     // gán trạm xảy ra sự cố/báo cáo
        report.setTitle(request.getTitle());            // tiêu đề sự cố
        report.setDescription(request.getDescription()); // mô tả chi tiết
        report.setSeverity(request.getSeverity());      // mức độ nghiêm trọng (enum)
        report.setStatus(ReportStatus.REPORTED);        // trạng thái ban đầu: REPORTED (mới được báo cáo)
        report.setReportedAt(LocalDateTime.now());      // thời gian tạo report

        // 5️⃣ Lưu report xuống DB
        Report saved = reportRepository.save(report);

        // 6️⃣ Map entity đã lưu sang DTO để trả cho client
        return mapper.mapToReport(saved);
    }

    /**
     * Tìm một Incident/Report theo ID.
     * - Chỉ đọc nên dùng TxType.SUPPORTS (không bắt buộc mở transaction mới).
     */
    @Override
    @Transactional(Transactional.TxType.SUPPORTS) // read-only / hỗ trợ khi có TX sẵn
    public ReportResponse findById(Long incidentId) {
        // 1️⃣ Tìm Report theo incidentId; nếu không tồn tại -> ErrorException
        Report report = reportRepository.findById(incidentId)
                .orElseThrow(() -> new ErrorException("Incident not found"));
        // 2️⃣ Map sang DTO để trả về
        return mapper.mapToReport(report);
    }

    /**
     * Lấy toàn bộ Incident/Report.
     * - Dùng cho admin hoặc dashboard xem tất cả report.
     */
    @Override
    @Transactional(Transactional.TxType.SUPPORTS) // chỉ đọc
    public List<ReportResponse> findAll() {
        // 1️⃣ Lấy toàn bộ report từ DB
        return reportRepository.findAll()
                .stream()
                // 2️⃣ Map từng Report entity sang ReportResponse DTO
                .map(mapper::mapToReport)
                .toList();
    }

    /**
     * Cập nhật trạng thái của một Incident theo ID.
     * - Tìm Report
     * - Nếu có: set status từ String -> enum ReportStatus
     * - Lưu lại
     * - Nếu không: ném ErrorException
     *
     * Lưu ý: method này không có @Transactional explicit type,
     * Jakarta @Transactional trên class mặc định readOnly=true, nhưng method này không override ->
     * tuy nhiên vẫn nên giữ như code gốc (theo yêu cầu) và tin rằng môi trường cấu hình hỗ trợ ghi.
     */
    @Override
    public void updateIncidentStatus(Long incidentId, String status) {
        // 1️⃣ Tìm incident/report theo ID
        Optional<Report> incident = reportRepository.findById(incidentId);

        if (incident.isPresent()) {
            // 2️⃣ Nếu tìm thấy -> lấy entity ra
            Report existingReport = incident.get();
            // 3️⃣ Convert String status -> Enum ReportStatus bằng valueOf
            existingReport.setStatus(ReportStatus.valueOf(status));
            // 4️⃣ Lưu lại report với trạng thái mới
            reportRepository.save(existingReport);
        } else {
            // 5️⃣ Nếu không tồn tại -> ném lỗi
            throw new ErrorException("Incident not found");
        }
    }
}
