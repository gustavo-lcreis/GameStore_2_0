package br.com.gamestore.pi_3.service;

import br.com.gamestore.pi_3.dto.PedidoResponse;
import br.com.gamestore.pi_3.dto.ResumoVendas;
import br.com.gamestore.pi_3.dto.VendasResponse;
import br.com.gamestore.pi_3.model.Pedido;
import br.com.gamestore.pi_3.model.StatusPedido;
import br.com.gamestore.pi_3.repository.PedidoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class VendaService {

    static final int TAMANHO_MAXIMO_PAGINA = 100;

    // Limites usados quando o admin não informa uma das datas (evita parâmetros nulos nas queries).
    private static final LocalDate DATA_MINIMA = LocalDate.of(1900, 1, 1);
    private static final LocalDate DATA_MAXIMA = LocalDate.of(2999, 12, 31);

    private final PedidoRepository pedidoRepository;

    public VendaService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    /** Vendas CONCLUIDAS no período (datas inclusivas), mais novas primeiro, com o resumo do período todo. */
    @Transactional(readOnly = true)
    public VendasResponse listar(LocalDate inicio, LocalDate fim, int page, int size) {
        if (inicio != null && fim != null && fim.isBefore(inicio)) {
            throw erro("A data final não pode ser anterior à data inicial.");
        }
        if (page < 0) throw erro("A página deve ser maior ou igual a zero.");
        if (size < 1 || size > TAMANHO_MAXIMO_PAGINA) {
            throw erro("O tamanho da página deve estar entre 1 e " + TAMANHO_MAXIMO_PAGINA + ".");
        }

        LocalDateTime de = (inicio != null ? inicio : DATA_MINIMA).atStartOfDay();
        LocalDateTime ate = (fim != null ? fim : DATA_MAXIMA).plusDays(1).atStartOfDay(); // fim inclusivo

        Sort ordem = Sort.by(Sort.Direction.DESC, "dataHora").and(Sort.by(Sort.Direction.DESC, "id"));
        Page<Pedido> pagina = pedidoRepository.findByStatusAndDataHoraGreaterThanEqualAndDataHoraLessThan(
                StatusPedido.CONCLUIDA, de, ate, PageRequest.of(page, size, ordem));

        BigDecimal soma = pedidoRepository.somarTotal(StatusPedido.CONCLUIDA, de, ate);
        BigDecimal valorTotal = (soma != null ? soma : BigDecimal.ZERO).setScale(2);

        return new VendasResponse(
                new ResumoVendas(pagina.getTotalElements(), valorTotal),
                pagina.getContent().stream().map(PedidoResponse::de).toList(),
                pagina.getNumber(), pagina.getSize(), pagina.getTotalElements(), pagina.getTotalPages());
    }

    private static ResponseStatusException erro(String mensagem) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
    }
}
