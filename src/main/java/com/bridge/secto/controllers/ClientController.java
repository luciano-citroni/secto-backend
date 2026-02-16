package com.bridge.secto.controllers;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bridge.secto.dtos.ClientRequestDto;
import com.bridge.secto.dtos.ClientResponseDto;
import com.bridge.secto.entities.Client;
import com.bridge.secto.entities.Company;
import com.bridge.secto.exceptions.ResourceNotFoundException;
import com.bridge.secto.exceptions.UnauthorizedActionException;
import com.bridge.secto.repositories.ClientRepository;
import com.bridge.secto.repositories.CompanyRepository;
import com.bridge.secto.services.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Endpoints para gerenciamento de clientes da empresa")
public class ClientController {

    private final ClientRepository clientRepository;
    private final CompanyRepository companyRepository;
    private final AuthService authService;

    @Operation(summary = "Get Clients by Company")
    @ApiResponse(responseCode = "200", description = "Successful operation")
    @GetMapping
    public ResponseEntity<List<ClientResponseDto>> getClients() {
        UUID userCompanyId = authService.getCurrentCompanyId();

        List<ClientResponseDto> dtos = clientRepository.findByCompanyIdAndStatusTrue(userCompanyId).stream()
            .map(this::mapToResponseDto)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Buscar clientes por CPF ou nome", description = "Pesquisa clientes da empresa autenticada por CPF ou nome. Remove automaticamente formatação de CPF (pontos e traços). A busca é case-insensitive e busca por correspondência parcial no CPF, nome ou sobrenome.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de clientes encontrados (pode ser vazia)")
    })
    @GetMapping("/search")
    public ResponseEntity<List<ClientResponseDto>> searchClients(
            @Parameter(description = "Termo de busca: CPF (com ou sem formatação) ou nome do cliente", example = "123.456.789-01 ou João")
            @RequestParam("q") String query) {
        UUID userCompanyId = authService.getCurrentCompanyId();

        // Remove formatting characters (dots and dashes) for CPF-like queries
        String cleanQuery = query.replaceAll("[.\\-/]", "").trim();

        if (cleanQuery.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<ClientResponseDto> dtos = clientRepository.searchByCompanyIdAndQuery(userCompanyId, cleanQuery).stream()
            .map(this::mapToResponseDto)
            .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Get Client by ID")
    @ApiResponse(responseCode = "200", description = "Successful operation")
    @GetMapping("/{id}")
    public ResponseEntity<ClientResponseDto> getClientById(@PathVariable UUID id) {
        UUID userCompanyId = authService.getCurrentCompanyId();

        Client client = clientRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        if (!client.getCompany().getId().equals(userCompanyId)) {
            throw new UnauthorizedActionException("Unauthorized to access this client");
        }

        return ResponseEntity.ok(mapToResponseDto(client));
    }

    @Operation(summary = "Create Client")
    @ApiResponse(responseCode = "200", description = "Successful operation")
    @PostMapping
    public ResponseEntity<ClientResponseDto> createClient(@Valid @RequestBody ClientRequestDto request) {
        UUID userCompanyId = authService.getCurrentCompanyId();

        // Check if CPF already exists
        if (request.getCpf() != null && !request.getCpf().isEmpty()) {
            clientRepository.findByCpf(request.getCpf())
                .ifPresent(existingClient -> {
                    throw new IllegalArgumentException("CPF already exists");
                });
        }

        Company company = companyRepository.findById(userCompanyId)
            .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        Client client = new Client();
        client.setName(request.getName());
        client.setSurname(request.getSurname());
        client.setBirthDate(request.getBirthDate());
        client.setCpf(request.getCpf());
        client.setRg(request.getRg());
        client.setAddress(request.getAddress());
        client.setStatus(request.getStatus() != null ? request.getStatus() : true);
        client.setCompany(company);

        client = clientRepository.save(client);

        return ResponseEntity.ok(mapToResponseDto(client));
    }

    @Operation(summary = "Update Client")
    @ApiResponse(responseCode = "200", description = "Successful operation")
    @PutMapping("/{id}")
    public ResponseEntity<ClientResponseDto> updateClient(@PathVariable UUID id, 
                                                         @Valid @RequestBody ClientRequestDto request) {
        UUID userCompanyId = authService.getCurrentCompanyId();

        Client client = clientRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        if (!client.getCompany().getId().equals(userCompanyId)) {
            throw new UnauthorizedActionException("Unauthorized to access this client");
        }

        // Check if CPF already exists for another client
        if (request.getCpf() != null && !request.getCpf().isEmpty()) {
            clientRepository.findByCpf(request.getCpf())
                .ifPresent(existingClient -> {
                    if (!existingClient.getId().equals(id)) {
                        throw new IllegalArgumentException("CPF already exists for another client");
                    }
                });
        }

        client.setName(request.getName());
        client.setSurname(request.getSurname());
        client.setBirthDate(request.getBirthDate());
        client.setCpf(request.getCpf());
        client.setRg(request.getRg());
        client.setAddress(request.getAddress());
        if (request.getStatus() != null) {
            client.setStatus(request.getStatus());
        }

        client = clientRepository.save(client);

        return ResponseEntity.ok(mapToResponseDto(client));
    }

    @Operation(summary = "Delete Client (Soft Delete)")
    @ApiResponse(responseCode = "200", description = "Successful operation")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(@PathVariable UUID id) {
        UUID userCompanyId = authService.getCurrentCompanyId();

        Client client = clientRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        if (!client.getCompany().getId().equals(userCompanyId)) {
            throw new UnauthorizedActionException("Unauthorized to access this client");
        }

        // Soft delete by setting status to false
        client.setStatus(false);
        clientRepository.save(client);

        return ResponseEntity.ok().build();
    }

    private ClientResponseDto mapToResponseDto(Client client) {
        ClientResponseDto dto = new ClientResponseDto();
        dto.setId(client.getId());
        dto.setName(client.getName());
        dto.setSurname(client.getSurname());
        dto.setBirthDate(client.getBirthDate());
        dto.setCpf(client.getCpf());
        dto.setRg(client.getRg());
        dto.setAddress(client.getAddress());
        dto.setStatus(client.getStatus());
        dto.setCompanyId(client.getCompany().getId());
        return dto;
    }
}