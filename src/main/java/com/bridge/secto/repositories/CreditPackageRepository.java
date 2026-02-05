package com.bridge.secto.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bridge.secto.entities.CreditPackage;

@Repository
public interface CreditPackageRepository extends JpaRepository<CreditPackage, UUID> {
    Optional<CreditPackage> findByIdentifier(String identifier);
    List<CreditPackage> findByActiveTrue();
}
