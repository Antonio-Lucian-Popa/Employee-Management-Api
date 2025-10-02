package com.asusoftware.Employee_Management_API.config;

import com.asusoftware.Employee_Management_API.user.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final UserRepository users;
    private final JwtService jwt;


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        Map<String, Object> attrs = token.getPrincipal().getAttributes();
        String email = (String) attrs.get("email");
        String tenant = TenantContext.getTenant();


        var user = users.findByTenantIdAndEmailIgnoreCase(tenant, email)
                .orElseThrow(() -> new BadCredentialsException("No account for this Google user in this tenant"));


        String access = jwt.generateAccessToken(user);
        String refresh = jwt.generateRefreshToken(user);


        String redirect = UriComponentsBuilder
                .fromUriString("https://app.frontend.local/oauth-success")
                .queryParam("access", access)
                .queryParam("refresh", refresh)
                .build().toUriString();


        getRedirectStrategy().sendRedirect(request, response, redirect);
    }
}
