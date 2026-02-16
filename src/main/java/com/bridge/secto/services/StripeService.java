package com.bridge.secto.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.bridge.secto.dtos.StripeProductDto;
import com.bridge.secto.entities.Company;
import com.bridge.secto.entities.CompanyCredit;
import com.bridge.secto.entities.CreditTransaction;
import com.bridge.secto.repositories.CompanyCreditRepository;
import com.bridge.secto.repositories.CompanyRepository;
import com.bridge.secto.repositories.CreditTransactionRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.Price;
import com.stripe.model.Product;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.PriceListParams;
import com.stripe.param.ProductListParams;
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

    @Value("${stripe.webhook.secret.minimal:whsec_placeholder}")
    private String endpointSecretMinimal;

    @Value("${stripe.success-url:http://localhost:3000/creditos?success=true&session_id={CHECKOUT_SESSION_ID}}")
    private String successUrl;

    @Value("${stripe.cancel-url:http://localhost:3000/creditos?canceled=true}")
    private String cancelUrl;

    private final CompanyCreditRepository companyCreditRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final CompanyRepository companyRepository;
    private final AuthService authService;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    /**
     * Fetch all active products with their prices from Stripe.
     * Only products with a "credits" metadata key are returned.
     */
    public List<StripeProductDto> listProducts() throws Exception {
        List<StripeProductDto> result = new ArrayList<>();

        ProductListParams productParams = ProductListParams.builder()
                .setActive(true)
                .setLimit(100L)
                .build();

        List<Product> products = Product.list(productParams).getData();
        log.info("Found {} active products from Stripe", products.size());

        for (Product product : products) {
            Map<String, String> metadata = product.getMetadata();
            log.info("Product '{}' (id={}) metadata: {}", product.getName(), product.getId(), metadata);
            if (metadata == null || !metadata.containsKey("credits")) {
                log.info("Skipping product '{}' — no 'credits' metadata key", product.getName());
                continue;
            }

            int credits;
            try {
                credits = Integer.parseInt(metadata.get("credits"));
            } catch (NumberFormatException e) {
                log.warn("Product {} has invalid credits metadata: {}", product.getId(), metadata.get("credits"));
                continue;
            }

            PriceListParams priceParams = PriceListParams.builder()
                    .setProduct(product.getId())
                    .setActive(true)
                    .setLimit(10L)
                    .build();

            List<Price> prices = Price.list(priceParams).getData();

            for (Price price : prices) {
                String type = price.getType();
                String interval = null;
                if (price.getRecurring() != null) {
                    interval = price.getRecurring().getInterval();
                }

                result.add(StripeProductDto.builder()
                        .productId(product.getId())
                        .priceId(price.getId())
                        .name(product.getName())
                        .description(product.getDescription())
                        .unitAmount(price.getUnitAmount())
                        .currency(price.getCurrency())
                        .type(type)
                        .interval(interval)
                        .credits(credits)
                        .build());
            }
        }

        return result;
    }

    /**
     * Create a Stripe Checkout Session using a Stripe price ID.
     * Uses SUBSCRIPTION mode for recurring prices, PAYMENT mode for one-time.
     */
    public String createCheckoutSession(String priceId, UUID companyId) throws Exception {
        Price price = Price.retrieve(priceId);
        boolean isRecurring = "recurring".equals(price.getType());

        Product product = Product.retrieve(price.getProduct());
        String creditsStr = product.getMetadata() != null ? product.getMetadata().get("credits") : null;
        if (creditsStr == null) {
            throw new IllegalArgumentException("Product does not have credits metadata");
        }

        SessionCreateParams.Mode mode = isRecurring
                ? SessionCreateParams.Mode.SUBSCRIPTION
                : SessionCreateParams.Mode.PAYMENT;

        SessionCreateParams.Builder builder = SessionCreateParams.builder()
                .setMode(mode)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPrice(priceId)
                        .build())
                .putMetadata("companyId", companyId.toString())
                .putMetadata("credits", creditsStr);

        if (isRecurring) {
            builder.setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                    .putMetadata("companyId", companyId.toString())
                    .putMetadata("credits", creditsStr)
                    .build());
        }

        Session session = Session.create(builder.build());
        return session.getUrl();
    }

    /**
     * Verify a Checkout Session by its ID and process credits if payment is complete.
     * Called from the frontend after redirect back from Stripe.
     */
    @Transactional
    public boolean verifyAndProcessPayment(String sessionId) throws Exception {
        Session session = Session.retrieve(sessionId);

        if (session == null) {
            log.warn("Session not found: {}", sessionId);
            return false;
        }

        String paymentStatus = session.getPaymentStatus();
        log.info("Session {} payment_status={}, mode={}", sessionId, paymentStatus, session.getMode());

        if (!"paid".equals(paymentStatus)) {
            log.info("Session {} not yet paid (status={})", sessionId, paymentStatus);
            return false;
        }

        // Check if already processed
        if (creditTransactionRepository.existsByStripeSessionId(sessionId)) {
            log.info("Session {} already processed, returning success", sessionId);
            return true;
        }

        // Try metadata from session first
        Map<String, String> metadata = session.getMetadata();
        String companyIdStr = metadata != null ? metadata.get("companyId") : null;
        String creditsStr = metadata != null ? metadata.get("credits") : null;

        // For subscriptions, metadata may be on the subscription
        if ((companyIdStr == null || creditsStr == null) && "subscription".equals(session.getMode())) {
            String subscriptionId = session.getSubscription();
            if (subscriptionId != null) {
                Subscription subscription = Subscription.retrieve(subscriptionId);
                Map<String, String> subMeta = subscription.getMetadata();
                if (subMeta != null) {
                    if (companyIdStr == null) companyIdStr = subMeta.get("companyId");
                    if (creditsStr == null) creditsStr = subMeta.get("credits");
                }
            }
        }

        if (companyIdStr == null || creditsStr == null) {
            log.warn("Session {} missing companyId or credits metadata", sessionId);
            return false;
        }

        addCredits(
                UUID.fromString(companyIdStr),
                new BigDecimal(creditsStr),
                sessionId
        );
        log.info("Verified and added {} credits for session {}", creditsStr, sessionId);
        return true;
    }

    @Transactional
    public void handleWebhook(String payload, String sigHeader, String webhookType) {
        Event event;

        try {
            String secretToUse = "minimal".equals(webhookType) ? endpointSecretMinimal : endpointSecret;
            event = Webhook.constructEvent(payload, sigHeader, secretToUse);
            log.info("Successfully validated {} webhook signature for event: {}", webhookType, event.getType());
        } catch (IllegalArgumentException e) {
            log.error("Invalid JSON payload in {} webhook", webhookType, e);
            return;
        } catch (SignatureVerificationException e) {
            log.error("Invalid signature for {} webhook", webhookType, e);
            return;
        }

        switch (event.getType()) {
            case "checkout.session.completed":
                handleCheckoutSessionCompleted(event, webhookType);
                break;
            case "invoice.payment_succeeded":
                handleInvoicePaymentSucceeded(event);
                break;
            default:
                log.info("Unhandled {} webhook event type: {}", webhookType, event.getType());
        }
    }

    private void handleCheckoutSessionCompleted(Event event, String webhookType) {
        log.info("Processing checkout.session.completed via {} webhook", webhookType);

        Session session = extractSession(event, webhookType);
        if (session == null) return;

        // Only process one-time payments here; subscriptions use invoice.payment_succeeded
        if ("payment".equals(session.getMode())) {
            processSessionCredits(session);
        } else {
            log.info("Skipping checkout credit allocation for subscription — handled via invoice.payment_succeeded");
        }
    }

    /**
     * Handle invoice.payment_succeeded — fires on each subscription billing cycle.
     */
    private void handleInvoicePaymentSucceeded(Event event) {
        log.info("Processing invoice.payment_succeeded");

        Optional<com.stripe.model.StripeObject> object = event.getDataObjectDeserializer().getObject();
        if (object.isEmpty()) {
            log.error("Could not deserialize invoice from webhook event");
            return;
        }

        if (!(object.get() instanceof Invoice)) {
            log.error("Expected Invoice but got: {}", object.get().getClass().getSimpleName());
            return;
        }

        Invoice invoice = (Invoice) object.get();

        if (!"paid".equals(invoice.getStatus())) {
            log.info("Invoice {} not paid (status={}), skipping", invoice.getId(), invoice.getStatus());
            return;
        }

        String subscriptionId = invoice.getSubscription();
        if (subscriptionId == null) {
            log.info("Invoice {} has no subscription, skipping", invoice.getId());
            return;
        }

        try {
            Subscription subscription = Subscription.retrieve(subscriptionId);
            Map<String, String> metadata = subscription.getMetadata();

            if (metadata == null || metadata.get("companyId") == null || metadata.get("credits") == null) {
                log.warn("Subscription {} missing companyId or credits metadata", subscriptionId);
                return;
            }

            addCredits(
                    UUID.fromString(metadata.get("companyId")),
                    new BigDecimal(metadata.get("credits")),
                    invoice.getId()
            );
            log.info("Added {} credits for subscription invoice {}", metadata.get("credits"), invoice.getId());

        } catch (Exception e) {
            log.error("Error processing invoice.payment_succeeded for subscription {}", subscriptionId, e);
        }
    }

    private Session extractSession(Event event, String webhookType) {
        if ("minimal".equals(webhookType)) {
            return extractSessionFromMinimalEvent(event);
        }

        Optional<com.stripe.model.StripeObject> object = event.getDataObjectDeserializer().getObject();
        if (object.isPresent() && object.get() instanceof Session) {
            return (Session) object.get();
        }

        log.error("Could not extract session from snapshot webhook event");
        return null;
    }

    private Session extractSessionFromMinimalEvent(Event event) {
        try {
            Optional<com.stripe.model.StripeObject> object = event.getDataObjectDeserializer().getObject();
            String sessionId = null;

            if (object.isPresent() && object.get() instanceof Session) {
                sessionId = ((Session) object.get()).getId();
            } else if (object.isPresent()) {
                String eventData = event.getData().toJson();
                int idStart = eventData.indexOf("\"id\":\"") + 6;
                if (idStart > 5) {
                    int idEnd = eventData.indexOf("\"", idStart);
                    if (idEnd > idStart) {
                        sessionId = eventData.substring(idStart, idEnd);
                    }
                }
            }

            if (sessionId != null) {
                return Session.retrieve(sessionId);
            }
        } catch (Exception e) {
            log.error("Error extracting session from minimal webhook", e);
        }
        return null;
    }

    private void processSessionCredits(Session session) {
        Map<String, String> metadata = session.getMetadata();
        if (metadata == null) return;

        String companyIdStr = metadata.get("companyId");
        String creditsStr = metadata.get("credits");

        if (companyIdStr != null && creditsStr != null) {
            try {
                addCredits(UUID.fromString(companyIdStr), new BigDecimal(creditsStr), session.getId());
                log.info("Successfully processed credits for session: {}", session.getId());
            } catch (NumberFormatException e) {
                log.error("Invalid credit amount or company ID format", e);
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

        // Capture the user who made the purchase
        try {
            authService.getCurrentUser().ifPresent(user -> {
                transaction.setPurchasedBy(user.getKeycloakId());
                transaction.setPurchasedByName(user.getName() != null ? user.getName() : user.getUsername());
            });
        } catch (Exception e) {
            log.debug("Could not resolve current user for transaction (webhook context): {}", e.getMessage());
        }

        try {
            creditTransactionRepository.save(transaction);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("Concurrent transaction detected for session {}", stripeSessionId);
            throw e;
        }

        log.info("Added {} credits to company {}", amount, companyId);
    }
}
