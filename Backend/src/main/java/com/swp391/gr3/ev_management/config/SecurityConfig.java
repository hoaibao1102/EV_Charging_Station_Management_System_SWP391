package com.swp391.gr3.ev_management.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import static org.springframework.security.config.Customizer.withDefaults;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.swp391.gr3.ev_management.entity.Users;
import com.swp391.gr3.ev_management.repository.UserRepository;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

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
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public AuthenticationProvider authenticationProvider(UserDetailsService uds, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(uds);
        provider.setPasswordEncoder(encoder);
        return provider;
    }@Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // ====== CORS cho Frontend dev (5173/3000) ======
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Origin FE cho dev — thêm origin khác nếu cần
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://localhost:5174",    // 👈 thêm dòng này
                "http://127.0.0.1:5174",    // 👈 thêm dòng này
                "http://localhost:3000",
                "http://127.0.0.1:3000"
        ));
        // Nếu cần gửi cookie/Authorization
        config.setAllowCredentials(true);
        // Methods cho API + preflight
        config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        // Headers cho phép (bao gồm Authorization, Content-Type…)
        config.setAllowedHeaders(List.of("*"));
        // (tuỳ) Header expose về FE
        config.setExposedHeaders(List.of("Location"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // áp cho toàn bộ API
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationProvider provider) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(withDefaults())                      // <-- BẬT CORS
                .authenticationProvider(provider)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // NHỚ có dấu "/" đầu path
                        .requestMatchers(
                                "/api/users/login",
                                "/api/users/register",
                                "/public/**",
                                "/error",
                                // Swagger / springdoc
                                "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/v3/api-docs.yaml",
                                // Cho phép preflight
                                "/**/*.css","/**/*.js","/","/index.html"
                        ).permitAll()
                        // (tuỳ) nếu có healthcheck
                        .requestMatchers("/actuator/**").permitAll()
                        // Cho phép request OPTIONS (preflight) đi qua
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint((req, res, e) -> {
                    res.setStatus(401);
                    res.setContentType("application/json");res.getWriter().write("{\"message\":\"Unauthorized\"}");
                }))
                .logout(AbstractHttpConfigurer::disable);

        return http.build();
    }
}