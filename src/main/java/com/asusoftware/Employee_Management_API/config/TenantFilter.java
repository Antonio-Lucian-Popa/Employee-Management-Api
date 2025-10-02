package com.asusoftware.Employee_Management_API.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TenantFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        try {
            String host = req.getServerName();
            String tenant = null;
            if (host != null && host.contains(".")) {
                tenant = host.split("\\.")[0];
            }
            if (tenant == null) {
                tenant = req.getHeader("X-Tenant");
            }
            if (tenant == null || tenant.isBlank()) {
                res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing tenant");
                return;
            }
            TenantContext.setTenant(tenant);
            chain.doFilter(req, res);
        } finally {
            TenantContext.clear();
        }
    }
}
