package com.asusoftware.Employee_Management_API.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class TenantResolver {

    private static final String HEADER = "X-Tenant"; // folosește-l în dev sau când nu ai DNS

    public String resolve(HttpServletRequest req) {
        // 1) Încercă header (ex: Postman/FE)
        String fromHeader = req.getHeader(HEADER);
        if (fromHeader != null && !fromHeader.isBlank()) {
            return fromHeader.trim().toLowerCase();
        }

        // 2) Subdomeniu (ex: acme.api.tudomeniu.com)
        String host = req.getServerName();
        if (host == null || host.isBlank()) return null;
        String[] parts = host.split("\\.");
        if (parts.length < 2) return null; // ex: "localhost" — nu are subdomeniu
        return parts[0].toLowerCase();     // ia primul label (acme)
    }
}
