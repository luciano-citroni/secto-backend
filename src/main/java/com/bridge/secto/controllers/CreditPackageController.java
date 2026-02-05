package com.bridge.secto.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bridge.secto.entities.CreditPackage;
import com.bridge.secto.repositories.CreditPackageRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/packages")
@RequiredArgsConstructor
@Tag(name = "Credit Packages", description = "Management of credit packages")
public class CreditPackageController {

    private final CreditPackageRepository creditPackageRepository;

    @GetMapping
    @Operation(summary = "List all active packages for purchase")
    public List<CreditPackage> listActivePackages() {
        return creditPackageRepository.findByActiveTrue();
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "List all packages (Admin only)")
    public List<CreditPackage> listAllPackages() {
        return creditPackageRepository.findAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Create a new package (Admin only)")
    public ResponseEntity<CreditPackage> createPackage(@RequestBody @Valid CreditPackage creditPackage) {
        return new ResponseEntity<>(creditPackageRepository.save(creditPackage), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Update an existing package (Admin only)")
    public ResponseEntity<CreditPackage> updatePackage(@PathVariable UUID id, @RequestBody CreditPackage packageDetails) {
        return creditPackageRepository.findById(id)
            .map(existingPackage -> {
                existingPackage.setName(packageDetails.getName());
                existingPackage.setIdentifier(packageDetails.getIdentifier());
                existingPackage.setPriceInCents(packageDetails.getPriceInCents());
                existingPackage.setCredits(packageDetails.getCredits());
                existingPackage.setActive(packageDetails.getActive());
                return new ResponseEntity<>(creditPackageRepository.save(existingPackage), HttpStatus.OK);
            })
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Delete a package (Admin only)")
    public ResponseEntity<Void> deletePackage(@PathVariable UUID id) {
        if (creditPackageRepository.existsById(id)) {
            creditPackageRepository.deleteById(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}
