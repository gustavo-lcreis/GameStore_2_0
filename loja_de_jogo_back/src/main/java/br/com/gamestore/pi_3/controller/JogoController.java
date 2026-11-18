// loja_de_jogo_back/src/main/java/br/com/gamestore/pi_3/controller/JogoController.java
package br.com.gamestore.pi_3.controller;

import br.com.gamestore.pi_3.dto.JogoRequest;
import br.com.gamestore.pi_3.model.Categoria;
import br.com.gamestore.pi_3.model.Jogo;
import br.com.gamestore.pi_3.repository.CategoriaRepository;
import br.com.gamestore.pi_3.repository.JogoRepository;
import br.com.gamestore.pi_3.service.JogoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// CORS é global (WebConfig), então não precisa de @CrossOrigin aqui.
@RestController
@RequestMapping("/api")
public class JogoController {

    private final JogoRepository jogoRepository;
    private final CategoriaRepository categoriaRepository;
    private final JogoService jogoService;

    public JogoController(JogoRepository jogoRepository,
                          CategoriaRepository categoriaRepository,
                          JogoService jogoService) {
        this.jogoRepository = jogoRepository;
        this.categoriaRepository = categoriaRepository;
        this.jogoService = jogoService;
    }

    // Público: lista (ARRAY simples). Filtros opcionais: /api/jogos?busca=neon&categoria=2
    @GetMapping("/jogos")
    public List<Jogo> listar(@RequestParam(required = false) String busca,
                             @RequestParam(required = false) Long categoria) {
        boolean temBusca = busca != null && !busca.isBlank();
        if (temBusca && categoria != null) {
            return jogoRepository.findByTituloContainingIgnoreCaseAndCategoriaId(busca.trim(), categoria);
        }
        if (temBusca) return jogoRepository.findByTituloContainingIgnoreCase(busca.trim());
        if (categoria != null) return jogoRepository.findByCategoriaId(categoria);
        return jogoRepository.findAll();
    }

    // Público: detalhe
    @GetMapping("/jogos/{id}")
    public ResponseEntity<Jogo> buscar(@PathVariable Long id) {
        return jogoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Público: categorias como [{ id, nome }]
    @GetMapping("/categorias")
    public List<Categoria> listarCategorias() {
        return categoriaRepository.findAll(Sort.by("nome"));
    }

    // Admin: criar. multipart/form-data = campos do jogo + arquivo "imagem" (opcional)
    @PostMapping(value = "/admin/jogos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Jogo criar(@Valid @ModelAttribute JogoRequest req,
                      @RequestPart(value = "imagem", required = false) MultipartFile imagem) {
        return jogoService.criar(req, imagem);
    }

    // Admin: editar. Sem arquivo novo, a imagem atual é mantida.
    @PutMapping(value = "/admin/jogos/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Jogo editar(@PathVariable Long id,
                       @Valid @ModelAttribute JogoRequest req,
                       @RequestPart(value = "imagem", required = false) MultipartFile imagem) {
        return jogoService.editar(id, req, imagem);
    }

    // Admin: excluir (apaga também o arquivo .jpg do jogo, se houver)
    @DeleteMapping("/admin/jogos/{id}")
    public void excluir(@PathVariable Long id) {
        jogoService.excluir(id);
    }
}