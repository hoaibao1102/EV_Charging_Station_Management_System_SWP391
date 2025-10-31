package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.entity.*;
import com.swp391.gr3.ev_management.enums.InvoiceStatus;
import com.swp391.gr3.ev_management.enums.TransactionStatus;
import com.swp391.gr3.ev_management.exception.ConflictException;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.repository.*;
import jakarta.servlet.http.HttpServletRequest;
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
    public String createVnPayPaymentUrl(Long userId,
                                        Long sessionId,
                                        Long paymentMethodId,
                                        String clientIp) throws Exception {

        // 1) Resolve driver từ userId
        Driver driver = driverRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new ErrorException("Driver not found for current user"));

        ChargingSession session = chargingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ErrorException("Session not found"));

        PaymentMethod method = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new ErrorException("Payment method not found"));

        // 1) Lấy invoice đã tạo ở bước stop session
        Invoice invoice = invoiceRepository.findBySession_SessionId(sessionId)
                .orElseThrow(() -> new ErrorException(
                        "No invoice found for session " + sessionId + ". Stop session must create an UNPAID invoice first."));

        // 2) Chỉ cho phép thanh toán khi invoice còn UNPAID
        if (invoice.getStatus() != InvoiceStatus.UNPAID) {
            throw new ConflictException("Invoice #" + invoice.getInvoiceId() + " is not UNPAID (current: " + invoice.getStatus() + ")");
        }

        // 3) Lấy amount/currency từ invoice để đảm bảo đúng số tiền cần thu
        double amount = invoice.getAmount();
        String currency = invoice.getCurrency(); // thường là "VND"

        // 4) Tạo Transaction PENDING gắn với invoice
        Transaction tx = Transaction.builder()
                .amount(amount)
                .currency(currency)
                .description("Thanh toán hóa đơn #" + invoice.getInvoiceId() + " qua VNPay")
                .status(TransactionStatus.PENDING) // hoặc TransactionStatus.PENDING.name()
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
        // Trim key để loại \r, \n, space vô tình khi copy
        String safeKey = key == null ? "" : key.trim();

        Mac hmac = Mac.getInstance("HmacSHA512");
        SecretKeySpec keySpec = new SecretKeySpec(safeKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
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
    public void handleVnPayReturn(HttpServletRequest request) throws Exception {
        // ==== 0) Chuẩn hóa secretKey ====
        this.secretKey = (this.secretKey == null) ? "" : this.secretKey.trim();

        // ==== 1) Lấy hash nhận được ====
        String receivedHashParam = request.getParameter("vnp_SecureHash");
        String receivedHash = (receivedHashParam == null) ? "" : receivedHashParam.trim();

        // ==== 2A) Cách A: ký theo RAW QUERY (percent-encoded gốc) ====
        String rawQuery = Optional.ofNullable(request.getQueryString()).orElse("");
        Map<String, String> vnpRaw = new HashMap<>();
        for (String pair : rawQuery.split("&")) {
            int i = pair.indexOf('=');
            if (i <= 0) continue;
            String k = pair.substring(0, i);
            String v = pair.substring(i + 1); // GIỮ NGUYÊN percent-encoding
            if (k.startsWith("vnp_")) vnpRaw.put(k, v);
        }
        String receivedHashRaw = Optional.ofNullable(vnpRaw.remove("vnp_SecureHash")).orElse(receivedHash);
        vnpRaw.remove("vnp_SecureHashType");
        SortedMap<String, String> rawSorted = new TreeMap<>(vnpRaw);
        StringBuilder rawDataToSign = new StringBuilder();
        for (Map.Entry<String, String> e : rawSorted.entrySet()) {
            if (rawDataToSign.length() > 0) rawDataToSign.append('&');
            rawDataToSign.append(e.getKey()).append('=').append(e.getValue()); // GIỮ NGUYÊN
        }
        String rawExpected = hmacSHA512(secretKey, rawDataToSign.toString());

        // ==== 2B) Cách B: ký theo PARAM MAP (đã decode) + RFC3986 re-encode ====
        Map<String, String[]> pm = request.getParameterMap();
        Map<String, String> vnpDecoded = new HashMap<>();
        pm.forEach((k, v) -> {
            if (k.startsWith("vnp_")) vnpDecoded.put(k, (v != null && v.length > 0) ? v[0] : "");
        });
        String receivedHashDec = Optional.ofNullable(vnpDecoded.remove("vnp_SecureHash")).orElse(receivedHash);
        vnpDecoded.remove("vnp_SecureHashType");
        SortedMap<String, String> decSorted = new TreeMap<>(vnpDecoded);
        StringBuilder decDataToSign = new StringBuilder();
        for (Map.Entry<String, String> e : decSorted.entrySet()) {
            if (decDataToSign.length() > 0) decDataToSign.append('&');
            decDataToSign.append(rfc3986(e.getKey())).append('=').append(rfc3986(e.getValue()));
        }
        String decExpected = hmacSHA512(secretKey, decDataToSign.toString());

        // ==== 3) Chấp nhận nếu một trong hai cách khớp ====
        boolean ok =
                (rawExpected.equalsIgnoreCase(receivedHash) || rawExpected.equalsIgnoreCase(receivedHashRaw))
                        || (decExpected.equalsIgnoreCase(receivedHash) || decExpected.equalsIgnoreCase(receivedHashDec));

        if (!ok) {
            // Bật log debug tạm thời để so đối chiếu (đừng log ở prod)
            System.out.println("[VNPay] received      = " + receivedHash);
            System.out.println("[VNPay] RAW  dataSign = " + rawDataToSign);
            System.out.println("[VNPay] RAW  expected = " + rawExpected);
            System.out.println("[VNPay] DEC  dataSign = " + decDataToSign);
            System.out.println("[VNPay] DEC  expected = " + decExpected);
            throw new SecurityException("Invalid VNPay signature");
        }

        // ==== 4) Lấy param nghiệp vụ của bạn (KHÔNG tham gia ký) ====
        Long invoiceId = Long.valueOf(request.getParameter("invoiceId"));
        Long transactionId = Long.valueOf(request.getParameter("transactionId"));

        // ==== 5) Xử lý trạng thái ====
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ErrorException("Invoice not found"));
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ErrorException("Transaction not found"));

        String responseCode = request.getParameter("vnp_ResponseCode");
        if ("00".equals(responseCode)) {
            tx.setStatus(TransactionStatus.COMPLETED);
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaidAt(LocalDateTime.now());
        } else {
            tx.setStatus(TransactionStatus.FAILED);
            if (invoice.getStatus() == InvoiceStatus.PENDING) {
                invoice.setStatus(InvoiceStatus.FAILED);
            }
        }
        transactionRepository.save(tx);
        invoiceRepository.save(invoice);

        System.out.println("[VNPay] URL=" + request.getRequestURL()
                + " | query=" + request.getQueryString()
                + " | method=" + request.getMethod());
    }

    // Encode chuẩn RFC3986 (space = %20, giữ ~)
    private static String rfc3986(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("%7E", "~");
    }
}
