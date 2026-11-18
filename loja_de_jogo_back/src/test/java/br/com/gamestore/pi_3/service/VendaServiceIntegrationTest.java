package br.com.gamestore.pi_3.service;

import br.com.gamestore.pi_3.dto.VendasResponse;
import br.com.gamestore.pi_3.model.Usuario;
import br.com.gamestore.pi_3.repository.PedidoRepository;
import br.com.gamestore.pi_3.repository.UsuarioRepository;
import br.com.gamestore.pi_3.support.PedidoTestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(VendaService.class)
class VendaServiceIntegrationTest {

    @Autowired VendaService service;
    @Autowired UsuarioRepository usuarios;
    @Autowired PedidoRepository pedidos;

    @BeforeEach
    void dados() {
        Usuario ana = usuarios.save(new Usuario(null, "Ana", "ana@x.com", "hash", "ROLE_USER"));
        pedidos.save(PedidoTestFactory.pedido(ana, LocalDateTime.of(2026, 11, 5, 12, 0), "10.00"));
        pedidos.save(PedidoTestFactory.pedido(ana, LocalDateTime.of(2026, 11, 10, 0, 0), "100.00"));      // 1º instante do dia 10
        pedidos.save(PedidoTestFactory.pedido(ana, LocalDateTime.of(2026, 11, 10, 23, 59, 59), "50.50")); // último instante do dia 10
        pedidos.save(PedidoTestFactory.pedido(ana, LocalDateTime.of(2026, 11, 11, 0, 0), "20.00"));       // já é o dia 11
    }

    @Test
    void semFiltroTraTodasAsVendasEOResumoDoTotal() {
        VendasResponse r = service.listar(null, null, 0, 10);

        assertThat(r.resumo().quantidadeVendas()).isEqualTo(4);
        assertThat(r.resumo().valorTotal()).isEqualByComparingTo("180.50");
        assertThat(r.vendas()).hasSize(4);
    }

    @Test
    void periodoDeUmDiaEInclusivoNasDuasPontas() {
        LocalDate dia = LocalDate.of(2026, 11, 10);

        VendasResponse r = service.listar(dia, dia, 0, 10);

        assertThat(r.resumo().quantidadeVendas()).isEqualTo(2);
        assertThat(r.resumo().valorTotal()).isEqualByComparingTo("150.50");
        assertThat(r.vendas()).extracting(v -> v.total().toPlainString()).containsExactly("50.50", "100.00");
    }

    @Test
    void apenasDataInicialOuApenasDataFinal() {
        assertThat(service.listar(LocalDate.of(2026, 11, 10), null, 0, 10).resumo().quantidadeVendas()).isEqualTo(3);
        assertThat(service.listar(null, LocalDate.of(2026, 11, 5), 0, 10).resumo().quantidadeVendas()).isEqualTo(1);
    }

    @Test
    void periodoSemVendasRetornaZero() {
        VendasResponse r = service.listar(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 31), 0, 10);

        assertThat(r.resumo().quantidadeVendas()).isZero();
        assertThat(r.resumo().valorTotal()).isEqualByComparingTo("0");
        assertThat(r.vendas()).isEmpty();
    }

    @Test
    void paginaOsResultadosMantendoOResumoDoPeriodoTodo() {
        VendasResponse p0 = service.listar(null, null, 0, 3);
        VendasResponse p1 = service.listar(null, null, 1, 3);

        assertThat(p0.vendas()).hasSize(3);
        assertThat(p1.vendas()).hasSize(1);
        assertThat(p0.totalPages()).isEqualTo(2);
        assertThat(p0.totalElements()).isEqualTo(4);
        assertThat(p0.resumo().valorTotal()).isEqualByComparingTo("180.50"); // não é só da página
        assertThat(p1.resumo().valorTotal()).isEqualByComparingTo("180.50");
        assertThat(p0.vendas().get(0).total()).isEqualByComparingTo("20.00"); // mais recente primeiro
    }

    @Test
    void rejeitaPeriodoInvertidoEPaginacaoInvalida() {
        LocalDate a = LocalDate.of(2026, 11, 10);
        LocalDate b = LocalDate.of(2026, 11, 1);

        assertThatThrownBy(() -> service.listar(a, b, 0, 10)).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.listar(null, null, -1, 10)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.listar(null, null, 0, 0)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.listar(null, null, 0, 101)).isInstanceOf(ResponseStatusException.class);
    }
}
