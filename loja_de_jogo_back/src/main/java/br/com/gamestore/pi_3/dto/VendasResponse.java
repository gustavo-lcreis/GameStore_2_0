package br.com.gamestore.pi_3.dto;

import java.util.List;

public record VendasResponse(ResumoVendas resumo, List<PedidoResponse> vendas,
                             int page, int size, long totalElements, int totalPages) {}
