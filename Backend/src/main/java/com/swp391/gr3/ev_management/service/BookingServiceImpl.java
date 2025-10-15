package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.CreateBookingRequest;
import com.swp391.gr3.ev_management.DTO.response.BookingResponse;
import com.swp391.gr3.ev_management.entity.*;
import com.swp391.gr3.ev_management.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final BookingSlotRepository bookingSlotRepository;
    private final SlotAvailabilityRepository slotAvailabilityRepository;
    private final UserVehicleRepository vehicleRepository;
    private final TariffRepository tariffRepository;


    @Override
    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request) {
        // 1. Lấy thông tin xe
        UserVehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        // 2. Lấy slot cụ thể trong SlotAvailability
        SlotAvailability slot = slotAvailabilityRepository.findById(request.getSlotId())
                .orElseThrow(() -> new RuntimeException("Slot not found"));

        // Kiểm tra slot có khả dụng không
        if (!"available".equalsIgnoreCase(slot.getStatus())) {
            throw new RuntimeException("Slot is not available for booking");
        }

        // 3. Lấy giá theo loại cổng sạc (từ Tariff)
        Tariff tariff = tariffRepository.findByConnectorType(slot.getConnectorType())
                .orElseThrow(() -> new RuntimeException("No tariff found for this connector type"));
        double price = tariff.getPricePerKWh();

        // 4. Lấy thông tin station và time từ slot
        ChargingStation station = slot.getTemplate().getConfig().getStation();
        LocalDateTime startTime = slot.getDate().with(slot.getTemplate().getStartTime());
        LocalDateTime endTime = slot.getDate().with(slot.getTemplate().getEndTime());

        // 5. Tạo Booking mới (KHÔNG set slot vì không có relationship trực tiếp)
        Booking booking = Booking.builder()
                .vehicle(vehicle)
                .station(station)
                .bookingTime(LocalDateTime.now())
                .scheduledStartTime(startTime)
                .scheduledEndTime(endTime)
                .status("pending")
                .build();
        bookingRepository.save(booking);

        // 5. Tạo BookingSlot (liên kết Booking ↔ SlotAvailability)
        BookingSlot bookingSlot = new BookingSlot();
        bookingSlot.setBooking(booking);
        bookingSlot.setSlot(slot);
        bookingSlot.setCreatedAt(LocalDateTime.now());
        bookingSlotRepository.save(bookingSlot);

        // 6. Cập nhật trạng thái slot
        slot.setStatus("booked");
        slotAvailabilityRepository.save(slot);

        // 7. Trả về response với Builder pattern
        return BookingResponse.builder()
                .bookingId(booking.getBookingId())
                .vehicleName(vehicle.getModel().getModel())
                .stationName(slot.getTemplate().getConfig().getStation().getStationName())
                .slotName("Slot " + slot.getSlotId()) // Hoặc lấy từ template nếu có
                .connectorType(slot.getConnectorType().getDisplayName())
                .timeRange(formatTimeRange(slot.getTemplate().getStartTime(), slot.getTemplate().getEndTime()))
                .bookingDate(slot.getDate())
                .price(price)
                .status(booking.getStatus())
                .build();
    }

    @Override
    @Transactional
    public BookingResponse confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!"pending".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException("Only pending bookings can be confirmed");
        }

        // Cập nhật trạng thái booking
        booking.setStatus("confirmed");
        booking.setUpdatedAt(LocalDateTime.now());
        bookingRepository.save(booking);

        // Cập nhật tất cả các slot của booking sang 'booked'
        for (BookingSlot bs : booking.getBookingSlots()) {
            SlotAvailability slot = bs.getSlot();
            slot.setStatus("booked");
            slotAvailabilityRepository.save(slot);
        }

        // Lấy slot đầu tiên để hiển thị thông tin ra response
        if (booking.getBookingSlots().isEmpty()) {
            throw new RuntimeException("No slots found for this booking");
        }

        BookingSlot firstSlot = booking.getBookingSlots().get(0);
        SlotAvailability slot = firstSlot.getSlot();
        double price = tariffRepository.findByConnectorType(slot.getConnectorType())
                .map(Tariff::getPricePerKWh)
                .orElse(0.0);

        return BookingResponse.builder()
                .bookingId(booking.getBookingId())
                .vehicleName(booking.getVehicle().getModel().getModel())
                .stationName(slot.getTemplate().getConfig().getStation().getStationName())
                .slotName("Slot " + slot.getSlotId())
                .connectorType(slot.getConnectorType().getDisplayName())
                .timeRange(formatTimeRange(slot.getTemplate().getStartTime(), slot.getTemplate().getEndTime()))
                .bookingDate(slot.getDate())
                .price(price)
                .status(booking.getStatus())
                .build();
    }

    private String formatTimeRange(LocalDateTime start, LocalDateTime end) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return start.format(formatter) + " - " + end.format(formatter);
    }
}
