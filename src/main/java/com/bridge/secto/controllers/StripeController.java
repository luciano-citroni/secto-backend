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
import com.bridge.secto.dtos.SubscriptionInfoDto;
import com.bridge.secto.entities.Company;
import com.bridge.secto.exceptions.BusinessRuleException;
import com.bridge.secto.exceptions.ResourceNotFoundException;
import com.bridge.secto.services.AuthService;
import com.bridge.secto.services.StripeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pagamentos", description = "Endpoints para gerenciamento de pagamentos e checkout via Stripe")
public class StripeController {

    private final StripeService stripeService;
    private final AuthService authService;

    @Operation(summary = "Listar produtos disponíveis", description = "Retorna todos os produtos ativos do Stripe com seus respectivos preços e quantidade de créditos")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de produtos retornada com sucesso"),
        @ApiResponse(responseCode = "500", description = "Erro ao buscar produtos no Stripe")
    })
    @GetMapping("/products")
    public ResponseEntity<List<StripeProductDto>> listProducts() {
        try {
            List<StripeProductDto> products = stripeService.listProducts();
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            throw new BusinessRuleException("Erro ao buscar produtos: " + e.getMessage());
        }
    }

    @Operation(summary = "Criar sessão de checkout", description = "Cria uma sessão de checkout no Stripe para compra de créditos. Retorna a URL de redirecionamento para o pagamento")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sessão de checkout criada com sucesso, retorna URL de redirecionamento"),
        @ApiResponse(responseCode = "404", description = "Empresa não encontrada para o usuário autenticado"),
        @ApiResponse(responseCode = "400", description = "Erro ao criar sessão de checkout")
    })
    @PostMapping("/checkout")
    public ResponseEntity<Map<String, String>> createCheckoutSession(
            @RequestBody CheckoutRequestDto request) {
        Company company = authService.getCurrentCompany()
            .orElseThrow(() -> new ResourceNotFoundException("Company not found for current user"));
        
        try {
            String url = stripeService.createCheckoutSession(request.getPriceId(), company.getId());
            return ResponseEntity.ok(Map.of("url", url));
        } catch (Exception e) {
            throw new BusinessRuleException(e.getMessage());
        }
    }

    @Operation(summary = "Verificar pagamento", description = "Verifica o status de um pagamento pelo ID da sessão Stripe e processa os créditos se confirmado")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Verificação realizada. Campo 'success' indica se os créditos foram adicionados")
    })
    @GetMapping("/verify-payment/{sessionId}")
    public ResponseEntity<Map<String, Object>> verifyPayment(
            @Parameter(description = "ID da sessão Stripe retornado após o checkout") @PathVariable String sessionId) {
        try {
            boolean credited = stripeService.verifyAndProcessPayment(sessionId);
            return ResponseEntity.ok(Map.of("success", credited));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @Operation(summary = "Obter assinatura ativa", description = "Retorna informações sobre a assinatura ativa da empresa do usuário logado")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Informações da assinatura retornadas com sucesso"),
        @ApiResponse(responseCode = "204", description = "Nenhuma assinatura ativa encontrada")
    })
    @GetMapping("/subscription")
    public ResponseEntity<SubscriptionInfoDto> getActiveSubscription() {
        Company company = authService.getCurrentCompany()
            .orElseThrow(() -> new ResourceNotFoundException("Company not found for current user"));

        try {
            SubscriptionInfoDto sub = stripeService.getActiveSubscription(company.getId());
            if (sub == null) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(sub);
        } catch (Exception e) {
            log.error("Erro ao buscar assinatura: {}", e.getMessage());
            return ResponseEntity.noContent().build();
        }
    }

    @Operation(summary = "Cancelar assinatura", description = "Cancela a assinatura ativa da empresa do usuário logado imediatamente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Assinatura cancelada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Erro ao cancelar assinatura")
    })
    @PostMapping("/cancel-subscription")
    public ResponseEntity<Map<String, String>> cancelSubscription() {
        Company company = authService.getCurrentCompany()
            .orElseThrow(() -> new ResourceNotFoundException("Company not found for current user"));

        try {
            stripeService.cancelActiveSubscription(company.getId());
            return ResponseEntity.ok(Map.of("message", "Assinatura cancelada com sucesso"));
        } catch (Exception e) {
            throw new BusinessRuleException("Erro ao cancelar assinatura: " + e.getMessage());
        }
    }

    @Operation(summary = "Webhook Stripe (snapshot)", description = "Endpoint para receber webhooks do Stripe no modo snapshot. Processa eventos de checkout completado e pagamentos de assinatura")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Webhook processado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Erro ao processar webhook")
    })
    @PostMapping("/webhook/stripe/snapshot")
    public ResponseEntity<String> handleSnapshotWebhook(
            @RequestBody String payload,
            @Parameter(description = "Assinatura do Stripe para validação do webhook") @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            stripeService.handleWebhook(payload, sigHeader, "snapshot");
            return ResponseEntity.ok("Received");
        } catch (IllegalArgumentException e) {
            log.error("Erro de validação no webhook snapshot: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Webhook signature/payload error");
        } catch (Exception e) {
            log.error("Erro ao processar webhook snapshot: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Webhook processing error");
        }
    }
}
