package com.asusoftware.Employee_Management_API.subscription;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhooks/stripe")
@RequiredArgsConstructor
public class StripeWebhookController {
    private final StripeService stripe;

    @PostMapping
    public void handle(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sig){
        stripe.handleWebhook(payload, sig);
    }
}
