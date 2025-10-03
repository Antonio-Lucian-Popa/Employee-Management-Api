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
import java.util.Map;

@Service
public class JwtService {

    @Value("${app.jwt.secret}") private String secret;
    @Value("${app.jwt.issuer}") private String issuer;
    @Value("${app.jwt.access-token-ttl}") private String accessTtlStr;   // ex: 15m
    @Value("${app.jwt.refresh-token-ttl}") private String refreshTtlStr; // ex: 30d
    @Value("${app.jwt.signup-token-ttl:10m}") private String signupTtlStr; // 👈 nou: default 10m

    private SecretKey key;
    private Duration accessTtl, refreshTtl, signupTtl; // 👈 nou

    @PostConstruct
    void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtl = parseDuration(accessTtlStr);
        this.refreshTtl = parseDuration(refreshTtlStr);
        this.signupTtl  = parseDuration(signupTtlStr); // 👈 nou
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

    /** 👇 NOU: token scurt pentru onboarding post-OAuth (ex: 10 minute).
     *  subject = email (dacă îl ai), iar extraClaims pot conține given_name/family_name etc. */
    public String signup(String email, Map<String, Object> extraClaims) {
        Instant now = Instant.now();
        var b = Jwts.builder()
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(signupTtl)))
                .claim("type", "signup")
                .claim("provider", "GOOGLE"); // sau dinamic, dacă vei avea mai mulți provideri
        if (email != null && !email.isBlank()) {
            b.subject(email.toLowerCase());
            b.claim("email", email.toLowerCase());
        }
        if (extraClaims != null) {
            extraClaims.forEach(b::claim);
        }
        return b.signWith(key).compact();
    }

    public Jws<Claims> parse(String token){
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
    }

    /** Helpers opționale (utile în handler/controller) */
    public static boolean isRefresh(Jws<Claims> jws) {
        return "refresh".equals(String.valueOf(jws.getPayload().get("type")));
    }
    public static boolean isSignup(Jws<Claims> jws) {
        return "signup".equals(String.valueOf(jws.getPayload().get("type")));
    }

    /** Acceptă formate scurte: 15m, 2h, 30d, 45s, 500ms; sau ISO-8601 (PT15M). */
    private static Duration parseDuration(String raw) {
        String s = raw.trim().toLowerCase();
        if (s.endsWith("ms")) return Duration.ofMillis(Long.parseLong(s.substring(0, s.length()-2)));
        long n = Long.parseLong(s.substring(0, s.length() - 1));
        if (s.endsWith("s"))  return Duration.ofSeconds(n);
        if (s.endsWith("m"))  return Duration.ofMinutes(n);
        if (s.endsWith("h"))  return Duration.ofHours(n);
        if (s.endsWith("d"))  return Duration.ofDays(n);
        return Duration.parse(raw);
    }
}
