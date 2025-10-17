package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.entity.User;
import com.swp391.gr3.ev_management.repository.UserRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final UserRepository userRepository;

    // Lấy secret từ properties (đề nghị lưu base64)
    @Value("${app.jwtSecret}")
    private String jwtSecret;

    private SecretKey getSignInKey() {
        // jwtSecret nên là Base64-encoded (recommended).
        // Nếu bạn muốn dùng hex, đổi phần decode tương ứng.
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(User users) {
        return Jwts.builder()
                .setSubject(String.valueOf(users.getUserId()))
                .claim("fullName", users.getName())
                .claim("role", users.getRole().getRoleName())
                .setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plusSeconds(900))) // 15 phút
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public User extractToken(String token) {
        String value = extractClaim(token, Claims::getSubject);
        long userId = Long.parseLong(value);
        return userRepository.findUserByUserId(userId);
    }

    public Claims extractAllClaims(String token) {
        return  Jwts.parser().
                verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }

    public Instant getExpirationFromJwt(String token) {
        Claims claims = extractAllClaims(token);
        Date exp = claims.getExpiration();
        return (exp == null) ? null : exp.toInstant();
    }

    public boolean validateToken(String token) {
        try {
            // validate signature + expiry by parsing
            extractAllClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private String stripBearer(String token) {
        if (token == null) return null;
        token = token.trim();
        if (token.toLowerCase().startsWith("bearer ")) {
            return token.substring(7).trim();
        }
        return token;
    }

    //Lấy userId từ HttpServletRequest
    public Long extractUserIdFromRequest(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) throw new JwtException("Invalid Authorization Header");
        String token = stripBearer(header);
        Claims claims = extractAllClaims(token);
        return Long.parseLong(claims.getSubject());
    }

}
