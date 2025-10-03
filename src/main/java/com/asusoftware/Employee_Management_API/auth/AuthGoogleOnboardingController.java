package com.asusoftware.Employee_Management_API.auth;

import com.asusoftware.Employee_Management_API.company.CompanyRepository;
import com.asusoftware.Employee_Management_API.config.JwtService;
import com.asusoftware.Employee_Management_API.invitation.InvitationRepository;
import com.asusoftware.Employee_Management_API.model.*;
import com.asusoftware.Employee_Management_API.user.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/google")
@RequiredArgsConstructor
public class AuthGoogleOnboardingController {

    private final JwtService jwt;
    private final UserRepository users;
    private final CompanyRepository companies;
    private final InvitationRepository invitations;

    @Value("${app.security.cookies-secure:false}")
    private boolean cookiesSecure;

    /** finalizează flow-ul OWNER: creează compania + OWNER pe baza signup token-ului */
    @PostMapping("/complete-owner")
    public ResponseEntity<?> completeOwner(@RequestBody CompleteOwnerReq req){
        Jws<Claims> jws = jwt.parse(req.getToken());
        Claims c = jws.getPayload();
        if (!"signup".equals(c.get("type")) || !"GOOGLE".equals(c.get("provider"))) {
            return ResponseEntity.badRequest().body("Invalid token type");
        }
        String email = String.valueOf(c.get("email")).toLowerCase();
        String first = String.valueOf(c.getOrDefault("given_name",""));
        String last  = String.valueOf(c.getOrDefault("family_name",""));

        // creează company (slug unic)
        if (!StringUtils.hasText(req.getSlug()) || !StringUtils.hasText(req.getCompanyName()))
            return ResponseEntity.badRequest().body("Missing company/slug");

        if (companies.findBySlugIgnoreCase(req.getSlug()).isPresent())
            return ResponseEntity.status(409).body("Slug already taken");

        Company company = companies.save(Company.builder()
                .id(UUID.randomUUID())
                .name(req.getCompanyName())
                .slug(req.getSlug().toLowerCase())
                .build());

        // creează user OWNER în tenantul nou
        AppUser owner = users.save(AppUser.builder()
                .tenantId(company.getSlug())
                .email(email)
                .firstName(first)
                .lastName(last)
                .role(Role.OWNER)
                .status(UserStatus.ACTIVE)
                .provider(AuthProvider.GOOGLE)
                .emailVerified(true)
                .build());

        // emite cookies pe noul tenant
        return ResponseEntity.ok(setAuthCookiesAndBody(owner, company.getSlug()));
    }

    /** finalizează flow-ul EMPLOYEE: consumă invitația */
    @PostMapping("/complete-invite")
    public ResponseEntity<?> completeInvite(@RequestBody CompleteInviteReq req){
        Jws<Claims> jws = jwt.parse(req.getToken());
        Claims c = jws.getPayload();
        if (!"signup".equals(c.get("type")) || !"GOOGLE".equals(c.get("provider"))) {
            return ResponseEntity.badRequest().body("Invalid token type");
        }
        String email = String.valueOf(c.get("email")).toLowerCase();

        Invitation inv = invitations.findByToken(req.getInvitationToken())
                .orElse(null);
        if (inv == null || inv.getExpiresAt().isBefore(OffsetDateTime.now()))
            return ResponseEntity.badRequest().body("Invalid or expired invitation");

        if (!email.equalsIgnoreCase(inv.getEmail()))
            return ResponseEntity.status(403).body("Invitation email does not match Google account");

        // creează user conform invitației
        AppUser user = users.findByTenantIdAndEmailIgnoreCase(inv.getTenantId(), email)
                .orElseGet(() -> users.save(AppUser.builder()
                        .tenantId(inv.getTenantId())
                        .email(email)
                        .firstName(String.valueOf(c.getOrDefault("given_name","")))
                        .lastName(String.valueOf(c.getOrDefault("family_name","")))
                        .role(inv.getRole())
                        .status(UserStatus.ACTIVE)
                        .provider(AuthProvider.GOOGLE)
                        .emailVerified(true)
                        .build()));

        inv.setAcceptedAt(OffsetDateTime.now());
        invitations.save(inv);

        return ResponseEntity.ok(setAuthCookiesAndBody(user, inv.getTenantId()));
    }

    private OnboardingResult setAuthCookiesAndBody(AppUser user, String tenant){
        String access = jwt.access(user.getId().toString(), tenant, user.getRole().name());
        String refresh = jwt.refresh(user.getId().toString());

        ResponseCookie accessCookie = ResponseCookie.from("access_token", access)
                .httpOnly(true).secure(cookiesSecure).sameSite("Lax").path("/").maxAge(Duration.ofMinutes(15)).build();
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refresh)
                .httpOnly(true).secure(cookiesSecure).sameSite("Lax").path("/").maxAge(Duration.ofDays(30)).build();

        // NOTĂ: într-un controller real, pune-le în header; aici returnăm și payload
        OnboardingResult res = new OnboardingResult();
        res.setTenant(tenant);
        res.setRole(user.getRole().name());
        res.setUserId(user.getId().toString());
        res.setSetCookies(new String[]{accessCookie.toString(), refreshCookie.toString()});
        return res;
    }

    @Data
    public static class CompleteOwnerReq {
        @NotBlank private String token;       // signup token din query param "t"
        @NotBlank private String companyName; // ex. "Acme SRL"
        @NotBlank private String slug;        // ex. "acme"
    }

    @Data
    public static class CompleteInviteReq {
        @NotBlank private String token;           // signup token din query param "t"
        @NotBlank private String invitationToken; // token invitație
    }

    @Data
    public static class OnboardingResult {
        private String userId;
        private String tenant;
        private String role;
        /** trimite aceste valori ca header 'Set-Cookie' din gateway/controller; aici le expunem pt. FE în dev */
        private String[] setCookies;
    }
}
