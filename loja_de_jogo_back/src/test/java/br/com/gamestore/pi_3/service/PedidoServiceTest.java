package br.com.gamestore.pi_3.service;

import br.com.gamestore.pi_3.dto.ItemPedidoRequest;
import br.com.gamestore.pi_3.dto.PedidoRequest;
import br.com.gamestore.pi_3.model.Jogo;
import br.com.gamestore.pi_3.model.Pedido;
import br.com.gamestore.pi_3.model.StatusPedido;
import br.com.gamestore.pi_3.model.Usuario;
import br.com.gamestore.pi_3.repository.JogoRepository;
import br.com.gamestore.pi_3.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock PedidoRepository pedidoRepository;
    @Mock JogoRepository jogoRepository;
    @InjectMocks PedidoService service;

    private final Usuario cliente = new Usuario(7L, "Ana", "ana@x.com", "hash", "ROLE_USER");

    private static Jogo jogo(Long id, String titulo, Double preco) {
        return new Jogo(id, titulo, null, preco, null, "PC", null);
    }

    private static PedidoRequest carrinho(ItemPedidoRequest... itens) {
        return new PedidoRequest(List.of(itens));
    }

    private void salvarRetornaArgumento() {
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void calculaSubtotaisQuantidadeETotalNoServidor() {
        when(jogoRepository.findAllById(any())).thenReturn(
                List.of(jogo(1L, "Neon Drift", 59.90), jogo(2L, "Puzzle Orbit", 19.90)));
        salvarRetornaArgumento();

        Pedido p = service.finalizar(cliente,
                carrinho(new ItemPedidoRequest(1L, 2), new ItemPedidoRequest(2L, 1)));

        assertThat(p.getStatus()).isEqualTo(StatusPedido.CONCLUIDA);
        assertThat(p.getQuantidadeTotal()).isEqualTo(3);
        assertThat(p.getTotal()).isEqualByComparingTo("139.70"); // 2 x 59,90 + 19,90
        assertThat(p.getItens()).hasSize(2);
        assertThat(p.getItens().get(0).getSubtotal()).isEqualByComparingTo("119.80");
        assertThat(p.getItens().get(1).getSubtotal()).isEqualByComparingTo("19.90");
        assertThat(p.getDataHora()).isNotNull();
        assertThat(p.getClienteEmail()).isEqualTo("ana@x.com");
        assertThat(p.getUsuario()).isSameAs(cliente);
    }

    @Test
    void naoPerdePrecisaoDeDoubleNoTotal() {
        when(jogoRepository.findAllById(any())).thenReturn(List.of(jogo(1L, "Jogo", 19.99)));
        salvarRetornaArgumento();

        Pedido p = service.finalizar(cliente, carrinho(new ItemPedidoRequest(1L, 3)));

        assertThat(p.getTotal()).isEqualByComparingTo("59.97");
    }

    @Test
    void somaLinhasRepetidasDoMesmoJogo() {
        when(jogoRepository.findAllById(any())).thenReturn(List.of(jogo(1L, "Neon Drift", 10.00)));
        salvarRetornaArgumento();

        Pedido p = service.finalizar(cliente,
                carrinho(new ItemPedidoRequest(1L, 2), new ItemPedidoRequest(1L, 3)));

        assertThat(p.getItens()).hasSize(1);
        assertThat(p.getItens().get(0).getQuantidade()).isEqualTo(5);
        assertThat(p.getTotal()).isEqualByComparingTo("50.00");
    }

    @Test
    void rejeitaCarrinhoVazio() {
        assertThatThrownBy(() -> service.finalizar(cliente, new PedidoRequest(List.of())))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getReason()).contains("vazio");
                });
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void rejeitaJogoInexistente() {
        when(jogoRepository.findAllById(any())).thenReturn(List.of(jogo(1L, "Neon Drift", 59.90)));

        assertThatThrownBy(() -> service.finalizar(cliente,
                carrinho(new ItemPedidoRequest(1L, 1), new ItemPedidoRequest(99L, 1))))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getReason()).contains("99");
                });
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void rejeitaJogoSemPrecoValido() {
        when(jogoRepository.findAllById(any())).thenReturn(List.of(jogo(1L, "Sem Preço", null)));

        assertThatThrownBy(() -> service.finalizar(cliente, carrinho(new ItemPedidoRequest(1L, 1))))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getReason()).contains("Sem Preço");
                });
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void rejeitaQuantidadeInvalida() {
        assertThatThrownBy(() -> service.finalizar(cliente, carrinho(new ItemPedidoRequest(1L, 0))))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.finalizar(cliente, carrinho(new ItemPedidoRequest(1L, 100))))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.finalizar(cliente,
                carrinho(new ItemPedidoRequest(1L, 60), new ItemPedidoRequest(1L, 60))))
                .isInstanceOf(ResponseStatusException.class); // 120 após consolidar
        verify(pedidoRepository, never()).save(any());
    }
}
