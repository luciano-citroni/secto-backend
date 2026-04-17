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
import com.bridge.secto.enums.Gender;
import com.bridge.secto.exceptions.BusinessRuleException;
import com.bridge.secto.exceptions.ResourceNotFoundException;
import com.bridge.secto.exceptions.UnauthorizedActionException;
import com.bridge.secto.repositories.ClientRepository;
import com.bridge.secto.repositories.CompanyRepository;
import com.bridge.secto.services.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Endpoints para gerenciamento de clientes da empresa. Inclui busca por CPF ou nome.")
@SecurityRequirement(name = "keycloak")
public class ClientController {

    private final ClientRepository clientRepository;
    private final CompanyRepository companyRepository;
    private final AuthService authService;

    @Operation(summary = "Listar clientes da empresa", description = "Retorna todos os clientes da empresa autenticada (ativos e inativos)")
    @ApiResponse(responseCode = "200", description = "Lista de clientes retornada com sucesso")
    @GetMapping
    public ResponseEntity<List<ClientResponseDto>> getClients() {
        UUID userCompanyId = authService.getCurrentCompanyId();

        List<ClientResponseDto> dtos = clientRepository.findByCompanyId(userCompanyId).stream()
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

    @Operation(summary = "Buscar cliente por ID", description = "Retorna os dados de um cliente específico da empresa autenticada")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
        @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    })
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

    @Operation(summary = "Criar cliente", description = "Cria um novo cliente associado à empresa autenticada. O CPF deve ser único por empresa.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cliente criado com sucesso"),
        @ApiResponse(responseCode = "400", description = "CPF já existe nesta empresa ou dados inválidos")
    })
    @PostMapping
    public ResponseEntity<ClientResponseDto> createClient(@Valid @RequestBody ClientRequestDto request) {
        UUID userCompanyId = authService.getCurrentCompanyId();

        // Check if CPF already exists for this company
        if (request.getCpf() != null && !request.getCpf().isEmpty()) {
            clientRepository.findByCompanyIdAndCpf(userCompanyId, request.getCpf())
                .ifPresent(existingClient -> {
                    throw new BusinessRuleException("Já existe um cliente com este CPF nesta empresa");
                });
        }

        Company company = companyRepository.findById(userCompanyId)
            .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        Client client = new Client();
        client.setFullName(request.getFullName());
        client.setBirthDate(request.getBirthDate());
        client.setCpf(request.getCpf());
        client.setRg(request.getRg());
        client.setAddress(request.getAddress());
        client.setPhone(request.getPhone());
        client.setEmail(request.getEmail());
        client.setStatus(request.getStatus() != null ? request.getStatus() : true);
        if (request.getGender() != null && !request.getGender().isEmpty()) {
            client.setGender(Gender.valueOf(request.getGender().toUpperCase()));
        }
        client.setRepresentativeName(request.getRepresentativeName());
        client.setRepresentativeCpf(request.getRepresentativeCpf());
        client.setCompany(company);

        client = clientRepository.save(client);

        return ResponseEntity.ok(mapToResponseDto(client));
    }

    @Operation(summary = "Atualizar cliente", description = "Atualiza os dados de um cliente existente da empresa autenticada")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cliente atualizado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Cliente não encontrado"),
        @ApiResponse(responseCode = "400", description = "CPF já existe para outro cliente nesta empresa")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ClientResponseDto> updateClient(@PathVariable UUID id, 
                                                         @Valid @RequestBody ClientRequestDto request) {
        UUID userCompanyId = authService.getCurrentCompanyId();

        Client client = clientRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        if (!client.getCompany().getId().equals(userCompanyId)) {
            throw new UnauthorizedActionException("Unauthorized to access this client");
        }

        // Check if CPF already exists for another client in the same company
        if (request.getCpf() != null && !request.getCpf().isEmpty()) {
            clientRepository.findByCompanyIdAndCpf(userCompanyId, request.getCpf())
                .ifPresent(existingClient -> {
                    if (!existingClient.getId().equals(id)) {
                        throw new BusinessRuleException("Já existe um cliente com este CPF nesta empresa");
                    }
                });
        }

        client.setFullName(request.getFullName());
        client.setBirthDate(request.getBirthDate());
        client.setCpf(request.getCpf());
        client.setRg(request.getRg());
        client.setAddress(request.getAddress());
        client.setPhone(request.getPhone());
        client.setEmail(request.getEmail());
        if (request.getStatus() != null) {
            client.setStatus(request.getStatus());
        }
        if (request.getGender() != null && !request.getGender().isEmpty()) {
            client.setGender(Gender.valueOf(request.getGender().toUpperCase()));
        }
        client.setRepresentativeName(request.getRepresentativeName());
        client.setRepresentativeCpf(request.getRepresentativeCpf());

        client = clientRepository.save(client);

        return ResponseEntity.ok(mapToResponseDto(client));
    }

    @Operation(summary = "Desativar cliente (Soft Delete)", description = "Desativa um cliente definindo status=false. O cliente não é removido do banco.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cliente desativado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    })
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
        dto.setFullName(client.getFullName());
        dto.setBirthDate(client.getBirthDate());
        dto.setCpf(client.getCpf());
        dto.setRg(client.getRg());
        dto.setAddress(client.getAddress());
        dto.setPhone(client.getPhone());
        dto.setEmail(client.getEmail());
        dto.setStatus(client.getStatus());
        dto.setGender(client.getGender() != null ? client.getGender().name() : null);
        dto.setRepresentativeName(client.getRepresentativeName());
        dto.setRepresentativeCpf(client.getRepresentativeCpf());
        dto.setCompanyId(client.getCompany().getId());
        return dto;
    }
}