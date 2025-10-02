package com.asusoftware.Employee_Management_API.subscription;



import com.asusoftware.Employee_Management_API.model.SubscriptionPlan;
import com.asusoftware.Employee_Management_API.model.SubscriptionStatus;
import com.asusoftware.Employee_Management_API.model.dto.PlanRequest;
import com.stripe.StripeClient;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class StripeService {

    private final SubscriptionService subs;

    @Value("${app.stripe.secret-key}") String secretKey;
    @Value("${app.stripe.webhook-secret}") String webhookSecret;
    @Value("${app.stripe.price-pro}") String pricePro;
    @Value("${app.stripe.price-enterprise}") String priceEnterprise;
    @Value("${app.stripe.frontend-success-url}") String successUrl;
    @Value("${app.stripe.frontend-cancel-url}") String cancelUrl;

    private StripeClient client(){ return new StripeClient(secretKey); }

    public Map<String,String> createCheckoutSession(PlanRequest req){
        String priceId = switch (req.plan()) {
            case PRO -> pricePro;
            case ENTERPRISE -> priceEnterprise;
            case FREE -> throw new IllegalArgumentException("FREE has no checkout");
        };

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(priceId).setQuantity(1L).build())
                .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(cancelUrl)
                .build();
        try {
            Session session = client().checkout().sessions().create(params);
            return Map.of("url", session.getUrl());
        } catch (Exception e) {
            throw new RuntimeException("Stripe error: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void handleWebhook(String payload, String signature){
        final Event event;
        try {
            event = Webhook.constructEvent(payload, signature, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new RuntimeException("Invalid Stripe signature");
        }

        switch (event.getType()) {
            case "checkout.session.completed" -> subs.setPlan(resolvePlan(event), SubscriptionStatus.ACTIVE);
            case "invoice.payment_succeeded"   -> subs.setPlan(resolvePlan(event), SubscriptionStatus.ACTIVE);
            case "invoice.payment_failed"      -> subs.setPlan(resolvePlan(event), SubscriptionStatus.PAST_DUE);
            case "customer.subscription.deleted" -> subs.setPlan(resolvePlan(event), SubscriptionStatus.EXPIRED);
            default -> { /* ignore */ }
        }
    }

    private SubscriptionPlan resolvePlan(Event event){
        // MVP: dacă ai mai multe prețuri, le diferențiezi după priceId
        // În practică, extragi price din obiectul de event; aici simplificăm
        // (poți salva ultimul priceId în Subscription la checkout).
        // Ca fallback: PRO
        return SubscriptionPlan.PRO;
    }
}
