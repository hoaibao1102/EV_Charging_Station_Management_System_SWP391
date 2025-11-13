package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.ConfirmPaymentRequest;
import com.swp391.gr3.ev_management.dto.response.ConfirmPaymentResponse;
import com.swp391.gr3.ev_management.dto.response.UnpaidInvoiceResponse;
import com.swp391.gr3.ev_management.entity.Invoice;
import com.swp391.gr3.ev_management.entity.PaymentMethod;
import com.swp391.gr3.ev_management.entity.StationStaff;
import com.swp391.gr3.ev_management.entity.Transaction;
import com.swp391.gr3.ev_management.enums.InvoiceStatus;
import com.swp391.gr3.ev_management.enums.PaymentProvider;
import com.swp391.gr3.ev_management.enums.TransactionStatus;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.mapper.ConfirmPaymentResponseMapper;
import com.swp391.gr3.ev_management.mapper.UnpaidInvoiceMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service // Đánh dấu đây là một Spring Service xử lý nghiệp vụ thanh toán tại trạm
@RequiredArgsConstructor // Lombok tạo constructor cho các field final
public class StaffPaymentServiceImpl implements StaffPaymentService {

    // ==================== Dependencies được inject ====================
    private final InvoiceService invoiceService;                       // Lấy/sửa Invoice
    private final TransactionService transactionService;               // Lưu lịch sử thanh toán
    private final PaymentMethodService paymentMethodService;           // Lấy hoặc tạo phương thức thanh toán
    private final StationStaffService stationStaffService;             // Xác thực Staff đang làm việc ở trạm
    private final UnpaidInvoiceMapper mapper;                          // Mapper Invoice -> DTO UnpaidInvoiceResponse
    private final ConfirmPaymentResponseMapper confirmPaymentResponseMapper; // Mapper trả về sau khi xác nhận thanh toán

    /**
     * ===============================================================
     *  XÁC NHẬN THANH TOÁN (STAFF)
     * ===============================================================
     * - Staff chỉ được quyền xác nhận thanh toán tại trạm mà họ đang làm việc.
     * - Kiểm tra invoice tồn tại và chưa được thanh toán.
     * - Kiểm tra số tiền thanh toán trùng khớp.
     * - Tạo Transaction trạng thái COMPLETED.
     * - Cập nhật Invoice sang PAID.
     * - Trả về thông tin xác nhận thanh toán.
     */
    @Override
    @Transactional
    public ConfirmPaymentResponse confirmPayment(ConfirmPaymentRequest request) {

        // 1️⃣ Kiểm tra Staff có tồn tại và đang ACTIVE tại trạm
        StationStaff stationStaff = stationStaffService.findActiveByStationStaffId(request.getStaffId())
                .orElseThrow(() -> new ErrorException("Staff not found or not active"));

        // 2️⃣ Lấy Invoice cần thanh toán
        Invoice invoice = invoiceService.findById(request.getInvoiceId())
                .orElseThrow(() -> new ErrorException("Invoice not found"));

        // 3️⃣ Kiểm tra staff có quyền tại trạm của invoice hay không
        if (!stationStaff.getStation().getStationId()
                .equals(invoice.getSession().getBooking().getStation().getStationId())) {
            throw new ErrorException("Staff has no permission for this station");
        }

        // 4️⃣ Kiểm tra invoice đã thanh toán chưa
        if ("paid".equalsIgnoreCase(String.valueOf(invoice.getStatus()))) {
            throw new ErrorException("Invoice already paid");
        }

        // 5️⃣ Kiểm tra số tiền thanh toán có khớp với invoice không
        if (request.getAmount() != invoice.getAmount()) {
            throw new ErrorException("Payment amount mismatch");
        }

        // 6️⃣ Lấy hoặc tạo PaymentMethod (STAFF xác nhận thanh toán VNPAY/CASH)
        PaymentMethod method = paymentMethodService
                .findByMethodTypeAndProvider(request.getPaymentMethod(), PaymentProvider.VNPAY)
                .orElseGet(() -> {
                    // Nếu không tồn tại -> tạo mới phương thức thanh toán
                    PaymentMethod m = new PaymentMethod();
                    m.setMethodType(request.getPaymentMethod());
                    m.setProvider(PaymentProvider.VNPAY);
                    m.setAccountNo("N/A"); // thanh toán trực tiếp tại trạm
                    m.setCreatedAt(LocalDateTime.now());
                    m.setUpdatedAt(LocalDateTime.now());
                    return paymentMethodService.save(m);
                });

        // 7️⃣ Tạo một Transaction mới và đánh dấu COMPLETED
        Transaction tx = new Transaction();
        tx.setInvoice(invoice);
        tx.setPaymentMethod(method);
        tx.setAmount(request.getAmount());
        tx.setCurrency("VND");
        tx.setStatus(TransactionStatus.COMPLETED);
        tx.setCreatedAt(LocalDateTime.now());
        tx.setUpdatedAt(LocalDateTime.now());

        // Lưu transaction
        transactionService.addTransaction(tx);

        // 8️⃣ Cập nhật Invoice sang trạng thái PAID
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(LocalDateTime.now());
        invoiceService.save(invoice);

        // 9️⃣ Trả về kết quả sau khi xác nhận
        return confirmPaymentResponseMapper.map(invoice, tx, stationStaff);
    }

    /**
     * ===============================================================
     *  LẤY DANH SÁCH INVOICE CHƯA THANH TOÁN TẠI TRẠM
     * ===============================================================
     * - Staff chỉ được xem invoice của trạm họ đang làm việc.
     */
    @Override
    public List<UnpaidInvoiceResponse> getUnpaidInvoicesByStation(Long stationId, Long userId) {

        // 1️⃣ Kiểm tra staff theo userId có đang làm việc tại trạm không
        StationStaff staff = stationStaffService.findActiveByUserId(userId)
                .orElseThrow(() -> new ErrorException("Staff not found or not active"));

        // 2️⃣ Xác minh staff thuộc trạm yêu cầu
        if (!staff.getStation().getStationId().equals(stationId)) {
            throw new ErrorException("Staff has no permission for this station");
        }

        // 3️⃣ Lấy danh sách invoice chưa thanh toán
        List<Invoice> invoices = invoiceService.findUnpaidInvoicesByStation(stationId);

        // 4️⃣ Map -> DTO response
        return invoices.stream()
                .map(mapper::mapToUnpaidInvoiceResponse)
                .collect(Collectors.toList());
    }
}
