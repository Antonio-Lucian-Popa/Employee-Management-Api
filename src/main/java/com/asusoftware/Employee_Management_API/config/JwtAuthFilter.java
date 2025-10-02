package com.asusoftware.Employee_Management_API.config;

import com.asusoftware.Employee_Management_API.model.Role;
import com.asusoftware.Employee_Management_API.model.dto.JwtPrincipal;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;


/**
 * Extrage JWT din Authorization: Bearer ... (sau cookie "access_token"),
 * îl validează și setează SecurityContext cu un JwtPrincipal.
 * Dacă tokenul are claim type=refresh, este ignorat (nu autentificăm pe refresh).
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Jws<Claims> jws = jwtService.parse(token);
                Claims body = jws.getPayload();

                // Nu autentificăm cu refresh token
                Object type = body.get("type");
                if (type != null && "refresh".equals(type.toString())) {
                    chain.doFilter(request, response);
                    return;
                }

                UUID userId = UUID.fromString(body.getSubject());
                String tenant = (String) body.get("tenant");
                String roleStr = (String) body.get("role");
                Role role = Role.valueOf(roleStr);

                // dacă TenantFilter nu a setat deja tenantul, îl luăm din JWT
                if (tenant != null && TenantContext.getTenant() == null) {
                    TenantContext.setTenant(tenant);
                }

                JwtPrincipal principal = new JwtPrincipal(userId, tenant, role);
                Collection<? extends GrantedAuthority> authorities =
                        List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));

                Authentication auth = new UsernamePasswordAuthenticationToken(
                        principal, null, authorities);
                ((UsernamePasswordAuthenticationToken) auth)
                        .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (IllegalArgumentException | JwtException ex) {
                // token invalid/expirat – curățăm contextul și mergem mai departe (va fi 401 pe endpoints protejate)
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }

    /** Caută tokenul în Authorization header sau în cookie-ul "access_token". */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("access_token".equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                    return c.getValue();
                }
            }
        }
        return null;
    }
}
