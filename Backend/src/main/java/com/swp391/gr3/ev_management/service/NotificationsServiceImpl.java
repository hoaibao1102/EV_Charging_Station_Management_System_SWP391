package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.response.NotificationResponse;
import com.swp391.gr3.ev_management.entity.Notification;
import com.swp391.gr3.ev_management.exception.ConflictException;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.mapper.NotificationMapper;
import com.swp391.gr3.ev_management.repository.NotificationsRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.swp391.gr3.ev_management.dto.response.CreateNotificationResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service // Đánh dấu class là Spring Service — chứa logic xử lý thông báo (Notification)
@RequiredArgsConstructor // Tự động tạo constructor cho các field final
public class NotificationsServiceImpl implements NotificationsService {

    // Repository thao tác với bảng Notification trong DB
    private final NotificationsRepository notificationsRepository;
    // Mapper chuyển đổi giữa Entity <-> DTO
    private final NotificationMapper notificationMapper;
    private final NotificationMapper mapper; // (dường như trùng với notificationMapper — nhưng vẫn được inject)

    @Override
    public void save(Notification noti) {
        // Lưu thông báo vào DB qua repository
        notificationsRepository.save(noti);
    }

    /**
     * Lấy danh sách tất cả thông báo của 1 user theo userId.
     * - Truy vấn DB theo userId
     * - Chuyển mỗi Notification entity thành DTO để trả về cho client
     */
    @Override
    public List<CreateNotificationResponse> getNotificationsByUser(Long userId) {
        return notificationsRepository.findByUser_UserId(userId)
                .stream()
                .map(mapper::mapToResponse) // map từng entity sang DTO (CreateNotificationResponse)
                .collect(Collectors.toList());
    }

    /**
     * Đếm số thông báo chưa đọc (status = "unread") của một user.
     * - Gọi hàm countByUser_UserIdAndStatus trong repository
     * - Trả về tổng số dạng Long
     */
    @Override
    public Long getUnreadCount(Long userId) {
        return notificationsRepository.countByUser_UserIdAndStatus(userId, "unread");
    }

    /**
     * Đánh dấu một thông báo là "đã đọc" (READ).
     * - Kiểm tra notification tồn tại
     * - Kiểm tra userId có phải chủ sở hữu không
     * - Cập nhật status và thời điểm đọc (readAt)
     * - Lưu lại vào DB
     */
    @Override
    @Transactional // Bật transaction vì có thao tác ghi (update)
    public void markAsRead(Long notificationId, Long userId) {
        // Lấy notification theo ID, nếu không có thì ném lỗi
        Notification n = notificationsRepository.findById(notificationId)
                .orElseThrow(() -> new ErrorException("Notification not found"));

        // Kiểm tra quyền: user hiện tại phải là chủ của notification này
        if (!n.getUser().getUserId().equals(userId)) {
            throw new ConflictException("No permission to mark this notification");
        }

        // Đặt trạng thái sang READ và ghi nhận thời gian đọc
        n.setStatus("READ");
        n.setReadAt(LocalDateTime.now());

        // Lưu lại thay đổi vào DB
        notificationsRepository.save(n);
    }

    /**
     * Lấy chi tiết một thông báo cụ thể theo ID.
     * - Tìm notification trong DB
     * - Kiểm tra quyền sở hữu (userId khớp)
     * - Nếu chưa đọc → đánh dấu là đã đọc và cập nhật readAt
     * - Trả về DTO NotificationResponse
     */
    @Override
    public NotificationResponse getNotificationById(Long notificationId, Long userId) {
        // Tìm thông báo theo ID
        Notification notification = notificationsRepository.findById(notificationId)
                .orElse(null);

        // Nếu không tìm thấy thì trả null (controller sẽ xử lý)
        if (notification == null) {
            return null;
        }

        // Kiểm tra quyền sở hữu (user hiện tại phải là người nhận thông báo)
        if (!notification.getUser().getUserId().equals(userId)) {
            throw new ConflictException("Bạn không có quyền truy cập thông báo này");
        }

        // Nếu thông báo chưa đọc → tự động chuyển sang trạng thái "READ"
        if ("UNREAD".equalsIgnoreCase(notification.getStatus())) {
            notification.setStatus("READ");
            notification.setReadAt(LocalDateTime.now());
            notificationsRepository.save(notification);
        }

        // Trả về DTO thông tin chi tiết (NotificationResponse)
        return notificationMapper.mapToNotificationResponse(notification);
    }
}
