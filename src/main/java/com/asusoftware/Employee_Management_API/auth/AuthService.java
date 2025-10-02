package com.asusoftware.Employee_Management_API.auth;

import com.asusoftware.Employee_Management_API.company.CompanyRepository;
import com.asusoftware.Employee_Management_API.config.JwtService;
import com.asusoftware.Employee_Management_API.model.*;
import com.asusoftware.Employee_Management_API.model.dto.LoginRequest;
import com.asusoftware.Employee_Management_API.model.dto.RegisterCompanyRequest;
import com.asusoftware.Employee_Management_API.model.dto.TokenResponse;
import com.asusoftware.Employee_Management_API.subscription.SubscriptionRepository;
import com.asusoftware.Employee_Management_API.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final CompanyRepository companies;
    private final UserRepository users;
    private final SubscriptionRepository subs;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    @Transactional
    public TokenResponse registerCompany(RegisterCompanyRequest req){
        // create company
        Company c = Company.builder()
                .name(req.companyName())
                .slug(req.slug())
                .build();
        companies.save(c);

        // create owner
        AppUser owner = AppUser.builder()
                .tenantId(req.slug())
                .email(req.ownerEmail().toLowerCase())
                .password(encoder.encode(req.password()))
                .firstName(req.ownerFirstName())
                .lastName(req.ownerLastName())
                .role(Role.OWNER)
                .status(UserStatus.ACTIVE)
                .provider(AuthProvider.LOCAL)
                .build();
        users.save(owner);

        // start subscription (trial)
        Subscription s = Subscription.builder()
                .company(c)
                .plan(SubscriptionPlan.FREE)
                .status(SubscriptionStatus.TRIAL)
                .startedAt(OffsetDateTime.now())
                .trialUntil(OffsetDateTime.now().plusDays(14))
                .build();
        subs.save(s);

        // tokens
        String access = jwt.access(owner.getId().toString(), req.slug(), owner.getRole().name());
        String refresh = jwt.refresh(owner.getId().toString());
        return new TokenResponse(access, refresh);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(String tenant, LoginRequest req){
        AppUser u = users.findByTenantIdAndEmailIgnoreCase(tenant, req.email())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        if (u.getPassword() == null || !encoder.matches(req.password(), u.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }
        return new TokenResponse(
                jwt.access(u.getId().toString(), tenant, u.getRole().name()),
                jwt.refresh(u.getId().toString())
        );
    }
}