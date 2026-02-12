package com.bridge.secto.controllers;

import java.util.List;
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
import com.bridge.secto.dtos.StripeProductDto;
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

    @GetMapping("/products")
    public ResponseEntity<List<StripeProductDto>> listProducts() {
        try {
            List<StripeProductDto> products = stripeService.listProducts();
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            throw new BusinessRuleException("Erro ao buscar produtos: " + e.getMessage());
        }
    }

    @PostMapping("/checkout")
    public ResponseEntity<Map<String, String>> createCheckoutSession(@RequestBody CheckoutRequestDto request) {
        Company company = authService.getCurrentCompany()
            .orElseThrow(() -> new ResourceNotFoundException("Company not found for current user"));
        
        try {
            String url = stripeService.createCheckoutSession(request.getPriceId(), company.getId());
            return ResponseEntity.ok(Map.of("url", url));
        } catch (Exception e) {
            throw new BusinessRuleException(e.getMessage());
        }
    }

    @GetMapping("/verify-payment/{sessionId}")
    public ResponseEntity<Map<String, Object>> verifyPayment(@PathVariable String sessionId) {
        try {
            boolean credited = stripeService.verifyAndProcessPayment(sessionId);
            return ResponseEntity.ok(Map.of("success", credited));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/webhook/stripe/snapshot")
    public ResponseEntity<String> handleSnapshotWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            stripeService.handleWebhook(payload, sigHeader, "snapshot");
            return ResponseEntity.ok("Received");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Webhook Error");
        }
    }

    @PostMapping("/webhook/stripe/minimal")
    public ResponseEntity<String> handleMinimalWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            stripeService.handleWebhook(payload, sigHeader, "minimal");
            return ResponseEntity.ok("Received");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Webhook Error");
        }
    }
}
