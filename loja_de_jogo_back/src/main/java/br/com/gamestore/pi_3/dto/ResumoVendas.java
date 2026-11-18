package br.com.gamestore.pi_3.dto;

import java.math.BigDecimal;

/** Totais do PERÍODO INTEIRO filtrado (não só da página atual). */
public record ResumoVendas(long quantidadeVendas, BigDecimal valorTotal) {}
