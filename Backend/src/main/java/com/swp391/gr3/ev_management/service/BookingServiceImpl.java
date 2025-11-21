package com.swp391.gr3.ev_management.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.swp391.gr3.ev_management.dto.request.BookingRequest;
import com.swp391.gr3.ev_management.dto.request.CreateBookingRequest;
import com.swp391.gr3.ev_management.dto.request.LightBookingInfo;
import com.swp391.gr3.ev_management.dto.response.BookingResponse;
import com.swp391.gr3.ev_management.dto.response.ConfirmedBookingView;
import com.swp391.gr3.ev_management.enums.BookingStatus;
import com.swp391.gr3.ev_management.enums.NotificationTypes;
import com.swp391.gr3.ev_management.enums.SlotStatus;
import com.swp391.gr3.ev_management.entity.*;
import com.swp391.gr3.ev_management.events.NotificationCreatedEvent;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.mapper.BookingResponseMapper;
import com.swp391.gr3.ev_management.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service // Đánh dấu đây là Spring Service thực thi logic nghiệp vụ cho Booking
@RequiredArgsConstructor // Tự động generate constructor cho các field final (DI qua constructor)
@Slf4j // Cung cấp logger (log.info/error/...)
public class BookingServiceImpl implements BookingService {

    // Múi giờ cho tenant (VN)
    private static final ZoneId TENANT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    // ==== Các dependency chính ====
    private final BookingsRepository bookingsRepository;                 // Repository thao tác với bảng Booking (CRUD)
    private final BookingSlotRepository bookingSlotRepository;           // Repository cho bảng BookingSlot (mapping Booking <-> SlotAvailability)
    private final SlotAvailabilityService slotAvailabilityService;       // Service xử lý SlotAvailability (trạng thái slot)
    private final UserVehicleService userVehicleService;                 // Service xử lý UserVehicle (xe của user)
    private final TariffService tariffService;                           // Service lấy biểu giá (Tariff) theo loại connector
    private final NotificationsService notificationsService;             // Service tạo/lưu Notification
    private final ApplicationEventPublisher eventPublisher;              // Dùng để publish event trong hệ thống (event-driven)
    private final ObjectMapper mapper;                                   // ObjectMapper để serialize/deserialize JSON
    private final BookingSlotLogRepository bookingSlotLogRepository;     // Repository lưu log slot sau khi confirm (BookingSlotLog)
    private final BookingResponseMapper bookingResponseMapper;           // Mapper chuyển từ Entity -> DTO BookingResponse
    private final StaffService staffService;                             // Service lấy thông tin Staff dựa theo userId

