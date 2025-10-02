package com.asusoftware.Employee_Management_API.subscription;

import com.asusoftware.Employee_Management_API.model.dto.PlanRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {
    private final StripeService stripe;

    @PostMapping("/checkout-session")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public Map<String,String> checkout(@RequestBody PlanRequest req){
        return stripe.createCheckoutSession(req);
    }
}