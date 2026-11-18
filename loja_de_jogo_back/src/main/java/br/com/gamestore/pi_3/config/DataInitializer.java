package br.com.gamestore.pi_3.config;

import br.com.gamestore.pi_3.model.Categoria;
import br.com.gamestore.pi_3.model.Jogo;
import br.com.gamestore.pi_3.model.Usuario;
import br.com.gamestore.pi_3.repository.CategoriaRepository;
import br.com.gamestore.pi_3.repository.JogoRepository;
import br.com.gamestore.pi_3.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Seed (dados iniciais). Roda uma vez a cada start da aplicação.
 * É IDEMPOTENTE: antes de inserir, verifica se o registro já existe,
 * então reiniciar (ou trocar H2 em memória por H2 em arquivo/MySQL) não duplica nada.
 */
@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarios;
    private final CategoriaRepository categorias;
    private final JogoRepository jogos;

    public DataInitializer(UsuarioRepository usuarios, CategoriaRepository categorias, JogoRepository jogos) {
        this.usuarios = usuarios;
        this.categorias = categorias;
        this.jogos = jogos;
    }

    @Override
    public void run(String... args) {
        seedAdmin();
        seedJogos();
        log.info("=== SEED CONCLUÍDO: {} categorias, {} jogos ===", categorias.count(), jogos.count());
    }

    private void seedAdmin() {
        if (usuarios.findByUsername("admin").isPresent()) return;
        Usuario admin = new Usuario();
        admin.setUsername("admin");
        admin.setPassword("admin123"); // só para a demo; em produção use hash (BCrypt)
        admin.setRole("ROLE_ADMIN");
        usuarios.save(admin);
        log.info("Admin criado -> login: admin / senha: admin123");
    }

    private void seedJogos() {
        jogo("Neon Drift",        "Corrida arcade em cidades futuristas.",   59.90, "PC",     "Corrida",    "neon-drift");
        jogo("Dungeon Bytes",     "RPG de masmorras com sorteio de fases.",  79.90, "PC",     "RPG",        "dungeon-bytes");
        jogo("Cozy Farm Stories", "Plante, colha e decore sua fazenda.",     39.90, "PC",     "Simulação",  "cozy-farm-stories");
        jogo("Sky Raiders",       "Combate aéreo cooperativo.",              99.90, "PS5",    "Ação",       "sky-raiders");
        jogo("Puzzle Orbit",      "Quebra-cabeças em gravidade zero.",       19.90, "PC",     "Puzzle",     "puzzle-orbit");
        jogo("Last Signal",       "Sobrevivência e mistério numa estação.",  89.90, "Xbox",   "Terror",     "last-signal");
        jogo("Kick Masters",      "Futebol de rua com habilidades.",         69.90, "PS5",    "Esporte",    "kick-masters");
        jogo("Blade Kingdom",     "Estratégia medieval em tempo real.",      74.90, "PC",     "Estratégia", "blade-kingdom");
        jogo("Pixel Quest",       "Plataforma retrô com chefes épicos.",     24.90, "Switch", "Plataforma", "pixel-quest");
        jogo("Deep Blue",         "Explore o fundo do mar em mundo aberto.", 94.90, "PC",     "Aventura",   "deep-blue");
    }

    /** Insere o jogo só se ainda não existir um com esse título. */
    private void jogo(String titulo, String descricao, double preco, String plataforma, String nomeCategoria, String slug) {
        if (jogos.existsByTitulo(titulo)) return;
        Categoria categoria = categorias.findByNome(nomeCategoria)
                .orElseGet(() -> categorias.save(new Categoria(null, nomeCategoria)));
        // imagem: arquivo estático do Angular em src/assets/jogos/<slug>.svg
        jogos.save(new Jogo(null, titulo, descricao, preco, "/assets/jogos/" + slug + ".svg", plataforma, categoria));
    }
}