    @Override
    @Transactional // Gộp tất cả bước tạo booking vào một transaction để đảm bảo toàn vẹn
    public BookingResponse createBooking(CreateBookingRequest request) {
        // 1️⃣ Lấy thông tin xe theo vehicleId từ request
        // Nếu không tồn tại UserVehicle tương ứng -> ném ErrorException
        UserVehicle vehicle = userVehicleService.findById(request.getVehicleId())
                .orElseThrow(() -> new ErrorException("Vehicle not found"));

        // 2️⃣ Lấy danh sách SlotAvailability dựa trên danh sách slotIds client gửi lên
        List<SlotAvailability> slots = slotAvailabilityService.findAllById(request.getSlotIds());
        if (slots.isEmpty()) {
            // Nếu không có slot nào tương ứng -> ném lỗi
            throw new ErrorException("No slots found");
        }

        // Kiểm tra tất cả slot đều đang AVAILABLE
        // Nếu có bất kỳ slot nào không AVAILABLE (vd: BOOKED, UNAVAILABLE, ...) -> không cho đặt
        for (SlotAvailability slot : slots) {
            if (slot.getStatus() != SlotStatus.AVAILABLE) {
                // slot.getSlotId() là ID của SlotAvailability, dùng để message dễ hiểu
                throw new ErrorException("Slot " + slot.getSlotId() + " is not available for booking");
            }
        }

        // (Giả định) Tất cả slot thuộc cùng một trạm sạc (ChargingStation)
        // Lấy station từ slot đầu tiên trong danh sách (vì đã đảm bảo cùng trạm từ logic trước đó)
        ChargingStation station = slots.get(0).getTemplate().getConfig().getStation();

        // 3️⃣ Tạo thực thể Booking mới
        // - bookingTime: thời điểm tạo booking (thời gian hiện tại)
        // - scheduledStartTime: thời điểm bắt đầu dự kiến, lấy từ slot đầu tiên
        // - scheduledEndTime: thời điểm kết thúc dự kiến, lấy từ slot cuối cùng
        // - status: PENDING (chờ xác nhận)
        Booking booking = Booking.builder()
                .vehicle(vehicle) // gán xe
                .station(station) // gán trạm
                .bookingTime(LocalDateTime.now()) // thời điểm user tạo booking
                // Lấy ngày từ slot + trộn với thời gian start của template -> tạo thành LocalDateTime start
                .scheduledStartTime(slots.get(0).getDate().with(slots.get(0).getTemplate().getStartTime()))
                // Tương tự với slot cuối cùng -> thời gian kết thúc
                .scheduledEndTime(slots.get(slots.size() - 1).getDate().with(slots.get(slots.size() - 1).getTemplate().getEndTime()))
                .status(BookingStatus.PENDING) // trạng thái ban đầu: PENDING
                .build();
        bookingsRepository.save(booking); // Lưu Booking xuống DB

        // 4️⃣ Tạo các bản ghi BookingSlot (mapping giữa Booking và SlotAvailability)
        // Đồng thời set trạng thái SlotAvailability -> BOOKED (tạm giữ chỗ, chưa phải CONFIRMED)
        for (SlotAvailability slot : slots) {
            BookingSlot bookingSlot = new BookingSlot();
            bookingSlot.setBooking(booking); // gắn booking vào
            bookingSlot.setSlot(slot);       // gắn slot vào
            bookingSlot.setCreatedAt(LocalDateTime.now()); // thời điểm tạo mapping
            bookingSlotRepository.save(bookingSlot);       // lưu BookingSlot

            // Cập nhật trạng thái slot sang BOOKED để các user khác không thể đặt chồng
            slot.setStatus(SlotStatus.BOOKED);
            slotAvailabilityService.save(slot); // lưu lại trạng thái slot
        }

        // 5️⃣ Lấy giá tham chiếu (pricePerKWh) cho loại connector sử dụng
        // Ở đây lấy theo slot đầu tiên trong danh sách
        double price = slots.stream()
                .findFirst() // lấy Optional<SlotAvailability> đầu tiên
                .flatMap(slot -> tariffService.findByConnectorType(
                        slot.getChargingPoint().getConnectorType() // lấy loại connector từ chargingPoint
                ).map(Tariff::getPricePerKWh))                     // map sang giá mỗi kWh
                .orElse(0.0); // nếu không tìm thấy Tariff -> giá mặc định 0.0

        // 6️⃣ (Tùy chọn) Build chuỗi khung giờ để hiển thị; ở đây mapper sẽ lo format response
        // Ghép các khung giờ của từng slot thành chuỗi "HH:mm - HH:mm, HH:mm - HH:mm, ..."
        String timeRanges = slots.stream()
                .map(slot -> formatTimeRange(slot.getTemplate().getStartTime(), slot.getTemplate().getEndTime()))
                .collect(Collectors.joining(", "));

        // Trả về DTO cho client
        // bookingResponseMapper.forCreate sẽ đóng gói Booking + slots + price thành BookingResponse
        return bookingResponseMapper.forCreate(booking, slots, price);
    }

    @Override
    @Transactional // Xác nhận booking: đổi trạng thái + cố định các slot + tạo thông báo
    public BookingResponse confirmBooking(Long bookingId) {
        // Lấy booking theo bookingId từ DB
        Booking booking = bookingsRepository.findById(bookingId)
                .orElseThrow(() -> new ErrorException("Booking not found"));

        // Chỉ cho phép confirm nếu booking đang ở trạng thái PENDING
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ErrorException("Only pending bookings can be confirmed");
        }

