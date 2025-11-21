package com.swp391.gr3.ev_management.service;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component // 🧩 Đánh dấu đây là một Spring Bean để có thể inject vào SecurityConfig
public class CustomOidcUserService extends OidcUserService {

    // 🔐 Cờ kiểm tra email phải được Google verify hay không.
    // Nếu muốn bỏ yêu cầu này → đổi thành false.
    private static final boolean REQUIRE_VERIFIED_EMAIL = true;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        // 📥 Gọi OidcUserService mặc định của Spring để lấy thông tin user từ Google
        OidcUser oidcUser = super.loadUser(userRequest);

        // 🔎 Kiểm tra email đã được Google xác minh hay chưa
        Boolean emailVerified = oidcUser.getEmailVerified();

        // ❗ Nếu chính sách yêu cầu email verified nhưng user chưa verify → chặn đăng nhập
        if (REQUIRE_VERIFIED_EMAIL && (emailVerified == null || !emailVerified)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_not_verified", "Google email is not verified", null),
                    "Google email is not verified"
            );
        }

        // 👮‍♂️ Map thêm quyền cho user (ví dụ mặc định cấp ROLE_USER)
        Set<GrantedAuthority> mapped = new HashSet<>(oidcUser.getAuthorities());
        mapped.add(new SimpleGrantedAuthority("ROLE_USER"));

        // 🔑 Trả về OIDC user mới với authorities đã tùy chỉnh
        // "sub" = subject ID của Google, dùng làm ID chính
        return new DefaultOidcUser(
                mapped,                  // quyền đã gán
                oidcUser.getIdToken(),   // token ID từ Google
                oidcUser.getUserInfo(),  // thông tin hồ sơ được Google trả về
                "sub"                    // claim dùng làm unique identifier
        );
    }
}