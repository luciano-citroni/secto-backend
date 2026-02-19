package com.bridge.secto.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bridge.secto.entities.Client;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {
    List<Client> findByCompanyId(UUID companyId);
    Optional<Client> findByCpf(String cpf);
    List<Client> findByCompanyIdAndStatusTrue(UUID companyId);
    Optional<Client> findByCompanyIdAndCpf(UUID companyId, String cpf);
    List<Client> findByCompanyIdAndCpfContaining(UUID companyId, String cpf);

    @Query("SELECT c FROM Client c WHERE c.company.id = :companyId AND ("
         + "LOWER(c.cpf) LIKE LOWER(CONCAT('%', :query, '%')) OR "
         + "LOWER(c.fullName) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Client> searchByCompanyIdAndQuery(@Param("companyId") UUID companyId, @Param("query") String query);
}