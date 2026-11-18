package br.com.gamestore.pi_3.repository;

import br.com.gamestore.pi_3.model.Pedido;
import br.com.gamestore.pi_3.model.StatusPedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    /** Intervalo semiaberto: dataHora >= de E dataHora < ate. */
    Page<Pedido> findByStatusAndDataHoraGreaterThanEqualAndDataHoraLessThan(
            StatusPedido status, LocalDateTime de, LocalDateTime ate, Pageable pageable);

    /** Soma dos totais no período. Retorna null quando não há pedidos (o service converte para zero). */
    @Query("select sum(p.total) from Pedido p "
            + "where p.status = :status and p.dataHora >= :de and p.dataHora < :ate")
    BigDecimal somarTotal(@Param("status") StatusPedido status,
                          @Param("de") LocalDateTime de,
                          @Param("ate") LocalDateTime ate);

    /** Mantém os pedidos do usuário, só removendo a referência a ele. Devolve quantos foram preservados. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Pedido p set p.usuario = null where p.usuario.id = :usuarioId")
    int desvincularUsuario(@Param("usuarioId") Long usuarioId);

    // ---------- Área do cliente ("Meus pedidos") ----------

    /** Pedidos do cliente, mais novos primeiro. O EntityGraph traz os itens no mesmo SELECT (sem N+1). */
    @EntityGraph(attributePaths = "itens")
    List<Pedido> findByUsuarioIdOrderByDataHoraDescIdDesc(Long usuarioId);

    /** Idempotência: pedido já criado por este usuário com esta chave. */
    @EntityGraph(attributePaths = "itens")
    Optional<Pedido> findByUsuarioIdAndChaveIdempotencia(Long usuarioId, String chaveIdempotencia);

    // ---------- Dashboard (agregações feitas no banco; nada é carregado em memória) ----------

    interface TotaisProjecao { Long getPedidos(); BigDecimal getFaturamento(); Long getItens(); }

    interface StatusProjecao { StatusPedido getStatus(); Long getPedidos(); BigDecimal getFaturamento(); }

    interface DiaProjecao { Integer getAno(); Integer getMes(); Integer getDia(); Long getPedidos(); BigDecimal getFaturamento(); }

    @Query("select count(p) as pedidos, sum(p.total) as faturamento, sum(p.quantidadeTotal) as itens "
            + "from Pedido p where p.status = :status and p.dataHora >= :de and p.dataHora < :ate")
    TotaisProjecao totais(@Param("status") StatusPedido status,
                          @Param("de") LocalDateTime de, @Param("ate") LocalDateTime ate);

    @Query("select p.status as status, count(p) as pedidos, sum(p.total) as faturamento "
            + "from Pedido p where p.dataHora >= :de and p.dataHora < :ate group by p.status")
    List<StatusProjecao> porStatus(@Param("de") LocalDateTime de, @Param("ate") LocalDateTime ate);

    /** Uma linha por dia COM vendas (no máx. 1 por dia do período). O service preenche os dias vazios e agrupa por mês. */
    @Query("select year(p.dataHora) as ano, month(p.dataHora) as mes, day(p.dataHora) as dia, "
            + "count(p) as pedidos, sum(p.total) as faturamento "
            + "from Pedido p where p.status = :status and p.dataHora >= :de and p.dataHora < :ate "
            + "group by year(p.dataHora), month(p.dataHora), day(p.dataHora) "
            + "order by year(p.dataHora), month(p.dataHora), day(p.dataHora)")
    List<DiaProjecao> vendasPorDia(@Param("status") StatusPedido status,
                                   @Param("de") LocalDateTime de, @Param("ate") LocalDateTime ate);
}
