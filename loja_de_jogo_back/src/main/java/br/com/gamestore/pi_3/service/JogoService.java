// loja_de_jogo_back/src/main/java/br/com/gamestore/pi_3/service/JogoService.java
package br.com.gamestore.pi_3.service;

import br.com.gamestore.pi_3.dto.JogoRequest;
import br.com.gamestore.pi_3.model.Jogo;
import br.com.gamestore.pi_3.repository.CategoriaRepository;
import br.com.gamestore.pi_3.repository.JogoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class JogoService {

    private final JogoRepository jogoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ImagemStorageService imagens;

    public JogoService(JogoRepository jogoRepository,
                       CategoriaRepository categoriaRepository,
                       ImagemStorageService imagens) {
        this.jogoRepository = jogoRepository;
        this.categoriaRepository = categoriaRepository;
        this.imagens = imagens;
    }

    public Jogo criar(JogoRequest req, MultipartFile imagem) {
        Jogo jogo = aplicar(new Jogo(), req);
        String novoCaminho = null;
        try {
            if (imagens.foiEnviado(imagem)) {
                novoCaminho = imagens.salvar(imagem);
                jogo.setImagem(novoCaminho);
            }
            Jogo salvo = jogoRepository.save(jogo);
            log.info("Jogo criado: id={} titulo='{}' imagem={}", salvo.getId(), salvo.getTitulo(), salvo.getImagem());
            return salvo;
        } catch (RuntimeException e) {
            imagens.remover(novoCaminho);   // não deixa arquivo órfão se o banco recusar
            throw e;
        }
    }

    public Jogo editar(Long id, JogoRequest req, MultipartFile imagem) {
        Jogo jogo = buscar(id);
        String caminhoAntigo = jogo.getImagem();
        aplicar(jogo, req);
        String novoCaminho = null;
        try {
            if (imagens.foiEnviado(imagem)) {
                novoCaminho = imagens.salvar(imagem);
                jogo.setImagem(novoCaminho);
            }
            Jogo salvo = jogoRepository.save(jogo);
            if (novoCaminho != null) imagens.remover(caminhoAntigo);   // só depois de gravar com sucesso
            log.info("Jogo atualizado: id={} imagem={}", salvo.getId(), salvo.getImagem());
            return salvo;
        } catch (RuntimeException e) {
            imagens.remover(novoCaminho);
            throw e;
        }
    }

    public void excluir(Long id) {
        Jogo jogo = buscar(id);
        jogoRepository.delete(jogo);
        imagens.remover(jogo.getImagem());
        log.info("Jogo excluído: id={}", id);
    }

    private Jogo buscar(Long id) {
        return jogoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jogo não encontrado"));
    }

    /** Copia os campos de texto do DTO e resolve categoriaId -> Categoria. A imagem é tratada à parte. */
    private Jogo aplicar(Jogo jogo, JogoRequest req) {
        jogo.setTitulo(req.titulo());
        jogo.setDescricao(req.descricao());
        jogo.setPreco(req.preco());
        jogo.setPlataforma(req.plataforma());
        if (req.categoriaId() != null) {
            jogo.setCategoria(categoriaRepository.findById(req.categoriaId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categoria inexistente")));
        } else {
            jogo.setCategoria(null);
        }
        return jogo;
    }
}