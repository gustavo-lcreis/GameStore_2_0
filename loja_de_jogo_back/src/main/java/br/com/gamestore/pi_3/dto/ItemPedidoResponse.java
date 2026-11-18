package br.com.gamestore.pi_3.dto;

import br.com.gamestore.pi_3.model.ItemPedido;

import java.math.BigDecimal;

public record ItemPedidoResponse(Long jogoId, String titulo, BigDecimal precoUnitario,
                                 int quantidade, BigDecimal subtotal) {

    public static ItemPedidoResponse de(ItemPedido i) {
        return new ItemPedidoResponse(i.getJogoId(), i.getTitulo(), i.getPrecoUnitario(),
                i.getQuantidade(), i.getSubtotal());
    }
}