        // Cập nhật trạng thái booking sang CONFIRMED
        booking.setStatus(BookingStatus.CONFIRMED);
        // Cập nhật thời gian update theo múi giờ TENANT_ZONE (Asia/Ho_Chi_Minh)
        booking.setUpdatedAt(LocalDateTime.now(TENANT_ZONE));
        bookingsRepository.save(booking); // Lưu thay đổi

        // Đảm bảo tất cả slot của booking được giữ ở trạng thái BOOKED
        // (Trong trường hợp có slot nào bị thay đổi trước đó thì reset lại)
        for (BookingSlot bs : booking.getBookingSlots()) {
            SlotAvailability s = bs.getSlot();
            s.setStatus(SlotStatus.BOOKED);   // status BOOKED thể hiện slot đang được giữ bởi booking này
            slotAvailabilityService.save(s);  // lưu lại
        }

        // Nếu booking không có BookingSlot nào -> ném lỗi vì dữ liệu không hợp lệ
        if (booking.getBookingSlots().isEmpty()) {
            throw new ErrorException("No slots found for this booking");
        }

        // Lấy slot đầu tiên trong danh sách BookingSlot (đã gắn với booking)
        // Slot này dùng để lấy thông tin stationName/connectorType/timeRange
        SlotAvailability slot = booking.getBookingSlots().get(0).getSlot();
        // Lấy giá theo connectorType của slot đầu tiên
        double price = tariffService.findByConnectorType(slot.getChargingPoint().getConnectorType())
                .map(Tariff::getPricePerKWh)
                .orElse(0.0);

        String stationName = slot.getTemplate().getConfig().getStation().getStationName();
        // build chuỗi "HH:mm - HH:mm" cho khung giờ của slot đầu tiên
        String timeRange = formatTimeRange(
                slot.getTemplate().getStartTime(),
                slot.getTemplate().getEndTime()
        );

        // Tạo Notification xác nhận booking cho user sở hữu xe
        Notification noti = new Notification();
        // booking.getVehicle().getDriver().getUser(): lấy User từ Booking -> Vehicle -> Driver -> User
        noti.setUser(booking.getVehicle().getDriver().getUser());
        noti.setTitle("Xác nhận đặt chỗ #" + booking.getBookingId());
        noti.setContentNoti("Trạm: " + stationName + " | Khung giờ: " + timeRange
                + " | Cổng: " + slot.getChargingPoint().getConnectorType().getDisplayName());
        noti.setType(NotificationTypes.BOOKING_CONFIRMED); // loại thông báo: xác nhận booking
        noti.setStatus(Notification.STATUS_UNREAD);         // trạng thái: chưa đọc
        noti.setBooking(booking);                           // gắn booking liên quan
        notificationsService.save(noti);                    // lưu thông báo

        // Xóa log cũ của booking này (nếu tồn tại) để tránh duplicate khi confirm nhiều lần
        if (bookingSlotLogRepository.existsByBooking_BookingId(bookingId)) {
            bookingSlotLogRepository.deleteByBooking_BookingId(bookingId);
        }

        // Lấy danh sách BookingSlot thuộc booking và sắp xếp theo thời gian bắt đầu thực tế
        // Mục đích: ghi log duration của từng slot theo đúng thứ tự thời gian
        List<BookingSlot> ordered = booking.getBookingSlots().stream()
                .sorted((a, b) -> {
                    var sa = a.getSlot(); var sb = b.getSlot();
                    // startA, startB: thời gian bắt đầu của slot A, B
                    var startA = sa.getDate().with(sa.getTemplate().getStartTime());
                    var startB = sb.getDate().with(sb.getTemplate().getStartTime());
                    return startA.compareTo(startB); // sắp xếp tăng dần theo thời gian bắt đầu
                })
                .toList();

