package com.asusoftware.Employee_Management_API.auth;



import com.asusoftware.Employee_Management_API.config.JwtService;
import com.asusoftware.Employee_Management_API.config.TenantContext;
import com.asusoftware.Employee_Management_API.model.dto.*;
import com.asusoftware.Employee_Management_API.user.EmailVerificationService;
import com.asusoftware.Employee_Management_API.user.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;
    private final EmailVerificationService emailVerificationService;
    private final JwtService jwt;

    // ✅ injectează repository-ul de useri
    private final UserRepository users;

    // ✅ citește secure-cookie flag din config (dev=false, prod=true)
    @Value("${app.security.cookies-secure:false}")
    private boolean cookiesSecure;

    @PostMapping("/register-company")
    public ResponseEntity<RegisterCompanyResponse> register(@Valid @RequestBody RegisterCompanyRequest req){
        var resp = service.registerCompany(req);
        return ResponseEntity.status(201).body(resp);
    }

    @GetMapping("/verify-email") // permis public în SecurityConfig
    public org.springframework.http.ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        emailVerificationService.verify(token);
        return org.springframework.http.ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-email/resend") // permis public; primește X-Tenant
    public org.springframework.http.ResponseEntity<Void> resend(@RequestParam String email) {
        String tenant = TenantContext.getTenant();
        emailVerificationService.resend(tenant, email.toLowerCase());
        return org.springframework.http.ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req){
        return service.login(TenantContext.getTenant(), req);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(
            @CookieValue(name = "refresh_token", required = false) String refresh) {

        if (refresh == null || refresh.isBlank()) {
            return ResponseEntity.status(401).build();
        }

        var jws = jwt.parse(refresh);
        var claims = jws.getPayload();
        if (!"refresh".equals(String.valueOf(claims.get("type")))) {
            return ResponseEntity.status(401).build();
        }

        String userId = claims.getSubject();
        // dacă nu pui tenant în refresh, ia-l din X-Tenant (Tenancy pe request)
        String tenant = TenantContext.getTenant();

        var user = users.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newAccess = jwt.access(userId, tenant, user.getRole().name());

        ResponseCookie accessCookie = ResponseCookie.from("access_token", newAccess)
                .httpOnly(true).secure(cookiesSecure).sameSite("Lax").path("/")
                .maxAge(Duration.ofMinutes(15)).build();

        // optional: rotează refresh-ul
        // String newRefresh = jwt.refresh(userId);
        // ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", newRefresh)
        //         .httpOnly(true).secure(cookiesSecure).sameSite("Lax").path("/")
        //         .maxAge(Duration.ofDays(30)).build();

        return ResponseEntity.noContent()
                .header("Set-Cookie", accessCookie.toString())
                // .header("Set-Cookie", refreshCookie.toString())
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal JwtPrincipal principal){
        return ResponseEntity.ok(principal);
    }
}
