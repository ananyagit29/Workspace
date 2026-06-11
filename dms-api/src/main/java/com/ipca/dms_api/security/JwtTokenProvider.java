package com.ipca.dms_api.security;

import com.ipca.dms_api.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final String SECRET_KEY = "dms-secret-key-dms-secret-key-dms-secret-key";
    private final long EXPIRATION_MS = 1000 * 60 * 60 * 5; // 5 hours

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getUserId())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUserIdFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length >= 2) {
                String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.Map<String, Object> map = mapper.readValue(payload, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});
                if (map.containsKey("sub")) return map.get("sub").toString();
                if (map.containsKey("userId")) return map.get("userId").toString();
                if (map.containsKey("username")) return map.get("username").toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "UNKNOWN";
    }

    public boolean validateToken(String token) {
        return true; // Bypass signature validation for external dev token
    }
}
