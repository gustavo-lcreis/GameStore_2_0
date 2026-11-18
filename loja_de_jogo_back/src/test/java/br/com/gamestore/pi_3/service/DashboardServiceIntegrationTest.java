package br.com.gamestore.pi_3.service;

import br.com.gamestore.pi_3.dto.DashboardResponse;
import br.com.gamestore.pi_3.model.Usuario;
import br.com.gamestore.pi_3.repository.PedidoRepository;
import br.com.gamestore.pi_3.repository.UsuarioRepository;
import br.com.gamestore.pi_3.service.DashboardService.Agrupamento;
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

/** Roda as agregações JPQL contra um H2 real. Mesmos 4 pedidos do VendaServiceIntegrationTest (R$ 180,50 no total). */
@DataJpaTest
@Import(DashboardService.class)
class DashboardServiceIntegrationTest {

    @Autowired DashboardService service;
    @Autowired UsuarioRepository usuarios;
    @Autowired PedidoRepository pedidos;

    @BeforeEach
    void dados() {
        Usuario ana = usuarios.save(new Usuario(null, "Ana", "ana@x.com", "hash", "ROLE_USER"));
        pedidos.save(PedidoTestFactory.pedido(ana, LocalDateTime.of(2026, 11, 5, 12, 0), "10.00"));
        pedidos.save(PedidoTestFactory.pedido(ana, LocalDateTime.of(2026, 11, 10, 0, 0), "100.00"));
        pedidos.save(PedidoTestFactory.pedido(ana, LocalDateTime.of(2026, 11, 10, 23, 59, 59), "50.50"));
        pedidos.save(PedidoTestFactory.pedido(ana, LocalDateTime.of(2026, 11, 11, 0, 0), "20.00"));
    }

    @Test
    void kpisDoPeriodo() {
        DashboardResponse r = service.consolidar(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 11), Agrupamento.DIA, 5);

        assertThat(r.kpis().totalPedidos()).isEqualTo(4);
        assertThat(r.kpis().totalPedidosGeral()).isEqualTo(4);
        assertThat(r.kpis().faturamento()).isEqualByComparingTo("180.50");
        assertThat(r.kpis().ticketMedio()).isEqualByComparingTo("45.13"); // 180,50 / 4 = 45,125 -> HALF_UP
        assertThat(r.kpis().jogosVendidos()).isEqualTo(4);
    }

    @Test
    void periodoMenorQueOHistoricoSeparaTotalGeralDoTotalDoPeriodo() {
        DashboardResponse r = service.consolidar(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 10), Agrupamento.DIA, 5);

        assertThat(r.kpis().totalPedidos()).isEqualTo(2);
        assertThat(r.kpis().totalPedidosGeral()).isEqualTo(4);
        assertThat(r.kpis().faturamento()).isEqualByComparingTo("150.50");
    }

    @Test
    void evolucaoPorDiaPreencheDiasSemVenda() {
        DashboardResponse r = service.consolidar(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 11), Agrupamento.DIA, 5);

        assertThat(r.evolucao()).hasSize(7);
        assertThat(r.evolucao().get(0).periodo()).isEqualTo("2026-11-05");
        assertThat(r.evolucao().get(0).faturamento()).isEqualByComparingTo("10.00");
        assertThat(r.evolucao().get(1).periodo()).isEqualTo("2026-11-06");
        assertThat(r.evolucao().get(1).pedidos()).isZero();
        assertThat(r.evolucao().get(5).periodo()).isEqualTo("2026-11-10");
        assertThat(r.evolucao().get(5).pedidos()).isEqualTo(2);
        assertThat(r.evolucao().get(5).faturamento()).isEqualByComparingTo("150.50");
        assertThat(r.evolucao().get(6).faturamento()).isEqualByComparingTo("20.00");
    }

    @Test
    void evolucaoPorMesAgrupaEPreencheMesesVazios() {
        DashboardResponse r = service.consolidar(LocalDate.of(2026, 10, 15), LocalDate.of(2026, 12, 2), Agrupamento.MES, 5);

        assertThat(r.evolucao()).extracting(p -> p.periodo()).containsExactly("2026-10", "2026-11", "2026-12");
        assertThat(r.evolucao().get(0).pedidos()).isZero();
        assertThat(r.evolucao().get(1).pedidos()).isEqualTo(4);
        assertThat(r.evolucao().get(1).faturamento()).isEqualByComparingTo("180.50");
        assertThat(r.evolucao().get(2).faturamento()).isEqualByComparingTo("0");
    }

    @Test
    void rankingDeJogosEPedidosPorStatus() {
        DashboardResponse r = service.consolidar(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), Agrupamento.DIA, 5);

        assertThat(r.topJogos()).hasSize(1);
        assertThat(r.topJogos().get(0).titulo()).isEqualTo("Neon Drift");
        assertThat(r.topJogos().get(0).quantidade()).isEqualTo(4);
        assertThat(r.topJogos().get(0).receita()).isEqualByComparingTo("180.50");

        assertThat(r.porStatus()).hasSize(1);
        assertThat(r.porStatus().get(0).status()).isEqualTo("CONCLUIDA");
        assertThat(r.porStatus().get(0).pedidos()).isEqualTo(4);
    }

    @Test
    void periodoSemVendasRetornaZerosSemQuebrar() {
        DashboardResponse r = service.consolidar(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 3), Agrupamento.DIA, 5);

        assertThat(r.kpis().totalPedidos()).isZero();
        assertThat(r.kpis().faturamento()).isEqualByComparingTo("0");
        assertThat(r.kpis().ticketMedio()).isEqualByComparingTo("0"); // sem divisão por zero
        assertThat(r.topJogos()).isEmpty();
        assertThat(r.evolucao()).hasSize(3);
    }

    @Test
    void rejeitaParametrosInvalidos() {
        LocalDate a = LocalDate.of(2026, 11, 10);
        LocalDate b = LocalDate.of(2026, 11, 1);

        assertThatThrownBy(() -> service.consolidar(a, b, Agrupamento.DIA, 5)).isInstanceOfSatisfying(
                ResponseStatusException.class, e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.consolidar(b, a, Agrupamento.DIA, 0)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.consolidar(b, a, Agrupamento.DIA, 21)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.consolidar(LocalDate.of(2024, 1, 1), LocalDate.of(2026, 1, 1), Agrupamento.DIA, 5))
                .isInstanceOf(ResponseStatusException.class);
    }
}
