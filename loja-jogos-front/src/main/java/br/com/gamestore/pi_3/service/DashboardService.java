package br.com.gamestore.pi_3.service;

import br.com.gamestore.pi_3.dto.DashboardResponse;
import br.com.gamestore.pi_3.dto.DashboardResponse.JogoMaisVendido;
import br.com.gamestore.pi_3.dto.DashboardResponse.Kpis;
import br.com.gamestore.pi_3.dto.DashboardResponse.PontoEvolucao;
import br.com.gamestore.pi_3.dto.DashboardResponse.PorStatus;
import br.com.gamestore.pi_3.model.StatusPedido;
import br.com.gamestore.pi_3.repository.ItemPedidoRepository;
import br.com.gamestore.pi_3.repository.PedidoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    public enum Agrupamento { DIA, MES }

    static final int DIAS_PADRAO = 30;
    static final int MAX_DIAS_POR_DIA = 366;
    static final int MAX_MESES = 120;
    static final int TOP_MAXIMO = 20;

    private final PedidoRepository pedidos;
    private final ItemPedidoRepository itens;

    public DashboardService(PedidoRepository pedidos, ItemPedidoRepository itens) {
        this.pedidos = pedidos;
        this.itens = itens;
    }

    /** Sem datas: últimos 30 dias (incluindo hoje). Datas inclusivas. 4 queries agregadas, todas no banco. */
    @Transactional(readOnly = true)
    public DashboardResponse consolidar(LocalDate inicio, LocalDate fim, Agrupamento agrupamento, int top) {
        LocalDate f = fim != null ? fim : LocalDate.now();
        LocalDate i = inicio != null ? inicio : f.minusDays(DIAS_PADRAO - 1L);
        Agrupamento ag = agrupamento != null ? agrupamento : Agrupamento.DIA;
        validar(i, f, ag, top);

        LocalDateTime de = i.atStartOfDay();
        LocalDateTime ate = f.plusDays(1).atStartOfDay(); // fim inclusivo; intervalo semiaberto [de, ate)

        PedidoRepository.TotaisProjecao t = pedidos.totais(StatusPedido.CONCLUIDA, de, ate);
        long qtdPedidos = nz(t.getPedidos());
        BigDecimal faturamento = nz(t.getFaturamento()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal ticket = qtdPedidos == 0
                ? BigDecimal.ZERO.setScale(2)
                : faturamento.divide(BigDecimal.valueOf(qtdPedidos), 2, RoundingMode.HALF_UP);
        Kpis kpis = new Kpis(pedidos.count(), qtdPedidos, faturamento, ticket, nz(t.getItens()));

        return new DashboardResponse(i, f, ag.name(), kpis,
                porStatus(de, ate), topJogos(de, ate, top), evolucao(i, f, ag, de, ate));
    }

    /** Todos os status do enum aparecem (zerados quando não há pedidos): novos status entram sem mudar a tela. */
    private List<PorStatus> porStatus(LocalDateTime de, LocalDateTime ate) {
        Map<StatusPedido, PedidoRepository.StatusProjecao> mapa = new EnumMap<>(StatusPedido.class);
        for (PedidoRepository.StatusProjecao s : pedidos.porStatus(de, ate)) mapa.put(s.getStatus(), s);

        List<PorStatus> lista = new ArrayList<>();
        for (StatusPedido st : StatusPedido.values()) {
            PedidoRepository.StatusProjecao s = mapa.get(st);
            lista.add(new PorStatus(st.name(), s == null ? 0 : nz(s.getPedidos()),
                    s == null ? BigDecimal.ZERO.setScale(2) : nz(s.getFaturamento()).setScale(2, RoundingMode.HALF_UP)));
        }
        return lista;
    }

    private List<JogoMaisVendido> topJogos(LocalDateTime de, LocalDateTime ate, int top) {
        return itens.ranking(StatusPedido.CONCLUIDA, de, ate, PageRequest.of(0, top)).stream()
                .map(r -> new JogoMaisVendido(r.getJogoId(), r.getTitulo(), nz(r.getQuantidade()),
                        nz(r.getReceita()).setScale(2, RoundingMode.HALF_UP)))
                .toList();
    }

    /** O banco devolve no máx. 1 linha por dia com venda; aqui agrupa por mês (se pedido) e preenche períodos vazios. */
    private List<PontoEvolucao> evolucao(LocalDate i, LocalDate f, Agrupamento ag, LocalDateTime de, LocalDateTime ate) {
        Map<String, long[]> pedidosPorChave = new HashMap<>();
        Map<String, BigDecimal> valorPorChave = new HashMap<>();
        for (PedidoRepository.DiaProjecao d : pedidos.vendasPorDia(StatusPedido.CONCLUIDA, de, ate)) {
            LocalDate data = LocalDate.of(d.getAno(), d.getMes(), d.getDia());
            String chave = ag == Agrupamento.MES ? YearMonth.from(data).toString() : data.toString();
            pedidosPorChave.computeIfAbsent(chave, k -> new long[1])[0] += nz(d.getPedidos());
            valorPorChave.merge(chave, nz(d.getFaturamento()), BigDecimal::add);
        }

        List<PontoEvolucao> serie = new ArrayList<>();
        if (ag == Agrupamento.MES) {
            YearMonth ultimo = YearMonth.from(f);
            for (YearMonth m = YearMonth.from(i); !m.isAfter(ultimo); m = m.plusMonths(1)) {
                serie.add(ponto(m.toString(), pedidosPorChave, valorPorChave));
            }
        } else {
            for (LocalDate d = i; !d.isAfter(f); d = d.plusDays(1)) {
                serie.add(ponto(d.toString(), pedidosPorChave, valorPorChave));
            }
        }
        return serie;
    }

    private static PontoEvolucao ponto(String chave, Map<String, long[]> qtd, Map<String, BigDecimal> valor) {
        long[] q = qtd.get(chave);
        return new PontoEvolucao(chave, q == null ? 0 : q[0],
                valor.getOrDefault(chave, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
    }

    private static void validar(LocalDate i, LocalDate f, Agrupamento ag, int top) {
        if (f.isBefore(i)) throw erro("A data final não pode ser anterior à data inicial.");
        if (top < 1 || top > TOP_MAXIMO) throw erro("O ranking deve ter entre 1 e " + TOP_MAXIMO + " jogos.");
        if (ag == Agrupamento.DIA && ChronoUnit.DAYS.between(i, f) + 1 > MAX_DIAS_POR_DIA) {
            throw erro("Para períodos acima de " + MAX_DIAS_POR_DIA + " dias, agrupe por mês.");
        }
        if (ag == Agrupamento.MES && ChronoUnit.MONTHS.between(YearMonth.from(i), YearMonth.from(f)) + 1 > MAX_MESES) {
            throw erro("O período máximo agrupado por mês é de " + MAX_MESES + " meses.");
        }
    }

    private static long nz(Long v) { return v == null ? 0L : v; }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private static ResponseStatusException erro(String mensagem) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
    }
}
