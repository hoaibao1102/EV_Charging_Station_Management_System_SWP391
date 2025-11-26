package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.entity.*;
import com.swp391.gr3.ev_management.enums.*;
import com.swp391.gr3.ev_management.events.NotificationCreatedEvent;
import com.swp391.gr3.ev_management.exception.ConflictException;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service // Đánh dấu đây là 1 Spring Service chứa logic xử lý thanh toán
@RequiredArgsConstructor // Tự động generate constructor cho các field final
public class PaymentService {

    // ====== Các service/phụ thuộc được inject ======
    private final InvoiceService invoiceService;                       // Làm việc với Invoice (hóa đơn)
    private final TransactionService transactionService;               // Làm việc với Transaction (giao dịch thanh toán)
    private final DriverService driverService;                         // Lấy thông tin Driver theo userId
    private final PaymentMethodService paymentMethodService;           // Lấy thông tin phương thức thanh toán
    private final ChargingSessionRepository chargingSessionRepository; // Lấy thông tin ChargingSession (phiên sạc)
    // ✅ thêm 2 bean sau để gửi thông báo
    private final NotificationsService notificationsService;           // Lưu Notification vào DB
    private final ApplicationEventPublisher eventPublisher;            // Publish event để gửi notify realtime/email...

    // (khuyến nghị) dùng timezone thống nhất cho toàn hệ thống
    private static final ZoneId TENANT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    // ====== Các config của VNPay được inject từ application.yml/properties ======

    @Value("${vnpay.tmnCode}") // Mã terminal code do VNPay cấp
    private String tmnCode;

    @Value("${vnpay.secretKey}") // Secret key dùng để ký HMAC SHA512
    private String secretKey;

