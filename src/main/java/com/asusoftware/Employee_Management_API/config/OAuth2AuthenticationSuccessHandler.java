package com.asusoftware.Employee_Management_API.config;

import com.asusoftware.Employee_Management_API.model.AppUser;
import com.asusoftware.Employee_Management_API.model.AuthProvider;
import com.asusoftware.Employee_Management_API.model.Role;
import com.asusoftware.Employee_Management_API.model.UserStatus;
import com.asusoftware.Employee_Management_API.user.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository users;
    private final JwtService jwt;

    /** Dacă true și nu găsim userul în tenant, îl creăm automat ca EMPLOYEE. */
    @Value("${app.security.allow-auto-provision:false}")
    private boolean allowAutoProvision;

    /** Unde redirecționăm după login (ex: http://localhost:5173/dashboard) */
    @Value("${FRONTEND_OAUTH_SUCCESS_URL:http://localhost:5173/dashboard}")
    private String frontendUrl;

    /** În dev pe HTTP trebuie false; în prod pe HTTPS -> true. */
    @Value("${app.security.cookies-secure:false}")
    private boolean cookiesSecure;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        Map<String, Object> attrs = token.getPrincipal().getAttributes();

        // Google oferă "email" & "sub"
        String email = (String) attrs.get("email");
        if (email == null || email.isBlank()) {
            throw new RuntimeException("OAuth2 login failed: email not provided by provider.");
        }
        email = email.toLowerCase();

        // Ia tenant-ul (din subdomeniu / header, setat de TenantFilter); fallback minimal dacă lipsește
        String tenant = TenantContext.getTenant();
        if (tenant == null || tenant.isBlank()) {
            String host = request.getServerName();
            if (host != null && host.contains(".")) tenant = host.split("\\.")[0];
        }
        if (tenant == null || tenant.isBlank()) {
            throw new RuntimeException("Tenant missing during OAuth2 callback.");
        }

        final String emailF = email;
        final String tenantF = tenant;

        // Caută userul în tenant sau creează-l (dacă e permis)
        AppUser user = users.findByTenantIdAndEmailIgnoreCase(tenantF, emailF)
                .orElseGet(() -> {
                    if (!allowAutoProvision) {
                        throw new RuntimeException("No account for this Google user in this tenant.");
                    }
                    AppUser u = AppUser.builder()
                            .tenantId(tenantF)
                            .email(emailF)
                            .firstName((String) attrs.getOrDefault("given_name", ""))
                            .lastName((String) attrs.getOrDefault("family_name", ""))
                            .role(Role.EMPLOYEE)
                            .status(UserStatus.ACTIVE)
                            .provider(AuthProvider.GOOGLE)
                            .build();
                    return users.save(u);
                });

        // Generează tokenuri
        String access = jwt.access(user.getId().toString(), tenant, user.getRole().name());
        String refresh = jwt.refresh(user.getId().toString());

        // Setează cookies HttpOnly (secure controlat din config pentru dev/prod)
        ResponseCookie accessCookie = ResponseCookie.from("access_token", access)
                .httpOnly(true)
                .secure(cookiesSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofMinutes(15))
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refresh)
                .httpOnly(true)
                .secure(cookiesSecure)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofDays(30))
                .build();

        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());

        // Redirect către FE
        getRedirectStrategy().sendRedirect(request, response, frontendUrl);
    }
}
