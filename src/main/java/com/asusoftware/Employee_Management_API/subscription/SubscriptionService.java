package com.asusoftware.Employee_Management_API.subscription;

import com.asusoftware.Employee_Management_API.company.CompanyRepository;
import com.asusoftware.Employee_Management_API.config.TenantContext;
import com.asusoftware.Employee_Management_API.model.Subscription;
import com.asusoftware.Employee_Management_API.model.SubscriptionPlan;
import com.asusoftware.Employee_Management_API.model.SubscriptionStatus;
import com.asusoftware.Employee_Management_API.model.dto.SubscriptionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository repo;
    private final CompanyRepository companies;

    @Transactional(readOnly = true)
    public SubscriptionDto getCurrent(){
        var company = companies.findBySlug(TenantContext.getTenant()).orElseThrow();
        Subscription s = repo.findByCompany_Id(company.getId()).orElseThrow();
        return new SubscriptionDto(company.getId(), s.getPlan(), s.getStatus(), s.getTrialUntil(), s.getNextPaymentDue());
    }

    @Transactional
    public void setPlan(SubscriptionPlan plan, SubscriptionStatus status){
        var company = companies.findBySlug(TenantContext.getTenant()).orElseThrow();
        Subscription s = repo.findByCompany_Id(company.getId()).orElseThrow();
        s.setPlan(plan);
        s.setStatus(status);
        repo.save(s);
    }
}
