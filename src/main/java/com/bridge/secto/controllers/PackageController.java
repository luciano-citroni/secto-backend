package com.bridge.secto.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bridge.secto.dtos.StripeProductDto;
import com.bridge.secto.exceptions.BusinessRuleException;
import com.bridge.secto.services.StripeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/packages")
@RequiredArgsConstructor
public class PackageController {

    private final StripeService stripeService;

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
