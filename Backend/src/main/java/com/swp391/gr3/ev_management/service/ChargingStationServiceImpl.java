package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.ChargingStationRequest;
import com.swp391.gr3.ev_management.dto.response.ChargingStationResponse;
import com.swp391.gr3.ev_management.entity.ChargingStation;
import com.swp391.gr3.ev_management.enums.ChargingStationStatus;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.mapper.ChargingStationMapper;
import com.swp391.gr3.ev_management.repository.ChargingStationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service // Đánh dấu class là một Spring Service (xử lý logic nghiệp vụ về trạm sạc)
@RequiredArgsConstructor // Tự động sinh constructor chứa các field final
public class ChargingStationServiceImpl implements ChargingStationService {

    // ====== Dependencies được inject qua constructor ======
    private final ChargingStationRepository chargingStationRepository; // Repository thao tác DB bảng ChargingStation (CRUD, query tùy chỉnh)
    private final ChargingStationMapper chargingStationMapper;         // Mapper chuyển đổi giữa Entity và DTO (request/response)

    /**
     * Tìm trạm sạc theo ID.
     * - Gọi repository để lấy ChargingStation từ DB.
     * - Map entity sang DTO response để trả về.
     */
    @Override
    public ChargingStationResponse findByStationId(long id) {
        // Lấy thông tin trạm theo ID (custom finder, không dùng Optional)
        // Nếu không tìm thấy thì trả về null (mapper cần handle null nếu có)
        ChargingStation station = chargingStationRepository.findByStationId(id);

        // Map entity sang DTO response (có thể ẩn bớt các field nhạy cảm, hoặc định dạng lại)
        return chargingStationMapper.toResponse(station);
    }

    /**
     * Lấy danh sách tất cả các trạm sạc trong hệ thống.
     * - Gọi repository findAll().
     * - Map danh sách entity sang danh sách DTO bằng Stream API.
     */
    @Override
    public List<ChargingStationResponse> getAllStations() {
        // Lấy toàn bộ bản ghi ChargingStation từ DB
        return chargingStationRepository.findAll() // Lấy toàn bộ danh sách từ DB
                .stream() // Chuyển sang Stream để có thể map từng phần tử
                // Với mỗi ChargingStation entity -> map sang ChargingStationResponse DTO
                .map(chargingStationMapper::toResponse) // Map từng entity -> DTO
                // Thu kết quả stream về List<ChargingStationResponse>
                .collect(Collectors.toList()); // Gom lại thành danh sách
    }

    /**
     * Tạo mới một trạm sạc.
     * - Map dữ liệu từ request sang entity.
     * - Đặt thời gian tạo nếu chưa có.
     * - Lưu vào DB và map sang DTO response để trả về.
     */
    @Override
    public ChargingStationResponse addChargingStation(ChargingStationRequest request) {
        // 1️⃣ Map dữ liệu từ request (ChargingStationRequest) sang entity ChargingStation
        //    - Mapper sẽ chịu trách nhiệm gán các trường: tên trạm, địa chỉ, toạ độ, status mặc định, ...
        ChargingStation station = chargingStationMapper.toEntity(request);

        // 2️⃣ Nếu entity chưa có thời gian tạo (createdAt == null) thì set là thời điểm hiện tại
        //    - Đảm bảo mọi bản ghi trạm khi lưu xuống DB đều có timestamp tạo
        if (station.getCreatedAt() == null) {
            station.setCreatedAt(LocalDateTime.now());
        }

        // 3️⃣ Lưu trạm mới vào DB (INSERT)
        //    - Sau khi save, nếu ID là auto-increment thì station sẽ có stationId mới
        ChargingStation saved = chargingStationRepository.save(station);

        // 4️⃣ Map entity đã lưu sang DTO để trả về cho client
        //    - Thường dùng DTO để che giấu các trường nội bộ, hoặc đổi tên field cho phù hợp API
        return chargingStationMapper.toResponse(saved);
    }

