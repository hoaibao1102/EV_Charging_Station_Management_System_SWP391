package com.swp391.gr3.ev_management.service;

import java.time.Instant;
import java.util.Date;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.swp391.gr3.ev_management.entity.User;
import com.swp391.gr3.ev_management.repository.UserRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service // Đánh dấu đây là Spring Service dùng để xử lý JWT (tạo/validate/đọc token)
@RequiredArgsConstructor // Lombok tạo constructor cho các field final để DI
public class TokenService {

    private final UserRepository userRepository; // Dùng để lấy thông tin User từ DB (khi đọc token ra userId)

    // Lấy secret từ properties (nên lưu dạng base64 trong file cấu hình)
    @Value("${app.jwtSecret}")
    private String jwtSecret;

    /**
     * Tạo SecretKey từ chuỗi secret.
     * - jwtSecret được cấu hình ở application.properties/yml dưới dạng Base64.
     * - Giải mã Base64 -> mảng bytes -> tạo HMAC key (HS256).
     */
    private SecretKey getSignInKey() {
        // jwtSecret nên là Base64-encoded (recommended).
        // Nếu bạn muốn dùng hex, đổi phần decode tương ứng.
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Sinh JWT cho 1 user cụ thể.
     * - Subject: userId (string).
     * - Claims: fullName, role.
     * - Expiration: now + 9000 giây (~15 phút, dù comment ghi 15 nhưng 9000s = 150 phút, tuỳ bạn).
     * - Ký bằng HS256 với secret key.
     */
    public String generateToken(User user) {
        String roleName = "DRIVER"; // giá trị mặc định nếu không lấy được role từ user
        try {
            // Thử lấy role từ entity User được truyền vào
            if (user.getRole() != null && user.getRole().getRoleName() != null) {
                roleName = user.getRole().getRoleName();
            } else {
                // Nếu role chưa được load (lazy) hoặc null -> load lại user từ DB rồi lấy role
                User fresh = userRepository.findUserByUserId(user.getUserId());
                if (fresh != null && fresh.getRole() != null && fresh.getRole().getRoleName() != null) {
                    roleName = fresh.getRole().getRoleName();
                }
            }
        } catch (Exception e) {
            // Nếu có lỗi gì trong quá trình lấy role -> fallback về "DRIVER"
            roleName = "DRIVER";
        }

        // Build JWT:
        // - setSubject: gán userId làm subject
        // - claim: thêm custom claim "fullName" và "role"
        // - setIssuedAt: thời điểm phát hành token
        // - setExpiration: thời điểm hết hạn token
        // - signWith: ký bằng HS256 với secret key
        return Jwts.builder()
                .setSubject(String.valueOf(user.getUserId()))
                .claim("fullName", user.getName())
                .claim("role", roleName)
                .setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plusSeconds(9000))) // 15 phút (theo comment, nhưng thực tế là 9000s)
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Đọc token JWT và trả về User tương ứng.
     * - Lấy subject từ token (chính là userId dạng String).
     * - parse Long -> tìm user trong DB.
     */
    public User extractToken(String token) {
        String value = extractClaim(token, Claims::getSubject); // lấy subject (userId)
        long userId = Long.parseLong(value);
        return userRepository.findUserByUserId(userId);
    }

    /**
     * Parse token và trả về toàn bộ Claims bên trong.
     * - Đồng thời validate chữ ký bằng verifyWith(getSignInKey()).
     */
    public Claims extractAllClaims(String token) {
        return  Jwts.parser().
                verifyWith(getSignInKey())        // dùng secret key để verify signature
                .build()
                .parseSignedClaims(token)         // parse token dạng signed
                .getPayload();                    // lấy phần payload (Claims)
    }

    /**
     * Helper generic để đọc 1 claim bất kỳ từ token.
     * - resolver: hàm truyền vào để lấy field từ Claims (ví dụ Claims::getSubject).
     */
    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = extractAllClaims(token); // parse toàn bộ claims trước
        return resolver.apply(claims);           // áp dụng hàm resolver để lấy ra giá trị mong muốn
    }

    /**
     * Lấy expiration time (Instant) từ JWT.
     * - Trả về null nếu claim exp không tồn tại.
     */
    public Instant getExpirationFromJwt(String token) {
        Claims claims = extractAllClaims(token);
        Date exp = claims.getExpiration();
        return (exp == null) ? null : exp.toInstant();
    }

    /**
     * Validate token cơ bản:
     * - Thử parse và verify signature + expiry.
     * - Nếu parse thành công -> token hợp lệ.
     * - Nếu ném ra JwtException / IllegalArgumentException -> token không hợp lệ.
     */
    public boolean validateToken(String token) {
        try {
            // validate signature + expiry bằng cách parse
            extractAllClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * Loại bỏ prefix "Bearer " nếu có trong token string.
     * - Dùng khi đọc header Authorization.
     */
    private String stripBearer(String token) {
        if (token == null) return null;
        token = token.trim();
        if (token.toLowerCase().startsWith("bearer ")) {
            return token.substring(7).trim(); // cắt chuỗi sau "Bearer "
        }
        return token;
    }

    /**
     * Lấy userId từ HttpServletRequest:
     * - Đọc header "Authorization".
     * - Kiểm tra phải bắt đầu bằng "Bearer ".
     * - Strip prefix "Bearer " -> lấy token raw.
     * - Parse claims -> lấy subject (userId).
     */
    //Lấy userId từ HttpServletRequest
    public Long extractUserIdFromRequest(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        // Nếu header không tồn tại hoặc không đúng format -> ném JwtException
        if (header == null || !header.startsWith("Bearer ")) throw new JwtException("Invalid Authorization Header");
        String token = stripBearer(header);        // loại "Bearer " ra khỏi token
        Claims claims = extractAllClaims(token);   // parse token để lấy claims
        return Long.parseLong(claims.getSubject()); // subject chính là userId dạng String
    }

}
