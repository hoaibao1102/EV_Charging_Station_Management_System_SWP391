package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.entity.*;
import com.swp391.gr3.ev_management.enums.InvoiceStatus;
import com.swp391.gr3.ev_management.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final InvoiceRepository invoiceRepository;
    private final TransactionRepository transactionRepository;
    private final DriverRepository driverRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final ChargingSessionRepository chargingSessionRepository;

    // Đưa các cấu hình ra application.yml/properties
    @Value("${vnpay.tmnCode}")
    private String tmnCode;

    @Value("${vnpay.secretKey}")
    private String secretKey;

    @Value("${vnpay.endpoint:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnpUrl;

    @Value("${vnpay.returnUrl}") // ví dụ: http://localhost:8080/api/payment/vnpay/return
    private String returnUrl;

    @Transactional
    public String createVnPayPaymentUrl(Long driverId,
                                        Long sessionId,
                                        Long paymentMethodId,
                                        String clientIp) throws Exception {

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found"));

        ChargingSession session = chargingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        PaymentMethod method = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new IllegalArgumentException("Payment method not found"));

        // 1) Lấy invoice đã tạo ở bước stop session
        Invoice invoice = invoiceRepository.findBySession_SessionId(sessionId)
                .orElseThrow(() -> new IllegalStateException(
                        "No invoice found for session " + sessionId + ". Stop session must create an UNPAID invoice first."));

        // 2) Chỉ cho phép thanh toán khi invoice còn UNPAID
        if (invoice.getStatus() != InvoiceStatus.UNPAID) {
            throw new IllegalStateException("Invoice #" + invoice.getInvoiceId() + " is not UNPAID (current: " + invoice.getStatus() + ")");
        }

        // 3) Lấy amount/currency từ invoice để đảm bảo đúng số tiền cần thu
        double amount = invoice.getAmount();
        String currency = invoice.getCurrency(); // thường là "VND"

        // 4) Tạo Transaction PENDING gắn với invoice
        Transaction tx = Transaction.builder()
                .amount(amount)
                .currency(currency)
                .description("Thanh toán hóa đơn #" + invoice.getInvoiceId() + " qua VNPay")
                .status("Pending") // hoặc TransactionStatus.PENDING.name()
                .driver(driver)
                .invoice(invoice)
                .paymentMethod(method)
                .build();
        tx = transactionRepository.save(tx);

        // 5) Tham số VNPay
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String createDate = LocalDateTime.now().format(fmt);

        // VNPay yêu cầu vnp_Amount = amount * 100
        long vnpAmount = Math.round(amount * 100);

        // Dùng transactionId để đối soát
        String txnRef = "TX" + tx.getTransactionId();

        Map<String, String> vnpParams = new TreeMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", tmnCode);
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_CurrCode", currency);      // "VND"
        vnpParams.put("vnp_TxnRef", txnRef);
        vnpParams.put("vnp_OrderInfo", "Thanh toan hoa don #" + invoice.getInvoiceId());
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Amount", String.valueOf(vnpAmount));
        vnpParams.put("vnp_ReturnUrl", returnUrl + "?invoiceId=" + invoice.getInvoiceId() + "&transactionId=" + tx.getTransactionId());
        vnpParams.put("vnp_CreateDate", createDate);
        vnpParams.put("vnp_IpAddr", clientIp);

        // 6) Ký hash
        String signData = buildQuery(vnpParams, true);
        String secureHash = hmacSHA512(secretKey, signData);

        vnpParams.put("vnp_SecureHash", secureHash);
        String query = buildQuery(vnpParams, false);

        return vnpUrl + "?" + query;
    }

    // buildQuery: nếu isForSign=true thì encode theo key=value nối &, không bao gồm vnp_SecureHash
    private String buildQuery(Map<String, String> params, boolean isForSign) throws Exception {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (isForSign && "vnp_SecureHash".equals(e.getKey())) continue;
            builder.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8.toString()))
                    .append("=")
                    .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8.toString()))
                    .append("&");
        }
        if (builder.length() > 0) builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }

    private String hmacSHA512(String key, String data) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac hmac = Mac.getInstance("HmacSHA512");
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        hmac.init(keySpec);
        byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    /**
     * Xử lý callback từ VNPay (returnUrl), cập nhật trạng thái Invoice/Transaction
     * VNPay sẽ trả về nhiều param như vnp_ResponseCode, vnp_TxnRef, vnp_SecureHash, ...
     */
    @Transactional
    public void handleVnPayReturn(Map<String, String> allParams) throws Exception {
        // 1) Xác thực chữ ký
        Map<String, String> sorted = new TreeMap<>(allParams);
        String receivedHash = sorted.remove("vnp_SecureHash"); // bỏ ra để ký lại
        String dataToSign = buildQuery(sorted, true);
        String expectedHash = hmacSHA512(secretKey, dataToSign);
        if (!expectedHash.equalsIgnoreCase(receivedHash)) {
            throw new SecurityException("Invalid VNPay signature");
        }

        // 2) Lấy invoiceId & transactionId từ returnUrl query (mình đã đính kèm khi tạo URL)
        Long invoiceId = Long.valueOf(sorted.get("invoiceId"));
        Long transactionId = Long.valueOf(sorted.get("transactionId"));

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        // 3) Đọc mã phản hồi VNPay
        String responseCode = sorted.get("vnp_ResponseCode"); // "00" là thành công
        if ("00".equals(responseCode)) {
            tx.setStatus("Completed"); // hoặc TransactionStatus.COMPLETED.name()
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaidAt(LocalDateTime.now());
        } else {
            tx.setStatus("Failed"); // hoặc FAILED
            // chỉ đổi Invoice sang FAILED nếu chưa có giao dịch thành công nào khác
            if (invoice.getStatus() == InvoiceStatus.PENDING) {
                invoice.setStatus(InvoiceStatus.FAILED);
            }
        }

        transactionRepository.save(tx);
        invoiceRepository.save(invoice);
    }
}
