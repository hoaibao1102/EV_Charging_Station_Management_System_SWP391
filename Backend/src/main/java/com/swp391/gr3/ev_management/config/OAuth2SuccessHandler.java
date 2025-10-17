package com.swp391.gr3.ev_management.config;

import com.swp391.gr3.ev_management.entity.Driver;
import com.swp391.gr3.ev_management.entity.Role;
import com.swp391.gr3.ev_management.entity.User;
import com.swp391.gr3.ev_management.enums.DriverStatus;
import com.swp391.gr3.ev_management.repository.DriverRepository;
import com.swp391.gr3.ev_management.repository.RoleRepository;
import com.swp391.gr3.ev_management.repository.UserRepository;
import com.swp391.gr3.ev_management.service.TokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.UUID;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final DriverRepository driverRepository;

    // FE sẽ nhận token tại URL này. Có thể override trong application.properties
    // app.oauth2.frontend-callback=http://localhost:5173/oauth2/callback
    @Value("${app.oauth2.frontend-callback:http://localhost:5173/}")
    private String frontendCallback;

    public OAuth2SuccessHandler(UserRepository userRepository,
                                RoleRepository roleRepository,
                                PasswordEncoder passwordEncoder,
                                TokenService tokenService, DriverRepository driverRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.driverRepository = driverRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        OidcUser oidc = (OidcUser) authentication.getPrincipal();

        // Lấy thông tin từ Google
        String email = oidc.getEmail();
        boolean emailVerified = Boolean.TRUE.equals(oidc.getEmailVerified());
        String givenName = oidc.getGivenName();
        String familyName = oidc.getFamilyName();
        String displayName = (givenName != null || familyName != null)
                ? ((givenName != null ? givenName : "") + " " + (familyName != null ? familyName : "")).trim()
                : oidc.getFullName();

        // Nếu email chưa verify hoặc rỗng => trả lỗi về FE
        if (!emailVerified || email == null || email.isBlank()) {
            String errorUrl = UriComponentsBuilder
                    .fromUriString(frontendCallback)
                    .queryParam("error", "EMAIL_NOT_VERIFIED")
                    .build().toUriString();
            response.sendRedirect(errorUrl);
            return;
        }

        // Tìm user theo email
        User user = userRepository.findByEmailWithRole(email);
        if (user == null) {
            // Tạo mới user mặc định ROLE_USER (id=3L) — giống register()
            Role role = roleRepository.findByRoleId(3L);
            if (role == null) {
                String errorUrl = UriComponentsBuilder
                        .fromUriString(frontendCallback)
                        .queryParam("error", "ROLE_USER_NOT_FOUND")
                        .build().toUriString();
                response.sendRedirect(errorUrl);
                return;
            }

            user = new User();
            user.setEmail(email.trim().toLowerCase());
            user.setName((displayName == null || displayName.isBlank()) ? email : displayName);
            user.setRole(role);

            // Đặt mật khẩu ngẫu nhiên (vì login bằng Google). Có thể buộc đổi sau.
            String randomPwd = "GOOGLE_" + UUID.randomUUID();
            user.setPasswordHash(passwordEncoder.encode(randomPwd));

            // PhoneNumber có thể để null; nếu schema của bạn bắt buộc, hãy map sau.
            user = userRepository.save(user);

            // 👉 TẠO BẢN GHI DRIVER nếu role là Driver
            if ("Driver".equalsIgnoreCase(role.getRoleName())) {
                Driver driver = new Driver();
                driver.setUser(user);
                driver.setStatus(DriverStatus.ACTIVE);

                driverRepository.save(driver);
            }
        }



        // Phát JWT theo logic TokenService hiện có
        String jwt = tokenService.generateToken(user);

        boolean needPhone = (user.getPhoneNumber() == null || user.getPhoneNumber().isBlank());

        // Redirect về FE kèm token
        String redirect = UriComponentsBuilder
                .fromUriString(frontendCallback)
                .queryParam("token", jwt)
                .queryParam("needPhone", needPhone)
                .build()
                .toUriString();

        response.setStatus(HttpServletResponse.SC_FOUND);
        response.sendRedirect(redirect);
    }
}
