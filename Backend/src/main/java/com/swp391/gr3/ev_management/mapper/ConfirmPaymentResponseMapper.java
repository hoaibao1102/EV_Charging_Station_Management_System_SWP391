package com.swp391.gr3.ev_management.mapper;

import com.swp391.gr3.ev_management.dto.response.ConfirmPaymentResponse;
import com.swp391.gr3.ev_management.entity.Invoice;
import com.swp391.gr3.ev_management.entity.StationStaff;
import com.swp391.gr3.ev_management.entity.Transaction;
import org.springframework.stereotype.Component;

@Component
public class ConfirmPaymentResponseMapper {

    public ConfirmPaymentResponse map(Invoice invoice, Transaction tx, StationStaff stationStaff) {
        return ConfirmPaymentResponse.builder()
                .transactionId(tx.getTransactionId())
                .invoiceId(invoice.getInvoiceId())
                .sessionId(invoice.getSession().getSessionId())
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .paymentMethod(tx.getPaymentMethod().getMethodType())
                .status(tx.getStatus())
                .paidAt(invoice.getPaidAt())
                .staffId(stationStaff.getStaff().getUser().getUserId())
                .staffName(stationStaff.getStaff().getUser().getName())
                .message("Payment confirmed successfully")
                .build();
    }
}
