package br.com.gamestore.pi_3.repository;

import br.com.gamestore.pi_3.model.ItemPedido;
import br.com.gamestore.pi_3.model.StatusPedido;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ItemPedidoRepository extends JpaRepository<ItemPedido, Long> {

    interface RankingProjecao { Long getJogoId(); String getTitulo(); Long getQuantidade(); BigDecimal getReceita(); }

    /**
     * Ranking de jogos mais vendidos no período. Agrupa por jogoId (não por título: se o jogo for renomeado
     * continua sendo um só); max(titulo) devolve um título qualquer do grupo. O Pageable só aplica o LIMIT (top N).
     */
    @Query("select i.jogoId as jogoId, max(i.titulo) as titulo, sum(i.quantidade) as quantidade, sum(i.subtotal) as receita "
            + "from ItemPedido i join i.pedido p "
            + "where p.status = :status and p.dataHora >= :de and p.dataHora < :ate "
            + "group by i.jogoId "
            + "order by sum(i.quantidade) desc, sum(i.subtotal) desc")
    List<RankingProjecao> ranking(@Param("status") StatusPedido status,
                                  @Param("de") LocalDateTime de, @Param("ate") LocalDateTime ate,
                                  Pageable pageable);
}
