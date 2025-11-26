package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.dto.response.DriverInvoiceDetail;
import com.swp391.gr3.ev_management.dto.response.UnpaidInvoiceResponse;
import com.swp391.gr3.ev_management.entity.Invoice;
import com.swp391.gr3.ev_management.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoice")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    /**
     * ================================
     * 1) Lấy chi tiết hóa đơn
     * ================================
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @GetMapping("/{invoiceId}")
    public DriverInvoiceDetail getInvoiceDetail(@PathVariable Long invoiceId) {
        return invoiceService.getInvoiceDetail(invoiceId);
    }

    /**
     * ================================
     * 2) Lấy danh sách hóa đơn chưa thanh toán của tài xế
     * ================================
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @PostMapping("/pay/{invoiceId}")
    public DriverInvoiceDetail payInvoice(@PathVariable Long invoiceId) {
        return invoiceService.payInvoice(invoiceId);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @GetMapping("/station/{stationId}/details")
    public List<DriverInvoiceDetail> getInvoiceDetailsByStation(@PathVariable Long stationId) {
        return invoiceService.getInvoiceDetailsByStation(stationId);
    }
}
