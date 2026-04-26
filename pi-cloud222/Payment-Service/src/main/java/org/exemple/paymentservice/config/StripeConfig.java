package org.exemple.paymentservice.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeConfig {

    @Value("${stripe.secret.key}")
    private String secretKey;

    @PostConstruct
    public void init() {
        System.out.println("STRIPE key loaded? " + (secretKey != null && !secretKey.isBlank()));
        System.out.println("STRIPE key starts with sk_test? " + (secretKey != null && secretKey.startsWith("sk_test_")));

        if (secretKey == null || secretKey.isBlank()) {
            throw new RuntimeException("STRIPE_SECRET_KEY is missing or empty");
        }

        Stripe.apiKey = secretKey;
    }
}