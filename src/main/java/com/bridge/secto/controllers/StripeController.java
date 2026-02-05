package com.bridge.secto.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bridge.secto.dtos.CheckoutRequestDto;
import com.bridge.secto.entities.Company;
import com.bridge.secto.exceptions.BusinessRuleException;
import com.bridge.secto.exceptions.ResourceNotFoundException;
import com.bridge.secto.services.AuthService;
import com.bridge.secto.services.StripeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class StripeController {

    private final StripeService stripeService;
    private final AuthService authService;

    @PostMapping("/checkout")
    public ResponseEntity<Map<String, String>> createCheckoutSession(@RequestBody CheckoutRequestDto request) {
        Company company = authService.getCurrentCompany()
            .orElseThrow(() -> new ResourceNotFoundException("Company not found for current user"));
        
        try {
            String url = stripeService.createCheckoutSession(request.getPackageId(), company.getId());
            return ResponseEntity.ok(Map.of("url", url));
        } catch (Exception e) {
            throw new BusinessRuleException(e.getMessage());
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            stripeService.handleWebhook(payload, sigHeader);
            return ResponseEntity.ok("Received");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Webhook Error");
        }
    }

    @GetMapping("/verify-payment/{sessionId}")
    public ResponseEntity<Map<String, Boolean>> verifyPayment(@PathVariable String sessionId) {
        try {
            boolean success = stripeService.verifyAndProcessSession(sessionId);
            return ResponseEntity.ok(Map.of("success", success));
        } catch (Exception e) {
             return ResponseEntity.badRequest().body(Map.of("success", false, "error", true));
        }
    }
}
