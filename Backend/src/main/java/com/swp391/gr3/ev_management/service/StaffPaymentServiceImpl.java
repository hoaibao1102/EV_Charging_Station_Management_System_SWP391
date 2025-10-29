package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.ConfirmPaymentRequest;
import com.swp391.gr3.ev_management.DTO.response.ConfirmPaymentResponse;
import com.swp391.gr3.ev_management.DTO.response.UnpaidInvoiceResponse;
import com.swp391.gr3.ev_management.entity.Invoice;
import com.swp391.gr3.ev_management.entity.PaymentMethod;
import com.swp391.gr3.ev_management.entity.StationStaff;
import com.swp391.gr3.ev_management.entity.Transaction;
import com.swp391.gr3.ev_management.enums.InvoiceStatus;
import com.swp391.gr3.ev_management.enums.PaymentProvider;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.mapper.UnpaidInvoiceMapper;
import com.swp391.gr3.ev_management.repository.InvoiceRepository;
import com.swp391.gr3.ev_management.repository.PaymentMethodRepository;
import com.swp391.gr3.ev_management.repository.StationStaffRepository;
import com.swp391.gr3.ev_management.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffPaymentServiceImpl implements StaffPaymentService {

    private final InvoiceRepository invoiceRepository;
    private final TransactionRepository transactionRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final StationStaffRepository stationStaffRepository;
    private final UnpaidInvoiceMapper mapper;

    @Override
    @Transactional
    public ConfirmPaymentResponse confirmPayment(ConfirmPaymentRequest request) {
        StationStaff stationStaff = stationStaffRepository.findActiveByStationStaffId(request.getStaffId())
                .orElseThrow(() -> new ErrorException("Staff not found or not active"));

        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new ErrorException("Invoice not found"));

        if (!stationStaff.getStation().getStationId()
                .equals(invoice.getSession().getBooking().getStation().getStationId())) {
            throw new ErrorException("Staff has no permission for this station");
        }

        if ("paid".equalsIgnoreCase(String.valueOf(invoice.getStatus()))) {
            throw new ErrorException("Invoice already paid");
        }

        if (request.getAmount() != invoice.getAmount()) {
            throw new ErrorException("Payment amount mismatch");
        }

        PaymentMethod method = paymentMethodRepository
                .findByMethodTypeAndProvider(request.getPaymentMethod(), PaymentProvider.VNPAY)
                .orElseGet(() -> {
                    PaymentMethod m = new PaymentMethod();
                    m.setMethodType(request.getPaymentMethod());
                    m.setProvider(PaymentProvider.VNPAY);
                    m.setAccountNo("N/A");
                    m.setCreatedAt(LocalDateTime.now());
                    m.setUpdatedAt(LocalDateTime.now());
                    return paymentMethodRepository.save(m);
                });

        Transaction tx = new Transaction();
        tx.setInvoice(invoice);
        tx.setPaymentMethod(method);
        tx.setAmount(request.getAmount());
        tx.setCurrency("VND");
        tx.setStatus("success");
        tx.setCreatedAt(LocalDateTime.now());
        tx.setUpdatedAt(LocalDateTime.now());
        transactionRepository.save(tx);

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(LocalDateTime.now());
        invoiceRepository.save(invoice);

        return ConfirmPaymentResponse.builder()
                .transactionId(tx.getTransactionId())
                .invoiceId(invoice.getInvoiceId())
                .sessionId(invoice.getSession().getSessionId())
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .paymentMethod(method.getMethodType())
                .status(tx.getStatus())
                .paidAt(invoice.getPaidAt())
                .staffId(stationStaff.getStaff().getUser().getUserId())
                .staffName(stationStaff.getStaff().getUser().getName())
                .message("Payment confirmed successfully")
                .build();
    }

    @Override
    public List<UnpaidInvoiceResponse> getUnpaidInvoicesByStation(Long stationId, Long userId) {
        StationStaff staff = stationStaffRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new ErrorException("Staff not found or not active"));

        if (!staff.getStation().getStationId().equals(stationId)) {
            throw new ErrorException("Staff has no permission for this station");
        }

        List<Invoice> invoices = invoiceRepository.findUnpaidInvoicesByStation(stationId);

        return invoices.stream()
                .map(mapper::mapToUnpaidInvoiceResponse)
                .collect(Collectors.toList());
    }
}
