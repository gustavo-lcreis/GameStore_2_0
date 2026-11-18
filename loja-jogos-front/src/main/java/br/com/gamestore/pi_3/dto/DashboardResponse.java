package br.com.gamestore.pi_3.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Indicadores consolidados do período. Faturamento e ticket médio consideram só pedidos CONCLUIDA. */
public record DashboardResponse(
        LocalDate inicio,
        LocalDate fim,
        String agrupamento,
        Kpis kpis,
        List<PorStatus> porStatus,
        List<JogoMaisVendido> topJogos,
        List<PontoEvolucao> evolucao) {

    /** totalPedidosGeral = histórico inteiro da loja; totalPedidos = só o período filtrado. */
    public record Kpis(long totalPedidosGeral, long totalPedidos, BigDecimal faturamento,
                       BigDecimal ticketMedio, long jogosVendidos) {}

    public record PorStatus(String status, long pedidos, BigDecimal faturamento) {}

    public record JogoMaisVendido(Long jogoId, String titulo, long quantidade, BigDecimal receita) {}

    /** periodo: "2026-11-05" (agrupamento DIA) ou "2026-11" (agrupamento MES). Períodos sem venda vêm com zero. */
    public record PontoEvolucao(String periodo, long pedidos, BigDecimal faturamento) {}
}
