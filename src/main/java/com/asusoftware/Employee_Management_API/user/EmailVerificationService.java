package com.asusoftware.Employee_Management_API.user;

import com.asusoftware.Employee_Management_API.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final UserRepository users;
    private final JavaMailSender mail;
    private final SecureRandom rng = new SecureRandom();

    /** Generează token, îl hash-uiește și-l salvează pe user (cu expirare), apoi trimite emailul. */
    @Transactional
    public void issueTokenAndEmail(AppUser user, String tenant) {
        String token = generateToken();                 // ex: 43 chars base64url
        String tokenHash = sha256Hex(token);           // 64 hex chars
        user.setEmailVerifTokenHash(tokenHash);
        user.setEmailVerifExpiresAt(OffsetDateTime.now().plusDays(2)); // 48h
        users.save(user);

        String link = "https://" + tenant + ".api.tudomeniu.com/api/v1/auth/verify-email?token=" + token;

        SimpleMailMessage m = new SimpleMailMessage();
        m.setTo(user.getEmail());
        m.setSubject("Verify your email");
        m.setText("""
        Salut!
        
        Te rugăm să-ți verifici emailul dând click pe linkul de mai jos:
        %s
        
        Linkul expiră în 48 de ore.
        
        Mulțumim!
        """.formatted(link));
        try { mail.send(m); } catch (Exception ignored) {}
    }

    /** Verifică tokenul primit din link; marchează userul ca verificat. */
    @Transactional
    public void verify(String token) {
        String hash = sha256Hex(token);
        AppUser u = users.findByEmailVerifTokenHash(hash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token"));

        if (u.getEmailVerified() != null && u.getEmailVerified()) {
            // deja verificat – consideră 200 OK
            return;
        }
        if (u.getEmailVerifExpiresAt() == null || OffsetDateTime.now().isAfter(u.getEmailVerifExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token expired");
        }

        u.setEmailVerified(true);
        u.setEmailVerifiedAt(OffsetDateTime.now());
        // “one-time”: invalidează tokenul ca să nu mai poată fi refolosit
        u.setEmailVerifTokenHash(null);
        u.setEmailVerifExpiresAt(null);
        users.save(u);
    }

    /** Trimite din nou un token dacă userul există și nu e verificat. */
    @Transactional
    public void resend(String tenant, String emailLower) {
        AppUser u = users.findByTenantIdAndEmailIgnoreCase(tenant, emailLower)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.OK,
                        "If the account exists, a verification email was sent."));
        if (Boolean.TRUE.equals(u.getEmailVerified())) return;
        issueTokenAndEmail(u, tenant);
    }

    private String generateToken() {
        byte[] bytes = new byte[32]; // 256-bit
        rng.nextBytes(bytes);
        // URL-safe, fără padding
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256Hex(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot hash token", e);
        }
    }
}