    /**
     * Cập nhật thông tin trạm sạc dựa theo ID.
     * - Tìm trạm hiện có trong DB.
     * - Nếu không tồn tại -> trả về null.
     * - Nếu có -> cập nhật các trường từ request và lưu lại.
     */
    @Override
    public ChargingStationResponse updateChargingStation(long id, ChargingStationRequest request) {
        // 1️⃣ Tìm trạm hiện tại theo ID (dùng custom finder)
        //     - Nếu không tồn tại sẽ trả về null (không dùng Optional ở đây)
        ChargingStation existing = chargingStationRepository.findByStationId(id);

        // 2️⃣ Nếu không tìm thấy trạm:
        //     - Hiện tại method trả về null (caller nên check null để xử lý 404 hoặc thông báo lỗi)
        //     - Tuỳ yêu cầu hệ thống, có thể đổi sang ném ErrorException để thống nhất flow
        if (existing == null) return null;

        // 3️⃣ Cập nhật các thuộc tính của entity từ request thông qua mapper
        //     - mapper.updateEntity(existing, request) sẽ set các field được phép chỉnh sửa (name, address, status,...)
        //     - Các field không được phép sửa (id, createdAt, ...) nên được giữ nguyên trong mapper
        chargingStationMapper.updateEntity(existing, request);

        // 4️⃣ Lưu bản ghi đã cập nhật vào DB (UPDATE)
        ChargingStation updated = chargingStationRepository.save(existing);

        // 5️⃣ Map entity đã update sang DTO để trả response cho client
        return chargingStationMapper.toResponse(updated);
    }

    /**
     * Cập nhật trạng thái hoạt động của trạm sạc.
     * - Kiểm tra trạm có tồn tại không.
     * - Nếu có, thay đổi status (ACTIVE, INACTIVE, MAINTENANCE, v.v...).
     * - Lưu lại và trả về DTO phản hồi.
     */
    @Override
    public ChargingStationResponse updateStationStatus(long stationId, ChargingStationStatus newStatus) {
        // 1️⃣ Tìm trạm theo ID (nếu không tồn tại -> station = null)
        ChargingStation station = chargingStationRepository.findByStationId(stationId);

        // 2️⃣ Nếu không tìm thấy trạm -> ném ErrorException để báo lỗi cho layer trên
        //    - Thay vì trả null, việc ném exception cho phép controller xử lý 404/400 rõ ràng hơn
        if (station == null) {
            throw new ErrorException("Station not found with id: " + stationId);
        }

        // ⚡ 3️⃣ Cập nhật trạng thái trạm và thời gian chỉnh sửa gần nhất
        //    - newStatus: có thể là ACTIVE, INACTIVE, MAINTENANCE,...
        //    - updatedAt: set về thời điểm hiện tại -> giúp tracking lịch sử chỉnh sửa
        station.setStatus(newStatus);
        station.setUpdatedAt(LocalDateTime.now());

        // 4️⃣ Lưu thay đổi vào DB (UPDATE)
        ChargingStation updated = chargingStationRepository.save(station);

        // 5️⃣ Map entity đã cập nhật sang DTO và trả về cho client
        return chargingStationMapper.toResponse(updated);
    }

    @Override
    public Optional<ChargingStation> findById(Long stationId) {
        // Trả về Optional<ChargingStation> để caller tự xử lý trường hợp không tìm thấy
        // (ví dụ: orElseThrow, orElse, isPresent, ...)
        return chargingStationRepository.findById(stationId);
    }

    @Override
    public ChargingStation findEntityById(Long stationId) {
        // Trả thẳng entity (có thể null) bằng custom finder
        // Dùng trong các service nội bộ khi cần làm việc trực tiếp với Entity thay vì DTO
        return chargingStationRepository.findByStationId(stationId);
    }

    @Override
    public Collection<ChargingStation> findAll() {
        // Lấy toàn bộ entity ChargingStation (không map sang DTO)
        // Thích hợp cho logic nội bộ, batch job, hoặc các dịch vụ khác muốn xử lý sâu hơn
        return chargingStationRepository.findAll();
    }
}
