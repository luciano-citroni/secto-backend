package com.bridge.secto.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bridge.secto.entities.Client;

@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {
    List<Client> findByCompanyId(UUID companyId);
    Optional<Client> findByCpf(String cpf);
    List<Client> findByCompanyIdAndStatusTrue(UUID companyId);
}