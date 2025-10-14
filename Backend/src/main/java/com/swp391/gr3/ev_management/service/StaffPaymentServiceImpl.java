package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.ConfirmPaymentRequest;
import com.swp391.gr3.ev_management.DTO.response.ConfirmPaymentResponse;
import com.swp391.gr3.ev_management.DTO.response.UnpaidInvoiceResponse;
import com.swp391.gr3.ev_management.entity.Invoice;
import com.swp391.gr3.ev_management.entity.PaymentMethod;
import com.swp391.gr3.ev_management.entity.StationStaff;
import com.swp391.gr3.ev_management.entity.Transaction;
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
        StationStaff staff = stationStaffRepository.findActiveByUserId(request.getStaffId())
                .orElseThrow(() -> new RuntimeException("Staff not found or not active"));

        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        System.out.println("DEBUG ✅ Step1: staff = " + staff.getUser().getName()
                + ", stationId=" + staff.getStation().getStationId());
        System.out.println("DEBUG ✅ Step2: invoice = " + invoice.getInvoiceId()
                + ", status=" + invoice.getStatus()
                + ", sessionId=" + invoice.getSession().getSessionId());
        System.out.println("DEBUG ✅ Step3: session.booking.stationId="
                + invoice.getSession().getBooking().getStation().getStationId());

        System.out.println("DEBUG Station compare: staff.stationId=" + staff.getStation().getStationId()
                + " vs booking.stationId=" + invoice.getSession().getBooking().getStation().getStationId());


        if (!staff.getStation().getStationId()
                .equals(invoice.getSession().getBooking().getStation().getStationId())) {
            throw new RuntimeException("Staff has no permission for this station");
        }

        if ("paid".equalsIgnoreCase(invoice.getStatus())) {
            throw new RuntimeException("Invoice already paid");
        }

        if (request.getAmount() != invoice.getAmount()) {
            throw new RuntimeException("Payment amount mismatch");
        }

        PaymentMethod method = paymentMethodRepository
                .findByMethodTypeAndProvider(request.getPaymentMethod(), "Cash")
                .orElseGet(() -> {
                    PaymentMethod m = new PaymentMethod();
                    m.setMethodType(request.getPaymentMethod());
                    m.setProvider("Cash");
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

        invoice.setStatus("paid");
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
                .staffId(staff.getUser().getUserId())
                .staffName(staff.getUser().getName())
                .message("Payment confirmed successfully")
                .build();
    }

    @Override
    public List<UnpaidInvoiceResponse> getUnpaidInvoicesByStation(Long stationId, Long staffId) {
        StationStaff staff = stationStaffRepository.findActiveByUserId(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found or not active"));

        if (!staff.getStation().getStationId().equals(stationId)) {
            throw new RuntimeException("Staff has no permission for this station");
        }

        List<Invoice> invoices = invoiceRepository.findUnpaidInvoicesByStation(stationId);

        return invoices.stream()
                .map(mapper::mapToUnpaidInvoiceResponse)
                .collect(Collectors.toList());
    }
}
