package com.asusoftware.Employee_Management_API.config;

import com.asusoftware.Employee_Management_API.model.AppUser;
import com.asusoftware.Employee_Management_API.model.AuthProvider;
import com.asusoftware.Employee_Management_API.model.Role;
import com.asusoftware.Employee_Management_API.model.UserStatus;
import com.asusoftware.Employee_Management_API.user.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
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
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        Map<String, Object> attrs = token.getPrincipal().getAttributes();

        String email = String.valueOf(attrs.get("email")).toLowerCase();
        String given = String.valueOf(attrs.getOrDefault("given_name", ""));
        String family = String.valueOf(attrs.getOrDefault("family_name", ""));

        // 1) rezolvă tenant (ThreadLocal, cookie „tenant_hint”, apoi param)
        String tenant = TenantContext.getTenant();
        if (tenant == null || tenant.isBlank()) tenant = readCookie(request, "tenant_hint");
        if (tenant == null || tenant.isBlank()) tenant = request.getParameter("tenant"); // fallback dev

        // 2) dacă avem tenant și user există în tenant => login direct
        if (tenant != null && !tenant.isBlank()) {
            var existing = users.findByTenantIdAndEmailIgnoreCase(tenant, email);
            if (existing.isPresent()) {
                // set cookies JWT
                var u = existing.get();
                String access = jwt.access(u.getId().toString(), tenant, u.getRole().name());
                String refresh = jwt.refresh(u.getId().toString());

                ResponseCookie a = ResponseCookie.from("access_token", access)
                        .httpOnly(true).secure(cookiesSecure).sameSite("Lax").path("/").maxAge(Duration.ofMinutes(15)).build();
                ResponseCookie r = ResponseCookie.from("refresh_token", refresh)
                        .httpOnly(true).secure(cookiesSecure).sameSite("Lax").path("/").maxAge(Duration.ofDays(30)).build();

                // curăță hint-ul
                ResponseCookie clear = ResponseCookie.from("tenant_hint","").path("/").maxAge(0).build();

                response.addHeader("Set-Cookie", a.toString());
                response.addHeader("Set-Cookie", r.toString());
                response.addHeader("Set-Cookie", clear.toString());

                getRedirectStrategy().sendRedirect(request, response, frontendSuccessUrl);
                return;
            }
        }

        // 3) altfel → onboarding cu signup token
        String signup = jwt.signupGoogleToken(email, given, family, Duration.ofMinutes(10));
        String url = UriComponentsBuilder.fromUriString(frontendOnboardingUrl)
                .queryParam("t", signup).build(true).toUriString();

        // curăță hint-ul
        ResponseCookie clear = ResponseCookie.from("tenant_hint","").path("/").maxAge(0).build();
        response.addHeader("Set-Cookie", clear.toString());

        getRedirectStrategy().sendRedirect(request, response, url);
    }

    private String readCookie(HttpServletRequest req, String name) {
        if (req.getCookies() == null) return null;
        for (Cookie c : req.getCookies()) if (name.equals(c.getName())) return c.getValue();
        return null;
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