package br.com.gamestore.pi_3.service;

import br.com.gamestore.pi_3.dto.ItemPedidoRequest;
import br.com.gamestore.pi_3.dto.PedidoRequest;
import br.com.gamestore.pi_3.dto.PedidoResponse;
import br.com.gamestore.pi_3.model.ItemPedido;
import br.com.gamestore.pi_3.model.Jogo;
import br.com.gamestore.pi_3.model.Pedido;
import br.com.gamestore.pi_3.model.StatusPedido;
import br.com.gamestore.pi_3.model.Usuario;
import br.com.gamestore.pi_3.repository.JogoRepository;
import br.com.gamestore.pi_3.repository.PedidoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PedidoService {

    static final int QUANTIDADE_MAXIMA_POR_JOGO = 99;

    private final PedidoRepository pedidoRepository;
    private final JogoRepository jogoRepository;

    public PedidoService(PedidoRepository pedidoRepository, JogoRepository jogoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.jogoRepository = jogoRepository;
    }

    /**
     * Finaliza a compra simulada: nada é cobrado, o pedido já nasce CONCLUIDA.
     * Preços, subtotais, quantidade e total vêm do banco/servidor; o cliente só informa jogoId e quantidade.
     */
    @Transactional
    public Pedido finalizar(Usuario cliente, PedidoRequest req) {
        return finalizar(cliente, req, null);
    }

    /**
     * Mesma compra, com chave de idempotência opcional (gravada no pedido; a constraint única
     * (usuario, chave) impede dois pedidos com a mesma chave mesmo em requests simultâneos).
     * TUDO acontece em UMA transação: se qualquer passo falhar, nenhum pedido/item fica gravado.
     */
    @Transactional
    public Pedido finalizar(Usuario cliente, PedidoRequest req, String chaveIdempotencia) {
        if (req == null || req.itens() == null || req.itens().isEmpty()) {
            throw erro("O carrinho está vazio.");
        }

        Map<Long, Integer> quantidades = consolidar(req.itens());

        Map<Long, Jogo> jogos = jogoRepository.findAllById(quantidades.keySet()).stream()
                .collect(Collectors.toMap(Jogo::getId, Function.identity()));

        List<Long> inexistentes = quantidades.keySet().stream().filter(id -> !jogos.containsKey(id)).toList();
        if (!inexistentes.isEmpty()) {
            throw erro("Jogo(s) inexistente(s): " + inexistentes);
        }

        List<String> indisponiveis = jogos.values().stream()
                .filter(j -> !disponivel(j)).map(Jogo::getTitulo).toList();
        if (!indisponiveis.isEmpty()) {
            throw erro("Jogo(s) indisponível(is) para compra: " + String.join(", ", indisponiveis));
        }

        Pedido pedido = new Pedido();
        pedido.setUsuario(cliente);
        pedido.setClienteNome(cliente.getNome() != null ? cliente.getNome() : cliente.getUsername());
        pedido.setClienteEmail(cliente.getUsername());
        pedido.setDataHora(LocalDateTime.now());
        pedido.setStatus(StatusPedido.CONCLUIDA);
        pedido.setChaveIdempotencia(chaveIdempotencia);

        BigDecimal total = BigDecimal.ZERO;
        int quantidadeTotal = 0;
        for (Map.Entry<Long, Integer> e : quantidades.entrySet()) {
            Jogo jogo = jogos.get(e.getKey());
            int quantidade = e.getValue();
            // BigDecimal.valueOf(double) usa a representação decimal ("19.99"), sem o ruído binário do double
            BigDecimal preco = BigDecimal.valueOf(jogo.getPreco()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal subtotal = preco.multiply(BigDecimal.valueOf(quantidade));

            ItemPedido item = new ItemPedido();
            item.setJogoId(jogo.getId());
            item.setTitulo(jogo.getTitulo());
            item.setPrecoUnitario(preco);
            item.setQuantidade(quantidade);
            item.setSubtotal(subtotal);
            pedido.adicionarItem(item);

            total = total.add(subtotal);
            quantidadeTotal += quantidade;
        }
        pedido.setQuantidadeTotal(quantidadeTotal);
        pedido.setTotal(total);

        Pedido salvo = pedidoRepository.save(pedido);
        log.info("Pedido {} concluído (simulado): usuario={} itens={} total={}",
                salvo.getId(), cliente.getUsername(), quantidadeTotal, total);
        return salvo;
    }

    /** "Meus pedidos": só os pedidos do usuário autenticado, mapeados DENTRO da transação (itens já carregados). */
    @Transactional(readOnly = true)
    public List<PedidoResponse> meusPedidos(Usuario cliente) {
        return pedidoRepository.findByUsuarioIdOrderByDataHoraDescIdDesc(cliente.getId()).stream()
                .map(PedidoResponse::de)
                .toList();
    }

    /** Pedido que este usuário já criou com esta chave (retry idempotente). */
    @Transactional(readOnly = true)
    public Optional<PedidoResponse> buscarPorChave(Usuario cliente, String chave) {
        return pedidoRepository.findByUsuarioIdAndChaveIdempotencia(cliente.getId(), chave)
                .map(PedidoResponse::de);
    }

    /** Une linhas repetidas do mesmo jogo somando as quantidades, preservando a ordem do carrinho. */
    private Map<Long, Integer> consolidar(List<ItemPedidoRequest> itens) {
        Map<Long, Integer> mapa = new LinkedHashMap<>();
        for (ItemPedidoRequest item : itens) {
            if (item == null || item.jogoId() == null) {
                throw erro("Item do carrinho sem jogo informado.");
            }
            if (item.quantidade() == null || item.quantidade() < 1) {
                throw erro("A quantidade de cada item deve ser no mínimo 1.");
            }
            mapa.merge(item.jogoId(), item.quantidade(), Integer::sum);
        }
        for (int quantidade : mapa.values()) {
            if (quantidade > QUANTIDADE_MAXIMA_POR_JOGO) {
                throw erro("A quantidade máxima por jogo é " + QUANTIDADE_MAXIMA_POR_JOGO + ".");
            }
        }
        return mapa;
    }

    /** O modelo não tem estoque nem flag "ativo": disponível = tem preço válido. Ponto único para evoluir a regra. */
    private static boolean disponivel(Jogo jogo) {
        return jogo.getPreco() != null && jogo.getPreco() > 0;
    }

    private static ResponseStatusException erro(String mensagem) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
    }
}
