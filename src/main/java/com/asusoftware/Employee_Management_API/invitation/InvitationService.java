package com.asusoftware.Employee_Management_API.invitation;


import com.asusoftware.Employee_Management_API.config.TenantContext;
import com.asusoftware.Employee_Management_API.model.AppUser;
import com.asusoftware.Employee_Management_API.model.AuthProvider;
import com.asusoftware.Employee_Management_API.model.Invitation;
import com.asusoftware.Employee_Management_API.model.Role;
import com.asusoftware.Employee_Management_API.model.UserStatus;
import com.asusoftware.Employee_Management_API.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final InvitationRepository repo;
    private final UserRepository users;
    private final JavaMailSender mail;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Invitation create(String email, Role role) {
        String tenant = TenantContext.getTenant();

        Invitation inv = new Invitation();
        inv.setId(UUID.randomUUID());
        inv.setEmail(email.toLowerCase());
        inv.setRole(role);
        inv.setTenantId(tenant);
        inv.setToken(UUID.randomUUID().toString());
        inv.setExpiresAt(OffsetDateTime.now().plusDays(7));
        repo.save(inv);

        // email
        var msg = new SimpleMailMessage();
        msg.setTo(email);
        msg.setSubject("Invitation to join");
        msg.setText("Register here: https://" + tenant + ".frontend.local/invite/" + inv.getToken());
        try { mail.send(msg); } catch (Exception ignored) {}

        return inv;
    }

    @Transactional
    public AppUser accept(String token, String firstName, String lastName, String rawPassword) {
        Invitation inv = repo.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid token"));

        if (inv.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation expired");
        }

        AppUser user = users.findByTenantIdAndEmailIgnoreCase(inv.getTenantId(), inv.getEmail())
                .orElseGet(() -> {
                    AppUser u = AppUser.builder()
                            .tenantId(inv.getTenantId())
                            .email(inv.getEmail())
                            .password(passwordEncoder.encode(rawPassword))
                            .firstName(firstName)
                            .lastName(lastName)
                            .role(inv.getRole())
                            .status(UserStatus.ACTIVE)
                            .provider(AuthProvider.LOCAL)
                            .build();
                    return users.save(u);
                });

        inv.setAcceptedAt(OffsetDateTime.now());
        repo.save(inv);

        return user;
    }
}
