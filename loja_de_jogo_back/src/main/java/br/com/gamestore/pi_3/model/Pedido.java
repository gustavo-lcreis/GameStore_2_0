package br.com.gamestore.pi_3.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Pedido (compra simulada). Não usa @Data de propósito: a relação bidirecional com os itens
 * geraria recursão em toString/hashCode.
 *
 * O vínculo com o usuário é OPCIONAL: ao excluir um usuário o pedido é mantido com usuario = null.
 * Por isso nome e e-mail do cliente são gravados no pedido (snapshot) e o histórico de vendas continua legível.
 */
@Entity
@Table(name = "pedidos", uniqueConstraints = @UniqueConstraint(
        name = "uk_pedido_usuario_chave", columnNames = {"usuario_id", "chave_idempotencia"}))
@Getter
@Setter
@NoArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false)
    private String clienteNome;

    @Column(nullable = false)
    private String clienteEmail;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPedido status;

    @Column(nullable = false)
    private Integer quantidadeTotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    /**
     * Chave enviada pelo cliente (header Idempotency-Key) para que repetir o POST /api/pedidos
     * (duplo clique, retry após timeout) devolva o MESMO pedido em vez de criar outro.
     * NULL = pedido sem chave (vários NULL são permitidos pela constraint única).
     */
    @Column(name = "chave_idempotencia", length = 64)
    private String chaveIdempotencia;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<ItemPedido> itens = new ArrayList<>();

    public void adicionarItem(ItemPedido item) {
        item.setPedido(this);
        itens.add(item);
    }
}