        // Ghi log duration cho từng slot:
        // - slotIndex: thứ tự slot trong chuỗi (0, 1, 2, ...)
        // - slotDurationMin: thời lượng của slot tính bằng phút
        for (int i = 0; i < ordered.size(); i++) {
            BookingSlot bs = ordered.get(i);
            SlotAvailability s = bs.getSlot();
            var start = s.getDate().with(s.getTemplate().getStartTime());  // thời gian bắt đầu
            var end   = s.getDate().with(s.getTemplate().getEndTime());    // thời gian kết thúc
            int durationMin = (int) java.time.Duration.between(start, end).toMinutes(); // tính số phút

            BookingSlotLog logEntry = BookingSlotLog.builder()
                    .booking(booking)          // gắn booking
                    .slotIndex(i)             // index slot trong chuỗi
                    .slotDurationMin(durationMin) // số phút của slot
                    .build();
            bookingSlotLogRepository.save(logEntry); // lưu log
        }

        // Publish event để các listener khác trong hệ thống lắng nghe và xử lý
        // Ví dụ: gửi email, gửi push notification qua các kênh khác.
        eventPublisher.publishEvent(new NotificationCreatedEvent(noti.getNotiId()));

        // Trả về DTO BookingResponse sau khi confirm, bao gồm thông tin booking, slot, price, timeRange...
        return bookingResponseMapper.forConfirm(booking, slot, price, timeRange);
    }

    // Hàm format "HH:mm - HH:mm" để hiển thị khung giờ.
    // Lưu ý: tham số hiện đang là LocalDateTime theo chữ ký, nhưng thực tế truyền LocalTime từ template.
    // Code gốc dùng như vậy, nên giữ nguyên chữ ký hàm và giả định đã chuyển đổi phù hợp ở nơi gọi/overload khác.
    private String formatTimeRange(LocalDateTime start, LocalDateTime end) {
        var f = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
        return start.format(f) + " - " + end.format(f);
    }

    /** 1) Build payload QR: gom dữ liệu Booking thành DTO -> JSON -> Base64 (URL-safe) */
    @Override
    public String buildQrPayload(Long bookingId) {
        // Lấy booking theo id từ DB, nếu không có -> ném IllegalArgumentException
        Booking b = bookingsRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Not found: " + bookingId));

        // Tạo DTO BookingRequest (phiên bản rút gọn) để nhúng vào QR
        BookingRequest dto = new BookingRequest();
        dto.setBookingId(b.getBookingId());
        dto.setStationId(b.getStation().getStationId()); // Lưu ID station, tránh nhúng nguyên object
        dto.setVehicleId(b.getVehicle().getVehicleId());
        dto.setBookingTime(b.getBookingTime());
        dto.setScheduledStartTime(b.getScheduledStartTime());
        dto.setScheduledEndTime(b.getScheduledEndTime());
        dto.setStatus(String.valueOf(b.getStatus())); // chuyển trạng thái BookingStatus -> String

        // Serialize DTO -> JSON -> mã hóa Base64 URL-safe (không padding)
        // Lý do dùng Base64 URL-safe: tránh các ký tự đặc biệt khó encode vào QR/URL
        try {
            String json = mapper.writeValueAsString(dto); // chuyển dto thành JSON
            return Base64.getUrlEncoder()
                    .withoutPadding() // bỏ padding '=' cho chuỗi gọn hơn
                    .encodeToString(json.getBytes(StandardCharsets.UTF_8)); // mã hóa bytes UTF-8 -> Base64 URL-safe
        } catch (Exception e) {
            // Nếu serialize thất bại -> ném ErrorException để xử lý phía trên
            throw new ErrorException("Failed to serialize QR payload");
        }
    }

    /** 2) Sinh ảnh QR (PNG) từ chuỗi payload đã mã hóa */
    @Override
    public byte[] generateQrPng(String payload, int size) {
        try {
            // Tạo ma trận QR (BitMatrix) từ chuỗi payload, kích thước size x size
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(payload, BarcodeFormat.QR_CODE, size, size);

            // Dùng ByteArrayOutputStream để ghi ảnh QR dạng PNG vào mảng byte
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
            return baos.toByteArray(); // trả về byte[] của file PNG
        } catch (Exception e) {
            // Nếu có lỗi trong quá trình tạo QR -> ném ErrorException
            throw new ErrorException("Failed to generate QR image");
        }
    }

    /** 3) Giải mã payload từ Base64 URL-safe -> JSON -> DTO */
    @Override
    public BookingRequest decodePayload(String base64) {
        try {
            // Giải mã chuỗi base64 URL-safe -> bytes UTF-8 -> String JSON
            String json = new String(Base64.getUrlDecoder().decode(base64), StandardCharsets.UTF_8);
            // Dùng ObjectMapper đọc JSON -> BookingRequest DTO
            return mapper.readValue(json, BookingRequest.class);
        } catch (Exception e) {
            // Nếu có lỗi (base64 sai, JSON sai format, ...) -> ném ErrorException
            throw new ErrorException("Failed to decode QR payload");
        }
    }

    @Override
    public BookingResponse getBookingById(Long bookingId) {
        // Tìm booking theo id; nếu không tồn tại -> ném ErrorException
        Booking b = bookingsRepository.findById(bookingId)
                .orElseThrow(() -> new ErrorException("Booking not found"));
        // Dùng mapper để chuyển entity Booking -> BookingResponse phù hợp cho view
        return bookingResponseMapper.view(b);
    }

    @Override
    @Transactional // Hủy booking: cập nhật trạng thái + trả slot + thông báo
    public BookingResponse cancelBooking(Long bookingId) {
        // 1) Lấy booking; nếu không có -> lỗi
        Booking booking = bookingsRepository.findById(bookingId)
                .orElseThrow(() -> new ErrorException("Booking not found"));

        // 2) Ràng buộc trạng thái: không được hủy nếu đã CANCELED hoặc COMPLETED
        if (booking.getStatus() == BookingStatus.CANCELED) {
            throw new ErrorException("Booking đã bị hủy trước đó");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new ErrorException("Không thể hủy booking đã hoàn thành");
        }

        // 3) Đảm bảo booking có ít nhất 1 slot để tính thời điểm bắt đầu (phục vụ rule hủy trước 30 phút)
        if (booking.getBookingSlots() == null || booking.getBookingSlots().isEmpty()) {
            throw new ErrorException("Booking không có slot nào để xác định thời gian bắt đầu");
        }

        // Lấy slot sớm nhất (theo startTime) trong các slot của booking
        SlotAvailability firstSlot = booking.getBookingSlots().stream()
                .sorted((a, b) -> {
                    var sa = a.getSlot();
                    var sb = b.getSlot();
                    var startA = sa.getDate().with(sa.getTemplate().getStartTime());
                    var startB = sb.getDate().with(sb.getTemplate().getStartTime());
                    return startA.compareTo(startB);
                })
                .findFirst()
                .get()          // vì đã kiểm tra isEmpty ở trên nên get() an toàn
                .getSlot();

        // Tính thời gian bắt đầu slot đầu tiên
        LocalDateTime slotStart = firstSlot.getDate().with(firstSlot.getTemplate().getStartTime());
        // Lấy thời gian hiện tại (server time)
        LocalDateTime now = LocalDateTime.now();

        // 4) Rule hủy: không cho phép hủy nếu còn dưới 30 phút trước khi slot đầu tiên bắt đầu
        // Điều kiện: now must be BEFORE (slotStart - 30 phút)
        if (!now.isBefore(slotStart.minusMinutes(30))) {
            throw new ErrorException("Không thể hủy đặt chỗ khi còn dưới 30 phút trước thời gian bắt đầu.");
        }

        // 5) Cho phép hủy: cập nhật trạng thái booking -> CANCELED
        booking.setStatus(BookingStatus.CANCELED);
        booking.setUpdatedAt(LocalDateTime.now()); // update thời gian thay đổi
        bookingsRepository.save(booking);          // lưu booking

        // 6) Giải phóng toàn bộ slot của booking về trạng thái AVAILABLE
        for (BookingSlot bs : booking.getBookingSlots()) {
            SlotAvailability slot = bs.getSlot();
            slot.setStatus(SlotStatus.AVAILABLE); // slot lại mở cho các user khác đặt
            slotAvailabilityService.save(slot);
        }

        // 7) Gửi Notification cho user về việc hủy booking
        // Dùng try-catch để nếu lỗi khi gửi thông báo thì chỉ log warn, không rollback hủy booking
        try {
            Notification noti = new Notification();
            noti.setUser(booking.getVehicle().getDriver().getUser()); // user sở hữu xe
            noti.setTitle("Hủy đặt chỗ #" + booking.getBookingId());
            noti.setContentNoti("Đặt chỗ của bạn tại trạm "
                    + booking.getStation().getStationName()
                    + " đã được hủy thành công.");
            noti.setType(NotificationTypes.BOOKING_CANCELED); // loại thông báo: hủy booking
            noti.setStatus(Notification.STATUS_UNREAD);
            noti.setBooking(booking);
            notificationsService.save(noti);

            // Publish event để các kênh khác xử lý (gửi email, push, ...)
            eventPublisher.publishEvent(new NotificationCreatedEvent(noti.getNotiId()));
        } catch (Exception e) {
            // Log warning, không throw exception ra ngoài để tránh rollback transaction hủy
            log.warn("[cancelBooking] Notification failed: {}", e.getMessage());
        }

        // 8) Lấy lại giá tham chiếu theo connectorType của slot đầu tiên để hiển thị trong response
        double price = tariffService.findByConnectorType(firstSlot.getChargingPoint().getConnectorType())
                .map(Tariff::getPricePerKWh)
                .orElse(0.0);

        // Trả về BookingResponse cho hành động hủy booking
        // forCancel: mapper sẽ build DTO phù hợp (bao gồm booking, danh sách slot, firstSlot, price, ...)
        return bookingResponseMapper.forCancel(booking, booking.getBookingSlots(), firstSlot, price);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ConfirmedBookingView> getConfirmedBookingsForStaff(Long userId) {
        // Từ userId (của tài khoản staff đang đăng nhập), tìm ra staffId tương ứng
        Long staffId = staffService.findIdByUserId(userId)
                .orElseThrow(() -> new ErrorException("Staff not found for current user"));
        // Lấy danh sách booking ở trạng thái CONFIRMED được gán cho staff này
        return bookingsRepository.findConfirmedBookingsByStaff(staffId);
    }

    @Override
    public Optional<Booking> findByBookingIdAndStatus(Long bookingId, BookingStatus bookingStatus) {
        // Tìm Booking theo bookingId và trạng thái cụ thể (vd: CONFIRMED, PENDING, ...)
        return bookingsRepository.findByBookingIdAndStatus(bookingId, bookingStatus);
    }

    @Override
    public void save(Booking booking) {
        // Wrapper đơn giản để lưu Booking (có thể dùng khi cập nhật trạng thái từ nơi khác)
        bookingsRepository.save(booking);
    }

    @Override
    public Optional<Booking> findByIdWithConnectorType(Long bookingId) {
        // Lấy Booking kèm theo thông tin connectorType (có thể dùng join fetch trong repository)
        return bookingsRepository.findByIdWithConnectorType(bookingId);
    }

    @Override
    public long countByCreatedAtBetween(LocalDateTime localDateTime, LocalDateTime localDateTime1) {
        // Đếm số booking được tạo trong khoảng thời gian nhất định
        return bookingsRepository.countByCreatedAtBetween(localDateTime, localDateTime1);
    }

    @Override
    public List<Booking> findByStatusAndStartTimeBetween(BookingStatus bookingStatus, LocalDateTime now, LocalDateTime next2Hours) {
        // Tìm danh sách booking theo trạng thái và khoảng thời gian bắt đầu
        return bookingsRepository.findByStatusAndScheduledStartTimeBetween(bookingStatus, now, next2Hours);
    }

    @Override
    public List<Booking> findTop5ByOrderByCreatedAtDesc() {
        // Lấy 5 booking mới nhất theo thời gian tạo
        return bookingsRepository.findTop5ByOrderByCreatedAtDesc();
    }

    @Override
    public Optional<LightBookingInfo> findLightBookingInfo(Long bookingId) {
        return bookingsRepository.findLightBookingInfo(bookingId);
    }
}
