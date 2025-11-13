package com.swp391.gr3.ev_management.config;

import com.swp391.gr3.ev_management.entity.User;
import com.swp391.gr3.ev_management.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import static org.aspectj.weaver.tools.cache.SimpleCacheFactory.path;

/**
 * JWT filter:
 *  - Đọc Bearer token từ header Authorization.
 *  - Xác thực chữ ký + hạn token thông qua TokenService.
 *  - Nếu hợp lệ, tạo Authentication và đưa vào SecurityContextHolder,
 *    để Spring Security hiểu request này đã đăng nhập với 1 user và 1 role nhất định.
 *
 * Phù hợp với TokenService:
 *  - validateToken(token)      → kiểm tra có hợp lệ/hết hạn không
 *  - extractToken(token)       → trả về User (subject = userId)
 *  - extractClaim(token, ...)  → lấy các claim (vd: "role") trong JWT
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final TokenService tokenService;          // Service xử lý JWT (tạo, verify, trích user, claim)
    private final AntPathMatcher pathMatcher = new AntPathMatcher(); // Hỗ trợ so pattern path (/**, v.v.)

    public JwtAuthFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    /**
     * Danh sách các path KHÔNG bắt buộc phải qua filter JWT (public, không cần login).
     * Có thể chỉnh sửa/ mở rộng tuỳ theo API của hệ thống.
     */
    private static final String[] PUBLIC_PATHS = new String[]{
            "/", "/index.html", "/error",
            "/static/**", "/public/**",
            "/swagger-ui.html", "/swagger-ui/**",
            "/v3/api-docs/**", "/v3/api-docs.yaml",
            "/api/users/login", "/api/users/register", "/api/users/logout",
            "/actuator/**"
    };

    /**
     * shouldNotFilter:
     *  - Trả true nếu KHÔNG muốn chạy filter cho request này.
     *  - Ở đây:
     *      + Bỏ qua tất cả request OPTIONS (CORS preflight).
     *      + Bỏ qua các path trong PUBLIC_PATHS.
     *      + Bỏ qua các path callback của VNPay (return/IPN).
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        // 1) Bỏ qua preflight CORS (OPTIONS) để không gây lỗi cho trình duyệt trước khi gọi thật
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        final String uri = request.getRequestURI(); // Lấy path đầy đủ, ví dụ "/api/users/login"

        // 2) Bỏ qua các endpoint xử lý VNPay callback, do VNPay không gửi JWT
        if (uri.startsWith("/api/payment/vnpay/")) return true;
        // (Có thể dùng pathMatcher: if (pathMatcher.match("/api/payment/vnpay/**", uri)) return true;)

        // 3) Bỏ qua toàn bộ path public khác (swagger, login, register,...)
        for (String pattern : PUBLIC_PATHS) {
            if (pathMatcher.match(pattern, uri)) return true;
        }

        // 4) Những path còn lại => CẦN filter JWT
        return false;
    }

    /**
     * doFilterInternal:
     *  - Chỉ được gọi nếu shouldNotFilter() trả về false.
     *  - Thực hiện:
     *      1) Kiểm tra SecurityContext xem đã có Authentication chưa (filter trước đã set chưa).
     *      2) Đọc header Authorization, lấy Bearer token nếu có.
     *      3) Dùng TokenService.validateToken() kiểm tra tính hợp lệ.
     *      4) Nếu hợp lệ: extract user + role, tạo Authentication đưa vào SecurityContext.
     *      5) Cho request đi tiếp trong filter chain.
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req,
                                    @NonNull HttpServletResponse res,
                                    @NonNull FilterChain chain) throws ServletException, IOException {

        // 1️⃣ Nếu SecurityContext đã có Authentication (ví dụ filter khác đã set rồi) thì không xử lý lại
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(req, res);
            return;
        }

        // 2️⃣ Lấy header Authorization: "Bearer <jwt>"
        final String auth = req.getHeader("Authorization");

        // Nếu không có header hoặc không bắt đầu bằng "Bearer " → coi như request không có JWT
        if (auth == null || !auth.startsWith("Bearer ")) {
            // Để cho Spring Security xử lý (endpoint yêu cầu auth sẽ tự trả 401/403)
            chain.doFilter(req, res);
            return;
        }

        // 3️⃣ Cắt "Bearer " để lấy token thuần
        final String token = auth.substring(7).trim();

        // 4️⃣ Xác thực token (signature, expiry...) bằng TokenService
        boolean valid = tokenService.validateToken(token);

        if (!valid) {
            // Token không hợp lệ / hết hạn → không set Authentication, để Security xử lý tiếp
            chain.doFilter(req, res);
            return;
        }

        // 5️⃣ Token hợp lệ → trích user từ token (subject = userId) qua TokenService
        User u = tokenService.extractToken(token);

        if (u == null) {
            // Không tìm thấy user tương ứng trong DB → bỏ qua
            chain.doFilter(req, res);
            return;
        }

        // 6️⃣ Lấy ROLE từ claim "role" trong JWT (không rely vào lazy u.getRole())
        String roleName = tokenService.extractClaim(token, c -> c.get("role", String.class));

        // Nếu claim không có thì fallback mặc định DRIVER
        if (roleName == null || roleName.isBlank()) roleName = "DRIVER";
        // Đảm bảo roleName theo chuẩn Spring: bắt đầu bằng "ROLE_"
        if (!roleName.startsWith("ROLE_")) roleName = "ROLE_" + roleName;

        // Tạo list quyền (ở đây chỉ có 1 role)
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(roleName));

        // 7️⃣ Tạo đối tượng Authentication:
        //     - principal: userId dạng String (subject trong token)
        //     - credentials: null (vì không cần password nữa)
        //     - authorities: danh sách quyền của user
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(String.valueOf(u.getUserId()), null, authorities);

        // 8️⃣ Đính kèm thêm chi tiết request (IP, session...) cho Authentication (optional nhưng tốt)
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));

        // 9️⃣ Đưa Authentication vào SecurityContext để cả request lifecycle biết user này đã đăng nhập
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 🔟 Cho request đi tiếp các filter/controller phía sau
        chain.doFilter(req, res);
    }

}
