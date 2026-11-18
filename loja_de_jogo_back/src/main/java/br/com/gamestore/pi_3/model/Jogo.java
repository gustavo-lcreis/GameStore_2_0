package br.com.gamestore.pi_3.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Os nomes dos campos PRECISAM ser iguais aos da interface Jogo do Angular
 * (src/app/models.ts): titulo, descricao, preco, imagem, plataforma, categoria.
 * Antes eram nome/imagemUrl -> o front recebia "titulo" = undefined.
 */
@Entity
@Table(name = "jogos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Jogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(length = 1000)
    private String descricao;

    private Double preco;

    /** Caminho/URL da capa. Ex.: /assets/jogos/neon-drift.svg */
    private String imagem;

    private String plataforma;

    // EAGER: a categoria já vem carregada e é serializada como { id, nome }
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;
}
