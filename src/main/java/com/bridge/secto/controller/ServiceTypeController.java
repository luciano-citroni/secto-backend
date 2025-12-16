package com.bridge.secto.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bridge.secto.entities.ServiceType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/service-types")
@Tag(name = "Service Types", description = "Gerenciamento de tipos de serviço para análise de voz")
public class ServiceTypeController {

    @Operation(
        summary = "Listar todos os tipos de serviço",
        description = "Retorna uma lista com todos os tipos de serviço cadastrados no sistema"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Lista retornada com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ServiceType.class)
            )
        ),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @GetMapping
    public ResponseEntity<List<ServiceType>> getAllServiceTypes() {
        // TODO: Implementar lógica de busca
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Buscar tipo de serviço por ID",
        description = "Retorna um tipo de serviço específico baseado no ID fornecido"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Tipo de serviço encontrado",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ServiceType.class)
            )
        ),
        @ApiResponse(responseCode = "404", description = "Tipo de serviço não encontrado"),
        @ApiResponse(responseCode = "400", description = "ID inválido fornecido")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ServiceType> getServiceTypeById(
        @Parameter(description = "ID único do tipo de serviço", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable UUID id
    ) {
        // TODO: Implementar lógica de busca por ID
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Criar novo tipo de serviço",
        description = "Cria um novo tipo de serviço no sistema"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201", 
            description = "Tipo de serviço criado com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ServiceType.class)
            )
        ),
        @ApiResponse(responseCode = "400", description = "Dados inválidos fornecidos"),
        @ApiResponse(responseCode = "409", description = "Tipo de serviço já existe")
    })
    @PostMapping
    public ResponseEntity<ServiceType> createServiceType(
        @Parameter(description = "Dados do tipo de serviço a ser criado", required = true)
        @RequestBody ServiceType serviceType
    ) {
        // TODO: Implementar lógica de criação
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(
        summary = "Atualizar tipo de serviço",
        description = "Atualiza os dados de um tipo de serviço existente"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Tipo de serviço atualizado com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ServiceType.class)
            )
        ),
        @ApiResponse(responseCode = "404", description = "Tipo de serviço não encontrado"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos fornecidos")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ServiceType> updateServiceType(
        @Parameter(description = "ID único do tipo de serviço", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable UUID id, 
        @Parameter(description = "Dados atualizados do tipo de serviço", required = true)
        @RequestBody ServiceType serviceTypeDetails
    ) {
        // TODO: Implementar lógica de atualização
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Deletar tipo de serviço",
        description = "Remove um tipo de serviço do sistema"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Tipo de serviço deletado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Tipo de serviço não encontrado"),
        @ApiResponse(responseCode = "409", description = "Não é possível deletar - tipo de serviço possui dependências")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServiceType(
        @Parameter(description = "ID único do tipo de serviço", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable UUID id
    ) {
        // TODO: Implementar lógica de remoção
        return ResponseEntity.noContent().build();
    }
}