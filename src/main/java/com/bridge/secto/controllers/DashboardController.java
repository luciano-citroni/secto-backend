package com.bridge.secto.controllers;

import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bridge.secto.dtos.DashboardResponseDto;
import com.bridge.secto.services.DashboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(
    name = "Dashboard", 
    description = "Endpoint para obtenção de dados consolidados da dashboard da empresa do usuário autenticado. "
        + "Fornece uma visão geral com métricas de créditos, usuários, clientes, scripts e análises, "
        + "permitindo filtrar o histórico de créditos por mês e ano."
)
@SecurityRequirement(name = "keycloak")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @Operation(
        summary = "Obter dados consolidados da dashboard",
        description = "Retorna todas as métricas e dados necessários para montar a dashboard da empresa do usuário logado.\n\n"
            + "**Dados retornados:**\n"
            + "- **Histórico de créditos**: lista completa de transações de crédito (compras e consumos) do mês/ano selecionado, ordenadas da mais recente para a mais antiga.\n"
            + "- **Total de créditos consumidos**: soma dos débitos (valor negativo) no período selecionado.\n"
            + "- **Total de créditos adquiridos**: soma das compras (valor positivo) no período selecionado.\n"
            + "- **Saldo atual**: saldo real de créditos válidos (não expirados) da empresa, independente do período selecionado.\n"
            + "- **Total de usuários**: quantidade de usuários cadastrados na empresa (obtido via Keycloak).\n"
            + "- **Total de clientes**: quantidade de clientes cadastrados na empresa.\n"
            + "- **Total de scripts**: quantidade de scripts criados pela empresa.\n"
            + "- **Total de análises no período**: quantidade de análises de áudio realizadas no mês/ano selecionado.\n\n"
            + "**Filtro por período:**\n"
            + "Os parâmetros `month` e `year` são opcionais. Se omitidos, o mês e ano atuais serão utilizados. "
            + "Isso permite consultar dados de meses anteriores para comparação e acompanhamento histórico."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Dados da dashboard retornados com sucesso.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DashboardResponseDto.class),
                examples = @ExampleObject(
                    name = "Exemplo de resposta",
                    summary = "Dashboard do mês de março/2026",
                    value = "{\"creditTransactions\":[{\"id\":\"a1b2c3d4-...\",\"amount\":-2.5,\"sourceType\":\"USAGE\",\"purchasedByName\":\"João Silva\",\"createdAt\":\"2026-03-05T14:30:00Z\"},{\"id\":\"e5f6g7h8-...\",\"amount\":50.00,\"sourceType\":\"ONE_TIME\",\"purchasedByName\":\"Maria Santos\",\"createdAt\":\"2026-03-01T10:00:00Z\",\"expiresAt\":\"2027-03-01T10:00:00Z\",\"remainingAmount\":47.50}],\"totalCreditsUsed\":-2.50,\"totalCreditsPurchased\":50.00,\"currentCreditBalance\":47.50,\"totalUsers\":5,\"totalClients\":42,\"totalScripts\":8,\"totalAnalysesInPeriod\":12,\"month\":3,\"year\":2026}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Token de autenticação ausente, inválido ou expirado. É necessário enviar um token JWT válido no header Authorization.",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "Erro interno do servidor. Pode ocorrer caso a empresa não possua conta de créditos configurada ou haja falha na comunicação com o Keycloak.",
            content = @Content
        )
    })
    public ResponseEntity<DashboardResponseDto> getDashboard(
            @Parameter(
                description = "Mês de referência para o filtro do histórico de créditos e análises. "
                    + "Valor entre 1 (janeiro) e 12 (dezembro). Se não informado, utiliza o mês atual.",
                example = "3",
                schema = @Schema(minimum = "1", maximum = "12")
            )
            @RequestParam(required = false) Integer month,
            @Parameter(
                description = "Ano de referência para o filtro do histórico de créditos e análises. "
                    + "Se não informado, utiliza o ano atual.",
                example = "2026"
            )
            @RequestParam(required = false) Integer year) {

        LocalDate now = LocalDate.now();
        int selectedMonth = (month != null) ? month : now.getMonthValue();
        int selectedYear = (year != null) ? year : now.getYear();

        DashboardResponseDto dashboard = dashboardService.getDashboardData(selectedMonth, selectedYear);
        return ResponseEntity.ok(dashboard);
    }
}
