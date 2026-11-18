package br.com.gamestore.pi_3.dto;

import br.com.gamestore.pi_3.model.Pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Serve tanto para a confirmação da compra quanto para a listagem de vendas do admin. */
public record PedidoResponse(Long id, LocalDateTime dataHora, String clienteNome, String clienteEmail,
                             String status, int quantidadeTotal, BigDecimal total,
                             List<ItemPedidoResponse> itens) {

    public static PedidoResponse de(Pedido p) {
        return new PedidoResponse(p.getId(), p.getDataHora(), p.getClienteNome(), p.getClienteEmail(),
                p.getStatus().name(), p.getQuantidadeTotal(), p.getTotal(),
                p.getItens().stream().map(ItemPedidoResponse::de).toList());
    }
}