    @Value("${vnpay.endpoint:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnpUrl; // Endpoint của VNPay (sandbox/production)

    @Value("${vnpay.returnUrl}") // ví dụ: http://localhost:8080/api/payment/vnpay/return
    private String returnUrl; // URL callback khi VNPay redirect về hệ thống

    /**
     * Tạo URL thanh toán VNPay cho 1 phiên sạc.
     * Bước chính:
     *  - Resolve driver, session, payment method
     *  - Lấy hóa đơn UNPAID tương ứng session
     *  - Tạo Transaction trạng thái PENDING
     *  - Build bộ tham số VNPay + ký HMAC + trả URL
     */
    @Transactional
    public String createVnPayPaymentUrl(Long userId,
                                        Long sessionId,
                                        Long paymentMethodId,
                                        String clientIp) throws Exception {

        // 1️⃣ Lấy driver theo userId (KHÔNG BẮT BUỘC NỮA)
        Driver driver = driverService.findByUser_UserId(userId).orElse(null);

        // 2️⃣ Invoice lấy theo session → invoice có thể không có driver
        Invoice invoice = invoiceService.findBySession_SessionId(sessionId)
                .orElseThrow(() -> new ErrorException(
                        "No invoice found for session " + sessionId + ". Stop session must create an UNPAID invoice first."));

        // 3️⃣ Lấy phương thức thanh toán theo paymentMethodId
        PaymentMethod method = paymentMethodService.findById(paymentMethodId)
                .orElseThrow(() -> new ErrorException("Payment method not found"));

        // 5️⃣ Ràng buộc: chỉ cho phép thanh toán khi Invoice đang ở trạng thái UNPAID
        if (invoice.getStatus() != InvoiceStatus.UNPAID) {
            throw new ConflictException("Invoice #" + invoice.getInvoiceId() + " is not UNPAID (current: " + invoice.getStatus() + ")");
        }

        // 6️⃣ Lấy số tiền và loại tiền từ Invoice (đảm bảo thanh toán đúng số đã tính)
        double amount = invoice.getAmount();
        String currency = invoice.getCurrency(); // thường là "VND"

        // 7️⃣ Tạo Transaction với trạng thái PENDING, gắn với Invoice, Driver, PaymentMethod
        Transaction tx = Transaction.builder()
                .amount(amount)
                .currency(currency)
                .description("Thanh toán hóa đơn #" + invoice.getInvoiceId() + " qua VNPay")
                .status(TransactionStatus.PENDING)
                .driver(driver) // driver có thể null
                .invoice(invoice)
                .paymentMethod(method)
                .build();
        tx = transactionService.save(tx);

        // 8️⃣ Chuẩn bị các tham số bắt buộc của VNPay
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String createDate = LocalDateTime.now().format(fmt); // thời gian tạo request

        // VNPay yêu cầu vnp_Amount = amount * 100 (đơn vị = tiền * 100)
        long vnpAmount = Math.round(amount * 100);

        // 9️⃣ Sử dụng transactionId làm vnp_TxnRef (mã tham chiếu giao dịch)
        String txnRef = "TX" + tx.getTransactionId();

        // 🔟 Dùng TreeMap để giữ param theo thứ tự key tăng dần (theo yêu cầu VNPay khi ký)
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
        // Khi VNPay redirect về returnUrl sẽ trả kèm invoiceId & transactionId để xử lý
        vnpParams.put("vnp_ReturnUrl", returnUrl + "?invoiceId=" + invoice.getInvoiceId() + "&transactionId=" + tx.getTransactionId());
        vnpParams.put("vnp_CreateDate", createDate);
        vnpParams.put("vnp_IpAddr", clientIp); // IP client (theo tài liệu VNPay)

        // 1️⃣1️⃣ Build chuỗi data để ký HMAC (KHÔNG bao gồm vnp_SecureHash)
        String signData = buildQuery(vnpParams, true);
        // 1️⃣2️⃣ Ký HMAC SHA512 với secretKey
        String secureHash = hmacSHA512(secretKey, signData);

        // 1️⃣3️⃣ Gắn thêm vnp_SecureHash vào param gửi đi
        vnpParams.put("vnp_SecureHash", secureHash);
        // 1️⃣4️⃣ Build query string hoàn chỉnh cho URL
        String query = buildQuery(vnpParams, false);

        // 1️⃣5️⃣ Trả về URL thanh toán VNPay
        return vnpUrl + "?" + query;
    }

    /**
     * buildQuery: build chuỗi query string từ map params.
     *  - Nếu isForSign = true: bỏ qua vnp_SecureHash, dùng cho việc ký.
     *  - Nếu isForSign = false: build đầy đủ để redirect sang VNPay.
     */
    private String buildQuery(Map<String, String> params, boolean isForSign) throws Exception {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            // Khi ký, bỏ qua vnp_SecureHash
            if (isForSign && "vnp_SecureHash".equals(e.getKey())) continue;
            builder.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8.toString()))
                    .append("=")
                    .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8.toString()))
                    .append("&");
        }
        // Xóa dấu & cuối cùng nếu có
        if (builder.length() > 0) builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }

    /**
     * Hàm ký HMAC SHA512: dùng secretKey để ký chuỗi data.
     * Trả về chuỗi hexa lowercase.
     */
    private String hmacSHA512(String key, String data) throws NoSuchAlgorithmException, InvalidKeyException {
        // Trim key để loại các ký tự thừa như \r, \n, khoảng trắng
        String safeKey = key == null ? "" : key.trim();

        Mac hmac = Mac.getInstance("HmacSHA512");
        SecretKeySpec keySpec = new SecretKeySpec(safeKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        hmac.init(keySpec);
        byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));

        // Convert byte[] -> hex string
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    /**
     * Xử lý callback từ VNPay (returnUrl), cập nhật trạng thái Invoice/Transaction.
     * Các bước:
     *  - Validate chữ ký (vnp_SecureHash) theo 2 cách (raw query & param map)
     *  - Lấy invoiceId & transactionId từ query custom
     *  - Nếu vnp_ResponseCode = "00" -> thành công -> set PAID/COMPLETED + notify
     *  - Ngược lại -> FAILED + notify
     */
    @Transactional
    public void handleVnPayReturn(HttpServletRequest request) throws Exception {
        // ==== 0) Chuẩn hóa secretKey ====
        this.secretKey = (this.secretKey == null) ? "" : this.secretKey.trim();

        // ==== 1) Lấy hash nhận được từ VNPay ====
        String receivedHashParam = request.getParameter("vnp_SecureHash");
        String receivedHash = (receivedHashParam == null) ? "" : receivedHashParam.trim();

        // ==== 2A) Cách A: ký theo RAW QUERY (dùng queryString gốc, giữ nguyên percent-encoding) ====
        String rawQuery = Optional.ofNullable(request.getQueryString()).orElse("");
        Map<String, String> vnpRaw = new HashMap<>();
        // Parse queryString thủ công
        for (String pair : rawQuery.split("&")) {
            int i = pair.indexOf('=');
            if (i <= 0) continue;
            String k = pair.substring(0, i);
            String v = pair.substring(i + 1); // GIỮ NGUYÊN percent-encoding, không decode
            if (k.startsWith("vnp_")) vnpRaw.put(k, v);
        }
        // Lấy vnp_SecureHash từ map raw (nếu có)
        String receivedHashRaw = Optional.ofNullable(vnpRaw.remove("vnp_SecureHash")).orElse(receivedHash);
        // vnp_SecureHashType không tham gia ký
        vnpRaw.remove("vnp_SecureHashType");
        // Sắp xếp key theo thứ tự tăng dần
        SortedMap<String, String> rawSorted = new TreeMap<>(vnpRaw);
        // Build chuỗi dataToSign: key=value&key2=value2 ...
        StringBuilder rawDataToSign = new StringBuilder();
        for (Map.Entry<String, String> e : rawSorted.entrySet()) {
            if (rawDataToSign.length() > 0) rawDataToSign.append('&');
            rawDataToSign.append(e.getKey()).append('=').append(e.getValue()); // GIỮ NGUYÊN giá trị (encoded)
        }
        String rawExpected = hmacSHA512(secretKey, rawDataToSign.toString());

        // ==== 2B) Cách B: ký theo PARAM MAP (đã decode) + encode lại theo RFC3986 ====
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

        // ==== 3) Chấp nhận chữ ký nếu 1 trong 2 cách khớp ====
        boolean ok =
                (rawExpected.equalsIgnoreCase(receivedHash) || rawExpected.equalsIgnoreCase(receivedHashRaw))
                        || (decExpected.equalsIgnoreCase(receivedHash) || decExpected.equalsIgnoreCase(receivedHashDec));

        if (!ok) {
            // Debug log phục vụ kiểm tra khi sai chữ ký (không nên log ở môi trường production)
            System.out.println("[VNPay] received      = " + receivedHash);
            System.out.println("[VNPay] RAW  dataSign = " + rawDataToSign);
            System.out.println("[VNPay] RAW  expected = " + rawExpected);
            System.out.println("[VNPay] DEC  dataSign = " + decDataToSign);
            System.out.println("[VNPay] DEC  expected = " + decExpected);
            throw new SecurityException("Invalid VNPay signature");
        }

        // ==== 4) Lấy các tham số nghiệp vụ của hệ thống (do mình thêm vào ReturnUrl) ====
        Long invoiceId = Long.valueOf(request.getParameter("invoiceId"));
        Long transactionId = Long.valueOf(request.getParameter("transactionId"));

        // ==== 5) Lấy Invoice & Transaction tương ứng để cập nhật ====
        Invoice invoice = invoiceService.findById(invoiceId)
                .orElseThrow(() -> new ErrorException("Invoice not found"));
        Transaction tx = transactionService.findById(transactionId)
                .orElseThrow(() -> new ErrorException("Transaction not found"));

        // vnp_ResponseCode = "00" -> thanh toán thành công
        String responseCode = request.getParameter("vnp_ResponseCode");
        if ("00".equals(responseCode)) {
            // 5.1) Cập nhật trạng thái transaction & invoice
            tx.setStatus(TransactionStatus.COMPLETED);
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaidAt(LocalDateTime.now(TENANT_ZONE));
            transactionService.addTransaction(tx);
            invoiceService.save(invoice);

            // ===== ✅ Gửi Notification: thanh toán thành công =====
            var driver = invoice.getDriver(); // lấy Driver từ Invoice
            var user = (driver != null) ? driver.getUser() : null;

            var session = invoice.getSession();
            var booking = (session != null) ? session.getBooking() : null;
            var station = (booking != null) ? booking.getStation() : null;

            String stationName = station != null ? station.getStationName() : "Trạm sạc";
            String title = "Thanh toán thành công hóa đơn #" + invoice.getInvoiceId();
            String content = "Số tiền: " + String.format("%,.0f", invoice.getAmount()) + " " + invoice.getCurrency()
                    + " | Trạm: " + stationName
                    + (session != null && session.getStartTime() != null
                    ? " | Thời gian sạc: " + session.getStartTime()
                    : "");

            Notification noti = new Notification();
            noti.setUser(user);
            noti.setTitle(title);
            noti.setContentNoti(content);
            noti.setType(NotificationTypes.PAYMENT_SUCCESS); // Enum thể hiện notify thanh toán thành công
            noti.setStatus(Notification.STATUS_UNREAD);
            noti.setTransaction(tx);
            noti.setSession(invoice.getSession());
            noti.setCreatedAt(LocalDateTime.now(TENANT_ZONE));
            notificationsService.save(noti);

            // bắn event để các listener (websocket, email, push...) xử lý
            eventPublisher.publishEvent(new NotificationCreatedEvent(noti.getNotiId()));

        } else {
            // 5.2) Trong trường hợp VNPay trả về mã lỗi != "00" -> giao dịch thất bại
            tx.setStatus(TransactionStatus.FAILED);
            if (invoice.getStatus() == InvoiceStatus.PENDING) {
                invoice.setStatus(InvoiceStatus.FAILED);
            }
            transactionService.addTransaction(tx);
            invoiceService.save(invoice);

            // ===== Notify: thanh toán thất bại =====
            var driver = invoice.getDriver();
            var user = (driver != null) ? driver.getUser() : null;

            Notification noti = new Notification();
            noti.setUser(user);
            noti.setTitle("Thanh toán thất bại cho hóa đơn #" + invoice.getInvoiceId());
            noti.setContentNoti("Mã phản hồi VNPay: " + responseCode + ". Vui lòng thử lại.");
            noti.setType(NotificationTypes.PAYMENT_FAILED);
            noti.setStatus(Notification.STATUS_UNREAD);
            noti.setTransaction(tx);
            noti.setSession(invoice.getSession());
            noti.setCreatedAt(LocalDateTime.now(TENANT_ZONE));
            notificationsService.save(noti);

            eventPublisher.publishEvent(new NotificationCreatedEvent(noti.getNotiId()));
        }
    }

    /**
     * Hàm encode theo chuẩn RFC3986 (space = %20, giữ ký tự ~)
     * Dùng khi build data ký HMAC theo cách decode-then-encode.
     */
    private static String rfc3986(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("%7E", "~");
    }

    /**
     * Xử lý thanh toán nội bộ (EVM - ví nội bộ, tiền mặt, v.v...)
     * Không qua cổng VNPay:
     *  - Tạo Transaction trạng thái COMPLETED luôn
     *  - Đổi Invoice sang PAID
     *  - Gửi Notification
     */
    @Transactional
    public String processEvmPayment(Long userId, Long sessionId, Long paymentMethodId) {
        // 1️⃣ Resolve các entity cần thiết

        // 1.1) Lấy driver theo userId
        Driver driver = driverService.findByUser_UserId(userId).orElse(null);

        // 1.2) Lấy session
        ChargingSession session = chargingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ErrorException("Session not found"));

        // 1.3) Lấy phương thức thanh toán (EVM/CASH...)
        PaymentMethod method = paymentMethodService.findById(paymentMethodId)
                .orElseThrow(() -> new ErrorException("Payment method not found"));

        // 2️⃣ Lấy invoice UNPAID gắn với session
        Invoice invoice = invoiceService.findBySession_SessionId(sessionId)
                .orElseThrow(() -> new ErrorException(
                        "No invoice found for session " + sessionId + ". Stop session must create an UNPAID invoice first."));

        // Ràng buộc: chỉ xử lý nếu invoice đang UNPAID
        if (invoice.getStatus() != InvoiceStatus.UNPAID) {
            throw new ConflictException("Invoice #" + invoice.getInvoiceId() + " is not UNPAID (current: " + invoice.getStatus() + ")");
        }

        double amount = invoice.getAmount();
        String currency = invoice.getCurrency();

        // 3️⃣ Tạo Transaction ở trạng thái COMPLETED luôn (do EVM xử lý nội bộ, không có callback)
        Transaction tx = Transaction.builder()
                .amount(amount)
                .currency(currency)
                .description("Thanh toán hóa đơn #" + invoice.getInvoiceId() + " qua EVM")
                .status(TransactionStatus.COMPLETED)
                .driver(driver) // driver có thể null
                .invoice(invoice)
                .paymentMethod(method)
                .build();
        tx = transactionService.save(tx);

        // 4️⃣ Cập nhật Invoice sang PAID
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(LocalDateTime.now(TENANT_ZONE));
        invoiceService.save(invoice);

        // 5️⃣ Gửi notification giống logic thanh toán VNPay thành công
