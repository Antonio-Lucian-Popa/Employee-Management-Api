package com.asusoftware.Employee_Management_API.auth;



import com.asusoftware.Employee_Management_API.config.JwtService;
import com.asusoftware.Employee_Management_API.config.TenantContext;
import com.asusoftware.Employee_Management_API.model.dto.*;
import com.asusoftware.Employee_Management_API.user.EmailVerificationService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;
    private final EmailVerificationService emailVerificationService;
    private final JwtService jwt;

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
    public TokenResponse refresh(@RequestBody RefreshRequest body){
        Jws<Claims> jws = jwt.parse(body.refreshToken());
        var claims = jws.getPayload();
        if (!"refresh".equals(String.valueOf(claims.get("type")))) throw new RuntimeException("Invalid refresh token");
        String userId = claims.getSubject();
        String tenant = TenantContext.getTenant();
        return new TokenResponse(jwt.access(userId, tenant, "EMPLOYEE"), body.refreshToken());
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal JwtPrincipal principal){
        return ResponseEntity.ok(principal);
    }
}
