package com.swp391.gr3.ev_management.mapper;

import com.swp391.gr3.ev_management.dto.response.BookingResponse;
import com.swp391.gr3.ev_management.entity.Booking;
import com.swp391.gr3.ev_management.entity.BookingSlot;
import com.swp391.gr3.ev_management.entity.SlotAvailability;
import com.swp391.gr3.ev_management.entity.UserVehicle;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class BookingResponseMapper {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    /* ---------------- Common helpers ---------------- */

    private String formatTimeRange(LocalDateTime start, LocalDateTime end) {
        return start.format(HH_MM) + " - " + end.format(HH_MM);
    }

    private String buildSlotNameFromAvailabilities(List<SlotAvailability> slots) {
        return "Slots: " + slots.stream()
                .map(s -> s.getSlotId().toString())
                .collect(Collectors.joining(", "));
    }

    private String buildSlotNameFromBookingSlots(List<BookingSlot> slots) {
        return "Slots: " + slots.stream()
                .map(bs -> bs.getSlot().getSlotId().toString())
                .collect(Collectors.joining(", "));
    }

    private String buildTimeRanges(List<SlotAvailability> slots) {
        return slots.stream()
                .map(s -> formatTimeRange(
                        s.getTemplate().getStartTime(),
                        s.getTemplate().getEndTime()))
                .collect(Collectors.joining(", "));
    }

    private SlotAvailability firstByChronology(List<BookingSlot> bookingSlots) {
        return bookingSlots.stream()
                .map(BookingSlot::getSlot)
                .sorted(Comparator.comparing(sa -> sa.getDate().with(sa.getTemplate().getStartTime())))
                .findFirst()
                .orElse(null);
    }

    /* ---------------- Mapping APIs ---------------- */

    /** Response khi tạo booking (nhiều slot). */
    public BookingResponse forCreate(Booking booking, List<SlotAvailability> slots, double price) {
        var first = slots.get(0);

        UserVehicle vehicle = booking.getVehicle();
        String vehicleName = null;

        if (vehicle != null && vehicle.getModel() != null) {
            vehicleName = vehicle.getModel().getModel();
        }

        return BookingResponse.builder()
                .bookingId(booking.getBookingId())
                .vehicleName(vehicleName)
                .stationName(first.getTemplate().getConfig().getStation().getStationName())
                .slotName(buildSlotNameFromAvailabilities(slots))
                .connectorType(first.getChargingPoint().getConnectorType().getDisplayName())
                .timeRange(buildTimeRanges(slots))
                .bookingDate(first.getDate())
                .price(price)
                .status(booking.getStatus())
                .build();
    }

    /** Response khi confirm (một slot đại diện). */
    public BookingResponse forConfirm(Booking booking, SlotAvailability slot, double price, String timeRange) {

        String vehicleName = "N/A";
        if (booking.getVehicle() != null &&
                booking.getVehicle().getModel() != null) {
            vehicleName = booking.getVehicle().getModel().getModel();
        }

        return BookingResponse.builder()
                .bookingId(booking.getBookingId())
                .vehicleName(vehicleName)
                .stationName(slot.getTemplate().getConfig().getStation().getStationName())
                .slotName("Slot " + slot.getSlotId())
                .connectorType(slot.getChargingPoint().getConnectorType().getDisplayName())
                .timeRange(timeRange)
                .bookingDate(slot.getDate())
                .price(price)
                .status(booking.getStatus())
                .build();
    }

    /** Response khi hủy (lấy slot sớm nhất để hiển thị). */
    public BookingResponse forCancel(Booking booking, List<BookingSlot> bookingSlots, SlotAvailability firstSlot, double price) {
        return BookingResponse.builder()
                .bookingId(booking.getBookingId())
                .vehicleName(booking.getVehicle().getModel().getModel())
                .stationName(booking.getStation().getStationName())
                .slotName(buildSlotNameFromBookingSlots(bookingSlots))
                .connectorType(firstSlot.getChargingPoint().getConnectorType().getDisplayName())
                .bookingDate(firstSlot.getDate())
                .price(price)
                .status(booking.getStatus())
                .build();
    }

    /**
     * Response tổng quát khi xem chi tiết booking (không bắt buộc có giá).
     * Cố gắng suy luận thông tin từ slot đầu tiên nếu có.
     */
    public BookingResponse view(Booking booking) {
        SlotAvailability firstSlot = booking.getBookingSlots() != null && !booking.getBookingSlots().isEmpty()
                ? firstByChronology(booking.getBookingSlots())
                : null;

        return BookingResponse.builder()
                .bookingId(booking.getBookingId())
                .vehicleName(booking.getVehicle().getModel().getModel())
                .stationName(booking.getStation().getStationName())
                .slotName(booking.getBookingSlots() != null && !booking.getBookingSlots().isEmpty()
                        ? buildSlotNameFromBookingSlots(booking.getBookingSlots())
                        : null)
                .connectorType(firstSlot != null
                        ? firstSlot.getChargingPoint().getConnectorType().getDisplayName()
                        : null)
                .bookingDate(firstSlot != null ? firstSlot.getDate() : null)
                .price(0.0) // không tính ở đây
                .status(booking.getStatus())
                .build();
    }
}