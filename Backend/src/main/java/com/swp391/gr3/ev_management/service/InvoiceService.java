package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.response.DriverInvoiceDetail;
import com.swp391.gr3.ev_management.dto.response.UnpaidInvoiceResponse;
import com.swp391.gr3.ev_management.entity.Invoice;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public interface InvoiceService {

    void save(Invoice invoice);

    Optional<Invoice> findBySession_SessionId(Long sessionId);

    Optional<Invoice> findById(Long invoiceId);

    List<Invoice> findUnpaidInvoicesByStation(Long stationId);

    double sumAll();

    double sumAmountBetween(LocalDateTime dayFrom, LocalDateTime dayTo);

    double sumByStationBetween(Long stationId, LocalDateTime dayFrom, LocalDateTime dayTo);

    DriverInvoiceDetail getDetail(Long invoiceId, Long userId);

    List<UnpaidInvoiceResponse> getUnpaidInvoices(Long userId);

    DriverInvoiceDetail getInvoiceDetail(Long invoiceId);

    DriverInvoiceDetail payInvoice(Long invoiceId);
}
