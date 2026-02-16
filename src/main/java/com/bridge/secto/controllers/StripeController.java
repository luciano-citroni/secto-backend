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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
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
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Webhook Error");
        }
    }

    @Operation(summary = "Webhook Stripe (minimal)", description = "Endpoint para receber webhooks do Stripe no modo minimal. Processa eventos de checkout completado e pagamentos de assinatura")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Webhook processado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Erro ao processar webhook")
    })
    @PostMapping("/webhook/stripe/minimal")
    public ResponseEntity<String> handleMinimalWebhook(
            @RequestBody String payload,
            @Parameter(description = "Assinatura do Stripe para validação do webhook") @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            stripeService.handleWebhook(payload, sigHeader, "minimal");
            return ResponseEntity.ok("Received");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Webhook Error");
        }
    }
}
