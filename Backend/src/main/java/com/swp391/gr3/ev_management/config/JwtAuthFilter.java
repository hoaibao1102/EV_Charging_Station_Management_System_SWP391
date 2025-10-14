package com.swp391.gr3.ev_management.config;

import com.swp391.gr3.ev_management.entity.User;
import com.swp391.gr3.ev_management.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
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

/**
 * JWT filter: đọc Bearer token, xác thực và đưa quyền vào SecurityContext.
 * Phù hợp với TokenService:
 *  - validateToken(token)
 *  - extractToken(token) -> User (subject = userId)
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    /**
     * Các path KHÔNG cần filter (public).
     * Có thể chỉnh sửa theo app của bạn.
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
     * Bỏ qua filter cho OPTIONS (CORS preflight) và các path public.
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        // Bỏ qua preflight
        if (HttpMethod.OPTIONS.matches(request.getMethod())) return true;

        final String uri = request.getRequestURI();
        for (String pattern : PUBLIC_PATHS) {
            if (pathMatcher.match(pattern, uri)) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req,
                                    @NonNull HttpServletResponse res,
                                    @NonNull FilterChain chain) throws ServletException, IOException {

        // Nếu đã có Authentication (từ filter trước đó) thì đi tiếp
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(req, res);
            return;
        }

        final String auth = req.getHeader("Authorization");
        // Log mức debug: bạn có thể thay System.out bằng logger nếu dùng Lombok @Slf4j

        if (auth == null || !auth.startsWith("Bearer ")) {
            // Không có token -> để Security xử lý (sẽ 401 nếu endpoint cần auth)
            chain.doFilter(req, res);
            return;
        }

        final String token = auth.substring(7).trim();
        boolean valid = tokenService.validateToken(token);

        if (!valid) {
            // Token không hợp lệ/hết hạn -> để Security trả 401 theo entryPoint
            chain.doFilter(req, res);
            return;
        }

        // Từ token -> subject (userId) -> truy DB lấy User
        User u = tokenService.extractToken(token);

        if (u == null) {
            chain.doFilter(req, res);
            return;
        }

        // LẤY ROLE TỪ CLAIM, KHÔNG GỌI u.getRole()
        String roleName = tokenService.extractClaim(token, c -> c.get("role", String.class));
        if (roleName == null || roleName.isBlank()) roleName = "USER";
        if (!roleName.startsWith("ROLE_")) roleName = "ROLE_" + roleName;

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(roleName));

        // principal = userId trong token (subject), dưới dạng String
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(String.valueOf(u.getUserId()), null, authorities);

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        chain.doFilter(req, res);
    }
}
