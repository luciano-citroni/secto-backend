package com.bridge.secto.services;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.bridge.secto.entities.Company;
import com.bridge.secto.entities.CompanyCredit;
import com.bridge.secto.entities.CreditPackage;
import com.bridge.secto.entities.CreditTransaction;
import com.bridge.secto.repositories.CompanyCreditRepository;
import com.bridge.secto.repositories.CompanyRepository;
import com.bridge.secto.repositories.CreditPackageRepository;
import com.bridge.secto.repositories.CreditTransactionRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeService {

    @Value("${stripe.secret.key}")
    private String stripeApiKey;

    @Value("${stripe.webhook.secret:whsec_placeholder}") 
    private String endpointSecret;

    @Value("${stripe.success-url:http://localhost:3000/creditos?success=true&session_id={CHECKOUT_SESSION_ID}}")
    private String successUrl;

    @Value("${stripe.cancel-url:http://localhost:3000/creditos?canceled=true}")
    private String cancelUrl;

    private final CompanyCreditRepository companyCreditRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final CompanyRepository companyRepository;
    private final CreditPackageRepository creditPackageRepository;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    public String createCheckoutSession(String packageIdentifier, UUID companyId) throws Exception {
        CreditPackage pack = creditPackageRepository.findByIdentifier(packageIdentifier)
            .orElseThrow(() -> new IllegalArgumentException("Invalid package ID: " + packageIdentifier));
        
        if (!Boolean.TRUE.equals(pack.getActive())) {
             throw new IllegalArgumentException("Package is not active");
        }

        SessionCreateParams params = SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.PAYMENT)
            .setSuccessUrl(successUrl)
            .setCancelUrl(cancelUrl)
            .addLineItem(SessionCreateParams.LineItem.builder()
                .setQuantity(1L)
                .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                    .setCurrency("brl")
                    .setUnitAmount(pack.getPriceInCents())
                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                        .setName(pack.getName())
                        .build())
                    .build())
                .build())
            .putMetadata("companyId", companyId.toString())
            .putMetadata("credits", String.valueOf(pack.getCredits()))
            .putMetadata("packageIdentifier", pack.getIdentifier())
            .build();

        Session session = Session.create(params);
        return session.getUrl();
    }

    @Transactional
    public boolean verifyAndProcessSession(String sessionId) throws Exception {
        Session session = Session.retrieve(sessionId);
        
        if ("paid".equals(session.getPaymentStatus())) {
             Map<String, String> metadata = session.getMetadata();
             if (metadata != null) {
                 String companyIdStr = metadata.get("companyId");
                 String creditsStr = metadata.get("credits");
                 
                 if (companyIdStr != null && creditsStr != null) {
                     addCredits(UUID.fromString(companyIdStr), new BigDecimal(creditsStr), session.getId());
                     return true;
                 }
             }
        }
        return false;
    }

    @Transactional
    public void handleWebhook(String payload, String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            log.error("Invalid signature", e);
            throw new RuntimeException("Invalid signature");
        } catch (Exception e) {
             log.error("Webhook error", e);
             throw new RuntimeException("Webhook error");
        }

        if ("checkout.session.completed".equals(event.getType())) {
             Optional<com.stripe.model.StripeObject> object = event.getDataObjectDeserializer().getObject();
             
             if (object.isPresent() && object.get() instanceof Session) {
                Session session = (Session) object.get();
                
                Map<String, String> metadata = session.getMetadata();
                if (metadata != null) {
                    String companyIdStr = metadata.get("companyId");
                    String creditsStr = metadata.get("credits");
                    
                    if (companyIdStr != null && creditsStr != null) {
                        try {
                            addCredits(UUID.fromString(companyIdStr), new BigDecimal(creditsStr), session.getId());
                        } catch (NumberFormatException e) {
                            log.error("Invalid credit amount or company ID format", e);
                        }
                    }
                }
             }
        }
    }

    private void addCredits(UUID companyId, BigDecimal amount, String stripeSessionId) {
        if (stripeSessionId != null && creditTransactionRepository.existsByStripeSessionId(stripeSessionId)) {
            log.info("Transaction for session {} already processed.", stripeSessionId);
            return;
        }

        CompanyCredit credit = companyCreditRepository.findByCompanyId(companyId);
        if (credit == null) {
             Company company = companyRepository.findById(companyId)
                 .orElseThrow(() -> new RuntimeException("Company not found: " + companyId));
             
             credit = new CompanyCredit();
             credit.setCreditAmount(BigDecimal.ZERO);
             credit.setCompany(company);
             credit = companyCreditRepository.save(credit);
             
             company.setCompanyCredit(credit);
             companyRepository.save(company);
        }
        
        credit.setCreditAmount(credit.getCreditAmount().add(amount));
        companyCreditRepository.save(credit);
        
        CreditTransaction transaction = new CreditTransaction();
        transaction.setCompanyCredit(credit);
        transaction.setAmount(amount);
        transaction.setStripeSessionId(stripeSessionId);
        
        try {
            creditTransactionRepository.save(transaction);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("Concurrent transaction detected for session {}", stripeSessionId);
            throw e; 
        }
        
        log.info("Added {} credits to company {}", amount, companyId);
    }
    
}
