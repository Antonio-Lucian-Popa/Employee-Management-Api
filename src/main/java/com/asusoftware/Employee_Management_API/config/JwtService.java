package com.asusoftware.Employee_Management_API.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    @Value("${app.jwt.secret}") private String secret;
    @Value("${app.jwt.issuer}") private String issuer;
    @Value("${app.jwt.access-token-ttl}") private String accessTtlStr;   // ex: 15m
    @Value("${app.jwt.refresh-token-ttl}") private String refreshTtlStr; // ex: 30d

    private SecretKey key;
    private Duration accessTtl, refreshTtl;

    @PostConstruct
    void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtl = parseDuration(accessTtlStr);
        this.refreshTtl = parseDuration(refreshTtlStr);
    }

    public String access(String sub, String tenant, String role){
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(sub)
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .claim("tenant", tenant)
                .claim("role", role)
                .signWith(key)
                .compact();
    }

    public String refresh(String sub){
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(sub)
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(refreshTtl)))
                .claim("type", "refresh")
                .signWith(key)
                .compact();
    }

    public Jws<Claims> parse(String token){
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
    }

    /** Acceptă formate scurte: 15m, 2h, 30d, 45s, 500ms; sau ISO-8601 (PT15M). */
    private static Duration parseDuration(String raw) {
        String s = raw.trim().toLowerCase();
        if (s.endsWith("ms")) return Duration.ofMillis(Long.parseLong(s.substring(0, s.length()-2)));
        long seconds = Long.parseLong(s.substring(0, s.length() - 1));
        if (s.endsWith("s"))  return Duration.ofSeconds(seconds);
        if (s.endsWith("m"))  return Duration.ofMinutes(seconds);
        if (s.endsWith("h"))  return Duration.ofHours(seconds);
        if (s.endsWith("d"))  return Duration.ofDays(seconds);
        // fallback: ISO-8601 (ex: PT15M)
        return Duration.parse(raw);
    }
}
