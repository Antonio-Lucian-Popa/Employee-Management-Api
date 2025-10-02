package com.asusoftware.Employee_Management_API.invitation;

import com.asusoftware.Employee_Management_API.model.Invitation;
import com.asusoftware.Employee_Management_API.model.Role;
import com.asusoftware.Employee_Management_API.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvitationService {
    private final InvitationRepository repo;
    private final UserRepository users;
    private final JavaMailSender mail;


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
        msg.setText("Register here: https://"+tenant+".frontend.local/invite/"+inv.getToken());
        mail.send(msg);


        return inv;
    }


    @Transactional
    public User accept(String token, String firstName, String lastName, String password) {
        var inv = repo.findByToken(token).orElseThrow(() -> new ChangeSetPersister.NotFoundException("Invalid token"));
        if (inv.getExpiresAt().isBefore(OffsetDateTime.now())) throw new BadRequestException("Expired");


        var user = users.findByTenantIdAndEmailIgnoreCase(inv.getTenantId(), inv.getEmail())
                .orElseGet(() -> users.save(User.createFromInvitation(inv, firstName, lastName, password)));


        inv.setAcceptedAt(OffsetDateTime.now());
        repo.save(inv);
        return user;
    }
}
