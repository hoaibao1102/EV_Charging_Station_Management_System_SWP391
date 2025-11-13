package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.entity.Invoice;
import com.swp391.gr3.ev_management.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service // Đánh dấu class là Spring Service (xử lý logic của Invoice)
@RequiredArgsConstructor // Tự động tạo constructor cho các field final (DI)
@Slf4j // Cho phép sử dụng logger (log.info, warn, error...)
public class InvoiceServiceImpl implements InvoiceService {

    // Repository làm việc trực tiếp với DB bảng Invoice (CRUD + query custom)
    private final InvoiceRepository invoiceRepository;

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
}
