package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.response.DriverInvoiceDetail;
import com.swp391.gr3.ev_management.dto.response.UnpaidInvoiceResponse;
import com.swp391.gr3.ev_management.entity.*;
import com.swp391.gr3.ev_management.enums.InvoiceStatus;
import com.swp391.gr3.ev_management.enums.PaymentProvider;
import com.swp391.gr3.ev_management.enums.TransactionStatus;
import com.swp391.gr3.ev_management.mapper.DriverInvoiceMapper;
import com.swp391.gr3.ev_management.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service // Đánh dấu class là Spring Service (xử lý logic của Invoice)
@RequiredArgsConstructor // Tự động tạo constructor cho các field final (DI)
@Slf4j // Cho phép sử dụng logger (log.info, warn, error...)
public class InvoiceServiceImpl implements InvoiceService {

    // Repository làm việc trực tiếp với DB bảng Invoice (CRUD + query custom)
    private final InvoiceRepository invoiceRepository;
    private final TariffService tariffService;
    private final DriverInvoiceMapper mapper;
    private final TransactionService transactionService;
    private final ChargingPointService chargingPointService;

    @Override
    public void save(Invoice invoice) {
        // Lưu hoặc cập nhật một hoá đơn vào DB
        invoiceRepository.save(invoice);
    }

    @Override
    public Optional<Invoice> findBySession_SessionId(Long sessionId) {
        // Tìm hoá đơn theo sessionId (mỗi phiên sạc có thể có 1 hoá đơn)
        return invoiceRepository.findBySession_SessionId(sessionId);
    }

    @Override
    public Optional<Invoice> findById(Long invoiceId) {
        // Tìm hoá đơn theo ID, trả Optional để tránh NullPointerException
        return invoiceRepository.findById(invoiceId);
    }

    @Override
    public List<Invoice> findUnpaidInvoicesByStation(Long stationId) {
        // Tìm tất cả hoá đơn chưa thanh toán thuộc một trạm
        return invoiceRepository.findUnpaidInvoicesByStation(stationId);
    }

    @Override
    public double sumAll() {
        // Tổng doanh thu từ tất cả hoá đơn
        return invoiceRepository.sumAll();
    }

    @Override
    public double sumAmountBetween(LocalDateTime dayFrom, LocalDateTime dayTo) {
        // Tổng tiền hoá đơn trong khoảng thời gian (dayFrom → dayTo)
        return invoiceRepository.sumAmountBetween(dayFrom, dayTo);
    }

    @Override
    public double sumByStationBetween(Long stationId, LocalDateTime dayFrom, LocalDateTime dayTo) {
        // Tổng doanh thu của một trạm trong khoảng thời gian (dayFrom → dayTo)
        return invoiceRepository.sumByStationBetween(stationId, dayFrom, dayTo);
    }

    @Override
    public DriverInvoiceDetail getDetail(Long invoiceId, Long userId) {
        Invoice invoice = invoiceRepository.findInvoiceDetail(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        Booking booking = invoice.getSession().getBooking();

        if (!booking.getVehicle().getDriver().getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("Forbidden");
        }

        // Lấy ChargingPoint
        ChargingPoint cp = booking.getBookingSlots().stream()
                .findFirst()
                .map(bs -> bs.getSlot().getChargingPoint())
                .orElse(null);

        assert cp != null;
        Long connectorTypeId = cp.getConnectorType().getConnectorTypeId();

        Double pricePerKwh = tariffService.findTariffByConnectorType(connectorTypeId)
                .stream()
                .findFirst()
                .map(Tariff::getPricePerKWh)
                .orElse(null);

        return mapper.toDto(invoice, booking, cp, pricePerKwh);
    }

    @Override
    public List<UnpaidInvoiceResponse> getUnpaidInvoices(Long userId) {
        return invoiceRepository.findUnpaidByUserId(userId);
    }

    // ================== DETAIL DÙNG CHUNG ==================
    private DriverInvoiceDetail buildInvoiceDetail(Invoice invoice) {
        Booking booking = invoice.getSession().getBooking();

        // Lấy ChargingPoint qua bookingSlots (null-safe)
        ChargingPoint cp = booking.getBookingSlots().stream()
                .filter(bs -> bs.getSlot() != null && bs.getSlot().getChargingPoint() != null)
                .map(bs -> bs.getSlot().getChargingPoint())
                .findFirst()
                .orElse(null);

        // 2) Nếu vẫn null → fallback theo station
        if (cp == null) {
            cp = chargingPointService
                    .findFirstByStation_StationId(booking.getStation().getStationId())
                    .orElse(null);
        }

        // Ưu tiên lấy connectorType từ CP, nếu không có thì fallback sang vehicle.model
        var connectorType = (cp != null && cp.getConnectorType() != null)
                ? cp.getConnectorType()
                : (booking.getVehicle() != null
                && booking.getVehicle().getModel() != null
                && booking.getVehicle().getModel().getConnectorType() != null)
                ? booking.getVehicle().getModel().getConnectorType()
                : null;

        Double pricePerKwh = null;
        if (connectorType != null) {
            Long connectorTypeId = connectorType.getConnectorTypeId();
            pricePerKwh = tariffService.findTariffByConnectorType(connectorTypeId)
                    .stream()
                    .findFirst()
                    .map(Tariff::getPricePerKWh)
                    .orElse(null);
        } else {
            log.warn("[INVOICE_DETAIL] Cannot resolve connectorType / pricePerKWh for invoiceId={}", invoice.getInvoiceId());
        }

        return mapper.toDto(invoice, booking, cp, pricePerKwh);
    }

    // ================== GET DETAIL ==================
    @Override
    public DriverInvoiceDetail getInvoiceDetail(Long invoiceId) {
        Invoice invoice = invoiceRepository.findInvoiceDetail(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        return buildInvoiceDetail(invoice);
    }

    // ================== PAY + RETURN DETAIL ==================
    @Override
    @Transactional
    public DriverInvoiceDetail payInvoice(Long invoiceId) {
        // 1) Lấy invoice (lấy luôn detail để lát build DTO)
        Invoice invoice = invoiceRepository.findInvoiceDetail(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        // 2) Chỉ cho phép thanh toán khi đang UNPAID
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new RuntimeException("Invoice already paid");
        }

        // 3) Tìm payment method EVM
        PaymentMethod evmMethod = transactionService.findByProvider(PaymentProvider.EVM)
                .orElseThrow(() -> new RuntimeException("Payment method EVM not found"));

        // 4) Cập nhật trạng thái invoice
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setUpdatedAt(LocalDateTime.now());
        invoiceRepository.save(invoice);

        // 5) Tạo transaction tương ứng
        Transaction transaction = Transaction.builder()
                .amount(invoice.getAmount())
                .currency(invoice.getCurrency())
                .description("Thanh toán hóa đơn #" + invoice.getInvoiceId() + " qua EVM")
                .status(TransactionStatus.COMPLETED)
                .invoice(invoice)
                .driver(invoice.getDriver())
                .paymentMethod(evmMethod)
                .build();

        transactionService.save(transaction);

        log.info("Invoice {} paid, transaction {} created",
                invoice.getInvoiceId(), transaction.getTransactionId());

        // 6) Trả về DriverInvoiceDetail (status lúc này đã là PAID)
        return buildInvoiceDetail(invoice);
    }
}
