package com.bridge.secto.dtos;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(
    description = "Objeto de resposta contendo todos os dados consolidados da dashboard da empresa. "
        + "Inclui o histórico detalhado de transações de crédito no período selecionado, "
        + "totais agregados de consumo e compra de créditos, saldo atual, "
        + "e contadores de usuários, clientes, scripts e análises."
)
public class DashboardResponseDto {

    @Schema(
        description = "Lista completa de transações de crédito realizadas no mês/ano selecionado, "
            + "ordenadas da mais recente para a mais antiga. Inclui tanto compras (valores positivos) "
            + "quanto consumos por análise (valores negativos). Cada transação contém detalhes como "
            + "valor, tipo de origem, quem realizou, data de criação e data de expiração."
    )
    private List<CreditTransactionResponseDTO> creditTransactions;

    @Schema(
        description = "Soma total dos créditos consumidos (débitos por uso/análise) no mês/ano selecionado. "
            + "Valor negativo representando o total gasto em análises de áudio no período.",
        example = "-15.50"
    )
    private BigDecimal totalCreditsUsed;

    @Schema(
        description = "Soma total dos créditos adquiridos (compras, assinaturas e adições manuais) no mês/ano selecionado. "
            + "Valor positivo representando o total de créditos que entraram na conta no período.",
        example = "50.00"
    )
    private BigDecimal totalCreditsPurchased;

    @Schema(
        description = "Saldo atual real de créditos válidos (não expirados) da empresa. "
            + "Este valor é independente do período selecionado e reflete o saldo disponível neste momento "
            + "para realizar novas análises. Créditos expirados são automaticamente desconsiderados.",
        example = "34.50"
    )
    private BigDecimal currentCreditBalance;

    @Schema(
        description = "Quantidade total de usuários cadastrados na empresa. "
            + "Inclui todos os usuários ativos vinculados à empresa no Keycloak (administradores e operadores).",
        example = "5"
    )
    private long totalUsers;

    @Schema(
        description = "Quantidade total de clientes cadastrados na empresa. "
            + "Inclui todos os clientes (ativos e inativos) vinculados à empresa.",
        example = "42"
    )
    private long totalClients;

    @Schema(
        description = "Quantidade total de scripts de atendimento criados pela empresa. "
            + "Contabiliza todos os scripts independentemente do tipo de serviço ou status.",
        example = "8"
    )
    private long totalScripts;

    @Schema(
        description = "Mês de referência utilizado no filtro (1 = janeiro, 12 = dezembro). "
            + "Corresponde ao parâmetro enviado na requisição ou ao mês atual caso não informado.",
        example = "3",
        minimum = "1",
        maximum = "12"
    )
    private int month;

    @Schema(
        description = "Ano de referência utilizado no filtro. "
            + "Corresponde ao parâmetro enviado na requisição ou ao ano atual caso não informado.",
        example = "2026"
    )
    private int year;

    @Schema(
        description = "Quantidade total de análises de áudio realizadas no mês/ano selecionado. "
            + "Contabiliza todas as análises concluídas (aprovadas e reprovadas) no período.",
        example = "12"
    )
    private long totalAnalysesInPeriod;
}
