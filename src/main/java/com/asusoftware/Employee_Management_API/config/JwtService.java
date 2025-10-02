package com.asusoftware.Employee_Management_API.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;


@Service
public class JwtService {
    @Value("${app.jwt.secret}") private String secret;
    @Value("${app.jwt.issuer}") private String issuer;
    @Value("${app.jwt.access-token-ttl}") private String accessTtlStr;
    @Value("${app.jwt.refresh-token-ttl}") private String refreshTtlStr;
    private Key key;
    private Duration accessTtl, refreshTtl;
    @PostConstruct
    void init(){
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtl = Duration.parse("PT" + accessTtlStr.toUpperCase().replace("D","DT").replace("M","M").replace("H","H"));
        this.refreshTtl = Duration.parse("PT" + refreshTtlStr.toUpperCase().replace("D","DT").replace("M","M").replace("H","H"));
    }
    public String access(String sub, String tenant, String role){
        Instant now = Instant.now();
        return Jwts.builder().subject(sub).issuer(issuer)
                .issuedAt(Date.from(now)).expiration(Date.from(now.plus(accessTtl)))
                .claim("tenant", tenant).claim("role", role)
                .signWith(key).compact();
    }
    public String refresh(String sub){
        Instant now = Instant.now();
        return Jwts.builder().subject(sub).issuer(issuer)
                .issuedAt(Date.from(now)).expiration(Date.from(now.plus(refreshTtl)))
                .claim("type","refresh").signWith(key).compact();
    }
    public Jws<Claims> parse(String token){
        return Jwts.parser().verifyWith((javax.crypto.SecretKey)key).build().parseSignedClaims(token);
    }
}
