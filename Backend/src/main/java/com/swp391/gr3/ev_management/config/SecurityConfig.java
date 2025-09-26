package com.swp391.gr3.ev_management.config;

import com.swp391.gr3.ev_management.entity.Users;
import com.swp391.gr3.ev_management.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> {
            Users u = userRepository.findUsersByPhoneNumber(username);
            if (u == null) throw new UsernameNotFoundException("User not found: " + username);

            String roleName = (u.getRoles() != null && u.getRoles().getRoleName() != null)
                    ? u.getRoles().getRoleName()
                    : "USER";
            if (!roleName.startsWith("ROLE_")) roleName = "ROLE_" + roleName;

            return org.springframework.security.core.userdetails.User
                    .withUsername(u.getPhoneNumber())
                    .password(u.getPassword())
                    .authorities(roleName)
                    .accountExpired(false)
                    .accountLocked(false)
                    .credentialsExpired(false)
                    .disabled(false)
                    .build();
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(UserDetailsService uds, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(uds);
        provider.setPasswordEncoder(encoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationProvider provider) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authenticationProvider(provider)
                .httpBasic(AbstractHttpConfigurer::disable)   // tắt popup Basic
                .formLogin(AbstractHttpConfigurer::disable)   // tắt trang login HTML mặc định
                .authorizeHttpRequests(auth -> auth
                        // CHỈNH cho đúng path controller của bạn (ví dụ @RequestMapping("/users"))
                        .requestMatchers("api/users/login", "api/users/register", "/public/**", "/error").permitAll()
                        // Swagger / springdoc
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
                        .anyRequest().authenticated()
                )
                // Trả JSON 401 thay vì redirect sang trang /login (HTML)
                .exceptionHandling(ex -> ex.authenticationEntryPoint((req, res, e) -> {
                    res.setStatus(401);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"message\":\"Unauthorized\"}");
                }))
                .logout(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
