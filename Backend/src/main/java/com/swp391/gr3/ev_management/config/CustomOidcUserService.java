package com.swp391.gr3.ev_management.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.HashSet;
import java.util.Set;

@Component
public class CustomOidcUserService extends OidcUserService {

    // Nếu bạn không muốn chặn email chưa verify, set cờ này = false
    private final boolean requireVerifiedEmail = true;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        // gọi service mặc định để lấy OidcUser từ Google
        OidcUser oidcUser = super.loadUser(userRequest);

        // Bảo vệ: yêu cầu email đã xác minh (tuỳ chính sách)
        Boolean emailVerified = oidcUser.getEmailVerified();
        if (requireVerifiedEmail && (emailVerified == null || !emailVerified)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_not_verified", "Google email is not verified", null),
                    "Google email is not verified");
        }

        // Chuẩn hoá authorities (có thể map thêm quyền tuỳ hệ thống)
        Set<GrantedAuthority> mapped = new HashSet<>(oidcUser.getAuthorities());
        mapped.add(new SimpleGrantedAuthority("ROLE_USER"));

        // Trả về DefaultOidcUser với authorities đã map, dùng "sub" làm claim ID
        return new DefaultOidcUser(mapped, oidcUser.getIdToken(), oidcUser.getUserInfo(), "sub");
    }
}