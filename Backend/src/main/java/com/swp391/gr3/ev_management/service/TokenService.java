package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.entity.Users;
import com.swp391.gr3.ev_management.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Service
public class TokenService {

    private final UserRepository userRepository;

    public TokenService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public SecretKey getSignInKey(){
        // Replace with your actual secret key
        String SECRET_KEY = "0c69ea7993456174304d1b49a3a31e2f70cd424c6cbc77176e4ee046bec9e7d2";
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Users users) {
        return Jwts.builder()
                .subject(users.getUserId() + "")
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60)) // 1 hours
                .signWith(getSignInKey())
                .compact();
    }

    public Users extractToken(String token) {
        String value = extractClaim(token,Claims::getSubject);
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
    public <T> T extractClaim(String token, Function<Claims,T> resolver){
        Claims claims = extractAllClaims(token);
        return  resolver.apply(claims);
    }
}
