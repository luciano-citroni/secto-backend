package com.bridge.secto.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bridge.secto.entities.Company;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {
    
    /**
     * Busca company pelo client_id para autenticação via client credentials
     */
    Optional<Company> findByClientId(String clientId);
    
}
