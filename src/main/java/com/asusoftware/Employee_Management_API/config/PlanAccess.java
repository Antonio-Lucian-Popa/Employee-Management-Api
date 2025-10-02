package com.asusoftware.Employee_Management_API.config;

import com.asusoftware.Employee_Management_API.company.CompanyRepository;
import com.asusoftware.Employee_Management_API.model.Subscription;
import com.asusoftware.Employee_Management_API.model.SubscriptionStatus;
import com.asusoftware.Employee_Management_API.subscription.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component("planAccess")
@RequiredArgsConstructor
public class PlanAccess {

    private final SubscriptionRepository subs;
    private final CompanyRepository companies;

    /** True dacă tenantul are oricare din planurile cerute SAU este în TRIAL. */
    public boolean hasAnyPlan(String... allowedPlans) {
        Subscription s = currentSubscription();
        if (s == null) return false;

        // Pe TRIAL dăm acces la feature-urile premium (ajustează după nevoie)
        if (s.getStatus() == SubscriptionStatus.TRIAL) return true;

        String current = s.getPlan().name();
        return Arrays.stream(allowedPlans).anyMatch(p -> p.equalsIgnoreCase(current));
    }

    /** True dacă planul curent este exact cel cerut (TRIAL NU contează ca premium aici). */
    public boolean hasPlan(String plan) {
        Subscription s = currentSubscription();
        return s != null && s.getStatus() != SubscriptionStatus.TRIAL && s.getPlan().name().equalsIgnoreCase(plan);
    }

    private Subscription currentSubscription() {
        String tenant = TenantContext.getTenant();
        var company = companies.findBySlug(tenant).orElse(null);
        if (company == null) return null;
        return subs.findByCompany_Id(company.getId()).orElse(null);
    }
}
