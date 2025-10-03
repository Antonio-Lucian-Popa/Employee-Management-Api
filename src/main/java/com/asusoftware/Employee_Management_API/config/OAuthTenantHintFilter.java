package com.asusoftware.Employee_Management_API.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

// package com.asusoftware.Employee_Management_API.config;
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class OAuthTenantHintFilter extends OncePerRequestFilter {

    @Value("${app.security.cookies-secure:false}")
    private boolean cookiesSecure;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String path = req.getRequestURI();
        if (path.startsWith("/oauth2/authorization/")) {
            String tenant = req.getParameter("tenant");
            if (tenant != null && !tenant.isBlank()) {
                ResponseCookie cookie = ResponseCookie.from("tenant_hint", tenant)
                        .httpOnly(true)
                        .secure(cookiesSecure)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(Duration.ofMinutes(10))
                        .build();
                res.addHeader("Set-Cookie", cookie.toString());
            }
        }
        chain.doFilter(req, res);
    }
}
