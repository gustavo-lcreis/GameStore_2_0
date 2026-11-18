package br.com.gamestore.pi_3.repository;

import br.com.gamestore.pi_3.model.Jogo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JogoRepository extends JpaRepository<Jogo, Long> {

    // Usado pelo seed para não duplicar jogos a cada restart
    boolean existsByTitulo(String titulo);

    // Filtros do catálogo (?busca= e ?categoria=)
    List<Jogo> findByTituloContainingIgnoreCase(String busca);
    List<Jogo> findByCategoriaId(Long categoriaId);
    List<Jogo> findByTituloContainingIgnoreCaseAndCategoriaId(String busca, Long categoriaId);
}
