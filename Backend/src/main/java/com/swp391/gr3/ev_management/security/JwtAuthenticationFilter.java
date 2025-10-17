package com.swp391.gr3.ev_management.security;

import com.swp391.gr3.ev_management.entity.User;
import com.swp391.gr3.ev_management.repository.UserRepository;
import com.swp391.gr3.ev_management.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(TokenService tokenService, UserRepository userRepository) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String auth = request.getHeader("Authorization");
        if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
            try {
                Long userId = tokenService.extractUserIdFromRequest(request);
                if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    Optional<User> userOpt = Optional.ofNullable(userRepository.findUserByUserId(userId));
                    if (userOpt.isPresent()) {
                        User u = userOpt.get();
                        String roleName = (u.getRole() != null && u.getRole().getRoleName() != null)
                                ? u.getRole().getRoleName() : "USER";
                        if (!roleName.startsWith("ROLE_")) roleName = "ROLE_" + roleName;

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        u.getPhoneNumber(),
                                        null,
                                        List.of(new SimpleGrantedAuthority(roleName))
                                );
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (Exception e) {
                // Token không hợp lệ -> bỏ qua, để Security xử lý Unauthorized ở bước sau
            }
        }

        filterChain.doFilter(request, response);
    }
}