//        var user = driver.getUser();
        var booking = (session != null) ? session.getBooking() : null;
        var station = (booking != null) ? booking.getStation() : null;

        String stationName = station != null ? station.getStationName() : "Trạm sạc";
        String title = "Thanh toán thành công hóa đơn #" + invoice.getInvoiceId();
        String content = "Số tiền: " + String.format("%,.0f", invoice.getAmount()) + " " + invoice.getCurrency()
                + " | Trạm: " + stationName
                + (session != null && session.getStartTime() != null
                ? " | Thời gian sạc: " + session.getStartTime()
                : "");

        Driver invDriver = invoice.getDriver();
        User users = (invDriver != null) ? invDriver.getUser() : null;

        Notification noti = new Notification();
        noti.setUser(users);
        noti.setTitle(title);
        noti.setContentNoti(content);
        noti.setType(NotificationTypes.PAYMENT_SUCCESS);
        noti.setStatus(Notification.STATUS_UNREAD);
        noti.setTransaction(tx);
        noti.setSession(invoice.getSession());
        noti.setCreatedAt(LocalDateTime.now(TENANT_ZONE));
        notificationsService.save(noti);

        if (users != null) {
            eventPublisher.publishEvent(new NotificationCreatedEvent(noti.getNotiId()));
        }

        return "Payment successful (EVM)";
    }

    /**
     * ✅ Helper kiểm tra PaymentMethod có phải là VNPay không
     *  - Thường dựa vào provider (VNPAY)
     */
    public boolean isVnPayMethod(Long paymentMethodId) {
        PaymentMethod method = paymentMethodService.findById(paymentMethodId)
                .orElseThrow(() -> new ErrorException("Payment method not found"));
        // tuỳ bạn định nghĩa, có thể dựa vào provider hoặc type
        return method.getProvider() == PaymentProvider.VNPAY;
    }

    /**
     * ✅ Helper kiểm tra PaymentMethod có phải là EVM nội bộ (ví dụ: CASH) không
     */
    public boolean isEvmMethod(Long paymentMethodId) {
        PaymentMethod method = paymentMethodService.findById(paymentMethodId)
                .orElseThrow(() -> new ErrorException("Payment method not found"));
        return method.getMethodType() == PaymentType.CASH;
    }
}
