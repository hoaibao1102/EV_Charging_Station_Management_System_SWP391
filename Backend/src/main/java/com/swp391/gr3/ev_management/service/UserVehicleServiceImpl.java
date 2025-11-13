package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.entity.UserVehicle;
import com.swp391.gr3.ev_management.repository.UserVehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service                           // Đánh dấu đây là Spring Service xử lý logic liên quan UserVehicle
@RequiredArgsConstructor           // Lombok tự sinh constructor cho các field final
@Slf4j                             // Tự tạo logger (log.info, log.error,...)
public class UserVehicleServiceImpl implements UserVehicleService {

    private final UserVehicleRepository userVehicleRepository;
    // Repository dùng để truy vấn bảng user_vehicle trong DB

    /**
     * Đếm số lượng UserVehicle có modelId tương ứng.
     * → Dùng cho thống kê, kiểm tra mức độ phổ biến của model.
     *
     * @param id ID của model xe
     * @return số lượng user sở hữu xe thuộc model đó
     */
    @Override
    public long countByModel_ModelId(Long id) {
        // Gọi repository để đếm số record có modelId
        return userVehicleRepository.countByModel_ModelId(id);
    }

    /**
     * Tìm UserVehicle theo vehicleId.
     *
     * @param vehicleId id của UserVehicle
     * @return Optional<UserVehicle> — có thể rỗng nếu không tìm thấy
     */
    @Override
    public Optional<UserVehicle> findById(Long vehicleId) {
        // Truy vấn theo PK
        return userVehicleRepository.findById(vehicleId);
    }
}
