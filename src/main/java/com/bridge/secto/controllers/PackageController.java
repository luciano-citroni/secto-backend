package com.bridge.secto.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bridge.secto.dtos.StripeProductDto;
import com.bridge.secto.exceptions.BusinessRuleException;
import com.bridge.secto.services.StripeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/packages")
@RequiredArgsConstructor
@Tag(name = "Pacotes de Créditos", description = "Endpoints para listagem de pacotes de créditos disponíveis")
public class PackageController {

    private final StripeService stripeService;

    @Operation(summary = "Listar pacotes de créditos", description = "Retorna todos os pacotes de créditos disponíveis (produtos ativos do Stripe com metadata de créditos)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de pacotes retornada com sucesso"),
        @ApiResponse(responseCode = "500", description = "Erro ao buscar pacotes no Stripe")
    })
    @GetMapping
    public ResponseEntity<List<StripeProductDto>> listPackages() {
        try {
            List<StripeProductDto> products = stripeService.listProducts();
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            throw new BusinessRuleException("Erro ao buscar pacotes: " + e.getMessage());
        }
    }
}
