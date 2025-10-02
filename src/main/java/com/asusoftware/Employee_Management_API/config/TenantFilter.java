package com.asusoftware.Employee_Management_API.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 20) // rulează devreme
public class TenantFilter extends OncePerRequestFilter {

    private final TenantResolver resolver;

    // rute publice (nu cer tenant)
    private static final Set<String> PUBLIC_PREFIXES = Set.of(
            "/api/v1/auth", "/v3/api-docs", "/swagger-ui", "/webhooks"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        boolean setHere = false;
        try {
            String path = req.getRequestURI();
            boolean isPublic = PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);

            if (!isPublic) {
                String tenant = resolver.resolve(req);
                if (tenant == null || tenant.isBlank()) {
                    res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing tenant");
                    return;
                }
                TenantContext.setTenant(tenant);
                setHere = true;
            }

            chain.doFilter(req, res);
        } finally {
            if (setHere) TenantContext.clear(); // IMPORTANT – curățăm ThreadLocal
        }
    }
}
