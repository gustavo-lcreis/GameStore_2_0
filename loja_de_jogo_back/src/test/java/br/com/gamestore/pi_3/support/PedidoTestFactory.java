package br.com.gamestore.pi_3.support;

import br.com.gamestore.pi_3.model.ItemPedido;
import br.com.gamestore.pi_3.model.Pedido;
import br.com.gamestore.pi_3.model.StatusPedido;
import br.com.gamestore.pi_3.model.Usuario;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class PedidoTestFactory {

    private PedidoTestFactory() {}

    /** Pedido CONCLUIDA com um único item, pronto para ser salvo. */
    public static Pedido pedido(Usuario cliente, LocalDateTime dataHora, String total) {
        Pedido p = new Pedido();
        p.setUsuario(cliente);
        p.setClienteNome(cliente.getNome());
        p.setClienteEmail(cliente.getUsername());
        p.setDataHora(dataHora);
        p.setStatus(StatusPedido.CONCLUIDA);
        p.setQuantidadeTotal(1);
        p.setTotal(new BigDecimal(total));

        ItemPedido item = new ItemPedido();
        item.setJogoId(1L);
        item.setTitulo("Neon Drift");
        item.setPrecoUnitario(new BigDecimal(total));
        item.setQuantidade(1);
        item.setSubtotal(new BigDecimal(total));
        p.adicionarItem(item);
        return p;
    }
}
