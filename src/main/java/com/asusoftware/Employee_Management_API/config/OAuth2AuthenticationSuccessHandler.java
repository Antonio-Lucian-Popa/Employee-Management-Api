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

    @Value("${app.security.allow-auto-provision:false}")
    private boolean allowAutoProvision;

    @Value("${FRONTEND_OAUTH_SUCCESS_URL:http://localhost:5173/dashboard}")
    private String frontendSuccessUrl;

    /** Nou: unde facem onboarding (global, fără tenant subdomeniu) */
    @Value("${FRONTEND_OAUTH_ONBOARDING_URL:http://localhost:5173/onboarding}")
    private String frontendOnboardingUrl;

    @Value("${app.security.cookies-secure:false}")
    private boolean cookiesSecure;

    @Value("${app.dev.tenant-override:}")
    private String devTenantOverride;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        Map<String, Object> attrs = token.getPrincipal().getAttributes();

        String email = String.valueOf(attrs.getOrDefault("email","")).toLowerCase();
        if (email.isBlank()) throw new RuntimeException("OAuth2 login failed: email missing");

        String tenant = TenantContext.getTenant();
        if (tenant == null || tenant.isBlank()) tenant = resolveTenantFallback(request);

        // 1) Dacă avem tenant și userul există -> login normal
        if (tenant != null && !tenant.isBlank()) {
            var userOpt = users.findByTenantIdAndEmailIgnoreCase(tenant, email);
            if (userOpt.isPresent()) {
                var user = userOpt.get();
                setAuthCookies(response,
                        jwt.access(user.getId().toString(), tenant, user.getRole().name()),
                        jwt.refresh(user.getId().toString()));
                getRedirectStrategy().sendRedirect(request, response, frontendSuccessUrl);
                return;
            }
            // Dacă ai ales să permiți auto-provision în tenantul existent (DEV opțional)
            if (allowAutoProvision) {
                var user = users.save(AppUser.builder()
                        .tenantId(tenant)
                        .email(email)
                        .firstName((String) attrs.getOrDefault("given_name",""))
                        .lastName((String) attrs.getOrDefault("family_name",""))
                        .role(Role.EMPLOYEE)
                        .status(UserStatus.ACTIVE)
                        .provider(AuthProvider.GOOGLE)
                        .emailVerified(true)
                        .build());
                setAuthCookies(response,
                        jwt.access(user.getId().toString(), tenant, user.getRole().name()),
                        jwt.refresh(user.getId().toString()));
                getRedirectStrategy().sendRedirect(request, response, frontendSuccessUrl);
                return;
            }
        }

        // 2) Nu avem cont/tenant → generăm signup_token și trimitem spre onboarding
        String signupToken = jwt.signup(email, Map.of(
                "given_name", attrs.getOrDefault("given_name",""),
                "family_name", attrs.getOrDefault("family_name",""),
                "candidate_tenant", tenant == null ? "" : tenant
        ));

        // Redirect cu token în query (FE îl folosește pt. completare)
        String url = UriComponentsBuilder.fromUriString(frontendOnboardingUrl)
                .queryParam("t", signupToken)
                .build().toUriString();
        getRedirectStrategy().sendRedirect(request, response, url);
    }

    private void setAuthCookies(HttpServletResponse response, String access, String refresh){
        ResponseCookie accessCookie = ResponseCookie.from("access_token", access)
                .httpOnly(true).secure(cookiesSecure).sameSite("Lax").path("/").maxAge(Duration.ofMinutes(15)).build();
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refresh)
                .httpOnly(true).secure(cookiesSecure).sameSite("Lax").path("/").maxAge(Duration.ofDays(30)).build();
        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());
    }

    private String resolveTenantFallback(HttpServletRequest request){
        String t = request.getHeader("X-Tenant");
        if (t != null && !t.isBlank()) return t.toLowerCase();

        String qp = request.getParameter("tenant");
        if (qp != null && !qp.isBlank()) return qp.toLowerCase();

        String host = request.getServerName();
        if (host != null && !host.isBlank()) {
            if (host.equalsIgnoreCase("localhost") || host.startsWith("localhost")) {
                if (devTenantOverride != null && !devTenantOverride.isBlank()) return devTenantOverride.toLowerCase();
            } else {
                String[] parts = host.split("\\.");
                if (parts.length >= 3) return parts[0].toLowerCase();
            }
        }
        return null;
    }
}