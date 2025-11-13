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

@Component // Đánh dấu class này là một bean Spring, để Spring Security autowire làm success handler
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    // ====== Dependencies chính ======
    private final UserRepository userRepository;         // Làm việc với entity User
    private final RoleRepository roleRepository;         // Lấy Role (ví dụ ROLE_USER/ROLE_DRIVER)
    private final PasswordEncoder passwordEncoder;       // Mã hoá password ngẫu nhiên cho user Google
    private final TokenService tokenService;             // Sinh JWT sau khi login thành công
    private final DriverRepository driverRepository;     // Lưu bản ghi Driver nếu user là tài xế

    // URL callback bên FE để nhận token sau khi đăng nhập Google thành công
    // Có thể cấu hình trong application.properties: app.oauth2.frontend-callback=...
    @Value("${app.oauth2.frontend-callback:http://localhost:5173/}")
    private String frontendCallback;

    // Constructor injection cho các dependency
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

    /**
     * Hàm được Spring Security gọi khi quá trình OAuth2 (Google) authentication thành công.
     * Tại đây ta:
     *  - Lấy thông tin OidcUser từ Google (email, tên, trạng thái verify email,...)
     *  - Kiểm tra email đã verify chưa
     *  - Kiểm tra user đã tồn tại trong DB chưa
     *      + Nếu chưa có: tạo user mới, set ROLE_USER (id=3L) giống flow register thường
     *      + (Tuỳ logic) nếu role là Driver thì tạo luôn bản ghi Driver ACTIVE
     *  - Generate JWT từ TokenService
     *  - Redirect về FE kèm token & flag needPhone (xem user đã có phone chưa)
     */
    @Override
    @Transactional(readOnly = true) // Đọc chính, nhưng vẫn có thể save user mới (Spring sẽ nâng cấp khi cần)
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        // 1️⃣ Lấy đối tượng OidcUser (user Google) từ Authentication principal
        OidcUser oidc = (OidcUser) authentication.getPrincipal();

        // 2️⃣ Lấy thông tin từ Google
        String email = oidc.getEmail();                            // Email Google
        boolean emailVerified = Boolean.TRUE.equals(oidc.getEmailVerified()); // Email đã verify hay chưa
        String givenName = oidc.getGivenName();                    // Tên
        String familyName = oidc.getFamilyName();                  // Họ
        String displayName = (givenName != null || familyName != null)
                ? ((givenName != null ? givenName : "") + " " + (familyName != null ? familyName : "")).trim()
                : oidc.getFullName();                              // Ghép họ tên nếu có, fallback fullName

        // 3️⃣ Nếu email chưa verify hoặc rỗng -> không cho login, redirect về FE kèm error
        if (!emailVerified || email == null || email.isBlank()) {
            String errorUrl = UriComponentsBuilder
                    .fromUriString(frontendCallback)
                    .queryParam("error", "EMAIL_NOT_VERIFIED") // FE sẽ đọc param này để hiển thị lỗi
                    .build().toUriString();
            response.sendRedirect(errorUrl);
            return;
        }

        // 4️⃣ Tìm user trong DB theo email (đã có role join)
        User user = userRepository.findByEmailWithRole(email);
        if (user == null) {
            // 4.1 Nếu chưa có user -> tạo mới với ROLE_USER mặc định (id=3L)
            Role role = roleRepository.findByRoleId(3L);
            if (role == null) {
                // Nếu không tìm được role 3L -> báo lỗi cho FE
                String errorUrl = UriComponentsBuilder
                        .fromUriString(frontendCallback)
                        .queryParam("error", "ROLE_USER_NOT_FOUND")
                        .build().toUriString();
                response.sendRedirect(errorUrl);
                return;
            }

            // 4.2 Khởi tạo user mới từ thông tin Google
            user = new User();
            user.setEmail(email.trim().toLowerCase());                                 // Chuẩn hoá email
            user.setName((displayName == null || displayName.isBlank()) ? email : displayName); // Tên hiển thị
            user.setRole(role);                                                        // Gán ROLE_USER (id=3)

            // 4.3 Tạo password ngẫu nhiên vì user login bằng Google nên không dùng password này để đăng nhập
            String randomPwd = "GOOGLE_" + UUID.randomUUID();
            user.setPasswordHash(passwordEncoder.encode(randomPwd));

            // 4.4 PhoneNumber có thể để null, sau này FE sẽ yêu cầu bổ sung
            user = userRepository.save(user);

            // 4.5 Nếu role tương ứng là Driver thì tạo bản ghi Driver (ACTIVE)
            if ("Driver".equalsIgnoreCase(role.getRoleName())) {
                Driver driver = new Driver();
                driver.setUser(user);
                driver.setStatus(DriverStatus.ACTIVE); // tài xế đang hoạt động

                driverRepository.save(driver);
            }
        }

        // 5️⃣ Sinh JWT cho user vừa login xong (cũ hoặc mới)
        String jwt = tokenService.generateToken(user);

        // 6️⃣ Kiểm tra user đã có phoneNumber chưa, để FE hiển thị form bổ sung nếu cần
        boolean needPhone = (user.getPhoneNumber() == null || user.getPhoneNumber().isBlank());

        // 7️⃣ Build URL redirect về FE, kèm token + needPhone
        String redirect = UriComponentsBuilder
                .fromUriString(frontendCallback)      // ví dụ: http://localhost:5173/oauth2/callback
                .queryParam("token", jwt)             // JWT để FE lưu (localStorage/cookie)
                .queryParam("needPhone", needPhone)   // true => FE hỏi thêm số điện thoại
                .build()
                .toUriString();

        // 8️⃣ Đặt HTTP status 302 (FOUND) rồi redirect
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.sendRedirect(redirect);
    }
}
